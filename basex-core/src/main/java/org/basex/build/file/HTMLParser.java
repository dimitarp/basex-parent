package org.basex.build.file;

import static org.basex.util.Token.*;

import java.io.*;
import java.lang.reflect.*;

import org.basex.build.xml.*;
import org.basex.core.*;
import org.basex.io.*;
import org.basex.io.in.*;
import org.basex.util.*;
import org.xml.sax.*;

/**
 * This class uses TagSoup to convert HTML input to well-formed XML.
 * If TagSoup is not found in the classpath, the original document is passed on.
 *
 * TagSoup was written by John Cowan and is based on the Apache 2.0 License:
 * {@code http://home.ccil.org/~cowan/XML/tagsoup/}.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class HTMLParser extends XMLParser {
  /** Name of HTML Parser. */
  public static final String NAME = "TagSoup";
  /** TagSoup URL. */
  private static final String FEATURES = "http://www.ccil.org/~cowan/tagsoup/features/";

  /** XML parser class string. */
  private static final String PCLASS = "org.ccil.cowan.tagsoup.Parser";
  /** XML writer class string. */
  private static final String WCLASS = "org.ccil.cowan.tagsoup.XMLWriter";
  /** HTML reader. */
  private static final Class<?> READER = Reflect.find(PCLASS);
  /** HTML writer. */
  private static final Constructor<?> WRITER =
      Reflect.find(Reflect.find(WCLASS), Writer.class);
  /** XML writer output property method. */
  private static final Method METHOD = Reflect.method(
      Reflect.find(WCLASS), "setOutputProperty",
      new Class[] { String.class, String.class });

  /**
   * Checks if a CatalogResolver is available.
   * @return result of check
   */
  public static boolean available() {
    return READER != null;
  }

  /**
   * Returns the name of the parser, or an empty string.
   * @return name of parser
   */
  public static String parser() {
    return available() ? NAME : "";
  }

  /**
   * Constructor.
   * @param source document source
   * @param pr database properties
   * @throws IOException I/O exception
   */
  public HTMLParser(final IO source, final Prop pr) throws IOException {
    this(source, pr.get(Prop.HTMLOPT), pr);
  }

  /**
   * Constructor.
   * @param source document source
   * @param options options
   * @param pr database properties
   * @throws IOException I/O exception
   */
  public HTMLParser(final IO source, final String options, final Prop pr)
      throws IOException {
    super(toXML(source, options), pr);
  }

  /**
   * Converts an HTML document to XML.
   * @param io io reference
   * @param options parsing options
   * @return parser
   * @throws IOException I/O exception
   */
  private static IO toXML(final IO io, final String options) throws IOException {
    // reader could not be initialized; fall back to XML
    if(READER == null) return io;

    try {
      // tries to extract the encoding from the input
      final TextInput ti = new TextInput(io);
      String enc = ti.encoding();
      final byte[] content = ti.content();

      // looks for a charset definition
      final byte[] encoding = token("charset=");
      int cs = indexOf(content, encoding);
      if(cs > 0) {
        // extracts the encoding string
        cs += encoding.length;
        int ce = cs;
        while(++ce < content.length && content[ce] > 0x28);
        enc = string(substring(content, cs, ce));
      }

      // define input
      final InputSource is = new InputSource(new ArrayInput(content));
      is.setEncoding(supported(enc) ? normEncoding(enc) : UTF8);
      // define output
      final StringWriter sw = new StringWriter();
      final XMLReader reader = (XMLReader) Reflect.get(READER);
      final Object writer = Reflect.get(WRITER, sw);

      // set TagSoup options
      final HTMLProp props = new HTMLProp(options);
      String p;
      if(props.is(HTMLProp.HTML)) {
        reader.setFeature("http://xml.org/sax/features/namespaces", false);
        opt("method", "html");
        opt("omit-xml-declaration", "yes");
      }
      if(props.is(HTMLProp.NONS)) {
        reader.setFeature("http://xml.org/sax/features/namespaces", false);
      }
      if(props.is(HTMLProp.OMITXML)) {
        opt("omit-xml-declaration", "yes");
      }
      if(!(p = props.get(HTMLProp.METHOD)).isEmpty()) {
        opt("method", p);
      }
      if(props.is(HTMLProp.NOBOGONS)) {
        reader.setFeature(FEATURES + "ignore-bogons", true);
      }
      if(props.is(HTMLProp.NODEFAULTS)) {
        reader.setFeature(FEATURES + "default-attributes", false);
      }
      if(props.is(HTMLProp.NOCOLONS)) {
        reader.setFeature(FEATURES + "translate-colons", true);
      }
      if(props.is(HTMLProp.NORESTART)) {
        reader.setFeature(FEATURES + "restart-elements", false);
      }
      if(props.is(HTMLProp.IGNORABLE)) {
        reader.setFeature(FEATURES + "ignorable-whitespace", true);
      }
      if(props.is(HTMLProp.EMPTYBOGONS)) {
        reader.setFeature(FEATURES + "bogons-empty", true);
      }
      if(props.is(HTMLProp.ANY)) {
        reader.setFeature(FEATURES + "bogons-empty", false);
      }
      if(props.is(HTMLProp.NOROOTBOGONS)) {
        reader.setFeature(FEATURES + "root-bogons", false);
      }
      if(props.is(HTMLProp.NOCDATA)) {
        reader.setFeature(FEATURES + "cdata-elements", false);
      }
      if(props.is(HTMLProp.LEXICAL)) {
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", writer);
      }
      if(!(p = props.get(HTMLProp.DOCTYPESYS)).isEmpty()) {
        opt("doctype-system", p);
      }
      if(!(p = props.get(HTMLProp.DOCTYPEPUB)).isEmpty()) {
        opt("doctype-public", p);
      }
      if(!(p = props.get(HTMLProp.ENCODING)).isEmpty()) {
        is.setEncoding(p);
      }
      // end TagSoup options

      reader.setContentHandler((ContentHandler) writer);
      reader.parse(is);
      return new IOContent(token(sw.toString()), io.name());

    } catch(final SAXException ex) {
      Util.errln(ex);
      return io;
    }
  }

  /**
   * Reflection invoke XMLWriter.setOutputProperty().
   * @param p property
   * @param v value
   */
  private static void opt(final String p, final String v) {
    Reflect.invoke(METHOD, WRITER, p, v);
  }
}
