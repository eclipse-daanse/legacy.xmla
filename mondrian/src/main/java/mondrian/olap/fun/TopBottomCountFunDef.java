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

import java.util.AbstractList;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.NativeEvaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.DelegatingTupleList;
import mondrian.calc.impl.TupleCollections;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.fun.sort.Sorter;

/**
 * Definition of the <code>TopCount</code> and <code>BottomCount</code> MDX builtin functions.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class TopBottomCountFunDef extends AbstractFunctionDefinition {
  boolean top;

  static final MultiResolver TopCountResolver =
    new MultiResolver(
      "TopCount",
      "TopCount(<Set>, <Count>[, <Numeric Expression>])",
      "Returns a specified number of items from the top of a set, optionally ordering the set first.",
      new String[] { "fxxnn", "fxxn" } ) {
      @Override
	protected FunctionDefinition createFunDef( Expression[] args, FunctionMetaData functionMetaData  ) {
        return new TopBottomCountFunDef( functionMetaData, true );
      }
    };

  static final MultiResolver BottomCountResolver =
    new MultiResolver(
      "BottomCount",
      "BottomCount(<Set>, <Count>[, <Numeric Expression>])",
      "Returns a specified number of items from the bottom of a set, optionally ordering the set first.",
      new String[] { "fxxnn", "fxxn" } ) {
      @Override
	protected FunctionDefinition createFunDef( Expression[] args, FunctionMetaData functionMetaData  ) {
        return new TopBottomCountFunDef( functionMetaData, false );
      }
    };

  public TopBottomCountFunDef( FunctionMetaData functionMetaData , final boolean top ) {
    super( functionMetaData );
    this.top = top;

  }

  @Override
public Calc compileCall( final ResolvedFunCall call, ExpressionCompiler compiler ) {
    // Compile the member list expression. Ask for a mutable list, because
    // we're going to sort it later.
    final TupleListCalc tupleListCalc =
      compiler.compileList( call.getArg( 0 ), true );
    final IntegerCalc integerCalc =
      compiler.compileInteger( call.getArg( 1 ) );
    final Calc orderCalc =
      call.getArgCount() > 2
        ? compiler.compileScalar( call.getArg( 2 ), true )
        : null;
    final int arity = call.getType().getArity();
    return new AbstractListCalc(
    		call.getType(),
      new Calc[] { tupleListCalc, integerCalc, orderCalc } ) {
      @Override
	public TupleList evaluateList( Evaluator evaluator ) {
        // Use a native evaluator, if more efficient.
        // TODO: Figure this out at compile time.
        SchemaReader schemaReader = evaluator.getSchemaReader();
        NativeEvaluator nativeEvaluator =
          schemaReader.getNativeSetEvaluator(
            call.getFunDef(), call.getArgs(), evaluator, this );
        if ( nativeEvaluator != null ) {
          return
            (TupleList) nativeEvaluator.execute( ResultStyle.LIST );
        }

        Integer n = integerCalc.evaluate( evaluator );
        if ( n == 0 || n ==null) {
          return TupleCollections.emptyList( arity );
        }

        TupleList list = tupleListCalc.evaluateList( evaluator );
        assert list.getArity() == arity;
        if ( list.isEmpty() ) {
          return list;
        }

        if ( orderCalc == null ) {
          // REVIEW: Why require "instanceof AbstractList"?
          if ( list instanceof AbstractList && list.size() <= n ) {
            return list;
          } else if ( top ) {
            return list.subList( 0, n );
          } else {
            return list.subList( list.size() - n, list.size() );
          }
        }

        return partiallySortList(
          evaluator, list, 
          Math.min( n, list.size() ) );
      }

      private TupleList partiallySortList(
        Evaluator evaluator,
        TupleList list,
        int n ) {
        assert list.size() > 0;
        assert n <= list.size();

        final int savepoint = evaluator.savepoint();
        try {
          switch ( list.getArity() ) {
            case 1:
              final List<Member> members =
                Sorter.partiallySortMembers(
                  evaluator.push(),
                  list.slice( 0 ),
                  orderCalc, n, top );
              return new UnaryTupleList( members );
            default:
              final List<List<Member>> tuples =
                Sorter.partiallySortTuples(
                  evaluator.push(),
                  list,
                  orderCalc, n, top );
              return new DelegatingTupleList(
                list.getArity(),
                tuples );
          }
        } finally {
          evaluator.restore( savepoint );
        }
      }

      @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
        return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
      }

    };
  }
}
