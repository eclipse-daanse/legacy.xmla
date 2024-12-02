/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap.agg;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static mondrian.util.Pair.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.flushSchemaCache;
import static org.opencube.junit5.TestUtil.getDialect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.daanse.jdbc.db.dialect.api.Datatype;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.SystemWideProperties;
import mondrian.rolap.BitKey;
import mondrian.rolap.RolapAggregator;
import mondrian.rolap.RolapUtil;
import mondrian.spi.SegmentBody;
import mondrian.spi.SegmentCache;
import mondrian.spi.SegmentColumn;
import mondrian.spi.SegmentHeader;
import mondrian.test.PerformanceTest;
import mondrian.util.ByteString;
import mondrian.util.Pair;

/**
 * <p>Test for <code>SegmentBuilder</code>.</p>
 *
 * @author mcampbell
 */
class SegmentBuilderTest {

    public static final double MOCK_CELL_VALUE = 123.123;

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
        RolapUtil.setHook(null);
    }

    @Test
    void testRollupWithNullAxisVals() {
        // Perform two rollups.  One with two columns each containing 3 values.
        // The second with two columns containing 2 values + null.
        // The rolled up values should be equal in the two cases.
        Pair<SegmentHeader, SegmentBody> rollupNoNulls = SegmentBuilder.rollup(
            makeSegmentMap(
                new String[] {"col1", "col2"}, null, 3, 9, true,
                new boolean[] {false, false}),// each axis sets null axis flag=F
            Collections.singleton("col2"),
            null, RolapAggregator.Sum, Datatype.NUMERIC, 1000, 0.5);
        Pair<SegmentHeader, SegmentBody> rollupWithNullMembers =
            SegmentBuilder.rollup(
                makeSegmentMap(
                    new String[] {"col1", "col2"}, null, 2, 9, true,
                    // each axis sets null axis flag=T
                    new boolean[] {true, true}),
                Collections.singleton("col2"),
                null, RolapAggregator.Sum, Datatype.NUMERIC, 1000, 0.5);
        assertArraysAreEqual(
            (double[]) rollupNoNulls.getValue().getValueArray(),
            (double[]) rollupWithNullMembers.getValue().getValueArray());
        assertTrue(
            rollupWithNullMembers.getValue().getNullAxisFlags().length == 1
                && rollupWithNullMembers.getValue().getNullAxisFlags()[0], "Rolled up column should have nullAxisFlag set.");
        assertEquals(
            rollupWithNullMembers.getKey().getConstrainedColumns()
                .get(0).columnExpression, "col2");
    }

    @Test
    void testRollupWithMixOfNullAxisValues() {
        // constructed segment has 3 columns:
        //    2 values in the first
        //    2 values + null in the second and third
        //  = 18 values
        Pair<SegmentHeader, SegmentBody> rollup = SegmentBuilder.rollup(
            makeSegmentMap(
                new String[] {"col1", "col2", "col3"}, null, 2, 18, true,
                // col2 & col3 have nullAxisFlag=T
                new boolean[] {false, true, true}),
            Collections.singleton("col2"),
            null, RolapAggregator.Sum, Datatype.NUMERIC, 1000, 0.5);

        // expected value is 6 * MOCK_CELL_VALUE for each of 3 column values,
        // since each of the 18 cells are being rolled up to 3 buckets
        double expectedVal = 6 * MOCK_CELL_VALUE;
        assertArraysAreEqual(
            new double[] { expectedVal, expectedVal, expectedVal },
            (double[]) rollup.getValue().getValueArray());
        assertTrue(
            rollup.getValue().getNullAxisFlags().length == 1
                && rollup.getValue().getNullAxisFlags()[0], "Rolled up column should have nullAxisFlag set.");
        assertEquals(
            "col2",
            rollup.getKey().getConstrainedColumns()
                .get(0).columnExpression);
    }

    @Test
    void testRollup2ColumnsWithMixOfNullAxisValues() {
        // constructed segment has 3 columns:
        //    2 values in the first
        //    2 values + null in the second and third
        //  = 18 values
        Pair<SegmentHeader, SegmentBody> rollup = SegmentBuilder.rollup(
            makeSegmentMap(
                new String[] {"col1", "col2", "col3"}, null, 2, 12, true,
                // col2 & col3 have nullAxisFlag=T
                new boolean[] {false, true, false}),
            new HashSet<>(Arrays.asList("col1", "col2")),
            null, RolapAggregator.Sum, Datatype.NUMERIC, 1000, 0.5);

        // expected value is 2 * MOCK_CELL_VALUE for each of 3 column values,
        // since each of the 12 cells are being rolled up to 3 * 2 buckets
        double expectedVal = 2 * MOCK_CELL_VALUE;
        assertArraysAreEqual(
            new double[] {expectedVal, expectedVal, expectedVal,
                expectedVal, expectedVal, expectedVal},
            (double[]) rollup.getValue().getValueArray());
        assertTrue(
            rollup.getValue().getNullAxisFlags().length == 2
                && !rollup.getValue().getNullAxisFlags()[0]
                && rollup.getValue().getNullAxisFlags()[1],
                "Rolled up column should have nullAxisFlag set to false for "
                        + "the first column, true for second column.");
        assertEquals(
            "col1",
            rollup.getKey().getConstrainedColumns()
                .get(0).columnExpression);
        assertEquals(
            "col2",
            rollup.getKey().getConstrainedColumns()
                .get(1).columnExpression);
    }

    @Test
    void testMultiSegRollupWithMixOfNullAxisValues() {
        // rolls up 2 segments.
        // Segment 1 has 3 columns:
        //    2 values in the first
        //    1 values + null in the second
        //    2 vals + null in the third
        //  = 12 values
        //  Segment 2 has the same 3 columns, difft values for 3rd column.
        //
        //  none of the columns are wildcarded.
        final Map<SegmentHeader, SegmentBody> map = makeSegmentMap(
            new String[]{"col1", "col2", "col3"},
            new String[][]{{"col1A", "col1B"}, {"col2A"}, {"col3A", "col3B"}},
            -1, 12, false,
            // col2 & col3 have nullAxisFlag=T
            new boolean[]{false, true, true});
        map.putAll(
            makeSegmentMap(
                new String[]{"col1", "col2", "col3"},
                new String[][]{{"col1A", "col1B"}, {"col2A"}, {"col3C",
                    "col3D"}},
                -1, 8, false,
                // col3 has nullAxisFlag=T
                new boolean[]{false, true, false}));
        Pair<SegmentHeader, SegmentBody> rollup = SegmentBuilder.rollup(
            map,
            Collections.singleton("col2"),
            null, RolapAggregator.Sum, Datatype.NUMERIC,
            1000,
            0.5);
        // expected value is 10 * MOCK_CELL_VALUE for each of 2 column values,
        // since the 20 cells across 2 segments are being rolled up to 2 buckets
        double expectedVal = 10 * MOCK_CELL_VALUE;
        assertArraysAreEqual(
            new double[]{ expectedVal, expectedVal},
            (double[]) rollup.getValue().getValueArray());
        assertTrue(
            rollup.getValue().getNullAxisFlags().length == 1
                && rollup.getValue().getNullAxisFlags()[0],
                "Rolled up column should have nullAxisFlag set to true for "
                        + "a single column.");
        assertEquals(
            "col2",
            rollup.getKey().getConstrainedColumns()
                .get(0).columnExpression);
    }


    private void assertArraysAreEqual(double[] expected, double[] actual) {
        assertTrue(
            doubleArraysEqual(actual, expected),
            "Expected double array:  "
                    + Arrays.toString(expected)
                    + ", but got "
                    + Arrays.toString(actual)
        );
    }

    private boolean doubleArraysEqual(
        double[] valueArray, double[] expectedVal)
    {
        if (valueArray.length != expectedVal.length) {
            return false;
        }
        double within = 0.00000001;
        for (int i = 0; i < valueArray.length; i++) {
            if (Math.abs(valueArray[i] - expectedVal[i]) > within) {
                return false;
            }
        }
        return true;
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNullMemberOffset(Context context) {
        // verifies that presence of a null member does not cause
        // offsets to be incorrect for a Segment rollup.
        // First query loads the cache with a segment that can fulfill the
        // second query.
        Connection connection = context.getConnection();
        executeQuery(connection,
            "select [Store Size in SQFT].[Store Sqft].members * "
            + "gender.gender.members  on 0 from sales");
        assertQueryReturns(connection,
            "select non empty [Store Size in SQFT].[Store Sqft].members on 0"
            + " from sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store Size in SQFT].[#null]}\n"
            + "{[Store Size in SQFT].[20319]}\n"
            + "{[Store Size in SQFT].[21215]}\n"
            + "{[Store Size in SQFT].[22478]}\n"
            + "{[Store Size in SQFT].[23598]}\n"
            + "{[Store Size in SQFT].[23688]}\n"
            + "{[Store Size in SQFT].[27694]}\n"
            + "{[Store Size in SQFT].[28206]}\n"
            + "{[Store Size in SQFT].[30268]}\n"
            + "{[Store Size in SQFT].[33858]}\n"
            + "{[Store Size in SQFT].[39696]}\n"
            + "Row #0: 39,329\n"
            + "Row #0: 26,079\n"
            + "Row #0: 25,011\n"
            + "Row #0: 2,117\n"
            + "Row #0: 25,663\n"
            + "Row #0: 21,333\n"
            + "Row #0: 41,580\n"
            + "Row #0: 2,237\n"
            + "Row #0: 23,591\n"
            + "Row #0: 35,257\n"
            + "Row #0: 24,576\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNullMemberOffset2ColRollup(Context context) {
        // verifies that presence of a null member does not cause
        // offsets to be incorrect for a Segment rollup involving 2
        // columns.  This tests a case where
        // SegmentBuilder.computeAxisMultipliers needs to factor in
        // the null axis flag.
        Connection connection = context.getConnection();
        executeQuery(connection,
            "select [Store Size in SQFT].[Store Sqft].members * "
            + "[store].[store state].members * time.[quarter].members on 0"
            + " from sales where [Product].[Food].[Produce].[Vegetables].[Fresh Vegetables]");
        assertQueryReturns(connection,
            "select non empty [Store Size in SQFT].[Store Sqft].members "
            + " * [store].[store state].members  on 0"
            + " from sales where [Product].[Food].[Produce].[Vegetables].[Fresh Vegetables]",
            "Axis #0:\n"
            + "{[Product].[Food].[Produce].[Vegetables].[Fresh Vegetables]}\n"
            + "Axis #1:\n"
            + "{[Store Size in SQFT].[#null], [Store].[USA].[CA]}\n"
            + "{[Store Size in SQFT].[#null], [Store].[USA].[WA]}\n"
            + "{[Store Size in SQFT].[20319], [Store].[USA].[OR]}\n"
            + "{[Store Size in SQFT].[21215], [Store].[USA].[WA]}\n"
            + "{[Store Size in SQFT].[22478], [Store].[USA].[CA]}\n"
            + "{[Store Size in SQFT].[23598], [Store].[USA].[CA]}\n"
            + "{[Store Size in SQFT].[23688], [Store].[USA].[CA]}\n"
            + "{[Store Size in SQFT].[27694], [Store].[USA].[OR]}\n"
            + "{[Store Size in SQFT].[28206], [Store].[USA].[WA]}\n"
            + "{[Store Size in SQFT].[30268], [Store].[USA].[WA]}\n"
            + "{[Store Size in SQFT].[33858], [Store].[USA].[WA]}\n"
            + "{[Store Size in SQFT].[39696], [Store].[USA].[WA]}\n"
            + "Row #0: 1,967\n"
            + "Row #0: 947\n"
            + "Row #0: 2,065\n"
            + "Row #0: 1,827\n"
            + "Row #0: 165\n"
            + "Row #0: 2,109\n"
            + "Row #0: 1,665\n"
            + "Row #0: 3,382\n"
            + "Row #0: 162\n"
            + "Row #0: 1,875\n"
            + "Row #0: 2,668\n"
            + "Row #0: 1,907\n");
    }

    @Test
    void testSegmentBodyIterator() {
        // checks that cell key coordinates are generated correctly
        // when a null member is present.
        List<Pair<SortedSet<Comparable>, Boolean>> axes =
            new ArrayList<>();
        axes.add(new Pair<SortedSet<Comparable>, Boolean>(
            new TreeSet<Comparable>(
                Arrays.asList("foo1", "bar1")), true)); // nullAxisFlag=T
        axes.add(new Pair<SortedSet<Comparable>, Boolean>(
            new TreeSet<Comparable>(
                Arrays.asList("foo2", "bar2", "baz3")), false));
        SegmentBody testBody = new DenseIntSegmentBody(
            new BitSet(), new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9},
            axes);
        Map valueMap = testBody.getValueMap();
        assertEquals(
            "{(0, 0)=1, "
            + "(0, 1)=2, "
            + "(0, 2)=3, "
            + "(1, 0)=4, "
            + "(1, 1)=5, "
            + "(1, 2)=6, "
            + "(2, 0)=7, "
            + "(2, 1)=8, "
            + "(2, 2)=9}",
            valueMap.toString());
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSparseRollup(Context context) {
        // functional test for a case that causes OOM if rollup creates
        // a dense segment.
        // This takes several seconds to run

        if (PerformanceTest.LOGGER.isDebugEnabled()) {
            // load the cache with a segment for the subsequent rollup
            Connection connection = context.getConnection();
            executeQuery(connection,
                "select NON EMPTY Crossjoin([Store Type].[Store Type].members, "
                + "CrossJoin([Promotion Media].[Media Type].members, "
                + " Crossjoin([Promotions].[Promotion Name].members, "
                + "Crossjoin([Store].[Store Name].Members, "
                + "Crossjoin( [product].[product name].members, "
                + "Crossjoin( [Customers].[Name].members,  "
                + "[Gender].[Gender].members)))))) on 1, "
                + "{ measures.[unit sales] } on 0 from [Sales]");

            executeQuery(connection,
                "select NON EMPTY Crossjoin([Store Type].[Store Type].members, "
                + "CrossJoin([Promotion Media].[Media Type].members, "
                + "Crossjoin([Promotions].[Promotion Name].members, "
                + "Crossjoin([Store].[Store Name].Members, "
                + "Crossjoin( [product].[product name].members, "
                + "[Customers].[Name].members))))) on 1, "
                + "{ measures.[unit sales] } on 0 from [Sales]");
            // second query will throw OOM if .rollup() attempts
            // to create a dense segment
        }
    }

    @Test
    void testRollupWithIntOverflowPossibility() {
        // rolling up a segment that would cause int overflow if
        // rolled up to a dense segment
        // MONDRIAN-1377

        // make a source segment w/ 3 cols, 47K vals each,
        // target segment has 2 of the 3 cols.
        // count of possible values will exceed Integer.MAX_VALUE
        Pair<SegmentHeader, SegmentBody> rollup =
            SegmentBuilder.rollup(
                makeSegmentMap(
                    new String[] {"col1", "col2", "col3"},
                    null, 47000, 4, false, null),
                new HashSet<>(Arrays.asList("col1", "col2")),
                null, RolapAggregator.Sum, Datatype.NUMERIC,
                1000, 0.5);
        assertTrue(rollup.right instanceof SparseSegmentBody);
    }

    @Test
    void testRollupWithOOMPossibility() {
        // rolling up a segment that would cause OOM if
        // rolled up to a dense segment
        // MONDRIAN-1377

        // make a source segment w/ 3 cols, 44K vals each,
        // target segment has 2 of the 3 cols.
        Pair<SegmentHeader, SegmentBody> rollup =
            SegmentBuilder.rollup(
                makeSegmentMap(
                    new String[] {"col1", "col2", "col3"},
                    null, 44000, 4, false, null),
                new HashSet<>(Arrays.asList("col1", "col2")),
                null, RolapAggregator.Sum, Datatype.NUMERIC,
                1000, 0.5);
        assertTrue(rollup.right instanceof SparseSegmentBody);
    }

    @Test
    void testRollupShouldBeDense() {
        // Fewer than 1000 column values in rolled up segment.
        Pair<SegmentHeader, SegmentBody> rollup =
            SegmentBuilder.rollup(
                makeSegmentMap(
                    new String[] {"col1", "col2", "col3"},
                    null, 10, 15, false, null),
                new HashSet<>(Arrays.asList("col1", "col2")),
                null, RolapAggregator.Sum, Datatype.NUMERIC,
                1000, 0.5);
        assertTrue(rollup.right instanceof DenseDoubleSegmentBody);

        // greater than 1K col vals, above density ratio
        rollup =
            SegmentBuilder.rollup(
                makeSegmentMap(
                    new String[] {"col1", "col2", "col3", "col4"},
                    null, 11, 10000, false, null),
                    // 1331 possible intersections (11*3)
                new HashSet<>(Arrays.asList("col1", "col2", "col3")),
                null, RolapAggregator.Sum, Datatype.NUMERIC,
                1000, 0.5);
        assertTrue(rollup.right instanceof DenseDoubleSegmentBody);
    }

    @Test
    void testRollupWithDenseIntBody() {
      //
      //  We have the following data:
      //
      //           1 _ _
      //    col2   1 2 _
      //           1 _ 1
      //            col1
      //   So, after rolling it up with the SUM function, we expect to get
      //
      //           3 2 1
      //            col1
      //
      String[][] colValues = dummyColumnValues(2, 3);
      int[] values = {1, 1, 1, 0, 2, 0, 1};

      BitSet nulls = new BitSet();
      for (int i = 0; i < values.length; i++) {
        if (values[i] == 0) {
          nulls.set(i);
        }
      }

      List<Pair<SortedSet<Comparable>, Boolean>> axes =
          new ArrayList<>();
      List<SegmentColumn> segmentColumns = new ArrayList<>();
      for (int i = 0; i < colValues.length; i++) {
        axes.add(of(toSortedSet(colValues[i]), false));
        segmentColumns.add(new SegmentColumn(
            "col" + (i + 1),
            colValues[i].length,
            toSortedSet(colValues[i])));
      }
      SegmentHeader header = makeDummySegmentHeader(segmentColumns);
      SegmentBody body = new DenseIntSegmentBody(nulls, values, axes);
      Map<SegmentHeader, SegmentBody> segmentsMap = singletonMap(header, body);

      Pair<SegmentHeader, SegmentBody> rollup =
          SegmentBuilder.rollup(
              segmentsMap, singleton("col1"),
              null, RolapAggregator.Sum, Datatype.NUMERIC,
              1000, 0.5);

      double[] result = (double[])rollup.right.getValueArray();
      double[] expected = {3, 2, 1};
      assertEquals(expected.length, result.length);
      for (int i = 0; i < expected.length; i++) {
        double exp = expected[i];
        double act = result[i];
        assertTrue(
                Math.abs(exp - act) < 1e-6,
                String.format("%d %f %f", i, exp, act));
      }
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testOverlappingSegments(Context context) {
        // MONDRIAN-2107
        // The segments created by the first 2 queries below overlap on
        //  [1997].[Q1].[1].  The rollup of these two segments should not
        // doubly-add that cell.
        // Also, these two segments have predicates optimized for 'quarter'
        // since 3 out of 4 quarters are present.  This means the
        //  header.getValues() will be null.  This has the potential
        // to cause issues with rollup since one segment body will have
        // 3 values for quarter, the other segment body will have a different
        // set of values.
        Connection connection = context.getConnection();
        flushSchemaCache(connection);
        //TestContext context = getTestContext().withFreshConnection();

        executeQuery(connection,
            "select "
            + "{[Time].[1997].[Q1].[1], [Time].[1997].[Q1].[2], [Time].[1997].[Q1].[3], "
            + "[Time].[1997].[Q2].[4], [Time].[1997].[Q2].[5], [Time].[1997].[Q2].[6],"
            + "[Time].[1997].[Q3].[7]} on 0 from sales");
        executeQuery(connection,
            "select "
            + "{[Time].[1997].[Q1].[1], [Time].[1997].[Q3].[8], [Time].[1997].[Q3].[9], "
            + "[Time].[1997].[Q4].[10], [Time].[1997].[Q4].[11], [Time].[1997].[Q4].[12],"
            + "[Time].[1998].[Q1].[1], [Time].[1998].[Q3].[8], [Time].[1998].[Q3].[9], "
            + "[Time].[1998].[Q4].[10], [Time].[1998].[Q4].[11], [Time].[1998].[Q4].[12]}"
            + "on 0 from sales");

        RolapUtil.setHook(
            new RolapUtil.ExecuteQueryHook()
        {
            @Override
			public void onExecuteQuery(String sql) {
                //  We shouldn't see a sum of unit_sales in SQL if using rollup.
                assertFalse(
                        sql.matches(".*sum\\([^ ]+unit_sales.*"),
                        "Expected cells to be pulled from cache");
            }
        });
        assertQueryReturns(connection,
            "select [Time].[1997].children on 0 from sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Time].[1997].[Q1]}\n"
            + "{[Time].[1997].[Q2]}\n"
            + "{[Time].[1997].[Q3]}\n"
            + "{[Time].[1997].[Q4]}\n"
            + "Row #0: 66,291\n"
            + "Row #0: 62,610\n"
            + "Row #0: 65,848\n"
            + "Row #0: 72,024\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonOverlappingRollupWithUnconstrainedColumn(Context context) {
        // MONDRIAN-2107
        // The two segments loaded by the 1st 2 queries will have predicates
        // optimized for Name.  Prior to the fix for 2107 this would
        // result in roughly half of the customers having empty results
        // for the 3rd query, since the values of only one of the two
        // segments would be loaded into the AxisInfo.
        Connection connection = context.getConnection();
        flushSchemaCache(connection);
        //TestContext context = getTestContext().withFreshConnection();
        final String query = "select customers.[name].members on 0 from sales";
        ((TestConfig)context.getConfig()).setEnableInMemoryRollup(false);
        Result result = executeQuery(connection, query);

        flushSchemaCache(connection);
        //context = getTestContext().withFreshConnection();
        ((TestConfig)context.getConfig()).setEnableInMemoryRollup(true);
        executeQuery(connection,
            "select "
            + "{[customers].[name].members} on 0 from sales where gender.f");
        executeQuery(connection,
            "select "
            + "{[customers].[name].members} on 0 from sales where gender.m");

        RolapUtil.setHook(
            new RolapUtil.ExecuteQueryHook()
        {
            @Override
			public void onExecuteQuery(String sql) {
                //  We shouldn't see a sum of unit_sales in SQL if using rollup.
                assertFalse(
                        sql.matches(".*sum\\([^ ]+unit_sales.*"),
                        "Expected cells to be pulled from cache");
            }
        });

        assertQueryReturns(connection,
                query,
                TestUtil.toString(result));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonOverlappingRollupWithUnconstrainedColumnAndHasNull(Context context) {
        // MONDRIAN-2107
        // Creates 10 segments, one for each city, with various sets
        // of [Store Sqft].  Some contain NULL, some do not.
        // Results from rollup should match results from a query not pulling
        // from cache.
        String[] states = {"[Canada].BC", "[USA].CA", "[Mexico].DF",
            "[Mexico].Guerrero", "[Mexico].Jalisco", "[USA].[OR]",
            "[Mexico].Veracruz", "[USA].WA", "[Mexico].Yucatan",
            "[Mexico].Zacatecas"};
        Connection connection = context.getConnection();
        flushSchemaCache(connection);
        //TestContext context = getTestContext().withFreshConnection();
        final String query =
            "select [Store Size in SQFT].[Store Sqft].members on 0 from sales";

        Result result = executeQuery(connection, query);
        flushSchemaCache(connection);
        //context = getTestContext().withFreshConnection();
        for (String state : states) {
            executeQuery(connection,
                String.format(
                    "select "
                    + "{[Store Size in SQFT].[Store Sqft].members} on 0 "
                    + "from sales where store.%s", state));
        }
        RolapUtil.setHook(
            new RolapUtil.ExecuteQueryHook()
        {
            @Override
			public void onExecuteQuery(String sql) {
                //  We shouldn't see a sum of unit_sales in SQL if using rollup.
                assertFalse(
                        sql.matches(".*sum\\([^ ]+unit_sales.*"),
                        "Expected cell to be pulled from cache");
            }
        });

        assertQueryReturns(connection,
            query,
            TestUtil.toString(result));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testBadRollupCausesGreaterThan12Iterations(Context context) {
        // http://jira.pentaho.com/browse/MONDRIAN-1729
        // The first two queries populate the cache with segments
        // capable of being rolled up to fulfill the 3rd query.
        // MONDRIAN-1729 involved the rollup being invalid, causing
        // an infinite loop.
        Connection connection = context.getConnection();
        flushSchemaCache(connection);
        //TestContext context = getTestContext().withFreshConnection();

        executeQuery(connection,
            "select "
            + "{[Time].[1998].[Q1].[2],[Time].[1998].[Q1].[3],"
            + "[Time].[1998].[Q2].[4],[Time].[1998].[Q2].[5],"
            + "[Time].[1998].[Q2].[5],[Time].[1998].[Q2].[6],"
            + "[Time].[1998].[Q3].[7]} on 0 from sales");

        executeQuery(connection,
            "select "
            + "{[Time].[1997].[Q1].[1], [Time].[1997].[Q3].[8], [Time].[1997].[Q3].[9], "
            + "[Time].[1997].[Q4].[10], [Time].[1997].[Q4].[11], [Time].[1997].[Q4].[12],"
            + "[Time].[1998].[Q1].[1], [Time].[1998].[Q3].[8], [Time].[1998].[Q3].[9], "
            + "[Time].[1998].[Q4].[10], [Time].[1998].[Q4].[11], [Time].[1998].[Q4].[12]}"
            + "on 0 from sales");

        executeQuery(connection, "select [Time].[1998].[Q1] on 0 from sales");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSameRollupRegardlessOfSegmentOrderWithEmptySegmentBody(Context context) {
        // http://jira.pentaho.com/browse/MONDRIAN-1729
        // rollup of segments {A, B} should produce the same resulting segment
        // regardless of whether rollup processes them in the order A,B or B,A.
        // MONDRIAN-1729 involved a case where the rollup segment was invalid
        // if processed in a particular order.
        // This tests a wildcarded segment (on year) rolled up w/ a seg
        // containing a single val.
        // The resulting segment contains only empty results (for 1998)
    	context.getConnection().getCacheControl(null).flushSchemaCache();
    	clearAggregationCache(context.getConnection());
        runRollupTest(context.getConnection(),
            // queries to populate the cache with segments which will be rolled
            // up
            new String[]{
                "select "
                + "{[Time].[1998].[Q1].[2],[Time].[1998].[Q1].[3],"
                + "[Time].[1998].[Q2].[4],[Time].[1998].[Q2].[5],"
                + "[Time].[1998].[Q2].[5],[Time].[1998].[Q2].[6],"
                + "[Time].[1998].[Q3].[7]} on 0 from sales",
                "select "
                + "{[Time].[1997].[Q1].[1], [Time].[1997].[Q3].[8], [Time].[1997].[Q3].[9], "
                + "[Time].[1997].[Q4].[10], [Time].[1997].[Q4].[11], [Time].[1997].[Q4].[12],"
                + "[Time].[1998].[Q1].[1], [Time].[1998].[Q3].[8], [Time].[1998].[Q3].[9], "
                + "[Time].[1998].[Q4].[10], [Time].[1998].[Q4].[11], [Time].[1998].[Q4].[12]}"
                + "on 0 from sales"},
            new String[]{
                // rollup columns
                "time_by_day.quarter",
                "time_by_day.the_year"
            },
            // expected header of the rolled up segment
            "*Segment Header\n"
            + "Schema:[FoodMart]\n"
            + "Cube:[Sales]\n"
            + "Measure:[Unit Sales]\n"
            + "Axes:[\n"
            + "    {time_by_day.quarter=('Q1','Q3')}\n"
            + "    {time_by_day.the_year=('1998')}]\n"
            + "Excluded Regions:[]\n"
            + "Compound Predicates:[]\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSameRollupRegardlessOfSegmentOrderWithData(Context context) {
        // http://jira.pentaho.com/browse/MONDRIAN-1729
        // Tests a wildcarded segment rolled up w/ a seg containing a single
        // val.  Both segments are associated w/ non empty results.
    	context.getConnection().getCacheControl(null).flushSchemaCache();
    	clearAggregationCache(context.getConnection());
        runRollupTest(context.getConnection(),
            new String[]{
                "select {{[Product].[Drink].[Alcoholic Beverages]},\n"
                + "{[Product].[Drink].[Beverages]},\n"
                + "{[Product].[Food].[Baked Goods]},\n"
                + "{[Product].[Non-Consumable].[Periodicals]}}\n on 0 from sales",
                "select "
                + "\n"
                + "{[Product].[Drink].[Dairy]}"
                + "on 0 from sales"},
            new String[]{
                "product_class.product_family"
            },
            "*Segment Header\n"
            + "Schema:[FoodMart]\n"
            + "Cube:[Sales]\n"
            + "Measure:[Unit Sales]\n"
            + "Axes:[\n"
            + "    {product_class.product_family=('Drink')}]\n"
            + "Excluded Regions:[]\n"
            + "Compound Predicates:[]\n");
    }

    private void clearAggregationCache(Connection connection) {
    	SegmentCache segmentCache =
    			connection.getContext().getAggregationManager()
            .cacheMgr.compositeCache;
    	segmentCache.getSegmentHeaders().stream().forEach(it -> segmentCache.remove(it));
	}

    void testSameRollupRegardlessOfSegmentOrderNoWildcards(Context context) {
        // http://jira.pentaho.com/browse/MONDRIAN-1729
        // Tests 2 segments, each w/ no wildcarded values.
        runRollupTest(context.getConnection(),
            new String[]{
                "select {{[Product].[Drink].[Alcoholic Beverages]},\n"
                + "{[Product].[Drink].[Beverages]},\n"
                + "{[Product].[Non-Consumable].[Periodicals]}}\n on 0 from sales",
                "select "
                + "\n"
                + "{[Product].[Drink].[Dairy]}"
                + "on 0 from sales"},
            new String[]{
                "product_class.product_family"
            },
            "*Segment Header\n"
            + "Schema:[FoodMart]\n"
            + "Cube:[Sales]\n"
            + "Measure:[Unit Sales]\n"
            + "Axes:[\n"
            + "    {product_class.product_family=('Drink')}]\n"
            + "Excluded Regions:[]\n"
            + "Compound Predicates:[]\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSameRollupRegardlessOfSegmentOrderThreeSegs(Context context) {
        // http://jira.pentaho.com/browse/MONDRIAN-1729
        // Tests 3 segments, each w/ no wildcarded values.
    	context.getConnection().getCacheControl(null).flushSchemaCache();
    	clearAggregationCache(context.getConnection());
        runRollupTest(context.getConnection(),
            new String[]{
                "select {{[Product].[Drink].[Alcoholic Beverages]},\n"
                + "{[Product].[Non-Consumable].[Periodicals]}}\n on 0 from sales",
                "select "
                + "\n"
                + "{[Product].[Drink].[Dairy]}"
                + "on 0 from sales",
                " select "
                + "{[Product].[Drink].[Beverages]} on 0 from sales"},
            new String[]{
                "product_class.product_family"
            },
            "*Segment Header\n"
            + "Schema:[FoodMart]\n"
            + "Cube:[Sales]\n"
            + "Measure:[Unit Sales]\n"
            + "Axes:[\n"
            + "    {product_class.product_family=('Drink')}]\n"
            + "Excluded Regions:[]\n"
            + "Compound Predicates:[]\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSegmentCreationForBoolean_True(Context context) {
        doTestSegmentCreationForBoolean(context.getConnection(), true);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSegmentCreationForBoolean_False(Context context) {
        doTestSegmentCreationForBoolean(context.getConnection(),false);
    }

    private void doTestSegmentCreationForBoolean(Connection connection, boolean value) {
        DatabaseProduct db =
            getDatabaseProduct(getDialect(connection).getDialectName());
        if (db == DatabaseProduct.ORACLE) {
            // Oracle does not support boolean type
            return;
        }

        final String queryToBeCached = String.format(
            "SELECT NON EMPTY [Store].[Store Country].members on COLUMNS "
            + "FROM [Store] "
            + "WHERE [Has coffee bar].[%b]", value);
        executeQuery(connection, queryToBeCached);

        final String query = ""
            + "SELECT NON EMPTY "
            + "CROSSJOIN([Store].[Store Country].members, [Has coffee bar].[has coffee bar].members) ON COLUMNS "
            + "FROM [Store]";

        final String expected = ""
            + "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Canada], [Has coffee bar].[true]}\n"
            + "{[Store].[Mexico], [Has coffee bar].[false]}\n"
            + "{[Store].[Mexico], [Has coffee bar].[true]}\n"
            + "{[Store].[USA], [Has coffee bar].[false]}\n"
            + "{[Store].[USA], [Has coffee bar].[true]}\n"
            + "Row #0: 57,564\n"
            + "Row #0: 133,275\n"
            + "Row #0: 109,737\n"
            + "Row #0: 113,881\n"
            + "Row #0: 157,139\n";

        assertQueryReturns(connection, query, expected);
    }


    /**
     * Loads the cache with the results of the queries
     * in cachePopulatingQueries, and then attempts to rollup all
     * cached segments based on the keepColumns array, checking
     * against expectedHeader.  Rolls up loading segment
     * in both forward and reverse order and verifies
     * same results both ways.
     * @return the rolled up SegmentHeader/SegmentBody pair
     */
    private Pair<SegmentHeader, SegmentBody> runRollupTest(
        Connection connection,
        String[] cachePopulatingQueries,
        String[] keepColumns,
        String expectedHeader)
    {
        ((TestConfig)connection.getContext().getConfig()).setOptimizePredicates(false);
        loadCacheWithQueries(connection, cachePopulatingQueries);
        Map<SegmentHeader, SegmentBody> map = getReversibleTestMap(connection, Order.FORWARD);
        Set<String> keepColumnsSet = new HashSet<>(Arrays.asList(keepColumns));
        Pair<SegmentHeader, SegmentBody> rolledForward = SegmentBuilder.rollup(
            map,
            keepColumnsSet,
                   // bitkey does not factor into rollup logic, so it's safe to
                   // use a dummy
            BitKey.Factory.makeBitKey(new BitSet()),
            RolapAggregator.Sum,
            Datatype.NUMERIC, 1000, 0.5);
        // Now try reversing the order the segments are retrieved
        loadCacheWithQueries(connection, cachePopulatingQueries);
        map = getReversibleTestMap(connection, Order.REVERSE);
        Pair<SegmentHeader, SegmentBody> rolledReverse = SegmentBuilder.rollup(
            map,
            keepColumnsSet,
            BitKey.Factory.makeBitKey(new BitSet()),
            RolapAggregator.Sum,
            Datatype.NUMERIC, 1000, 0.5);
        assertEquals(expectedHeader, removeJdkDependentStrings(rolledForward.getKey().toString()));
        // the header of the rolled up segment should be the same
        // regardless of the order the segments were processed
        assertEquals(rolledForward.getKey(), rolledReverse.getKey());
        assertEquals(
            rolledForward.getValue().getValueMap().size(),
            rolledReverse.getValue().getValueMap().size());
        SystemWideProperties.instance().populateInitial();
        return rolledForward;
    }

    private void loadCacheWithQueries(Connection connection, String [] queries) {
        flushSchemaCache(connection);
        //TestContext context = getTestContext().withFreshConnection();
        for (String query : queries) {
            executeQuery(connection, query);
        }
    }

    enum Order {
        FORWARD, REVERSE
    }
    /**
     * Creates a Map<SegmentHeader,SegmentBody> based on the set of
     * segments currently in the cache.  The Map overrides the entrySet()
     * method to provide an ordered set of elements based
     * on Header.getUniqueID(), ordered according to the order param.
     * @param connection  The connection
     * @param order  The order to sort the elements returned by entrySet(),
     *               FORWARD or REVERSE
     */
    private Map<SegmentHeader, SegmentBody> getReversibleTestMap(Connection connection,
        final Order order)
    {
        SegmentCache cache =
                connection.getContext().getAggregationManager()
            .cacheMgr.compositeCache;

        List<SegmentHeader> headers = cache.getSegmentHeaders();
        Map<SegmentHeader, SegmentBody> testMap =
            new HashMap<>() {
            @Override
			public Set<Entry<SegmentHeader, SegmentBody>> entrySet() {
                List<Entry<SegmentHeader, SegmentBody>> list =
                    new ArrayList<>(super.entrySet());
                Collections.sort(
                    list,
                    new Comparator<Entry<SegmentHeader, SegmentBody>>() {
                        @Override
						public int compare(
                            Entry<SegmentHeader, SegmentBody> o1,
                            Entry<SegmentHeader, SegmentBody> o2)
                        {
                            int ret = o1.getKey().getUniqueID().compareTo(
                                o2.getKey().getUniqueID());
                            return order == Order.REVERSE ? -ret : ret;
                        }
                    });
                LinkedHashSet<Entry<SegmentHeader, SegmentBody>>
                    orderedSet =
                    new LinkedHashSet<>();
                orderedSet.addAll(list);
                return orderedSet;
            }
            @Override
			public Set<SegmentHeader> keySet() {
                List<SegmentHeader> list = new ArrayList<>(super.keySet());
                Collections.sort(
                    list,
                    new Comparator<SegmentHeader>() {
                        @Override
						public int compare(
                            SegmentHeader o1,
                            SegmentHeader o2)
                        {
                            int ret = o1.getUniqueID().compareTo(
                                o2.getUniqueID());
                            return order == Order.REVERSE ? ret : -ret;
                        }
                    });
                LinkedHashSet<SegmentHeader>
                    orderedSet =
                    new LinkedHashSet<>();
                orderedSet.addAll(list);
                return orderedSet;
            }
        };
        for (SegmentHeader header : headers) {
            testMap.put(header, cache.get(header));
        }
        assertFalse(testMap.isEmpty(), String.format(
                "SegmentMap is empty. No segmentIds matched test parameters. "
                        + "Full segment cache: %s", headers));
        return testMap;
    }


    /**
     * Creates a rough segment map for testing purposes, containing
     * the array of column names passed in, with numValsPerCol dummy
     * values placed per column.  Populates the cells in the body with
     * numPopulatedCells dummy values placed in the first N places of the
     * values array.
     */
    private Map<SegmentHeader, SegmentBody> makeSegmentMap(
        String[] colNames, String[][] colVals,
        int numValsPerCol, int numPopulatedCells,
        boolean wildcardCols, boolean[] nullAxisFlags)
    {
        if (colVals == null) {
            colVals = dummyColumnValues(colNames.length, numValsPerCol);
        }

        Pair<SegmentHeader, SegmentBody> headerBody = makeDummyHeaderBodyPair(
            colNames,
            colVals,
            numPopulatedCells, wildcardCols, nullAxisFlags);
        Map<SegmentHeader, SegmentBody> map =
            new HashMap<>();
        map.put(headerBody.left, headerBody.right);

        return map;
    }

    private Pair<SegmentHeader, SegmentBody> makeDummyHeaderBodyPair(
        String[] colExps, String[][] colVals, int numCellVals,
        boolean wildcardCols, boolean[] nullAxisFlags)
    {
        final List<SegmentColumn> constrainedColumns =
            new ArrayList<>();

        final List<Pair<SortedSet<Comparable>, Boolean>> axes =
            new ArrayList<>();
        for (int i = 0; i < colVals.length; i++) {
            String colExp = colExps[i];
            SortedSet<Comparable> headerVals = null;
            SortedSet<Comparable> vals = toSortedSet(colVals[i]);
            if (!wildcardCols) {
                headerVals = vals;
            }
            boolean nullAxisFlag = nullAxisFlags != null && nullAxisFlags[i];
            constrainedColumns.add(
                new SegmentColumn(
                    colExp,
                    colVals[i].length,
                    headerVals));
            axes.add(Pair.of(vals, nullAxisFlag));
        }

        Object [] cells = new Object[numCellVals];
        for (int i = 0; i < numCellVals; i++) {
            cells[i] = MOCK_CELL_VALUE; // assign a non-null val
        }
        return Pair.<SegmentHeader, SegmentBody>of(
            makeDummySegmentHeader(constrainedColumns),
            new DenseObjectSegmentBody(
                cells,
                axes));
    }

    private SegmentHeader makeDummySegmentHeader(
        List<SegmentColumn> constrainedColumns)
    {
        return new SegmentHeader(
            "dummySchemaName",
            new ByteString(new byte[0]),
            "dummyCubeName",
            "dummyMeasureName",
            constrainedColumns,
            Collections.<String>emptyList(),
            "dummyFactTable",
            BitKey.Factory.makeBitKey(3),
            Collections.<SegmentColumn>emptyList());
    }

    private String [][] dummyColumnValues(int cols, int numVals) {
        String [][] dummyColVals = new String[cols][numVals];
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < numVals; j++) {
                dummyColVals[i][j] = "c" + i + "v" + j;
            }
        }
        return dummyColVals;
    }

  private static SortedSet<Comparable> toSortedSet(Comparable... comparables) {
    List<Comparable> list = asList(comparables);
    return new TreeSet<>(list);
  }

  void testRollupWithNonUniqueColumns() {
      Pair<SegmentHeader, SegmentBody> rollup =
          SegmentBuilder.rollup(
              makeSegmentMap(
                  new String[] {"col1", "col2", "col3", "col2"},
                  new String[][] {{"0.0"}, {"0.0"}, {"0.0"}, {"0.0"}},
                  10, 15, false, null),
              new HashSet<>(Arrays.asList("col1", "col2")),
              null, RolapAggregator.Sum, Datatype.NUMERIC,
              1000, 0.5);
      assertEquals(3, rollup.left.getConstrainedColumns().size());
  }

  public String removeJdkDependentStrings(String data) {
      data = data.replaceAll("(?m)^Checksum:.*(?:\\r?\\n)?","");
      data = data.replaceAll("(?m)^ID:.*(?:\\r?\\n)?","");
      return data;
  }

}
