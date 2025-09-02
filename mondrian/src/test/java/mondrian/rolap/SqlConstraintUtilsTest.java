/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2003-2005 Julian Hyde
 * Copyright (C) 2005-2020 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */

package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Evaluator.SetEvaluator;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractTupleCursor;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;
import  org.eclipse.daanse.olap.server.ExecutionImpl;
import org.eclipse.daanse.rolap.common.RolapEvaluator;
import org.eclipse.daanse.rolap.common.RolapEvaluatorRoot;
import org.eclipse.daanse.rolap.common.RolapStar;
import org.eclipse.daanse.rolap.common.SqlConstraintUtils;
import org.eclipse.daanse.rolap.common.TupleConstraintStruct;
import org.eclipse.daanse.rolap.common.aggmatcher.AggStar;
import org.eclipse.daanse.rolap.common.sql.SqlQuery;
import org.eclipse.daanse.rolap.element.CompoundSlicerRolapMember;
import org.eclipse.daanse.rolap.element.RolapCube;
import org.eclipse.daanse.rolap.element.RolapCubeLevel;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mockito;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * <code>SqlConstraintUtilsTest</code> tests the functions defined in
 * {@link SqlConstraintUtils}.
 *
 */
class SqlConstraintUtilsTest {

    private void assertSameContent(
        String msg, Collection<Member> expected, Collection<Member> actual)
    {
        if (expected == null) {
            assertEquals(expected, actual, msg);
        }
        assertEquals(expected.size(), actual.size(), msg + " size");
        Iterator<Member> itExpected = expected.iterator();
        Iterator<Member> itActual = actual.iterator();
        for (int i = 0; itExpected.hasNext(); i++) {
            assertEquals(
                itActual.next(), itExpected.next(), msg + " [" + i + "]");
        }
    }

    private void assertApartExpandSupportedCalculatedMembers(
        String msg,
        Member[] expectedByDefault,
        Member[] expectedOnDisjoint,
        Member[] argMembersArray,
        Evaluator evaluator)
    {
      final List<Member> expectedListOnDisjoin =
          Collections.unmodifiableList(Arrays.asList(expectedOnDisjoint));
      final List<Member> expectedListByDefault =
          Collections.unmodifiableList(Arrays.asList(expectedByDefault));
      final List<Member> argMembersList =
          Collections.unmodifiableList(Arrays.asList(argMembersArray));
      assertSameContent(
          msg + " - (list, eval)",
          expectedListByDefault,
          SqlConstraintUtils.expandSupportedCalculatedMembers(
              argMembersList, evaluator).getMembers());
      assertSameContent(
          msg + " - (list, eval, false)",
          expectedListByDefault,
          SqlConstraintUtils.expandSupportedCalculatedMembers(
              argMembersList, evaluator, false).getMembers());
      assertSameContent(
          msg + " - (list, eval, true)",
          expectedListOnDisjoin,
          SqlConstraintUtils.expandSupportedCalculatedMembers(
              argMembersList, evaluator, true).getMembers());
    }

    private Member makeNoncalculatedMember(String toString) {
        Member member = Mockito.mock(Member.class);
        assertEquals(false, member.isCalculated());
        Mockito.doReturn("mock[" + toString + "]").when(member).toString();
        return member;
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testReplaceCompoundSlicerPlaceholder(Context<?> context) {
        final Connection connection = context.getConnectionWithDefaultRole();

        final String queryText =
            "SELECT {[Measures].[Customer Count]} ON 0 "
            + "FROM [Sales] "
            + "WHERE [Time].[1997]";

        final Query query = connection.parseQuery(queryText);
        final QueryAxis querySlicerAxis = query.getSlicerAxis();
        final Member slicerMember =
            ((MemberExpression)querySlicerAxis.getSet()).getMember();
        final Hierarchy slicerHierarchy =
            query.getCube().getTimeHierarchy(null);

        final ExecutionImpl execution = new ExecutionImpl(query.getStatement(), Optional.empty());
        final RolapEvaluatorRoot rolapEvaluatorRoot =
            new RolapEvaluatorRoot(execution);
        final RolapEvaluator rolapEvaluator =
            new RolapEvaluator(rolapEvaluatorRoot);
        final Member expectedMember = slicerMember;
        setSlicerContext(rolapEvaluator, expectedMember);

        CompoundSlicerRolapMember placeHolderMember =
            Mockito.mock(CompoundSlicerRolapMember.class);
        Mockito.doReturn(slicerHierarchy)
        .when(placeHolderMember).getHierarchy();
        // tested call
        Member r = SqlConstraintUtils.replaceCompoundSlicerPlaceholder(
            placeHolderMember, rolapEvaluator);
        // test
        assertSame(expectedMember, r);
    }



    // test with a placeholder member
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testExpandSupportedCalculatedMembers2(Context<?> context) {
      final Connection connection = context.getConnectionWithDefaultRole();

      final String queryText =
          "SELECT {[Measures].[Customer Count]} ON 0 "
          + "FROM [Sales] "
          + "WHERE [Time].[1997]";

      final Query query = connection.parseQuery(queryText);
      final QueryAxis querySlicerAxis = query.getSlicerAxis();
      final Member slicerMember =
          ((MemberExpression)querySlicerAxis.getSet()).getMember();
      final Hierarchy slicerHierarchy =
          query.getCube().getTimeHierarchy(null);

      final ExecutionImpl execution = new ExecutionImpl(query.getStatement(), Optional.empty());
      final RolapEvaluatorRoot rolapEvaluatorRoot =
          new RolapEvaluatorRoot(execution);
      final RolapEvaluator rolapEvaluator =
          new RolapEvaluator(rolapEvaluatorRoot);
      final Member expectedMember = slicerMember;
      setSlicerContext(rolapEvaluator, expectedMember);

      CompoundSlicerRolapMember placeHolderMember =
          Mockito.mock(CompoundSlicerRolapMember.class);
      Mockito.doReturn(slicerHierarchy)
      .when(placeHolderMember).getHierarchy();

      Member endMember0 = makeNoncalculatedMember("0");

      // (0, placeholder)
      Member[] argMembers = new Member[] {endMember0, placeHolderMember};
      Member[] expectedMembers = new Member[] {endMember0, slicerMember};
      Member[] expectedMembersOnDisjoin = new Member[] {endMember0};
      assertApartExpandSupportedCalculatedMembers(
          "(0, placeholder)",
          expectedMembers, expectedMembersOnDisjoin, argMembers,
          rolapEvaluator);
    }

    public TupleConstraintStruct getCalculatedMember(
        final List<List<Member>> table,
        int arity)
    {
        Member memberMock = mock(Member.class);

        Expression[] funCallArgExps = new Expression[0];
        ResolvedFunCallImpl funCallArgMock = new ResolvedFunCallImpl(
            mock(FunctionDefinition.class),
            funCallArgExps, mock(TupleType.class));

        Expression[] funCallExps = {funCallArgMock};
        ResolvedFunCallImpl funCallMock = new ResolvedFunCallImpl(
            mock(FunctionDefinition.class), funCallExps, mock(TupleType.class));

        when(memberMock.getExpression()).thenReturn(funCallMock);

        Evaluator evaluatorMock = mock(Evaluator.class);

        SetEvaluator setEvaluatorMock = mock(
            SetEvaluator.class);

        TupleIterable tupleIterableMock = mock(TupleIterable.class);

        when(tupleIterableMock.iterator()).thenReturn(table.iterator());
        when(tupleIterableMock.getArity()).thenReturn(arity);

        AbstractTupleCursor cursor = new AbstractTupleCursor(arity) {
            Iterator<List<Member>> iterator = table.iterator();
            List<Member> curList;

            @Override
            public boolean forward() {
                boolean hasNext = iterator.hasNext();
                if (hasNext) {
                    curList = iterator.next();
                } else {
                    curList = null;
                }
                return hasNext;
            }

            @Override
            public List<Member> current() {
                return curList;
            }
        };

        when(tupleIterableMock.tupleCursor()).thenReturn(cursor);

        when(setEvaluatorMock.evaluateTupleIterable())
            .thenReturn(tupleIterableMock);

        when(evaluatorMock.getSetEvaluator(eq(funCallArgMock), anyBoolean()))
            .thenReturn(setEvaluatorMock);

        TupleConstraintStruct constraint = new TupleConstraintStruct();
        SqlConstraintUtils.expandSetFromCalculatedMember(
            evaluatorMock, memberMock, constraint);
        return constraint;
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testConstrainLevel(Context<?> context){

        final RolapCubeLevel level = mock( RolapCubeLevel.class);
        final RolapCube baseCube = mock(RolapCube.class);
        final RolapStar.Column column = mock(RolapStar.Column.class);

        final AggStar aggStar = null;
        final Dialect dialect =  context.getDialect();
        final SqlQuery query = new SqlQuery(dialect, context.getConfigValue(ConfigConstants.GENERATE_FORMATTED_SQL, ConfigConstants.GENERATE_FORMATTED_SQL_DEFAULT_VALUE, Boolean.class));

        when(level.getBaseStarKeyColumn(baseCube)).thenReturn(column);
        when(column.getNameColumn()).thenReturn(column);
        when(column.generateExprString(query)).thenReturn("dummyName");

        String[] columnValue = new String[1];
        columnValue[0] = "dummyValue";

        StringBuilder levelStrBuilder = SqlConstraintUtils.constrainLevel(level, query, baseCube, aggStar, columnValue, false);
        assertEquals("dummyName = 'dummyValue'",  levelStrBuilder.toString());
    }

    private void setSlicerContext(RolapEvaluator e, Member m) {
      List<Member> members = new ArrayList<>();
      members.add( m );
      Map<Hierarchy, Set<Member>> membersByHierarchy = new HashMap<>();
      membersByHierarchy.put( m.getHierarchy(), new HashSet<>(members) );
      e.setSlicerContext( members, membersByHierarchy );
    }
}
