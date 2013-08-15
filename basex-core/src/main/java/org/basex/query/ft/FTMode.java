package org.basex.query.ft;

import java.util.*;

import org.basex.util.*;

/**
 * Search mode.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public enum FTMode {
  /** All option. */
  ALL,
  /** All words option. */
  ALL_WORDS,
  /** Any option. */
  ANY,
  /** Any words option. */
  ANY_WORD,
  /** Phrase search. */
  PHRASE;

  /**
   * Returns the specified mode, or {@code null}.
   * @param mode mode
   * @return mode enumeration
   */
  public static FTMode get(final byte[] mode) {
    final String md = Token.string(mode).toLowerCase(Locale.ENGLISH);
    for(final FTMode m : values()) {
      final String s = m.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH);
      if(s.equals(md)) return m;
    }
    return null;
  }
}
