package org.basex.query.func;

import static org.basex.query.util.Err.*;

import java.util.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.iter.*;
import org.basex.query.util.unit.*;
import org.basex.query.value.item.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Unit functions.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class FNUnit extends StandardFunc {
  /**
   * Constructor.
   * @param ii input info
   * @param f function definition
   * @param e arguments
   */
  public FNUnit(final InputInfo ii, final Function f, final Expr... e) {
    super(ii, f, e);
  }

  @Override
  public Item item(final QueryContext ctx, final InputInfo ii) throws QueryException {
    switch(sig) {
      case _UNIT_ASSERT:    return assrt(ctx);
      case _UNIT_FAIL:      return fail(ctx);
      case _UNIT_TEST:      return test(ctx);
      case _UNIT_TEST_URIS: return testUris(ctx);
      default:              return super.item(ctx, ii);
    }
  }

  /**
   * Performs the assert function.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  private Item assrt(final QueryContext ctx) throws QueryException {
    final byte[] str = expr.length < 2 ? null : checkStr(expr[1], ctx);
    if(expr[0].ebv(ctx, info).bool(info)) return null;
    throw str == null ? UNIT_ASSERT.thrw(info) : UNIT_MESSAGE.thrw(info, str);
  }

  /**
   * Performs the fail function.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  private Item fail(final QueryContext ctx) throws QueryException {
    throw UNIT_MESSAGE.thrw(info, checkStr(expr[0], ctx));
  }

  /**
   * Performs the test function.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  private Item test(final QueryContext ctx) throws QueryException {
    final Unit unit = new Unit(ctx, info);
    if(expr.length == 0) return unit.test();

    final ArrayList<StaticFunc> funcs = new ArrayList<StaticFunc>();
    final Iter ir = ctx.iter(expr[0]);
    for(Item it; (it = ir.next()) != null;) {
      final FItem fi = checkFunc(it, ctx);
      if(fi instanceof FuncItem) funcs.add(((FuncItem) fi).func);
    }
    return unit.test(funcs);
  }

  /**
   * Performs the test-uris function.
   * @param ctx query context
   * @return resulting value
   * @throws QueryException query exception
   */
  private Item testUris(final QueryContext ctx) throws QueryException {
    checkCreate(ctx);
    final TokenList tl = new TokenList();
    final Iter ir = ctx.iter(expr[0]);
    for(Item it; (it = ir.next()) != null;) tl.add(checkStr(it));
    return new Suite(ctx, info).test(tl);
  }
}
