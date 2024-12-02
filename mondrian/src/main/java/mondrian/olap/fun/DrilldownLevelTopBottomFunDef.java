/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2020 Hitachi Vantara.
 * Copyright (C) 2021 Sergei Semenkov
 *
 * All rights reserved.
 */

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.NativeEvaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.calc.impl.ValueCalc;
import mondrian.olap.fun.sort.Sorter;
import mondrian.olap.type.ScalarType;

/**
 * Definition of the <code>DrilldownLevelTop</code> and
 * <code>DrilldownLevelBottom</code> MDX builtin functions.
 *
 * <p>Syntax:
 *
 * <blockquote>
 * DrilldownLevelTop(Set_Expression, Count [, [Level_Expression][, Numeric_Expression]])<br/>
 * DrilldownLevelBottom(Set_Expression, Count [, [Level_Expression][, Numeric_Expression]])
 * </blockquote>
 *
 * @author jhyde
 * @since Oct 18, 2007
 */
class DrilldownLevelTopBottomFunDef extends AbstractFunctionDefinition {
  final boolean top;

  static final MultiResolver DrilldownLevelTopResolver =
    new MultiResolver(
      "DrilldownLevelTop",
      "DrilldownLevelTop(Set_Expression, Count [, [Level_Expression][, Numeric_Expression]])",
      "Drills down the topmost members of a set, at a specified level, to one level below.",
      new String[] { "fxxn", "fxxnl", "fxxnln", "fxxnen" } ) {
      @Override
	protected FunctionDefinition createFunDef( Expression[] args, FunctionMetaData functionMetaData  ) {
        return new DrilldownLevelTopBottomFunDef( functionMetaData, true );
      }
    };

  static final MultiResolver DrilldownLevelBottomResolver =
    new MultiResolver(
      "DrilldownLevelBottom",
      "DrilldownLevelBottom(Set_Expression, Count [, [Level_Expression][, Numeric_Expression]])",
      "Drills down the bottommost members of a set, at a specified level, to one level below.",
      new String[] { "fxxn", "fxxnl", "fxxnln", "fxxnen" } ) {
      @Override
	protected FunctionDefinition createFunDef( Expression[] args, FunctionMetaData functionMetaData  ) {
        return new DrilldownLevelTopBottomFunDef( functionMetaData, false );
      }
    };

  public DrilldownLevelTopBottomFunDef(
    FunctionMetaData functionMetaData ,
    final boolean top ) {
    super( functionMetaData );
    this.top = top;
  }

  @Override
public Calc compileCall( final ResolvedFunCall call, ExpressionCompiler compiler ) {
    // Compile the member list expression. Ask for a mutable list, because
    // we're going to insert members into it later.
    final TupleListCalc tupleListCalc =
      compiler.compileList( call.getArg( 0 ), true );
    final IntegerCalc integerCalc =
      compiler.compileInteger( call.getArg( 1 ) );
    final LevelCalc levelCalc =
      call.getArgCount() > 2
        && call.getArg( 2 ).getCategory() != DataType.EMPTY
        ? compiler.compileLevel( call.getArg( 2 ) )
        : null;
    final Calc orderCalc =
      call.getArgCount() > 3
        ? compiler.compileScalar( call.getArg( 3 ), true )
        : new ValueCalc(
               ScalarType.INSTANCE  );
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

        TupleList list = tupleListCalc.evaluateList( evaluator );
        Integer n = integerCalc.evaluate( evaluator );
        if ( n == null || n <= 0 ) {
          return list;
        }
        Level level;
        if ( levelCalc == null ) {
          level = null;
        } else {
          level = levelCalc.evaluate( evaluator );
        }
        List<Member> result = new ArrayList<>();
        assert list.getArity() == 1;
        for ( Member member : list.slice( 0 ) ) {
          result.add( member );
          if ( level != null && member.getLevel() != level ) {
            if ( level.getDimension() != member.getDimension() ) {
              throw FunUtil.newEvalException(
                DrilldownLevelTopBottomFunDef.this.getFunctionMetaData(),
                new StringBuilder("Level '")
                  .append(level.getUniqueName())
                  .append("' not compatible with member '")
                  .append(member.getUniqueName())
                  .append("'").toString() );
            }
            continue;
          }
          List<Member> children =
            schemaReader.getMemberChildren( member );
          final int savepoint = evaluator.savepoint();
          List<Member> sortedChildren;
          try {
            evaluator.setNonEmpty( false );
            sortedChildren =
              Sorter.sortMembers(
                evaluator,
                children,
                children,
                orderCalc,
                top,
                true );
          } finally {
            evaluator.restore( savepoint );
          }
          int x = Math.min( n, sortedChildren.size() );
          for ( int i = 0; i < x; i++ ) {
            result.add( sortedChildren.get( i ) );
          }
        }
        return new UnaryTupleList( result );
      }

      @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
        return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
      }
    };
  }
}
