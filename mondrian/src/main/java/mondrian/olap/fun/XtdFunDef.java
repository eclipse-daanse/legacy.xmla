/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2021 Hitachi Vantara
// All Rights Reserved.
*/

package mondrian.olap.fun;



import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.LevelType;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.DimensionType;
import mondrian.olap.MondrianException;
import mondrian.olap.Util;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

import java.text.MessageFormat;

/**
 * Definition of <code>Ytd</code>, <code>Qtd</code>, <code>Mtd</code>, and <code>Wtd</code> MDX builtin functions.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class XtdFunDef extends AbstractFunctionDefinition {
  private final LevelType levelType;

  static final ResolverImpl MtdResolver =
      new ResolverImpl( "Mtd", "Mtd([<Member>])",
          "A shortcut function for the PeriodsToDate function that specifies the level to be Month.", new String[] {
            "fx", "fxm" }, LevelType.TIME_MONTHS);

  static final ResolverImpl QtdResolver =
      new ResolverImpl( "Qtd", "Qtd([<Member>])",
          "A shortcut function for the PeriodsToDate function that specifies the level to be Quarter.", new String[] {
            "fx", "fxm" }, LevelType.TIME_QUARTERS);

  static final ResolverImpl WtdResolver =
      new ResolverImpl( "Wtd", "Wtd([<Member>])",
          "A shortcut function for the PeriodsToDate function that specifies the level to be Week.", new String[] {
            "fx", "fxm" }, LevelType.TIME_WEEKS);

  static final ResolverImpl YtdResolver =
      new ResolverImpl( "Ytd", "Ytd([<Member>])",
          "A shortcut function for the PeriodsToDate function that specifies the level to be Year.", new String[] {
            "fx", "fxm" }, LevelType.TIME_YEARS);

  private static final String TIMING_NAME = XtdFunDef.class.getSimpleName();
    private final static String timeArgNeeded = "Argument to function ''{0}'' must belong to Time hierarchy.";

    public XtdFunDef( FunctionMetaData functionMetaData , LevelType levelType ) {
    super( functionMetaData );
    this.levelType = levelType;
  }

  @Override
public Type getResultType( Validator validator, Expression[] args ) {
    if ( args.length == 0 ) {
      // With no args, the default implementation cannot
      // guess the hierarchy.
      RolapHierarchy defaultTimeHierarchy =
          ( (RolapCube) validator.getQuery().getCube() ).getTimeHierarchy( getFunctionMetaData().operationAtom().name() );
      return new SetType( MemberType.forHierarchy( defaultTimeHierarchy ) );
    }
    final Type type = args[0].getType();
    if ( type.getDimension().getDimensionType() != DimensionType.TIME_DIMENSION) {
      throw new MondrianException(MessageFormat.format(timeArgNeeded, getFunctionMetaData().operationAtom().name() ));
    }
    return super.getResultType( validator, args );
  }

  private Level getLevel( Evaluator evaluator ) {
    switch ( levelType ) {
      case TIME_YEARS:
        return evaluator.getCube().getYearLevel();
      case TIME_QUARTERS:
        return evaluator.getCube().getQuarterLevel();
      case TIME_MONTHS:
        return evaluator.getCube().getMonthLevel();
      case TIME_WEEKS:
        return evaluator.getCube().getWeekLevel();
      case TIME_DAYS:
        return evaluator.getCube().getWeekLevel();
      default:
        throw Util.badValue( levelType );
    }
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final Level level = getLevel( compiler.getEvaluator() );
    switch ( call.getArgCount() ) {
      case 0:
        return new AbstractListCalc( call.getType(), new Calc[0] ) {
          @Override
		public TupleList evaluateList( Evaluator evaluator ) {
            evaluator.getTiming().markStart( XtdFunDef.TIMING_NAME );
            try {
              return new UnaryTupleList( FunUtil.periodsToDate( evaluator, level, null ) );
            } finally {
              evaluator.getTiming().markEnd( XtdFunDef.TIMING_NAME );
            }
          }

          @Override
		public boolean dependsOn( Hierarchy hierarchy ) {
            return hierarchy.getDimension().getDimensionType() == mondrian.olap.DimensionType.TIME_DIMENSION;
          }
        };
      default:
        final MemberCalc memberCalc = compiler.compileMember( call.getArg( 0 ) );
        return new AbstractListCalc( call.getType(), new Calc[] { memberCalc } ) {
          @Override
		public TupleList evaluateList( Evaluator evaluator ) {
            evaluator.getTiming().markStart( XtdFunDef.TIMING_NAME );
            try {
              return new UnaryTupleList( FunUtil.periodsToDate( evaluator, level, memberCalc.evaluate( evaluator ) ) );
            } finally {
              evaluator.getTiming().markEnd( XtdFunDef.TIMING_NAME );
            }
          }
        };
    }
  }

  private static class ResolverImpl extends MultiResolver {
    private final LevelType levelType;

    public ResolverImpl( String name, String signature, String description, String[] signatures,
        LevelType levelType ) {
      super( name, signature, description, signatures );
      this.levelType = levelType;
    }

    @Override
	protected FunctionDefinition createFunDef( Expression[] args, FunctionMetaData functionMetaData ) {
      return new XtdFunDef( functionMetaData, levelType );
    }
  }
}
