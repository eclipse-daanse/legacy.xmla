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
package org.eclipse.daanse.olap.function.def.logical;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.util.Bug;

import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;

class IsEmptyFunDefTest {
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsEmptyWithAggregate(Context context) {
        TestUtil.assertQueryReturns(context.getConnectionWithDefaultRole(),
            "WITH MEMBER [gender].[foo] AS 'isEmpty(Aggregate({[Gender].m}))' "
                + "SELECT {Gender.foo} on 0 from sales",
            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Gender].[foo]}\n"
                + "Row #0: false\n" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testIsEmpty(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        assertBooleanExprReturns(connection, "[Gender].[All Gender].Parent IS NULL", true );

        // Any functions that return a member from parameters that
        // include a member and that member is NULL also give a NULL.
        // Not a runtime exception.
        assertBooleanExprReturns(connection,
            "[Gender].CurrentMember.Parent.NextMember IS NULL",
            true );

        if ( !Bug.BugMondrian207Fixed ) {
            return;
        }

        // When resolving a tuple's value in the cube, if there is
        // at least one NULL member in the tuple should return a
        // NULL cell value.
        assertBooleanExprReturns(connection,
            "IsEmpty(([Time].currentMember.Parent, [Measures].[Unit Sales]))",
            false );
        assertBooleanExprReturns(connection,
            "IsEmpty(([Time].currentMember, [Measures].[Unit Sales]))",
            false );

        // EMPTY refers to a genuine cell value that exists in the cube space,
        // and has no NULL members in the tuple,
        // but has no fact data at that crossing,
        // so it evaluates to EMPTY as a cell value.
        assertBooleanExprReturns(connection,
            "IsEmpty(\n"
                + " ([Time].[1997].[Q4].[12],\n"
                + "  [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth "
                + "Imported Beer],\n"
                + "  [Store].[All Stores].[USA].[WA].[Bellingham]))", true );
        assertBooleanExprReturns(connection,
            "IsEmpty(\n"
                + " ([Time].[1997].[Q4].[11],\n"
                + "  [Product].[All Products].[Drink].[Alcoholic Beverages].[Beer and Wine].[Beer].[Portsmouth].[Portsmouth "
                + "Imported Beer],\n"
                + "  [Store].[All Stores].[USA].[WA].[Bellingham]))", false );

        // The empty set is neither EMPTY nor NULL.
        // should give 0 as a result, not NULL and not EMPTY.
        TestUtil.assertQueryReturns(connection,
            "WITH SET [empty set] AS '{}'\n"
                + " MEMBER [Measures].[Set Size] AS 'Count([empty set])'\n"
                + " MEMBER [Measures].[Set Size Is Empty] AS 'CASE WHEN IsEmpty([Measures].[Set Size]) THEN 1 ELSE 0 END '\n"
                + "SELECT [Measures].[Set Size] on columns", "" );

        TestUtil.assertQueryReturns(connection,
            "WITH SET [empty set] AS '{}'\n"
                + "WITH MEMBER [Measures].[Set Size] AS 'Count([empty set])'\n"
                + "SELECT [Measures].[Set Size] on columns", "" );

        // Run time errors are BAD things.  They should not occur
        // in almost all cases.  In fact there should be no
        // logically formed MDX that generates them.  An ERROR
        // value in a cell though is perfectly legal - e.g. a
        // divide by 0.
        // E.g.
        String foo =
            "WITH [Measures].[Ratio This Period to Previous] as\n"
                + "'([Measures].[Sales],[Time].CurrentMember/([Measures].[Sales],[Time].CurrentMember.PrevMember)'\n"
                + "SELECT [Measures].[Ratio This Period to Previous] ON COLUMNS,\n"
                + "[Time].Members ON ROWS\n"
                + "FROM ...";

        // For the [Time].[All Time] row as well as the first
        // year, first month etc, the PrevMember will evaluate to
        // NULL, the tuple will evaluate to NULL and the division
        // will implicitly convert the NULL to 0 and then evaluate
        // to an ERROR value due to a divide by 0.

        // This leads to another point: NULL and EMPTY values get
        // implicitly converted to 0 when treated as numeric
        // values for division and multiplication but for addition
        // and subtraction, NULL is treated as NULL (5+NULL yields
        // NULL).
        // I have no idea about how EMPTY works.  I.e. is does
        // 5+EMPTY yield 5 or EMPTY or NULL or what?
        // E.g.
        String foo2 =
            "WITH MEMBER [Measures].[5 plus empty] AS\n"
                + "'5+([Product].[All Products].[Ski boots],[Geography].[All Geography].[Hawaii])'\n"
                + "SELECT [Measures].[5 plus empty] ON COLUMNS\n"
                + "FROM ...";
        // Does this yield EMPTY, 5, NULL or ERROR?

        // Lastly, IS NULL and IS EMPTY are both legal and
        // distinct.  <<Object>> IS {<<Object>> | NULL}  and
        // <<Value>> IS EMPTY.
        // E.g.
        // a)  [Time].CurrentMember.Parent IS [Time].[Year].[2004]
        // is also a perfectly legal expression and better than
        // [Time].CurrentMember.Parent.Name="2004".
        // b) ([Measures].[Sales],[Time].FirstSibling) IS EMPTY is
        // a legal expression.


        // Microsoft's site says that the EMPTY value participates in 3 value
        // logic e.g. TRUE AND EMPTY gives EMPTY, FALSE AND EMPTY gives FALSE.
        // todo: test for this
    }

}
