/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2020 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.olap.fun;

import java.util.List;
import java.util.Map;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.resolver.ParametersCheckingFunctionDefinitionResolver;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.olap.Util;
import mondrian.olap.fun.sort.Sorter;

/**
 * Definition of the <code>TopPercent</code>, <code>BottomPercent</code>,
 * <code>TopSum</code> and <code>BottomSum</code> MDX builtin functions.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class TopBottomPercentSumFunDef extends AbstractFunctionDefinition {

  
  static final OperationAtom atomTopPercent = new FunctionOperationAtom("TopPercent");
  static final FunctionMetaData fmdTopPercent = new FunctionMetaDataR(atomTopPercent,
		  "Sorts a set and returns the top N elements whose cumulative total is at least a specified percentage.",
		  "TopPercent(<Set>, <Percentage>, <Numeric Expression>)", DataType.SET,
		  new DataType[] { DataType.SET, DataType.NUMERIC, DataType.NUMERIC });
  static final FunctionResolver TopPercentResolver = new ParametersCheckingFunctionDefinitionResolver(
		  new TopBottomPercentSumFunDef(fmdTopPercent, true, true));
  
  static final OperationAtom atomBottomPercent = new FunctionOperationAtom("BottomPercent");
  static final FunctionMetaData fmdBottomPercent = new FunctionMetaDataR(atomBottomPercent,
		  "Sorts a set and returns the bottom N elements whose cumulative total is at least a specified percentage.",
		  "BottomPercent(<Set>, <Percentage>, <Numeric Expression>)", DataType.SET,
		  new DataType[] { DataType.SET, DataType.NUMERIC, DataType.NUMERIC });
  static final FunctionResolver BottomPercentResolver = new ParametersCheckingFunctionDefinitionResolver(
		  new TopBottomPercentSumFunDef(fmdBottomPercent, false, true));
  
  static final OperationAtom atomTopSum = new FunctionOperationAtom("TopSum");
  static final FunctionMetaData fmdTopSum = new FunctionMetaDataR(atomTopSum,
		  "Sorts a set and returns the top N elements whose cumulative total is at least a specified value.",
		  "TopSum(<Set>, <Value>, <Numeric Expression>)", DataType.SET,
		  new DataType[] { DataType.SET, DataType.NUMERIC, DataType.NUMERIC });
  static final FunctionResolver TopSumResolver = new ParametersCheckingFunctionDefinitionResolver(
		  new TopBottomPercentSumFunDef(fmdTopSum, true, false));
  
  static final OperationAtom atomBottomSum = new FunctionOperationAtom("BottomSum");
  static final FunctionMetaData fmdBottomSum = new FunctionMetaDataR(atomBottomSum,
		  "Sorts a set and returns the bottom N elements whose cumulative total is at least a specified value.",
		  "BottomSum(<Set>, <Value>, <Numeric Expression>)", DataType.SET,
		  new DataType[] { DataType.SET, DataType.NUMERIC, DataType.NUMERIC });
  static final FunctionResolver BottomSumResolver = new ParametersCheckingFunctionDefinitionResolver(
		  new TopBottomPercentSumFunDef(fmdBottomSum, false, false));

  
  
  /**
   * Whether to calculate top (as opposed to bottom).
   */
  final boolean top;
  /**
   * Whether to calculate percent (as opposed to sum).
   */
  final boolean percent;
  
  public TopBottomPercentSumFunDef(
    FunctionMetaData functionMetaData , boolean top, boolean percent ) {
    super( functionMetaData );
    this.top = top;
    this.percent = percent;
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final TupleListCalc tupleListCalc =
      compiler.compileList( call.getArg( 0 ), true );
    final DoubleCalc doubleCalc = compiler.compileDouble( call.getArg( 1 ) );
    final Calc calc = compiler.compileScalar( call.getArg( 2 ), true );
    return new CalcImpl( call, tupleListCalc, doubleCalc, calc );
  }

  private static class ResolverImpl extends MultiResolver {
    private final boolean top;
    private final boolean percent;

    public ResolverImpl(
      final String name, final String signature,
      final String description, final String[] signatures,
      boolean top, boolean percent ) {
      super( name, signature, description, signatures );
      this.top = top;
      this.percent = percent;
    }

    @Override
	protected FunctionDefinition createFunDef( Expression[] args, FunctionMetaData functionMetaData  ) {
      return new TopBottomPercentSumFunDef( functionMetaData, top, percent );
    }
  }

  private class CalcImpl extends AbstractListCalc {
    private final TupleListCalc tupleListCalc;
    private final DoubleCalc doubleCalc;
    private final Calc calc;

    public CalcImpl(
      ResolvedFunCall call,
      TupleListCalc tupleListCalc,
      DoubleCalc doubleCalc,
      Calc calc ) {
      super( call.getType(), new Calc[] { tupleListCalc, doubleCalc, calc } );
      this.tupleListCalc = tupleListCalc;
      this.doubleCalc = doubleCalc;
      this.calc = calc;
    }

    @Override
	public TupleList evaluateList( Evaluator evaluator ) {
      TupleList list = tupleListCalc.evaluateList( evaluator );
      Double target = doubleCalc.evaluate( evaluator );
      if ( list.isEmpty() ) {
        return list;
      }
      Map<List<Member>, Object> mapMemberToValue =
        Sorter.evaluateTuples( evaluator, calc, list );
      final int savepoint = evaluator.savepoint();
      try {
        evaluator.setNonEmpty( false );
        list = Sorter.sortTuples(
          evaluator,
          list,
          list,
          calc,
          top,
          true,
          getType().getArity() );
      } finally {
        evaluator.restore( savepoint );
      }
      if ( percent ) {
        FunUtil.toPercent( list, mapMemberToValue );
      }
      double runningTotal = 0;
      int memberCount = list.size();
      int nullCount = 0;
      for ( int i = 0; i < memberCount; i++ ) {
        if ( runningTotal >= target ) {
          list = list.subList( 0, i );
          break;
        }
        final List<Member> key = list.get( i );
        final Object o = mapMemberToValue.get( key );
        if ( o == Util.nullValue ) {
          nullCount++;
        } else if ( o instanceof Number ) {
          runningTotal += ( (Number) o ).doubleValue();
        } else if ( o instanceof Exception ) {
          // ignore the error
        } else {
          throw Util.newInternal(
            new StringBuilder("got ").append(o).append(" when expecting Number").toString() );
        }
      }

      // MSAS exhibits the following behavior. If the value of all members
      // is null, then the first (or last) member of the set is returned
      // for percent operations.
      if ( memberCount > 0 && percent && nullCount == memberCount ) {
        return top
          ? list.subList( 0, 1 )
          : list.subList( memberCount - 1, memberCount );
      }
      return list;
    }

    @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
      return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
    }
  }
}
