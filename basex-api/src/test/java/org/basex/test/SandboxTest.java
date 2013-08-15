package org.basex.test;

import static org.junit.Assert.*;

import java.io.*;

import org.basex.core.*;
import org.basex.io.*;
import org.basex.io.out.*;
import org.basex.util.*;
import org.junit.*;

/**
 * If this class is extended, tests will be run in a sandbox.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public abstract class SandboxTest {
  /** Default output stream. */
  public static final PrintStream OUT = System.out;
  /** Default error stream. */
  public static final PrintStream ERR = System.err;
  /** Null output stream. */
  public static final PrintStream NULL = new PrintStream(new NullOutput());
  /** Test name. */
  public static final String NAME = Util.name(SandboxTest.class);
  /** Database context. */
  protected static Context context;
  /** Clean up files. */
  protected static boolean cleanup;

  /**
   * Creates the sandbox.
   */
  @BeforeClass
  public static void createContext() {
    final IOFile sb = sandbox();
    sb.delete();
    assertTrue("Sandbox could not be created.", sb.md());
    context = new Context();
    initContext(context);
    cleanup = true;
  }

  /**
   * Initializes the specified context.
   * @param ctx context
   */
  protected static void initContext(final Context ctx) {
    final IOFile sb = sandbox();
    ctx.mprop.set(MainProp.DBPATH, sb.path() + "/data");
    ctx.mprop.set(MainProp.WEBPATH, sb.path() + "/webapp");
    ctx.mprop.set(MainProp.RESTXQPATH, sb.path() + "/webapp");
    ctx.mprop.set(MainProp.REPOPATH, sb.path() + "/repo");
  }

  /**
   * Removes test databases and closes the database context.
   */
  @AfterClass
  public static void closeContext() {
    if(cleanup) {
      context.close();
      assertTrue("Sandbox could not be deleted.", sandbox().delete());
    }
  }

  /**
   * Returns the sandbox database path.
   * @return database path
   */
  protected static IOFile sandbox() {
    return new IOFile(Prop.TMP, NAME);
  }
}
