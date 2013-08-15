package org.basex.gui.editor;

import static org.basex.util.Token.*;

import java.awt.*;

import org.basex.gui.*;

/**
 * This abstract class defines a framework for a simple syntax
 * highlighting in text panels.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
abstract class Syntax {
  /** Standard color. */
  protected static final Color TEXT = Color.BLACK;
  /** Comment color. */
  protected static final Color COMMENT = new Color(0, 160, 160);
  /** String color. */
  protected static final Color STRING = GUIConstants.RED;
  /** Keyword color. */
  protected static final Color KEYWORD = GUIConstants.BLUE;
  /** Function color. */
  protected static final Color FUNCTION = new Color(160, 0, 160);
  /** Variable color. */
  protected static final Color VARIABLE = GUIConstants.GREEN;

  /** Simple syntax. */
  static final Syntax SIMPLE = new Syntax() {
    @Override
    public void init() { }
    @Override
    public Color getColor(final EditorText tokens) { return TEXT; }
  };

  /**
   * Initializes the highlighter.
   */
  public abstract void init();

  /**
   * Returns the color for the current token.
   * @param tokens tokenizer
   * @return color
   */
  public abstract Color getColor(final EditorText tokens);

  /**
   * Returns the start of a comment.
   * @return comment start
   */
  public byte[] commentOpen() {
    return EMPTY;
  }

  /**
   * Returns the end of a comment.
   * @return comment end
   */
  public byte[] commentEnd() {
    return EMPTY;
  }
}
