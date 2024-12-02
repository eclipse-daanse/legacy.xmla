/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2021 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

import mondrian.calc.impl.ValueCalc;

/**
 * Definition of the <code>Avg</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class AvgFunDef extends AbstractAggregateFunDef {
  static final ReflectiveMultiResolver Resolver =
      new ReflectiveMultiResolver( "Avg", "Avg(<Set>[, <Numeric Expression>])",
          "Returns the average value of a numeric expression evaluated over a set.", new String[] { "fnx", "fnxn" },
          AvgFunDef.class );

  private static final String TIMING_NAME = AvgFunDef.class.getSimpleName();

  public AvgFunDef( FunctionMetaData functionMetaData ) {
    super( functionMetaData );
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final TupleListCalc tupleListCalc = compiler.compileList( call.getArg( 0 ) );
    final Calc calc =
        call.getArgCount() > 1 ? compiler.compileScalar( call.getArg( 1 ), true ) : new ValueCalc( call.getType() );
    return new AbstractProfilingNestedDoubleCalc( call.getType(), new Calc[] { tupleListCalc, calc } ) {
      @Override
	public Double evaluate( Evaluator evaluator ) {
        evaluator.getTiming().markStart( AvgFunDef.TIMING_NAME );
        final int savepoint = evaluator.savepoint();
        try {
          TupleList memberList = AbstractAggregateFunDef.evaluateCurrentList( tupleListCalc, evaluator );
          evaluator.setNonEmpty( false );
          return (Double) FunUtil.avg( evaluator, memberList, calc );
        } finally {
          evaluator.restore( savepoint );
          evaluator.getTiming().markEnd( AvgFunDef.TIMING_NAME );
        }
      }

      @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
        return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
      }
    };
  }
}
