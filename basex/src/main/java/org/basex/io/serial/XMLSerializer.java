package org.basex.io.serial;

import static org.basex.data.DataText.*;
import static org.basex.query.util.Err.*;

import java.io.*;

import org.basex.query.value.item.*;

/**
 * This class serializes data as XML.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public class XMLSerializer extends OutputSerializer {
  /** Root elements. */
  private boolean root;

  /**
   * Constructor, specifying serialization options.
   * @param os output stream reference
   * @param p serialization properties
   * @throws IOException I/O exception
   */
  XMLSerializer(final OutputStream os, final SerializerProp p) throws IOException {
    super(os, p, V10, V11);
  }

  @Override
  protected void startOpen(final byte[] t) throws IOException {
    if(tags.isEmpty()) {
      if(root) check();
      root = true;
    }
    super.startOpen(t);
  }

  @Override
  protected void finishText(final byte[] v) throws IOException {
    if(tags.isEmpty()) check();
    super.finishText(v);
  }

  @Override
  protected void atomic(final Item i) throws IOException {
    if(tags.isEmpty()) check();
    super.atomic(i);
  }

  /**
   * Checks if document serialization is valid.
   * @throws SerializerException serializer exception
   */
  private void check() throws SerializerException {
    if(!saomit) SERSA.thrwSerial();
    if(docsys != null) SERDT.thrwSerial();
  }
}
