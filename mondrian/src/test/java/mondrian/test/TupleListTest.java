/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.model.Cube;
import org.eclipse.daanse.olap.api.model.Member;
import org.eclipse.daanse.olap.api.model.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestingContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import mondrian.calc.TupleCollections;
import mondrian.calc.TupleList;
import mondrian.calc.impl.ArrayTupleList;
import mondrian.calc.impl.DelegatingTupleList;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.SchemaReader;
import mondrian.olap.Util;
import mondrian.rolap.RolapConnection;
import mondrian.server.Locus;

/**
 * Unit test for {@link TupleList} and common implementations.
 *
 * @author jhyde
 */
class TupleListTest {

    @Test
    void testTupleList() {
        assertTrue(TupleCollections.createList(1) instanceof UnaryTupleList);
        assertTrue(TupleCollections.createList(2) instanceof ArrayTupleList);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    void testUnaryTupleList(TestingContext context) {
        // empty list
        final TupleList list0 = new UnaryTupleList();
        assertTrue(list0.isEmpty());
        assertEquals(0, list0.size());

        assertEquals(list0, TupleCollections.emptyList(1));

        TupleList list1 = new UnaryTupleList();
        assertEquals(list0, list1);
        final Member storeUsaMember = xxx(context.createConnection(),"[Store].[USA]");
        list1.add(Collections.singletonList(storeUsaMember));
        assertFalse(list1.isEmpty());
        assertEquals(1, list1.size());
        assertNotSame(list0, list1);

        TupleList list2 = new UnaryTupleList();
        list2.addTuple(new Member[]{storeUsaMember});
        assertFalse(list2.isEmpty());
        assertEquals(1, list2.size());
        assertEquals(list1, list2);

        list2.clear();
        assertEquals(list0, list2);
        assertEquals(list2, list0);

        // For various lists, sublist returns the whole thing.
        for (TupleList list : Arrays.asList(list0, list1, list2)) {
            assertEquals(list, list.subList(0, list.size()));
            assertNotSame(list, list.subList(0, list.size()));
        }

        // Null members OK (at least for TupleList).
        list1.addTuple(new Member[]{null});
        list1.add(Collections.<Member>singletonList(null));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    void testArrayTupleList(TestingContext context) {
        Connection connection = context.createConnection();
        final Member genderFMember = xxx(connection, "[Gender].[F]");
        final Member genderMMember = xxx(connection,"[Gender].[M]");

        // empty list
        final TupleList list0 = new ArrayTupleList(2);
        assertTrue(list0.isEmpty());
        assertEquals(0, list0.size());

        assertIterableEquals(list0, TupleCollections.emptyList(2));

        TupleList list1 = new ArrayTupleList(2);
        assertEquals(list0, list1);
        final Member storeUsaMember = xxx(connection,"[Store].[USA]");
        list1.add(Arrays.asList(storeUsaMember, genderFMember));
        assertFalse(list1.isEmpty());
        assertEquals(1, list1.size());
        assertNotSame(list0, list1);

        try {
            list1.add(Arrays.asList(storeUsaMember));
            fail("expected error");
        } catch (IllegalArgumentException e) {
            assertEquals("Tuple length does not match arity", e.getMessage());
        }
        try {
            list1.addTuple(new Member[] {storeUsaMember});
            fail("expected error");
        } catch (IllegalArgumentException e) {
            assertEquals("Tuple length does not match arity", e.getMessage());
        }
        try {
            list1.add(
                Arrays.asList(storeUsaMember, genderFMember, genderFMember));
            fail("expected error");
        } catch (IllegalArgumentException e) {
            assertEquals("Tuple length does not match arity", e.getMessage());
        }
        try {
            list1.addTuple(
                new Member[]{storeUsaMember, genderFMember, genderFMember});
            fail("expected error");
        } catch (IllegalArgumentException e) {
            assertEquals("Tuple length does not match arity", e.getMessage());
        }

        TupleList list2 = new ArrayTupleList(2);
        list2.addTuple(new Member[]{storeUsaMember, genderFMember});
        assertFalse(list2.isEmpty());
        assertEquals(1, list2.size());
        assertEquals(list1, list2);

        list2.clear();
        assertEquals(list0, list2);
        assertEquals(list2, list0);

        assertEquals("[]", list0.toString());
        assertEquals("[[[Store].[USA], [Gender].[F]]]", list1.toString());
        assertEquals("[]", list2.toString());

        // For various lists, sublist returns the whole thing.
        for (TupleList list : Arrays.asList(list0, list1, list2)) {
            final TupleList sublist = list.subList(0, list.size());
            assertNotNull(sublist);
            assertNotNull(sublist.toString());
            assertEquals(sublist.isEmpty(), list.isEmpty());
            assertIterableEquals(list, sublist);
            assertNotSame(list, sublist);
        }

        // Null members OK (at least for TupleList).
        list1.addTuple(storeUsaMember, null);
        list1.add(Arrays.asList(storeUsaMember, null));

        TupleList fm = new ArrayTupleList(2);
        fm.addTuple(genderFMember, storeUsaMember);
        fm.addTuple(genderMMember, storeUsaMember);
        checkProject(fm);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    void testDelegatingTupleList(TestingContext context) {
        Connection connection = context.createConnection();
        final Member genderFMember = xxx(connection, "[Gender].[F]");
        final Member genderMMember = xxx(connection, "[Gender].[M]");
        final Member storeUsaMember = xxx(connection, "[Store].[USA]");

        final List<List<Member>> arrayList = new ArrayList<>();
        TupleList fm = new DelegatingTupleList(2, arrayList);

        fm.addTuple(genderFMember, storeUsaMember);
        fm.addTuple(genderMMember, storeUsaMember);

        assertEquals(2, fm.size());
        assertEquals(2, fm.getArity());
        assertEquals(
            "[[[Gender].[F], [Store].[USA]], [[Gender].[M], [Store].[USA]]]",
            fm.toString());

        checkProject(fm);
    }

    /**
     * This is a test for MONDRIAN-1040. The DelegatingTupleList.slice()
     * method was mixing up the column and index variables.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    void testDelegatingTupleListSlice(TestingContext context) {
        // Functional test.
        Connection connection = context.createConnection();
        assertQueryReturns(connection,
            "select {[Measures].[Store Sales]} ON COLUMNS, Hierarchize(Except({[Customers].[All Customers], [Customers].[All Customers].Children}, {[Customers].[All Customers]})) ON ROWS from [Sales] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Store Sales]}\n"
            + "Axis #2:\n"
            + "{[Customers].[Canada]}\n"
            + "{[Customers].[Mexico]}\n"
            + "{[Customers].[USA]}\n"
            + "Row #0: \n"
            + "Row #1: \n"
            + "Row #2: 565,238.13\n");
        Locus.execute(
            (RolapConnection)connection,
            "testDelegatingTupleListSlice",
            new Locus.Action<Void>() {
                @Override
				public Void execute() {
                    // Unit test
                    final Member genderFMember = xxx(connection, "[Gender].[F]");
                    final Member storeUsaMember = xxx(connection, "[Store].[USA]");
                    final List<List<Member>> arrayList =
                        new ArrayList<>();
                    final TupleList fm =
                        new DelegatingTupleList(2, arrayList);
                    fm.addTuple(genderFMember, storeUsaMember);
                    final List<Member> sliced = fm.slice(0);
                    assertEquals(1, sliced.size());
                    assertEquals(1, fm.size());
                    return null;
                }
            });
    }

    private void checkProject(TupleList fm) {
        assertEquals(2, fm.size());
        assertEquals(2, fm.getArity());
        assertEquals(2, fm.project(new int[] {0}).size());
        assertEquals(fm.slice(0), fm.project(new int[] {0}).slice(0));
        assertEquals(fm.slice(1), fm.project(new int[] {1}).slice(0));
        assertEquals(
            "[[[Gender].[F]], [[Gender].[M]]]",
            fm.project(new int[] {0}).toString());
        assertEquals(
            "[[[Store].[USA]], [[Store].[USA]]]",
            fm.project(new int[] {1}).toString());

        // Also check cloneList.
        assertEquals(0, fm.cloneList(100).size());
        assertEquals(fm.size(), fm.cloneList(-1).size());
        assertEquals(fm, fm.cloneList(-1));
        assertNotSame(fm, fm.cloneList(-1));
    }

    /**
     * Queries a member of the Sales cube.
     *
     * @param memberName Unique name of member
     * @return The member
     */
    private Member xxx(Connection connection, String memberName) {
        Schema schema = connection.getSchema();
        final boolean fail = true;
        Cube salesCube = schema.lookupCube("Sales", fail);
        final SchemaReader schemaReader =
            salesCube.getSchemaReader(null).withLocus(); // unrestricted
        return schemaReader.getMemberByUniqueName(
            Util.parseIdentifier(memberName), true);
    }
}
