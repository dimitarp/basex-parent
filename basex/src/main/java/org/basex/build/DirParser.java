package org.basex.build;

import static org.basex.core.Text.*;
import static org.basex.data.DataText.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import org.basex.core.*;
import org.basex.core.cmd.*;
import org.basex.data.*;
import org.basex.io.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * This class recursively scans files and directories and parses all
 * relevant files.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class DirParser extends Parser {
  /** Number of skipped files to log. */
  private static final int SKIPLOG = 10;
  /** Skipped files. */
  private final StringList skipped = new StringList();
  /** File pattern. */
  private final Pattern filter;
  /** Initial file path. */
  private final String root;

  /** Parse archives in directories. */
  private final boolean archives;
  /** Skip corrupt files in directories. */
  private final boolean skipCorrupt;
  /** Add ignored files as raw files. */
  private final boolean addRaw;
  /** DTD parsing. */
  private final boolean dtd;
  /** Raw parsing. */
  private final boolean rawParser;
  /** Database path for storing binary files. */
  private final IOFile rawPath;

  /** Last source. */
  private IO lastSrc;
  /** Parser reference. */
  private Parser parser;
  /** Element counter. */
  private int c;

  /**
   * Constructor.
   * @param source source path
   * @param pr database properties
   * @param path future database path
   */
  public DirParser(final IO source, final Prop pr, final IOFile path) {
    super(source, pr);
    final String parent = source.dirPath();
    root = parent.endsWith("/") ? parent : parent + '/';
    skipCorrupt = prop.is(Prop.SKIPCORRUPT);
    archives = prop.is(Prop.ADDARCHIVES);
    addRaw = prop.is(Prop.ADDRAW);
    dtd = prop.is(Prop.DTD);
    rawParser = prop.get(Prop.PARSER).toLowerCase(Locale.ENGLISH).equals(DataText.M_RAW);

    filter = !source.isDir() && !source.isArchive() ? null :
      Pattern.compile(IOFile.regex(pr.get(Prop.CREATEFILTER)));
    // choose binary storage if (disk-based) database path is known and
    // if raw parser or "add raw" option were chosen
    rawPath = path != null && (addRaw || rawParser) ? new IOFile(path, M_RAW) : null;
  }

  @Override
  public void parse(final Builder build) throws IOException {
    build.meta.filesize = 0;
    build.meta.original = src.path();
    parse(build, src);
  }

  /**
   * Parses the specified file or its children.
   * @param b builder
   * @param io current input
   * @throws IOException I/O exception
   */
  private void parse(final Builder b, final IO io) throws IOException {
    if(io instanceof IOFile && io.isDir()) {
      for(final IO f : ((IOFile) io).children()) parse(b, f);
    } else if(archives && io.name().toLowerCase(Locale.ENGLISH).endsWith(IO.GZSUFFIX)) {
      // process GZIP archive
      final GZIPInputStream is = new GZIPInputStream(io.inputStream());
      src = new IOStream(is, io.name().replaceAll("\\..*", IO.XMLSUFFIX));
      parseResource(b);
      is.close();
    } else if(archives && io.isArchive()) {
      // process ZIP archive
      final ZipInputStream is = new ZipInputStream(io.inputStream());
      for(ZipEntry ze; (ze = is.getNextEntry()) != null;) {
        if(ze.isDirectory()) continue;
        src = new IOStream(is, ze.getName());
        src.length(ze.getSize());
        parseResource(b);
      }
      is.close();
    } else {
      // process regular file
      src = io;
      parseResource(b);
    }
  }

  /**
   * Parses the current source.
   * @param b builder instance
   * @throws IOException I/O exception
   */
  private void parseResource(final Builder b) throws IOException {
    b.checkStop();

    // add file size for database meta information
    final long l = src.length();
    if(l != -1) b.meta.filesize += l;

    // use global target as path prefix
    String targ = target;
    String path = src.path();

    // add relative path without root (prefix) and file name (suffix)
    final String name = src.name();
    if(path.endsWith('/' + name)) {
      path = path.substring(0, path.length() - name.length());
      if(path.startsWith(root)) path = path.substring(root.length());
      targ = (targ + path).replace("//", "/");
    }

    // check if file passes the name filter pattern
    boolean exclude = false;
    if(filter != null) {
      final String nm = Prop.CASE ? src.name() : src.name().toLowerCase(Locale.ENGLISH);
      exclude = !filter.matcher(nm).matches();
    }

    if(exclude) {
      // exclude file: check if will be added as raw file
      if(addRaw && rawPath != null) {
        Store.store(src.inputSource(), new IOFile(rawPath, targ + name));
      }
    } else {
      if(rawParser) {
        // store input in raw format if database path is known
        if(rawPath != null) {
          Store.store(src.inputSource(), new IOFile(rawPath, targ + name));
        }
      } else {
        // store input as XML
        boolean ok = true;
        IO in = src;
        if(skipCorrupt) {
          // parse file twice to ensure that it is well-formed
          try {
            // cache file contents to allow or speed up a second run
            if(!(src instanceof IOContent || dtd)) {
              in = new IOContent(src.read());
              in.name(src.name());
            }
            parser = Parser.singleParser(in, prop, targ);
            MemBuilder.build("", parser);
          } catch(final IOException ex) {
            Util.debug(ex);
            skipped.add(src.path());
            ok = false;
          }
        }

        // parse file
        if(ok) {
          parser = Parser.singleParser(in, prop, targ);
          parser.parse(b);
        }
        parser = null;
        // dump debug data
        if(Prop.debug && (++c & 0x3FF) == 0) Util.err(";");
      }
    }
  }

  @Override
  public String info() {
    final TokenBuilder tb = new TokenBuilder();
    if(!skipped.isEmpty()) {
      tb.add(SKIPPED).add(COL).add(NL);
      final int s = skipped.size();
      for(int i = 0; i < s && i < SKIPLOG; i++) {
        tb.add(LI).add(skipped.get(i)).add(NL);
      }
      if(s > SKIPLOG) {
        tb.add(LI).addExt(MORE_SKIPPED_X, s - SKIPLOG).add(NL);
      }
    }
    return tb.toString();
  }

  @Override
  public String det() {
    return parser != null ? parser.detail() : src.path();
  }

  @Override
  public double prog() {
    if(parser != null) return parser.progress();
    if(lastSrc == src) return 1;
    lastSrc = src;
    return Math.random();
  }

  @Override
  public void close() throws IOException {
    if(parser != null) parser.close();
  }
}
