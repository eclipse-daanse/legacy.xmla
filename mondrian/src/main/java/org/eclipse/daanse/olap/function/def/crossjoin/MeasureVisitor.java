/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.crossjoin;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.fun.ResolvedFunCallFinder;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;

/**
 * Traverses the function call tree of the non empty crossjoin function and populates the queryMeasureSet with base
 * measures
 */
public class MeasureVisitor extends MdxVisitorImpl {

    private final Set<Member> queryMeasureSet;
    private final ResolvedFunCallFinder finder;
    private final Set<Member> activeMeasures = new HashSet<>();

    /**
     * Creates a MeasureVisitor.
     *
     * @param queryMeasureSet
     *          Set of measures in query
     * @param crossJoinCall
     *          Measures referencing this call should be excluded from the list of measures found
     */
    MeasureVisitor( Set<Member> queryMeasureSet, ResolvedFunCall crossJoinCall ) {
      this.queryMeasureSet = queryMeasureSet;
      this.finder = new ResolvedFunCallFinder( crossJoinCall );
    }

    @Override
    public Object visitParameterExpression( ParameterExpression parameterExpr ) {
      final Parameter parameter = parameterExpr.getParameter();
      final Type type = parameter.getType();
      if ( type instanceof org.eclipse.daanse.olap.api.type.MemberType ) {
        final Object value = parameter.getValue();
        if ( value instanceof Member member ) {
          process( member );
        }
      }

      return null;
    }

    @Override
    public Object visitMemberExpression( MemberExpression memberExpr ) {
      Member member = memberExpr.getMember();
      process( member );
      return null;
    }

    private void process( final Member member ) {
      if ( member.isMeasure() ) {
        if ( member.isCalculated() ) {
          if ( activeMeasures.add( member ) ) {
            Expression exp = member.getExpression();
            finder.found = false;
            exp.accept( finder );
            if ( !finder.found ) {
              exp.accept( this );
            }
            activeMeasures.remove( member );
          }
        } else {
          queryMeasureSet.add( member );
        }
      }
    }
  }
