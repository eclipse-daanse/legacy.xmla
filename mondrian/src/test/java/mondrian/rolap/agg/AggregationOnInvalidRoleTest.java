/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2015-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap.agg;

import java.util.List;
import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.test.loader.CsvDBTestCase;

/**
 * @author Andrey Khayrutdinov
 */
class AggregationOnInvalidRoleTest extends CsvDBTestCase {

    @Override
    protected String getFileName() {
        return "mondrian_2225.csv";
    }

    @BeforeEach
    public void beforeEach() {
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }


    protected void prepareContext(Context<?> context) {
        super.prepareContext(context);
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void test_ExecutesCorrectly_WhenIgnoringInvalidMembers(Context<?> context) {
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setIgnoreInvalidMembers(true);
        prepareContext(context);
        Connection connection = ((TestContext)context).getConnection(new ConnectionProps(List.of("Test")));
        //TestContext<?> context = getTestContext().withFreshConnection();
        try {
            executeAnalyzerQuery(connection);
        } finally {
            connection.close();
        }
    }

    /*
    static final String CUBE = ""
        + "<Cube name=\"mondrian2225\" visible=\"true\" cache=\"true\" enabled=\"true\">"
        + "  <Table name=\"mondrian2225_fact\">"
        + "    <AggName name=\"mondrian2225_agg\" ignorecase=\"true\">"
        + "      <AggFactCount column=\"fact_count\"/>"
        + "      <AggMeasure column=\"fact_Measure\" name=\"[Measures].[Measure]\"/>"
        + "      <AggLevel column=\"dim_code\" name=\"[Product Code].[Code]\" collapsed=\"true\"/>"
        + "    </AggName>"
        + "  </Table>"
        + "  <Dimension type=\"StandardDimension\" visible=\"true\" foreignKey=\"customer_id\" highCardinality=\"false\" name=\"Customer\">"
        + "    <Hierarchy name=\"Customer\" visible=\"true\" hasAll=\"true\" primaryKey=\"customer_id\">"
        + "      <Table name=\"mondrian2225_customer\"/>"
        + "        <Level name=\"First Name\" visible=\"true\" column=\"customer_name\" type=\"String\" uniqueMembers=\"false\" levelType=\"Regular\" hideMemberIf=\"Never\"/>"
        + "    </Hierarchy>"
        + "  </Dimension>"
        + "  <Dimension type=\"StandardDimension\" visible=\"true\" foreignKey=\"product_ID\" highCardinality=\"false\" name=\"Product Code\">"
        + "    <Hierarchy name=\"Product Code\" visible=\"true\" hasAll=\"true\" primaryKey=\"product_id\">"
        + "      <Table name=\"mondrian2225_dim\"/>"
        + "      <Level name=\"Code\" visible=\"true\" column=\"product_code\" type=\"String\" uniqueMembers=\"false\" levelType=\"Regular\" hideMemberIf=\"Never\"/>"
        + "    </Hierarchy>"
        + "  </Dimension>"
        + "  <Measure name=\"Measure\" column=\"fact\" aggregator=\"sum\" visible=\"true\"/>"
        + "</Cube>";

    static final String ROLE = ""
        + "<Role name=\"Test\">"
        + "  <SchemaGrant access=\"none\">"
        + "    <CubeGrant cube=\"mondrian2225\" access=\"all\">"
        + "      <HierarchyGrant hierarchy=\"[Customer.Customer]\" topLevel=\"[Customer.Customer].[First Name]\" access=\"custom\">"
        + "        <MemberGrant member=\"[Customer.Customer].[NonExistingName]\" access=\"all\"/>"
        + "      </HierarchyGrant>"
        + "    </CubeGrant>"
        + "  </SchemaGrant>"
        + "</Role>";
    */
    static void executeAnalyzerQuery(Connection connection) {
        // select measures on columns
        // and sorted lexicography products on rows
        String queryFromAnalyzer = ""
            + "with "
            + "  set [*NATIVE_CJ_SET_WITH_SLICER] as 'Filter([*BASE_MEMBERS__Product Code_], (NOT IsEmpty([Measures].[Measure])))'"
            + "  set [*NATIVE_CJ_SET] as '[*NATIVE_CJ_SET_WITH_SLICER]'"
            + "  set [*BASE_MEMBERS__Product Code_] as '[Product Code].[Product Code].[Code].Members'"
            + "  set [*BASE_MEMBERS__Measures_] as '{[Measures].[Measure]}'"
            + "  set [*CJ_ROW_AXIS] as 'Generate([*NATIVE_CJ_SET], {[Product Code].[Product Code].CurrentMember})'"
            + "  set [*SORTED_ROW_AXIS] as 'Order([*CJ_ROW_AXIS], [Product Code].[Product Code].CurrentMember.OrderKey, BASC)' "
            + "select "
            + "  [*BASE_MEMBERS__Measures_] on columns,"
            + "  [*SORTED_ROW_AXIS] on rows "
            + "from [mondrian2225]";

        String expected = ""
            + "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Measure]}\n"
            + "Axis #2:\n"
            + "{[Product Code].[Product Code].[eight]}\n"
            + "{[Product Code].[Product Code].[five]}\n"
            + "{[Product Code].[Product Code].[four]}\n"
            + "{[Product Code].[Product Code].[mdg]}\n"
            + "{[Product Code].[Product Code].[three]}\n"
            + "{[Product Code].[Product Code].[tst]}\n"
            + "{[Product Code].[Product Code].[two]}\n"
            + "Row #0: 175\n"
            + "Row #1: 5\n"
            + "Row #2: 4\n"
            + "Row #3: 2\n"
            + "Row #4: 3\n"
            + "Row #5: 1,000\n"
            + "Row #6: 2\n";

        TestUtil.assertQueryReturns(connection, queryFromAnalyzer, expected);
    }

    protected Function<CatalogMapping, PojoMappingModifier> getModifierFunction(){
        return AggregationOnInvalidRoleTestModifier::new;
    }

}
