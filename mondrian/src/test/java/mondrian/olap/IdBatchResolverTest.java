/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2006-2017 Hitachi Vantara and others
// All Rights Reserved.
 */
package mondrian.olap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.MatchType;
import org.eclipse.daanse.olap.api.NameSegment;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.SubtotalVisibility;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.AxisOrdinal;
import org.eclipse.daanse.olap.api.query.component.CellProperty;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Subcube;
import org.eclipse.daanse.olap.query.base.QueryProviderImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.parser.JavaccParserValidatorImpl;
import mondrian.parser.MdxParserValidator;
import mondrian.rolap.RolapConnection;
import mondrian.server.ExecutionImpl;
import mondrian.server.LocusImpl;


class IdBatchResolverTest  {

	@Mock
     QueryImpl query;

    @Captor
     ArgumentCaptor<List<NameSegment>> childNames;

    @Captor
     ArgumentCaptor<Member> parentMember;

    @Captor
     ArgumentCaptor<MatchType> matchType;



    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

    }
    @AfterEach
    public void  afterEach(){
    	SystemWideProperties.instance().populateInitial();
    }
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testSimpleEnum(Context context) {
        assertContains(
            "Resolved map omitted one or more members",
            batchResolve(context,
                "SELECT "
                + "{[Product].[Food].[Dairy],"
                + "[Product].[Food].[Deli],"
                + "[Product].[Food].[Eggs],"
                + "[Product].[Food].[Produce],"
                + "[Product].[Food].[Starchy Foods]}"
                + "on 0 FROM SALES"),
            list(
                "[Product].[Food].[Dairy]",
                "[Product].[Food].[Deli]",
                "[Product].[Food].[Eggs]",
                "[Product].[Food].[Produce]",
                "[Product].[Food].[Starchy Foods]"));

        // verify lookupMemberChildrenByNames is called as expected with
        // batched children's names.
        verify(
            query.getSchemaReader(true), times(2))
            .lookupMemberChildrenByNames(
                parentMember.capture(),
                childNames.capture(),
                matchType.capture());

        assertEquals(
            "[Product].[All Products]",
            parentMember.getAllValues().get(0).getUniqueName());
        assertTrue(childNames.getAllValues().get(0).size() == 1);
        assertEquals(
            "Food",
            childNames.getAllValues().get(0).get(0).getName());

        assertEquals(
            "[Product].[Food]",
            parentMember.getAllValues().get(1).getUniqueName());
        assertTrue(childNames.getAllValues().get(1).size() == 5);

        assertEquals(
            "[[Dairy], [Deli], [Eggs], [Produce], [Starchy Foods]]",
            sortedNames(childNames.getAllValues().get(1)));
    }
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testCalcMemsNotResolved(Context context) {
        assertFalse(
            batchResolve(context,
                "with member time.foo as '1' member time.bar as '2' "
                + " select "
                + " {[Time].[foo], [Time].[bar], "
                + "  [Time].[1997],"
                + "  [Time].[1997].[Q1], [Time].[1997].[Q2]} "
                + " on 0 from sales ")
                .removeAll(list("[Time].[foo]", "[Time].[bar]")),
                "Resolved map should not contain calc members");
        // .removeAll will only return true if the set has changed, i.e. if
        // one ore more of the members were present.
    }
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testLevelReferenceHandled(Context context) {
        // make sure ["Week", 1997] don't get batched as children of
        // [Time.Weekly].[All]
        batchResolve(context,
            "with member Gender.levelRef as "
            + "'Sum(Descendants([Time.Weekly].CurrentMember, [Time.Weekly].Week))' "
            + "select Gender.levelRef on 0 from sales where [Time.Weekly].[1997]");
        verify(
            query.getSchemaReader(true), times(1))
            .lookupMemberChildrenByNames(
                parentMember.capture(),
                childNames.capture(),
                matchType.capture());
        assertEquals(
            "[Time.Weekly].[All Time.Weeklys]",
            parentMember.getAllValues().get(0).getUniqueName());
        assertEquals(
            "[[1997]]",
            sortedNames(childNames.getAllValues().get(0)));
    }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testPhysMemsResolvedWhenCalcsMixedIn(Context context) {
        assertContains(
            "Resolved map omitted one or more members",
            batchResolve(context,
                "with member time.foo as '1' member time.bar as '2' "
                + " select "
                + " {[Time].[foo], [Time].[bar], "
                + "  [Time].[1997],"
                + "  [Time].[1997].[Q1], [Time].[1997].[Q2]} "
                + " on 0 from sales "),
            list(
                "[Time].[1997]",
                "[Time].[1997].[Q1]",
                "[Time].[1997].[Q2]"));
        verify(
            query.getSchemaReader(true), times(1))
            .lookupMemberChildrenByNames(
                parentMember.capture(),
                childNames.capture(),
                matchType.capture());
        assertEquals(
            "[Time].[1997]",
            parentMember.getAllValues().get(0).getUniqueName());
        assertTrue(childNames.getAllValues().get(0).size() == 2);
        assertEquals(
            "[[Q1], [Q2]]",
            sortedNames(childNames.getAllValues().get(0)));
    }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAnalyzerFilterMdx(Context context) {
        assertContains(
            "Resolved map omitted one or more members",
            batchResolve(context,
                "WITH\n"
                + "SET [*NATIVE_CJ_SET] AS 'NONEMPTYCROSSJOIN([*BASE_MEMBERS__Promotions_],[*BASE_MEMBERS__Store_])'\n"
                + "SET [*BASE_MEMBERS__Store_] AS '{[Store].[USA].[WA].[Bellingham],[Store].[USA].[CA].[Beverly Hills],[Store].[USA].[WA].[Bremerton],[Store].[USA].[CA].[Los Angeles]}'\n"
                + "SET [*SORTED_COL_AXIS] AS 'ORDER([*CJ_COL_AXIS],[Promotions].CURRENTMEMBER.ORDERKEY,BASC)'\n"
                + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
                + "SET [*CJ_ROW_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Store].CURRENTMEMBER)})'\n"
                + "SET [*BASE_MEMBERS__Promotions_] AS '{[Promotions].[Bag Stuffers],[Promotions].[Best Savings],[Promotions].[Big Promo],[Promotions].[Big Time Discounts],[Promotions].[Big Time Savings],[Promotions].[Bye Bye Baby]}'\n"
                + "SET [*SORTED_ROW_AXIS] AS 'ORDER([*CJ_ROW_AXIS],[Store].CURRENTMEMBER.ORDERKEY,BASC,ANCESTOR([Store].CURRENTMEMBER,[Store].[Store State]).ORDERKEY,BASC)'\n"
                + "SET [*CJ_COL_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Promotions].CURRENTMEMBER)})'\n"
                + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n"
                + "SELECT\n"
                + "CROSSJOIN([*SORTED_COL_AXIS],[*BASE_MEMBERS__Measures_]) ON COLUMNS\n"
                + ",NON EMPTY\n"
                + "[*SORTED_ROW_AXIS] ON ROWS\n"
                + "FROM [Sales]"),
            list(
                "[Store].[USA].[WA].[Bellingham]",
                "[Store].[USA].[CA].[Beverly Hills]",
                "[Store].[USA].[WA].[Bremerton]",
                "[Store].[USA].[CA].[Los Angeles]",
                "[Promotions].[Bag Stuffers]", "[Promotions].[Best Savings]",
                "[Promotions].[Big Promo]", "[Promotions].[Big Time Discounts]",
                "[Promotions].[Big Time Savings]",
                "[Promotions].[Bye Bye Baby]"));

        verify(
            query.getSchemaReader(true), times(5))
            .lookupMemberChildrenByNames(
                parentMember.capture(),
                childNames.capture(),
                matchType.capture());

        assertEquals(
            "[Promotions].[All Promotions]",
            parentMember.getAllValues().get(0).getUniqueName());
        assertTrue(childNames.getAllValues().get(0).size() == 6);
        assertEquals(
            "[[Bag Stuffers], [Best Savings], [Big Promo], "
            + "[Big Time Discounts], [Big Time Savings], [Bye Bye Baby]]",
            sortedNames(childNames.getAllValues().get(0)));

        assertEquals(
            "[Store].[USA].[CA]",
            parentMember.getAllValues().get(3).getUniqueName());
        assertTrue(childNames.getAllValues().get(3).size() == 2);
        assertEquals(
            "[[Beverly Hills], [Los Angeles]]",
            sortedNames(childNames.getAllValues().get(3)));
    }
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testSetWithNullMember(Context context) {
        assertContains(
            "Resolved map omitted one or more members",
            batchResolve(context,
                "WITH\n"
                + "SET [*NATIVE_CJ_SET] AS 'FILTER([*BASE_MEMBERS__Store Size in SQFT_], NOT ISEMPTY ([Measures].[Unit Sales]))'\n"
                + "SET [*BASE_MEMBERS__Store Size in SQFT_] AS '{[Store Size in SQFT].[#null],[Store Size in SQFT].[20319],[Store Size in SQFT].[21215],[Store Size in SQFT].[22478],[Store Size in SQFT].[23598]}'\n"
                + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
                + "SET [*CJ_SLICER_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Store Size in SQFT].CURRENTMEMBER)})'\n"
                + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n"
                + "SELECT\n"
                + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
                + "FROM [Sales]\n"
                + "WHERE ([*CJ_SLICER_AXIS])"),
            list(
                "[Store Size in SQFT].[#null]",
                "[Store Size in SQFT].[20319]",
                "[Store Size in SQFT].[21215]",
                "[Store Size in SQFT].[22478]",
                "[Store Size in SQFT].[23598]"));

        verify(
            query.getSchemaReader(true), times(1))
            .lookupMemberChildrenByNames(
                parentMember.capture(),
                childNames.capture(),
                matchType.capture());

        assertEquals(
            "[Store Size in SQFT].[All Store Size in SQFTs]",
            parentMember.getAllValues().get(0).getUniqueName());
        assertTrue(childNames.getAllValues().get(0).size() == 5);
        assertEquals(
            "[[#null], [20319], [21215], [22478], [23598]]",
            sortedNames(childNames.getAllValues().get(0)));
    }
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMultiHierarchyNonSSAS(Context context) {
        SystemWideProperties.instance().SsasCompatibleNaming = false;
        assertContains(
            "Resolved map omitted one or more members",
            batchResolve(context,
                "WITH\n"
                + "SET [*NATIVE_CJ_SET] AS 'FILTER([*BASE_MEMBERS__Time.Weekly_], NOT ISEMPTY ([Measures].[Unit Sales]))'\n"
                + "SET [*BASE_MEMBERS__Time.Weekly_] AS '{[Time.Weekly].[1997].[4],[Time.Weekly].[1997].[5],[Time.Weekly].[1997].[6]}'\n"
                + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
                + "SET [*CJ_SLICER_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Time.Weekly].CURRENTMEMBER)})'\n"
                + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n"
                + "SELECT\n"
                + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
                + "FROM [Sales]\n"
                + "WHERE ([*CJ_SLICER_AXIS])"),
            list(
                "[Time.Weekly].[1997].[4]",
                "[Time.Weekly].[1997].[5]",
                "[Time.Weekly].[1997].[6]"));

        verify(
            query.getSchemaReader(true), times(2))
            .lookupMemberChildrenByNames(
                parentMember.capture(),
                childNames.capture(),
                matchType.capture());
        assertEquals(
            "[Time.Weekly].[All Time.Weeklys]",
            parentMember.getAllValues().get(0).getUniqueName());
        assertTrue(childNames.getAllValues().get(0).size() == 1);
        assertEquals(
            "1997",
            childNames.getAllValues().get(0).get(0).getName());
        assertEquals(
            "[[4], [5], [6]]",
            sortedNames(childNames.getAllValues().get(1)));
    }
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMultiHierarchySSAS(Context context) {
        SystemWideProperties.instance().SsasCompatibleNaming = true;
        assertContains(
            "Resolved map omitted one or more members",
            batchResolve(context,
                "WITH\n"
                + "SET [*NATIVE_CJ_SET] AS 'FILTER([*BASE_MEMBERS__Time.Weekly_], NOT ISEMPTY ([Measures].[Unit Sales]))'\n"
                + "SET [*BASE_MEMBERS__Time.Weekly_] AS '{[Time].[Weekly].[1997].[4],[Time].[Weekly].[1997].[5],[Time].[Weekly].[1997].[6]}'\n"
                + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
                + "SET [*CJ_SLICER_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Time].[Weekly].CURRENTMEMBER)})'\n"
                + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Unit Sales]', FORMAT_STRING = 'Standard', SOLVE_ORDER=500\n"
                + "SELECT\n"
                + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
                + "FROM [Sales]\n"
                + "WHERE ([*CJ_SLICER_AXIS])"),
            list(
                "[Time].[Weekly].[1997].[4]",
                "[Time].[Weekly].[1997].[5]",
                "[Time].[Weekly].[1997].[6]"));

        verify(
            query.getSchemaReader(true), times(2))
            .lookupMemberChildrenByNames(
                parentMember.capture(),
                childNames.capture(),
                matchType.capture());
        assertEquals(
            "[Time].[Weekly].[All Weeklys]",
            parentMember.getAllValues().get(0).getUniqueName());
        assertTrue(
            childNames.getAllValues().get(0).size() == 1);
        assertEquals(
            "1997",
            childNames.getAllValues().get(0).get(0).getName());
        assertEquals(
            "[[4], [5], [6]]",
            sortedNames(childNames.getAllValues().get(1)));
    }
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testParentChild(Context context) {
        // P-C resolution will not result in consolidated SQL, but it should
        // still correctly identify children and attempt to resolve them
        // together.
        assertContains(
            "Resolved map omitted one or more members",
            batchResolve(context,
                "WITH\n"
                + "SET [*NATIVE_CJ_SET] AS 'FILTER([*BASE_MEMBERS__Employees_], NOT ISEMPTY ([Measures].[Number of Employees]))'\n"
                + "SET [*BASE_MEMBERS__Employees_] AS '{[Employees].[Sheri Nowmer].[Derrick Whelply],[Employees].[Sheri Nowmer].[Michael Spence]}'\n"
                + "SET [*BASE_MEMBERS__Measures_] AS '{[Measures].[*FORMATTED_MEASURE_0]}'\n"
                + "SET [*CJ_SLICER_AXIS] AS 'GENERATE([*NATIVE_CJ_SET], {([Employees].CURRENTMEMBER)})'\n"
                + "MEMBER [Measures].[*FORMATTED_MEASURE_0] AS '[Measures].[Number of Employees]', FORMAT_STRING = '#,#', SOLVE_ORDER=500\n"
                + "SELECT\n"
                + "[*BASE_MEMBERS__Measures_] ON COLUMNS\n"
                + "FROM [HR]\n"
                + "WHERE ([*CJ_SLICER_AXIS])"),
                list(
                    "[Employees].[Sheri Nowmer].[Derrick Whelply]",
                    "[Employees].[Sheri Nowmer].[Michael Spence]"));

        verify(
            query.getSchemaReader(true), times(2))
            .lookupMemberChildrenByNames(
                parentMember.capture(),
                childNames.capture(),
                matchType.capture());
        assertEquals(
            "[Employees].[Sheri Nowmer]",
            parentMember.getAllValues().get(1).getUniqueName());
        assertTrue(childNames.getAllValues().get(1).size() == 2);
    }


    private void assertContains(
        String msg, Collection<String> strings, Collection<String> list)
    {
        if (!strings.containsAll(list)) {
            List<String> copy = new ArrayList<>(list);
            copy.removeAll(strings);
            fail(
                String.format(
                    "%s\nMissing: %s", msg,
                Arrays.toString(copy.toArray())));
        }
    }

    public Set<String> batchResolve(Context context,String mdx) {
        IdBatchResolver batchResolver = makeTestBatchResolver(context,mdx);
        Map<QueryComponent, QueryComponent> resolvedIdents = batchResolver.resolve();
        Set<String> resolvedNames = getResolvedNames(resolvedIdents);
        return resolvedNames;
    }

    private String sortedNames(List<NameSegment> items) {
        return Arrays.toString(items.stream()
            .sorted((o1, o2)->o1.getName().compareTo(o2.getName())).toArray());
    }

    private Collection<String> list(String... items) {
        return Arrays.asList(items);
    }

    private Set<String> getResolvedNames(
        Map<QueryComponent, QueryComponent> resolvedIdents)
    {
		return resolvedIdents.keySet().stream().map(Object::toString).collect(Collectors.toSet());

    }

    public IdBatchResolver makeTestBatchResolver(Context context,String mdx) {
    	TestUtil.flushSchemaCache(context.getConnectionWithDefaultRole());
        FactoryImpl factoryImpl = new FactoryImplTestWrapper();
        //MdxParserValidator parser = new JavaccParserValidatorImpl(factoryImpl);

        RolapConnection conn = (RolapConnection) spy(
        		context.getConnectionWithDefaultRole());
        when(conn.getQueryProvider()).thenReturn(new QueryProviderWrapper());
        //when(conn.createParser()).thenReturn(parser);

        query = conn.parseQuery(mdx);
        LocusImpl.push(new LocusImpl(new ExecutionImpl(
            query.getStatement(), Optional.of(Duration.ofMillis(Integer.MAX_VALUE))),
            "batchResolveTest", "batchResolveTest"));

        return new IdBatchResolver(query);
    }

    private class QueryTestWrapper extends QueryImpl {
        private SchemaReader spyReader;

        public QueryTestWrapper(
            Statement statement,
            Formula[] formulas,
            QueryAxis[] axes,
            String cube,
            QueryAxisImpl slicerAxis,
            CellProperty[] cellProps,
            boolean strictValidation)
        {
            super(
                statement,
                Util.lookupCube(statement.getSchemaReader(), cube, true),
                formulas,
                axes,
                slicerAxis,
                cellProps,
                new Parameter[0],
                strictValidation);
        }

        @Override
        public void resolve() {
            // for testing purposes we want to defer resolution till after
            //  Query init (resolve is called w/in constructor).
            // We do still need formulas to be created, though.
            if (getFormulas() != null) {
                for (Formula formula : getFormulas()) {
                    formula.createElement(this);
                }
            }
        }

        @Override
        public synchronized SchemaReader getSchemaReader(
            boolean accessControlled)
        {
            if (spyReader == null) {
            	spyReader=spy( new SpySchemaReader(super.getSchemaReader(accessControlled)));
            }
            return spyReader;
        }
    }

    class FactoryImplTestWrapper extends FactoryImpl {

        @Override
        public QueryImpl makeQuery(
            Statement statement,
            Formula[] formulae,
            QueryAxis[] axes,
            Subcube subcube,
            Expression slicer,
            CellProperty[] cellProps,
            boolean strictValidation)
        {
            final QueryAxisImpl slicerAxis =
                slicer == null
                    ? null
                    : new QueryAxisImpl(
                        false, slicer, AxisOrdinal.StandardAxisOrdinal.SLICER,
                        SubtotalVisibility.Undefined, new Id[0]);
            return new QueryTestWrapper(
                statement, formulae, axes, subcube.getCubeName(), slicerAxis, cellProps,
                strictValidation);
        }
    }

    class QueryProviderWrapper extends QueryProviderImpl {
        @Override
        public Query createQuery(Statement statement,
                                 Formula[] formula,
                                 QueryAxis[] axes,
                                 Subcube subcube,
                                 QueryAxis slicerAxis,
                                 CellProperty[] cellProps,
                                 boolean strictValidation) {

            final QueryAxisImpl slicerAxisW =
                slicerAxis == null || slicerAxis.getSet() == null
                    ? null
                    : new QueryAxisImpl(
                    false, slicerAxis.getSet(), AxisOrdinal.StandardAxisOrdinal.SLICER,
                    SubtotalVisibility.Undefined, new Id[0]);
            return new QueryTestWrapper(
                statement, formula, axes, subcube.getCubeName(), slicerAxisW, cellProps,
                strictValidation);
        }
    }
}
