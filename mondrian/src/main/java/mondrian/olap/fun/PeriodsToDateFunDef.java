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
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.Util;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

/**
 * Definition of the <code>PeriodsToDate</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class PeriodsToDateFunDef extends AbstractFunctionDefinition {
  static final ReflectiveMultiResolver Resolver =
      new ReflectiveMultiResolver( "PeriodsToDate", "PeriodsToDate([<Level>[, <Member>]])",
          "Returns a set of periods (members) from a specified level starting with the first period and ending with a specified member.",
          new String[] { "fx", "fxl", "fxlm" }, PeriodsToDateFunDef.class );

  private static final String TIMING_NAME = PeriodsToDateFunDef.class.getSimpleName();

  public PeriodsToDateFunDef( FunctionMetaData functionMetaData ) {
    super( functionMetaData );
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

    if ( args.length >= 2 ) {
      Type hierarchyType = args[0].getType();
      MemberType memberType = (MemberType) args[1].getType();
      if ( memberType.getHierarchy() != null && hierarchyType.getHierarchy() != null && memberType
          .getHierarchy() != hierarchyType.getHierarchy() ) {
        throw Util.newError( "Type mismatch: member must belong to hierarchy " + hierarchyType.getHierarchy()
            .getUniqueName() );
      }
    }

    // If we have at least one arg, it's a level which will
    // tell us the type.
    return super.getResultType( validator, args );
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final LevelCalc levelCalc = call.getArgCount() > 0 ? compiler.compileLevel( call.getArg( 0 ) ) : null;
    final MemberCalc memberCalc = call.getArgCount() > 1 ? compiler.compileMember( call.getArg( 1 ) ) : null;
    final RolapHierarchy timeHierarchy =
        levelCalc == null ? ( (RolapCube) compiler.getEvaluator().getCube() ).getTimeHierarchy( getFunctionMetaData().operationAtom().name() ) : null;

    return new AbstractListCalc( call.getType(), new Calc[] { levelCalc, memberCalc } ) {
      @Override
	public TupleList evaluateList( Evaluator evaluator ) {
        evaluator.getTiming().markStart( PeriodsToDateFunDef.TIMING_NAME );
        try {
          final Member member;
          final Level level;
          if ( levelCalc == null ) {
            member = evaluator.getContext( timeHierarchy );
            level = member.getLevel().getParentLevel();
          } else {
            level = levelCalc.evaluate( evaluator );
            if ( memberCalc == null ) {
              member = evaluator.getContext( level.getHierarchy() );
            } else {
              member = memberCalc.evaluate( evaluator );
            }
          }
          return new UnaryTupleList( FunUtil.periodsToDate( evaluator, level, member ) );
        } finally {
          evaluator.getTiming().markEnd( PeriodsToDateFunDef.TIMING_NAME );
        }
      }

      @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
        if ( super.dependsOn( hierarchy ) ) {
          return true;
        }
        if ( memberCalc != null ) {
          return false;
        } else if ( levelCalc != null ) {
          return levelCalc.getType().usesHierarchy( hierarchy, true );
        } else {
          return hierarchy == timeHierarchy;
        }
      }
    };
  }
}
