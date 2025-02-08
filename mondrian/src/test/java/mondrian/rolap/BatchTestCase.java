/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertEqualsVerbose;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.upgradeQuery;
import static org.opencube.junit5.TestUtil.withSchema;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.CacheControl;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Locus;
import org.eclipse.daanse.olap.api.Quoting;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.opencube.junit5.TestUtil;
import org.slf4j.LoggerFactory;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.IdImpl;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapNative.Listener;
import mondrian.rolap.RolapNative.NativeEvent;
import mondrian.rolap.RolapNative.TupleEvent;
import mondrian.rolap.agg.AggregationManager;
import mondrian.rolap.agg.AndPredicate;
import mondrian.rolap.agg.CellRequest;
import mondrian.rolap.agg.GroupingSet;
import mondrian.rolap.agg.OrPredicate;
import mondrian.rolap.agg.Segment;
import mondrian.rolap.agg.SegmentWithData;
import mondrian.rolap.agg.ValueColumnPredicate;
import mondrian.rolap.cache.HardSmartCache;
import mondrian.server.ExecutionImpl;
import mondrian.server.LocusImpl;
import mondrian.test.SqlPattern;
import mondrian.util.Pair;

/**
 * To support all <code>Batch</code> related tests.
 *
 * @author Thiyagu
  * @since 06-Jun-2007
 */
public class BatchTestCase{


    protected final String tableTime = "time_by_day";
    protected final String tableProductClass = "product_class";
    protected final String tableCustomer = "customer";
    protected final String fieldYear = "the_year";
    protected final String fieldProductFamily = "product_family";
    protected final String fieldProductDepartment = "product_department";
    protected final String[] fieldValuesYear = {"1997"};
    protected final String[] fieldValuesProductFamily = {
        "Food", "Non-Consumable", "Drink"
    };
    protected final String[] fieldValueProductDepartment = {
        "Alcoholic Beverages", "Baked Goods", "Baking Goods",
         "Beverages", "Breakfast Foods", "Canned Foods",
        "Canned Products", "Carousel", "Checkout", "Dairy",
        "Deli", "Eggs", "Frozen Foods", "Health and Hygiene",
        "Household", "Meat", "Packaged Foods", "Periodicals",
        "Produce", "Seafood", "Snack Foods", "Snacks",
        "Starchy Foods"
    };
    protected final String[] fieldValuesGender = {"M", "F"};
    protected final String cubeNameSales = "Sales";
    protected final String measureUnitSales = "[Measures].[Unit Sales]";
    protected String fieldGender = "gender";

    protected BatchLoader.Batch createBatch(
        Connection connection,
        BatchLoader fbcr,
        String[] tableNames, String[] fieldNames, String[][] fieldValues,
        String cubeName, String measure)
    {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < tableNames.length; i++) {
            values.add(fieldValues[i][0]);
        }
        BatchLoader.Batch batch = fbcr.new Batch(
            createRequest(connection,
                cubeName, measure, tableNames, fieldNames,
                values.toArray(new String[values.size()])));

        addRequests(connection,
            batch, cubeName, measure, tableNames, fieldNames,
            fieldValues, new ArrayList<String>(), 0);
        return batch;
    }

    protected BatchLoader.Batch createBatch(
        Connection connection,
        BatchLoader fbcr,
        String[] tableNames, String[] fieldNames, String[][] fieldValues,
        String cubeName, String measure, CellRequestConstraint constraint)
    {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < tableNames.length; i++) {
            values.add(fieldValues[i][0]);
        }
        BatchLoader.Batch batch = fbcr.new Batch(
            createRequest(connection,
                cubeName, measure, tableNames, fieldNames,
                values.toArray(new String[values.size()]), constraint));

        addRequests(connection,
            batch, cubeName, measure, tableNames, fieldNames,
            fieldValues, new ArrayList<String>(), 0, constraint);
        return batch;
    }

    private void addRequests(Connection connection,
        BatchLoader.Batch batch,
        String cubeName,
        String measure,
        String[] tableNames,
        String[] fieldNames,
        String[][] fieldValues,
        List<String> selectedValues,
        int currPos)
    {
        if (currPos < fieldNames.length) {
            for (int j = 0; j < fieldValues[currPos].length; j++) {
                selectedValues.add(fieldValues[currPos][j]);
                addRequests(connection,
                    batch, cubeName, measure, tableNames,
                    fieldNames, fieldValues, selectedValues, currPos + 1);
                selectedValues.remove(fieldValues[currPos][j]);
            }
        } else {
            batch.add(
                createRequest(connection,
                    cubeName, measure, tableNames, fieldNames,
                    selectedValues.toArray(new String[selectedValues.size()])));
        }
    }

    private void addRequests(
        Connection connection,
        BatchLoader.Batch batch,
        String cubeName,
        String measure,
        String[] tableNames,
        String[] fieldNames,
        String[][] fieldValues,
        List<String> selectedValues,
        int currPos,
        CellRequestConstraint constraint)
    {
        if (currPos < fieldNames.length) {
            for (int j = 0; j < fieldValues[currPos].length; j++) {
                selectedValues.add(fieldValues[currPos][j]);
                addRequests(connection,
                    batch, cubeName, measure, tableNames,
                    fieldNames, fieldValues, selectedValues, currPos + 1,
                    constraint);
                selectedValues.remove(fieldValues[currPos][j]);
            }
        } else {
            batch.add(
                createRequest(connection,
                    cubeName, measure, tableNames, fieldNames,
                    selectedValues.toArray(
                        new String[selectedValues.size()]), constraint));
        }
    }

    protected GroupingSet getGroupingSet(
        Connection connection,
        final String[] tableNames,
        final String[] fieldNames,
        final String[][] fieldValues,
        final String cubeName,
        final String measure)
    {
        return LocusImpl.execute(
            ((RolapConnection)connection),
            "BatchTestCase.getGroupingSet",
            new LocusImpl.Action<GroupingSet>() {
                @Override
				public GroupingSet execute() {
                    final RolapCube cube = getCube(connection, cubeName);
                    AbstractBasicContext abc = (AbstractBasicContext) connection.getContext();
                    final BatchLoader fbcr =
                        new BatchLoader(
                            LocusImpl.peek(),
                            abc.getAggregationManager().cacheMgr,
                            cube.getStar().getSqlQueryDialect(),
                            cube);
                    BatchLoader.Batch batch =
                        createBatch(connection,
                            fbcr,
                            tableNames, fieldNames,
                            fieldValues, cubeName,
                            measure);
                    GroupingSetsCollector collector =
                        new GroupingSetsCollector(true);
                    final List<Future<Map<Segment, SegmentWithData>>>
                        segmentFutures =
                            new ArrayList<>();
                    batch.loadAggregation(collector, segmentFutures);
                    return collector.getGroupingSets().get(0);
                }
            });
    }

    /**
     * Checks that a given sequence of cell requests results in a
     * particular SQL statement being generated.
     *
     * <p>Always clears the cache before running the requests.
     *
     * <p>Runs the requests once for each SQL pattern in the current
     * dialect. If there are multiple patterns, runs the MDX query multiple
     * times, and expects to see each SQL statement appear. If there are no
     * patterns in this dialect, the test trivially succeeds.
     *
     * @param requests Sequence of cell requests
     * @param patterns Set of patterns
     */
    protected void assertRequestSql(Connection connection,
        CellRequest[] requests,
        SqlPattern[] patterns)
    {
        assertRequestSql(connection, requests, patterns, false);
    }

    /**
     * Checks that a given sequence of cell requests results in a
     * particular SQL statement being generated.
     *
     * <p>Always clears the cache before running the requests.
     *
     * <p>Runs the requests once for each SQL pattern in the current
     * dialect. If there are multiple patterns, runs the MDX query multiple
     * times, and expects to see each SQL statement appear. If there are no
     * patterns in this dialect, the test trivially succeeds.
     *
     * @param requests Sequence of cell requests
     * @param patterns Set of patterns
     * @param negative Set to false in order to 'expect' a query or
     * true to 'forbid' a query.
     */
    protected void assertRequestSql(
        Connection connection,
        CellRequest[] requests,
        SqlPattern[] patterns,
        boolean negative)
    {
        final RolapStar star = requests[0].getMeasure().getStar();
        final String cubeName = requests[0].getMeasure().getCubeName();
        final RolapCube cube = lookupCube(connection, cubeName);
        final Dialect sqlDialect = star.getSqlQueryDialect();
        DatabaseProduct d = getDatabaseProduct(sqlDialect.getDialectName());
        SqlPattern sqlPattern = SqlPattern.getPattern(d, patterns);
        if (d == DatabaseProduct.UNKNOWN) {
            // If the dialect is not one in the pattern set, do not run the
            // test. We do not print any warning message.
            return;
        }

        boolean patternFound = false;
        for (SqlPattern pattern : patterns) {
            if (!pattern.hasDatabaseProduct(d)) {
                continue;
            }

            patternFound = true;

            clearCache(connection, cube);

            String sql = sqlPattern.getSql();
            String trigger = sqlPattern.getTriggerSql();
            switch (d) {
            case ORACLE:
                sql = sql.replaceAll(" =as= ", " ");
                trigger = trigger.replaceAll(" =as= ", " ");
                break;
            case TERADATA:
                sql = sql.replaceAll(" =as= ", " as ");
                trigger = trigger.replaceAll(" =as= ", " as ");
                break;
            }

            // Create a dummy DataSource which will throw a 'bomb' if it is
            // asked to execute a particular SQL statement, but will otherwise
            // behave exactly the same as the current DataSource.
            RolapUtil.setHook(new TriggerHook(trigger));
            Bomb bomb;
            final ExecutionImpl execution =
                new ExecutionImpl(
                    ((Connection) connection).getInternalStatement(),
                    Optional.of(Duration.ofMillis(1000)));
            AbstractBasicContext abc = (AbstractBasicContext) execution.getMondrianStatement()
                    .getMondrianConnection()
                    .getContext();
            final AggregationManager aggMgr =
                abc.getAggregationManager();
            final Locus locus =
                new LocusImpl(
                    execution,
                    "BatchTestCase",
                    "BatchTestCase");
            try {
                FastBatchingCellReader fbcr =
                    new FastBatchingCellReader(
                        execution, getCube(connection, cubeName), aggMgr);
                for (CellRequest request : requests) {
                    fbcr.recordCellRequest(request);
                }
                // The FBCR will presume there is a current Locus in the stack,
                // so let's create a mock one.
                LocusImpl.push(locus);
                fbcr.loadAggregations();
                bomb = null;
            } catch (Bomb e) {
                bomb = e;
            } catch (RuntimeException e) {
                // Walk up the exception tree and see if the root cause
                // was a SQL bomb.
                bomb = Util.getMatchingCause(e, Bomb.class);
                if (bomb == null) {
                    throw e;
                }
            } finally {
                RolapUtil.setHook(null);
                LocusImpl.pop(locus);
            }
            if (!negative && bomb == null) {
                fail("expected query [" + sql + "] did not occur");
            } else if (negative && bomb != null) {
                fail("forbidden query [" + sql + "] detected");
            }
            assertEqualsVerbose(
                replaceQuotes(sql),
                replaceQuotes(bomb.sql));
        }

        // Print warning message that no pattern was specified for the current
        // dialect.
        if (!patternFound) {
            String warnDialect =
                connection.getContext().getConfig().warnIfNoPatternForDialect();

            if (warnDialect.equals(d.toString())) {
                System.out.println(
                    "[No expected SQL statements found for dialect \""
                    + sqlDialect.toString()
                    + "\" and test not run]");
            }
        }
    }

    private RolapCube lookupCube(Connection connection, String cubeName) {
        for (Cube cube : connection.getCatalog().getCubes()) {
            if (cube.getName().equals(cubeName)) {
                return (RolapCube) cube;
            }
        }
        return null;
    }


    /**
     * Checks that a given MDX query results in a particular SQL statement
     * being generated.
     *
     * @param connection Connection
     * @param mdxQuery MDX query
     * @param patterns Set of patterns for expected SQL statements
     */
    protected void assertQuerySql(
        Connection connection,
        String mdxQuery,
        SqlPattern[] patterns)
    {
        assertQuerySqlOrNot(
            connection, mdxQuery, patterns, false, false, true);
    }

    /**
     * Checks that a given MDX query does not result in a particular SQL
     * statement being generated.
     *
     * @param mdxQuery MDX query
     * @param patterns Set of patterns for expected SQL statements
     */
    protected void assertNoQuerySql(Connection connection,
        String mdxQuery,
        SqlPattern[] patterns)
    {
        assertQuerySqlOrNot(
                connection, mdxQuery, patterns, true, false, true);
    }

    /**
     * Checks that a given MDX query results in a particular SQL statement
     * being generated.
     *
     * @param mdxQuery MDX query
     * @param patterns Set of patterns, one for each dialect.
     * @param clearCache whether to clear cache before running the query
     */
    protected void assertQuerySql(
        Connection connection,
        String mdxQuery,
        SqlPattern[] patterns,
        boolean clearCache)
    {
        assertQuerySqlOrNot(
            connection, mdxQuery, patterns, false, false, clearCache);
    }

    /**
     * During MDX query parse and execution, checks that the query results
     * (or does not result) in a particular SQL statement being generated.
     *
     * <p>Parses and executes the MDX query once for each SQL
     * pattern in the current dialect. If there are multiple patterns, runs the
     * MDX query multiple times, and expects to see each SQL statement appear.
     * If there are no patterns in this dialect, the test trivially succeeds.
     *
     * @param connection Connection
     * @param mdxQuery MDX query
     * @param patterns Set of patterns
     * @param negative false to assert if SQL is generated;
     *                 true to assert if SQL is NOT generated
     * @param bypassSchemaCache whether to grab a new connection and bypass the
     *        schema cache before parsing the MDX query
     * @param clearCache whether to clear cache before executing the MDX query
     */
    protected void assertQuerySqlOrNot(
        Connection connection,
        String mdxQuery,
        SqlPattern[] patterns,
        boolean negative,
        boolean bypassSchemaCache,
        boolean clearCache)
    {

        mdxQuery = upgradeQuery(mdxQuery);

        // Run the test once for each pattern in this dialect.
        // (We could optimize and run it once, collecting multiple queries, and
        // comparing all queries at the end.)
        Dialect dialect = getDialect(connection);
        DatabaseProduct d = getDatabaseProduct(dialect.getDialectName());
        boolean patternFound = false;
        for (SqlPattern sqlPattern : patterns) {
            if (!sqlPattern.hasDatabaseProduct(d)) {
                // If the dialect is not one in the pattern set, skip the
                // test. If in the end no pattern is located, print a warning
                // message if required.
                continue;
            }

            patternFound = true;

            String sql = sqlPattern.getSql();
            String trigger = sqlPattern.getTriggerSql();

            sql = dialectize(d, sql);
            trigger = dialectize(d, trigger);

            // Create a dummy DataSource which will throw a 'bomb' if it is
            // asked to execute a particular SQL statement, but will otherwise
            // behave exactly the same as the current DataSource.
            final TriggerHook hook = new TriggerHook(trigger);
            RolapUtil.setHook(hook);
            Bomb bomb = null;
            try {
                if (bypassSchemaCache) {
                    //connection =
                    //    testContext.withSchemaPool(false).getConnection();
                }
                final Query query = connection.parseQuery(mdxQuery);
                if (clearCache) {
                    clearCache(connection, (RolapCube)query.getCube());
                }
                final Result result = connection.execute(query);
//                discard(result);
                bomb = null;
            } catch (Bomb e) {
                bomb = e;
            } catch (RuntimeException e) {
                // Walk up the exception tree and see if the root cause
                // was a SQL bomb.
                bomb = Util.getMatchingCause(e, Bomb.class);
                if (bomb == null) {
                    throw e;
                }
            } finally {
                RolapUtil.setHook(null);
            }
            if (negative) {
                if (bomb != null || hook.foundMatch()) {
                    fail("forbidden query [" + sql + "] detected");
                }
            } else {
                if (bomb == null && !hook.foundMatch()) {
                    fail("expected query [" + sql + "] did not occur");
                }
                if (bomb != null) {
                    assertEquals(
                        replaceQuotes(
                            sql.replaceAll("\r\n", "\n")),
                        replaceQuotes(
                            bomb.sql.replaceAll("\r\n", "\n")));
                }
            }
        }

        // Print warning message that no pattern was specified for the current
        // dialect.
        if (!patternFound) {
            String warnDialect =
                connection.getContext().getConfig().warnIfNoPatternForDialect();

            if (warnDialect.equals(d.toString())) {
                System.out.println(
                    "[No expected SQL statements found for dialect \""
                    + dialect.toString()
                    + "\" and test not run]");
            }
        }
    }

    protected SqlPattern[] sqlPattern(DatabaseProduct db, String sql) {
        return new SqlPattern[]{new SqlPattern(db, sql, sql.length())};
    }

    protected SqlPattern[] mysqlPattern(String sql) {
        return sqlPattern(DatabaseProduct.MYSQL, sql);
    }

    protected String dialectize(DatabaseProduct d, String sql) {
        sql = sql.replaceAll("\r\n", "\n");
        switch (d) {
        case ORACLE:
            return sql.replaceAll(" =as= ", " ");
        case GREENPLUM:
        case POSTGRES:
        case TERADATA:
            return sql.replaceAll(" =as= ", " as ");
        case DERBY:
            return sql.replaceAll("`", "\"");
        case ACCESS:
            return sql.replaceAll(
                "ISNULL\\(([^)]*)\\)",
                "Iif($1 IS NULL, 1, 0)");
        default:
            return sql;
        }
    }

    private void clearCache(Connection connection,  RolapCube cube) {
        // Clear the cache for the Sales cube, so the query runs as if
        // for the first time. (TODO: Cleaner way to do this.)
        final Cube salesCube =
                connection.getCatalog().lookupCube("Sales", false);
        if (salesCube != null) {
            RolapHierarchy hierarchy =
                    (RolapHierarchy) salesCube.lookupHierarchy(
                            new IdImpl.NameSegmentImpl("Store", Quoting.UNQUOTED),
                            false);
            if (hierarchy != null) {
                SmartMemberReader memberReader =
                    (SmartMemberReader) hierarchy.getMemberReader();
                MemberCacheHelper cacheHelper = memberReader.cacheHelper;
                cacheHelper.mapLevelToMembers.cache.clear();
                cacheHelper.mapMemberToChildren.cache.clear();
            }
        }
        // Flush the cache, to ensure that the query gets executed.
        cube.clearCachedAggregations(true);

        CacheControl cacheControl = connection.getCacheControl(null);
        final CacheControl.CellRegion measuresRegion =
            cacheControl.createMeasuresRegion(cube);
        cacheControl.flush(measuresRegion);
        waitForFlush(cacheControl, measuresRegion, cube.getName());
    }

    private void waitForFlush(
        final CacheControl cacheControl,
        final CacheControl.CellRegion measuresRegion,
        final String cubeName)
    {
        int i = 100;
        while (true) {
            try {
                Thread.sleep(i);
            } catch (InterruptedException e) {
                fail(e.getMessage());
            }
            String cacheState = getCacheState(cacheControl, measuresRegion);
            if (regionIsEmpty(cacheState, cubeName)) {
                break;
            }
            i *= 2;
            if (i > 6400) {
                fail(
                    "Cache didn't flush in sufficient time\nCache Was: \n"
                    + cacheState);
                break;
            }
        }
    }

    private String getCacheState(
        final CacheControl cacheControl,
        final CacheControl.CellRegion measuresRegion)
    {
        StringWriter out = new StringWriter();
        cacheControl.printCacheState(new PrintWriter(out), measuresRegion);
        return out.toString();
    }

    private boolean regionIsEmpty(
        final String cacheState, final String cubeName)
    {
        return !cacheState.contains("Cube:[" + cubeName + "]");
    }

    private static String replaceQuotes(String s) {
        s = s.replace('`', '\"');
        s = s.replace('\'', '\"');
        return s;
    }

    protected CellRequest createRequest(Connection connection,
        final String cube, final String measure,
        final String table, final String column, final String value)
    {
        return createRequest(connection,
            cube, measure,
            new String[]{table}, new String[]{column}, new String[]{value});
    }

    protected CellRequest createRequest(Connection connection,
        final String cube, final String measureName,
        final String[] tables, final String[] columns, final String[] values)
    {
        RolapStar.Measure starMeasure = getMeasure(connection, cube, measureName);
        CellRequest request = new CellRequest(starMeasure, false, false);
        final RolapStar star = starMeasure.getStar();
        for (int i = 0; i < tables.length; i++) {
            String table = tables[i];
            if (table != null && table.length() > 0) {
                String column = columns[i];
                String value = values[i];
                final RolapStar.Column storeTypeColumn =
                    star.lookupColumn(table, column);
                request.addConstrainedColumn(
                    storeTypeColumn,
                    new ValueColumnPredicate(storeTypeColumn, value));
            }
        }
        return request;
    }

    protected CellRequest createRequest(Connection connection,
        final String cube, final String measure,
        final String table, final String column, final String value,
        CellRequestConstraint aggConstraint)
    {
        return createRequest(connection,
            cube, measure,
            new String[]{table}, new String[]{column}, new String[]{value},
            aggConstraint);
    }

    protected CellRequest createRequest(Connection connection,
        final String cube, final String measureName,
        final String[] tables, final String[] columns, final String[] values,
        CellRequestConstraint aggConstraint)
    {
        RolapStar.Measure starMeasure = getMeasure(connection, cube, measureName);

        CellRequest request =
            createRequest(connection, cube, measureName, tables, columns, values);
        final RolapStar star = starMeasure.getStar();

        request.addAggregateList(
            aggConstraint.getBitKey(star),
            aggConstraint.toPredicate(star));
        return request;
    }

    protected void updateSchemaIfNeed(Context context, String currentTestCaseName){
        Optional<Function<CatalogMapping, PojoMappingModifier>> oModifier = getModifier(currentTestCaseName);
        if (oModifier.isPresent()) {
            withSchema(context, oModifier.get());
        }
    }

    protected Optional<Function<CatalogMapping, PojoMappingModifier>> getModifier(String currentTestCaseName) {
        return Optional.empty();
    }

    static CellRequestConstraint makeConstraintYearQuarterMonth(
        List<String[]> values)
    {
        String[] aggConstraintTables =
            new String[] { "time_by_day", "time_by_day", "time_by_day" };
        String[] aggConstraintColumns =
            new String[] { "the_year", "quarter", "month_of_year" };
        List<String[]> aggConstraintValues = new ArrayList<>();

        for (String[] value : values) {
            assert value.length == 3;
            aggConstraintValues.add(value);
        }

        return new CellRequestConstraint(
            aggConstraintTables, aggConstraintColumns, aggConstraintValues);
    }

    static CellRequestConstraint makeConstraintCountryState(
        List<String[]> values)
    {
        String[] aggConstraintTables =
            new String[] { "store", "store"};
        String[] aggConstraintColumns =
            new String[] { "store_country", "store_state"};
        List<String[]> aggConstraintValues = new ArrayList<>();

        for (String[] value : values) {
            assert value.length == 2;
            aggConstraintValues.add(value);
        }

        return new CellRequestConstraint(
            aggConstraintTables, aggConstraintColumns, aggConstraintValues);
    }

    static CellRequestConstraint makeConstraintProductFamilyDepartment(
        List<String[]> values)
    {
        String[] aggConstraintTables =
            new String[] { "product_class", "product_class"};
        String[] aggConstraintColumns =
            new String[] { "product_family", "product_department"};
        List<String[]> aggConstraintValues = new ArrayList<>();

        for (String[] value : values) {
            assert value.length == 2;
            aggConstraintValues.add(value);
        }

        return new CellRequestConstraint(
            aggConstraintTables, aggConstraintColumns, aggConstraintValues);
    }

    void clearAndHardenCache(MemberCacheHelper helper) {
        helper.mapLevelToMembers.setCache(
            new HardSmartCache<Pair<RolapLevel, Object>, List<RolapMember>>());
        helper.mapMemberToChildren.setCache(
            new HardSmartCache<Pair<RolapMember, Object>, List<RolapMember>>());
        helper.mapKeyToMember.clear();
        helper.mapParentToNamedChildren.setCache(
            new HardSmartCache<RolapMember, Collection<RolapMember>>());
    }

    protected RolapStar.Measure getMeasure(Connection connection, String cube, String measureName) {
        //final Connection connection = getFoodMartConnection(context);
        final boolean fail = true;
        Cube salesCube = connection.getCatalog().lookupCube(cube, fail);
        Member measure = salesCube.getCatalogReader(null).getMemberByUniqueName(
            Util.parseIdentifier(measureName), fail);
        return RolapStar.getStarMeasure(measure);
    }

    protected RolapCube getCube(Connection connection, final String cube) {
        final boolean fail = true;
        return (RolapCube) connection.getCatalog().lookupCube(cube, fail);
    }

    /**
     * Make sure the mdx runs correctly and not in native mode.
     *
     * @param rowCount number of rows returned
     * @param mdx      query
     */
    protected void checkNotNative(Context context, int rowCount, String mdx) {
        checkNotNative(context, rowCount, mdx, null);
    }

    /**
     * Makes sure the MDX runs correctly and not in native mode.
     *
     * @param rowCount       Number of rows returned
     * @param mdx            Query
     * @param expectedResult Expected result string
     */
    protected void checkNotNative(Context context,
        int rowCount,
        String mdx,
        String expectedResult)
    {
        context.getConnectionWithDefaultRole().getCacheControl(null).flushSchemaCache();
        //Connection con =
        //    getTestContext().withSchemaPool(false).getConnection();
        Connection con = context.getConnectionWithDefaultRole();
        RolapNativeRegistry reg = getRegistry(con);
        reg.setListener(
            new Listener() {
                @Override
				public void foundEvaluator(NativeEvent e) {
                    fail("should not be executed native");
                }

                @Override
				public void foundInCache(TupleEvent e) {
                }

                @Override
				public void executingSql(TupleEvent e) {
                }
            });

        TestCase c = new TestCase(con, 0, rowCount, mdx);
        Result result = c.run();

        if (expectedResult != null) {
            String nonNativeResult = TestUtil.toString(result);
            if (!nonNativeResult.equals(expectedResult)) {
                assertEqualsVerbose(
                    expectedResult,
                    nonNativeResult,
                    false,
                    "Non Native implementation returned different result than "
                    + "expected; MDX=" + mdx);
            }
        }
    }

    RolapNativeRegistry getRegistry(Connection connection) {
        RolapCube cube =
            (RolapCube) connection.getCatalog().lookupCube("Sales", true);
        RolapCatalogReader schemaReader =
            (RolapCatalogReader) cube.getCatalogReader();
        return schemaReader.getCatalog().getNativeRegistry();
    }

    /**
     * Runs a query twice, with native crossjoin optimization enabled and
     * disabled. If both results are equal, its considered correct.
     *
     * @param resultLimit Maximum result size of all the MDX operations in this
     *                    query. This might be hard to estimate as it is usually
     *                    larger than the rowCount of the final result. Setting
     *                    it to 0 will cause this limit to be ignored.
     * @param rowCount    Number of rows returned
     * @param mdx         Query
     */
    protected void checkNative(Context context,
        int resultLimit, int rowCount, String mdx)
    {
        checkNative(context, resultLimit, rowCount, mdx, null, false);
    }

    /**
     * Runs a query twice, with native crossjoin optimization enabled and
     * disabled. If both results are equal,and both aggree with the expected
     * result, it is considered correct.
     *
     * <p>Optionally the query can be run with
     * fresh connection. This is useful if the test case sets its certain
     * mondrian properties, e.g. native properties like:
     * mondrian.native.filter.enable
     *
     * @param resultLimit     Maximum result size of all the MDX operations in
     *                        this query. This might be hard to estimate as it
     *                        is usually larger than the rowCount of the final
     *                        result. Setting it to 0 will cause this limit to
     *                        be ignored.
     * @param rowCount        Number of rows returned. (That is, the number
     *                        of positions on the last axis of the query.)
     * @param mdx             Query
     * @param expectedResult  Expected result string
     * @param freshConnection Whether fresh connection is required
     */
    protected void checkNative(
        Context context,
        int resultLimit,
        int rowCount,
        String mdx,
        String expectedResult,
        boolean freshConnection)
    {
        // Don't run the test if we're testing expression dependencies.
        // Expression dependencies cause spurious interval calls to
        // 'level.getMembers()' which create false negatives in this test.
        if (context.getConfig().testExpDependencies() > 0) {
            return;
        }

        context.getConnectionWithDefaultRole().getCacheControl(null).flushSchemaCache();
        try {
            LoggerFactory.getLogger(getClass()).debug("*** Native: " + mdx);
            boolean reuseConnection = !freshConnection;
            //Connection con =
            //    getTestContext()
            //        .withSchemaPool(reuseConnection)
            //        .getConnection();
            Connection con = context.getConnectionWithDefaultRole();
            RolapNativeRegistry reg = getRegistry(con);
            reg.useHardCache(true);
            TestListener listener = new TestListener();
            reg.setListener(listener);
            reg.setEnabled(true);
            TestCase c = new TestCase(con, resultLimit, rowCount, mdx);
            Result result = c.run();
            String nativeResult = TestUtil.toString(result);
            if (!listener.isFoundEvaluator()) {
                fail("expected native execution of " + mdx);
            }
            if (!listener.isExecuteSql()) {
                fail("cache is empty: expected SQL query to be executed");
            }
            if (SystemWideProperties.instance().EnableRolapCubeMemberCache)
            {
                // run once more to make sure that the result comes from cache
                // now
                listener.setExecuteSql(false);
                c.run();
                if (listener.isExecuteSql()) {
                    fail("expected result from cache when query runs twice");
                }
            }
            con.close();

            LoggerFactory.getLogger(getClass()).debug("*** Interpreter: " + mdx);

            context.getConnectionWithDefaultRole().getCacheControl(null).flushSchemaCache();
            //con = getTestContext().withSchemaPool(false).getConnection();
            con = context.getConnectionWithDefaultRole();
            reg = getRegistry(con);
            listener.setFoundEvaluator(false);
            reg.setListener(listener);
            // disable RolapNativeSet
            reg.setEnabled(false);
            result = executeQuery(mdx, con);
            String interpretedResult = TestUtil.toString(result);
            if (listener.isFoundEvaluator()) {
                fail("did not expect native executions of " + mdx);
            }

            if (expectedResult != null) {
                assertEqualsVerbose(
                    expectedResult,
                    nativeResult,
                    false,
                    "Native implementation returned different result than "
                    + "expected; MDX=" + mdx);
                assertEqualsVerbose(
                    expectedResult,
                    interpretedResult,
                    false,
                    "Interpreter implementation returned different result than "
                    + "expected; MDX=" + mdx);
            }

            if (!nativeResult.equals(interpretedResult)) {
                assertEqualsVerbose(
                    interpretedResult,
                    nativeResult,
                    false,
                    "Native implementation returned different result than "
                    + "interpreter; MDX=" + mdx);
            }
        } finally {
            Connection con = context.getConnectionWithDefaultRole();
            RolapNativeRegistry reg = getRegistry(con);
            reg.setEnabled(true);
            reg.useHardCache(false);
        }
    }

    private static int getRowCount(Result result) {
        return result.getAxes()[result.getAxes().length - 1]
            .getPositions().size();
    }

    public Result executeQuery(String mdx, Connection connection) {
    	Query query = connection.parseQuery(mdx);
        query.setResultStyle(ResultStyle.LIST);
        return connection.execute(query);
    }

    /**
     * Convenience method for debugging; please do not delete.
     */
    public void assertNotNative(Context context, String mdx) {
        new BatchTestCase().checkNotNative(context, mdx, null);
    }

    public static void checkNotNative(Context context, String mdx, Result expectedResult) {
        BatchTestCase test = new BatchTestCase();
        test.checkNotNative(context,
                getRowCount(expectedResult),
                mdx,
                TestUtil.toString(expectedResult));
    }

    public static void checkNative(Context context, String mdx, Result expectedResult) {
        BatchTestCase test = new BatchTestCase();
        test.checkNative(context,
                0,
                getRowCount(expectedResult),
                mdx,
                TestUtil.toString(expectedResult),
                true);
    }
    /**
     * Convenience method for debugging; please do not delete.
     */
    public void assertNative(Context context, String mdx) {
        new BatchTestCase().checkNative(context,0, 0, mdx, null, true);
    }

    /**
     * Runs an MDX query with a predefined resultLimit and checks the number of
     * positions of the row axis. The reduced resultLimit ensures that the
     * optimization is present.
     */
    protected class TestCase {
        /**
         * Maximum number of rows to be read from SQL. If more than this number
         * of rows are read, the test will fail.
         */
        final int resultLimit;

        /**
         * MDX query to execute.
         */
        final String query;

        /**
         * Number of positions we expect on rows axis of result.
         */
        final int rowCount;

        /**
         * Mondrian connection.
         */
        final Connection con;

        public TestCase(
            Connection con, int resultLimit, int rowCount, String query)
        {
            this.con = con;
            this.resultLimit = resultLimit;
            this.rowCount = rowCount;
            this.query = query;
        }

        protected Result run() {
            con.getCacheControl(null).flushSchemaCache();
            Integer monLimit =
                SystemWideProperties.instance().ResultLimit;
            int oldLimit = monLimit;
            try {
                SystemWideProperties.instance().ResultLimit = this.resultLimit;
                Result result = executeQuery(query, con);

                // Check the number of positions on the last axis, which is
                // the ROWS axis in a 2 axis query.
                int numAxes = result.getAxes().length;
                Axis a = result.getAxes()[numAxes - 1];
                final int positionCount = a.getPositions().size();
                assertEquals(rowCount, positionCount);
                return result;
            } finally {
                SystemWideProperties.instance().ResultLimit = oldLimit;
            }
        }
    }

    /**
     * Fake exception to interrupt the test when we see the desired query.
     * It is an {@link Error} because we need it to be unchecked
     * ({@link Exception} is checked), and we don't want handlers to handle
     * it.
     */
    static class Bomb extends Error {
        final String sql;

        Bomb(final String sql) {
            this.sql = sql;
        }
    }

    private static class TriggerHook implements RolapUtil.ExecuteQueryHook {
        private final String trigger;
        private boolean foundMatch = false;

        public TriggerHook(String trigger) {
            this.trigger =
                trigger
                    .replaceAll("\r\n", "")
                    .replaceAll("\r", "")
                    .replaceAll("\n", "");
        }

        private boolean matchTrigger(String sql) {
            if (trigger == null) {
                return true;
            }
            // Cleanup the endlines.
            sql =
                sql
                    .replaceAll("\r\n", "")
                    .replaceAll("\r", "")
                    .replaceAll("\n", "");
            // different versions of mysql drivers use different quoting, so
            // ignore quotes
            String s = replaceQuotes(sql);
            String t = replaceQuotes(trigger);
            if (s.startsWith(t) && !foundMatch) {
                foundMatch = true;
            }
            return s.startsWith(t);
        }

        @Override
		public void onExecuteQuery(String sql) {
            if (matchTrigger(sql)) {
                throw new Bomb(sql);
            }
        }

        public boolean foundMatch() {
            return foundMatch;
        }
    }

    static class CellRequestConstraint {
        String[] tables;
        String[] columns;
        List<String[]> valueList;

        CellRequestConstraint(
            String[] tables,
            String[] columns,
            List<String[]> valueList)
        {
            this.tables = tables;
            this.columns = columns;
            this.valueList = valueList;
        }

        BitKey getBitKey(RolapStar star) {
            return star.getBitKey(tables, columns);
        }

        StarPredicate toPredicate(RolapStar star) {
            RolapStar.Column starColumn[] = new RolapStar.Column[tables.length];
            for (int i = 0; i < tables.length; i++) {
                String table = tables[i];
                String column = columns[i];
                starColumn[i] = star.lookupColumn(table, column);
            }

            List<StarPredicate> orPredList = new ArrayList<>();
            for (String[] values : valueList) {
                assert (values.length == tables.length);
                List<StarPredicate> andPredList =
                    new ArrayList<>();
                for (int i = 0; i < values.length; i++) {
                    andPredList.add(
                        new ValueColumnPredicate(starColumn[i], values[i]));
                }
                final StarPredicate predicate =
                    andPredList.size() == 1
                        ? andPredList.get(0)
                        : new AndPredicate(andPredList);
                orPredList.add(predicate);
            }

            return orPredList.size() == 1
                ? orPredList.get(0)
                : new OrPredicate(orPredList);
        }
    }

    /**
     * Gets notified on various test events:
     * <ul>
     * <li>when a matching native evaluator was found
     * <li>when SQL is executed
     * <li>when result is found in the cache
     * </ul>
     */
    static class TestListener implements Listener {
        boolean foundEvaluator;
        boolean foundInCache;
        boolean executeSql;

        boolean isExecuteSql() {
            return executeSql;
        }

        void setExecuteSql(boolean excecuteSql) {
            this.executeSql = excecuteSql;
        }

        boolean isFoundEvaluator() {
            return foundEvaluator;
        }

        void setFoundEvaluator(boolean foundEvaluator) {
            this.foundEvaluator = foundEvaluator;
        }

        boolean isFoundInCache() {
            return foundInCache;
        }

        void setFoundInCache(boolean foundInCache) {
            this.foundInCache = foundInCache;
        }

        @Override
		public void foundEvaluator(NativeEvent e) {
            this.foundEvaluator = true;
        }

        @Override
		public void foundInCache(TupleEvent e) {
            this.foundInCache = true;
        }

        @Override
		public void executingSql(TupleEvent e) {
            this.executeSql = true;
        }
    }

}

// End BatchTestCase.java

