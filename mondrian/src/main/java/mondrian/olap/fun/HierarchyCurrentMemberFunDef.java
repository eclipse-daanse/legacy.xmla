/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2020 Hitachi Vantara..  All rights reserved.
*/
package mondrian.olap.fun;

import java.util.Map;
import java.util.Set;

import mondrian.olap.exceptions.CurrentMemberWithCompoundSlicerException;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.MondrianException;
import mondrian.rolap.RolapEvaluator;
import mondrian.rolap.RolapHierarchy;

/**
 * Definition of the <code>&lt;Hierarchy&gt;.CurrentMember</code> MDX builtin function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class HierarchyCurrentMemberFunDef extends AbstractFunctionDefinition {
  private static final Logger LOGGER = LoggerFactory.getLogger( HierarchyCurrentMemberFunDef.class );

  static final HierarchyCurrentMemberFunDef instance = new HierarchyCurrentMemberFunDef();

  private HierarchyCurrentMemberFunDef() {
    super( "CurrentMember", "Returns the current member along a hierarchy during an iteration.", "pmh" );
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final HierarchyCalc hierarchyCalc = compiler.compileHierarchy( call.getArg( 0 ) );
    final Hierarchy hierarchy = hierarchyCalc.getType().getHierarchy();
    if ( hierarchy != null ) {
      return new CurrentMemberFixedCalc( call.getType(), hierarchy );
    } else {
      return new CurrentMemberCalc( call.getType(), hierarchyCalc );
    }
  }

  /**
   * Compiled implementation of the Hierarchy.CurrentMember function that evaluates the hierarchy expression first.
   */
  public static class CurrentMemberCalc extends AbstractProfilingNestedMemberCalc {
    private final HierarchyCalc hierarchyCalc;

    public CurrentMemberCalc( Type type, HierarchyCalc hierarchyCalc ) {
      super( type, new Calc[] { hierarchyCalc } );
      this.hierarchyCalc = hierarchyCalc;
    }



    @Override
	public Member evaluate( Evaluator evaluator ) {
      Hierarchy hierarchy = hierarchyCalc.evaluate( evaluator );
      HierarchyCurrentMemberFunDef.validateSlicerMembers( hierarchy, evaluator );
      return evaluator.getContext( hierarchy );
    }

    @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
      return hierarchyCalc.getType().usesHierarchy( hierarchy, false );
    }
  }

  /**
   * Compiled implementation of the Hierarchy.CurrentMember function that uses a fixed hierarchy.
   */
  public static class CurrentMemberFixedCalc extends AbstractProfilingNestedMemberCalc {
    // getContext works faster if we give RolapHierarchy rather than
    // Hierarchy
    private final RolapHierarchy hierarchy;

    public CurrentMemberFixedCalc( Type type, Hierarchy hierarchy ) {
      super( type, new Calc[] {} );
      assert hierarchy != null;
      this.hierarchy = (RolapHierarchy) hierarchy;
    }



    @Override
	public Member evaluate( Evaluator evaluator ) {
      HierarchyCurrentMemberFunDef.validateSlicerMembers( hierarchy, evaluator );
      return evaluator.getContext( hierarchy );
    }

    @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
      return this.hierarchy == hierarchy;
    }

  }

  private static void validateSlicerMembers( Hierarchy hierarchy, Evaluator evaluator ) {
    if ( evaluator instanceof RolapEvaluator rev ) {

      String alertValue = evaluator.getSchemaReader().getContext().getConfig().currentMemberWithCompoundSlicerAlert();

      if ( alertValue.equalsIgnoreCase( "OFF" ) ) {
        return; // No validation
      }

      Map<Hierarchy, Set<Member>> map = rev.getSlicerMembersByHierarchy();
      Set<Member> members = map.get( hierarchy );

      if ( members != null && members.size() > 1 ) {
          MondrianException exception =
            new CurrentMemberWithCompoundSlicerException( hierarchy.getUniqueName() );

        if ( alertValue.equalsIgnoreCase( "WARN" ) ) {
          HierarchyCurrentMemberFunDef.LOGGER.warn( exception.getMessage() );
        } else if ( alertValue.equalsIgnoreCase("ERROR") ) {
          throw new CurrentMemberWithCompoundSlicerException( hierarchy.getUniqueName() );
        }
      }
    }
  }
}
