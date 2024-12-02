/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2021 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

/**
 * Definition of the <code>Count</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class CountFunDef extends AbstractAggregateFunDef {
  static final List<String> ReservedWords =  List.of( "INCLUDEEMPTY", "EXCLUDEEMPTY" );

  static final ReflectiveMultiResolver Resolver =
      new ReflectiveMultiResolver( "Count", "Count(<Set>[, EXCLUDEEMPTY | INCLUDEEMPTY])",
          "Returns the number of tuples in a set, empty cells included unless the optional EXCLUDEEMPTY flag is used.",
          new String[] { "fnx", "fnxy" }, CountFunDef.class, CountFunDef.ReservedWords );
  private static final String TIMING_NAME = CountFunDef.class.getSimpleName();

  public CountFunDef( FunctionMetaData functionMetaData ) {
    super( functionMetaData );
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final Calc calc = compiler.compileAs( call.getArg( 0 ), null, ResultStyle.ITERABLE_ANY );
    final boolean includeEmpty =
        call.getArgCount() < 2 || ( (Literal) call.getArg( 1 ) ).getValue().equals( "INCLUDEEMPTY" );
    return new AbstractProfilingNestedIntegerCalc( call.getType(), new Calc[] { calc } ) {
      @Override
	public Integer evaluate( Evaluator evaluator ) {
        evaluator.getTiming().markStart( CountFunDef.TIMING_NAME );
        final int savepoint = evaluator.savepoint();
        try {
          evaluator.setNonEmpty( false );
          final int count;
          if ( calc instanceof TupleIteratorCalc tupleIteratorCalc ) {
            TupleIterable iterable = evaluateCurrentIterable( tupleIteratorCalc, evaluator );
            count = FunUtil.count( evaluator, iterable, includeEmpty );
          } else {
            // must be TupleListCalc
            TupleListCalc tupleListCalc = (TupleListCalc) calc;
            TupleList list = AbstractAggregateFunDef.evaluateCurrentList( tupleListCalc, evaluator );
            count = FunUtil.count( evaluator, list, includeEmpty );
          }
          return count;
        } finally {
          evaluator.restore( savepoint );
          evaluator.getTiming().markEnd( CountFunDef.TIMING_NAME );
        }
      }

      @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
        // COUNT(<set>, INCLUDEEMPTY) is straightforward -- it
        // depends only on the dimensions that <Set> depends
        // on.
        if ( super.dependsOn( hierarchy ) ) {
          return true;
        }
        if ( includeEmpty ) {
          return false;
        }
        // COUNT(<set>, EXCLUDEEMPTY) depends only on the
        // dimensions that <Set> depends on, plus all
        // dimensions not masked by the set.
        return !calc.getType().usesHierarchy( hierarchy, true );
      }
    };
  }
}
