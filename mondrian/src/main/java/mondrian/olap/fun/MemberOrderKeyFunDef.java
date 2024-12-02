/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.fun.sort.OrderKey;

/**
 * Definition of the <code>&lt;Member&gt;.OrderKey</code> MDX builtin function.
 *
 * <p>Syntax:
 * <blockquote><code>&lt;Member&gt;.OrderKey</code></blockquote>
 *
 * @author kvu
 * @since Nov 10, 2008
 */
public final class MemberOrderKeyFunDef extends AbstractFunctionDefinition {
  static final MemberOrderKeyFunDef instance =
    new MemberOrderKeyFunDef();

  /**
   * Creates the singleton MemberOrderKeyFunDef.
   */
  private MemberOrderKeyFunDef() {
    super(
      "OrderKey", "Returns the member order key.", "pvm" );
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final MemberCalc memberCalc =
      compiler.compileMember( call.getArg( 0 ) );
    return new CalcImpl( call.getType(), memberCalc );
  }

  public static class CalcImpl extends AbstractProfilingNestedCalc {
    private final MemberCalc memberCalc;

    /**
     * Creates a Calc
     *
     * @param exp        Source expression
     * @param memberCalc Compiled expression to calculate member
     */
    public CalcImpl(Type type, MemberCalc memberCalc ) {
      super( type, new Calc[] { memberCalc } );
      this.memberCalc = memberCalc;
    }

    @Override
	public OrderKey evaluate( Evaluator evaluator ) {
      return new OrderKey( memberCalc.evaluate( evaluator ) );
    }


  }
}
