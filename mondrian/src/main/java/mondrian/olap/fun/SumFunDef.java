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
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

import mondrian.calc.impl.ValueCalc;
import mondrian.olap.ResultStyleException;

/**
 * Definition of the <code>Sum</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class SumFunDef extends AbstractAggregateFunDef {
  static final ReflectiveMultiResolver Resolver =
      new ReflectiveMultiResolver( "Sum", "Sum(<Set>[, <Numeric Expression>])",
          "Returns the sum of a numeric expression evaluated over a set.", new String[] { "fnx", "fnxn" },
          SumFunDef.class );
  private static final String TIMING_NAME = SumFunDef.class.getSimpleName();

  public SumFunDef( FunctionMetaData functionMetaData ) {
    super( functionMetaData );
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    // What is the desired type to use to get the underlying values
    for ( ResultStyle r : compiler.getAcceptableResultStyles() ) {
      Calc calc;
      switch ( r ) {
        case ITERABLE:
        case ANY:
          // Consumer wants ITERABLE or ANY to be used
          // return compileCallIterable(call, compiler);
          calc = compileCall( call, compiler, ResultStyle.ITERABLE );
          if ( calc != null ) {
            return calc;
          }
          break;
        case MUTABLE_LIST:
          // Consumer wants MUTABLE_LIST
          calc = compileCall( call, compiler, ResultStyle.MUTABLE_LIST );
          if ( calc != null ) {
            return calc;
          }
          break;
        case LIST:
          // Consumer wants LIST to be used
          // return compileCallList(call, compiler);
          calc = compileCall( call, compiler, ResultStyle.LIST );
          if ( calc != null ) {
            return calc;
          }
          break;
      }
    }
    throw ResultStyleException.generate( ResultStyle.ITERABLE_LIST_MUTABLELIST_ANY, compiler
        .getAcceptableResultStyles() );
  }

  protected Calc compileCall( final ResolvedFunCall call, ExpressionCompiler compiler, ResultStyle resultStyle ) {
    final Calc ncalc = compiler.compileIter( call.getArg( 0 ) );
    if ( ncalc == null ) {
      return null;
    }
    final Calc calc =
        call.getArgCount() > 1 ? compiler.compileScalar( call.getArg( 1 ), true ) : new ValueCalc( call.getType() );
    // we may have asked for one sort of Calc, but here's what we got.
    if ( ncalc instanceof TupleListCalc ) {
      return genListCalc( call, (TupleListCalc) ncalc, calc );
    } else {
      return genIterCalc( call, (TupleIteratorCalc) ncalc, calc );
    }
  }

  protected Calc genIterCalc( final ResolvedFunCall call, final TupleIteratorCalc tupleIteratorCalc, final Calc calc ) {
    return new AbstractProfilingNestedDoubleCalc( call.getType(), new Calc[] { tupleIteratorCalc, calc } ) {
      @Override
	public Double evaluate( Evaluator evaluator ) {
        evaluator.getTiming().markStart( SumFunDef.TIMING_NAME );
        final int savepoint = evaluator.savepoint();
        try {
          TupleIterable iterable = evaluateCurrentIterable( tupleIteratorCalc, evaluator );
          return FunUtil.sumDouble( evaluator, iterable, calc );
        } finally {
          evaluator.restore( savepoint );
          evaluator.getTiming().markEnd( SumFunDef.TIMING_NAME );
        }
      }

      @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
        return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
      }
    };
  }

  protected Calc genListCalc( final ResolvedFunCall call, final TupleListCalc tupleListCalc, final Calc calc ) {
    return new AbstractProfilingNestedDoubleCalc( call.getType(), new Calc[] { tupleListCalc, calc } ) {
      @Override
	public Double evaluate( Evaluator evaluator ) {
        evaluator.getTiming().markStart( SumFunDef.TIMING_NAME );
        final int savepoint = evaluator.savepoint();
        try {
          TupleList memberList = AbstractAggregateFunDef.evaluateCurrentList( tupleListCalc, evaluator );
          evaluator.setNonEmpty( false );
          return FunUtil.sumDouble( evaluator, memberList, calc );
        } finally {
          evaluator.restore( savepoint );
          evaluator.getTiming().markEnd( SumFunDef.TIMING_NAME );
        }
      }

      @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
        return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
      }
    };
  }
}
