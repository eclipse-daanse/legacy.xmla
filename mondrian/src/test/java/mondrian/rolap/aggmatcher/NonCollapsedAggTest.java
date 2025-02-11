/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap.aggmatcher;

import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.MeasureGroupMapping;
import org.eclipse.daanse.rolap.mapping.api.model.PhysicalCubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationColumnNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ColumnMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;

/**
 * Testcase for non-collapsed levels in agg tables.
 *
 * @author Luc Boudreau
 */
class NonCollapsedAggTest extends AggTableTestCase {
	/*
    private static final String CUBE_1 =
        "<Cube name=\"foo\">\n"
        + "    <Table name=\"foo_fact\">\n"
        + "        <AggName name=\"agg_tenant\">\n"
        + "            <AggFactCount column=\"fact_count\"/>\n"
        + "            <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\"/>\n"
        + "            <AggLevel name=\"[dimension.tenant].[tenant]\"\n"
        + "                column=\"tenant_id\" collapsed=\"false\"/>\n"
        + "        </AggName>\n"
        + "        <AggName name=\"agg_line_class\">\n"
        + "            <AggFactCount column=\"fact_count\"/>\n"
        + "            <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\"/>\n"
        + "            <AggLevel name=\"[dimension.distributor].[line class]\"\n"
        + "                column=\"line_class_id\" collapsed=\"false\"/>\n"
        + "        </AggName>\n"
        + "        <AggName name=\"agg_line_class\">\n"
        + "            <AggFactCount column=\"fact_count\"/>\n"
        + "            <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\"/>\n"
        + "            <AggLevel name=\"[dimension.network].[line class]\"\n"
        + "                column=\"line_class_id\" collapsed=\"false\"/>\n"
        + "        </AggName>\n"
        + "    </Table>\n"
        + "    <Dimension name=\"dimension\" foreignKey=\"line_id\">\n"
        + "        <Hierarchy name=\"tenant\" hasAll=\"true\" allMemberName=\"All tenants\"\n"
        + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
        + "            <Join leftKey=\"line_id\" rightKey=\"line_id\"\n"
        + "                rightAlias=\"line_tenant\">\n"
        + "                <Table name=\"line\"/>\n"
        + "                <Join leftKey=\"tenant_id\" rightKey=\"tenant_id\">\n"
        + "                    <Table name=\"line_tenant\"/>\n"
        + "                    <Table name=\"tenant\"/>\n"
        + "                </Join>\n"
        + "            </Join>\n"
        + "            <Level name=\"tenant\" table=\"tenant\" column=\"tenant_id\" nameColumn=\"tenant_name\" uniqueMembers=\"true\"/>\n"
        + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
        + "        </Hierarchy>\n"
        + "        <Hierarchy name=\"distributor\" hasAll=\"true\" allMemberName=\"All distributors\"\n"
        + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
        + "            <Join leftKey=\"line_id\" rightKey=\"line_id\" rightAlias=\"line_line_class\">\n"
        + "                <Table name=\"line\"/>\n"
        + "                <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class\">\n"
        + "                    <Table name=\"line_line_class\"/>\n"
        + "                    <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class_distributor\">\n"
        + "                        <Table name=\"line_class\"/>\n"
        + "                        <Join leftKey=\"distributor_id\" rightKey=\"distributor_id\">\n"
        + "                            <Table name=\"line_class_distributor\"/>\n"
        + "                            <Table name=\"distributor\"/>\n"
        + "                        </Join>\n"
        + "                    </Join>\n"
        + "                </Join>\n"
        + "            </Join>\n"
        + "            <Level name=\"distributor\" table=\"distributor\" column=\"distributor_id\" nameColumn=\"distributor_name\"/>\n"
        + "            <Level name=\"line class\" table=\"line_class\" column=\"line_class_id\" nameColumn=\"line_class_name\" uniqueMembers=\"true\"/>\n"
        + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
        + "        </Hierarchy>\n"
        + "        <Hierarchy name=\"network\" hasAll=\"true\" allMemberName=\"All networks\"\n"
        + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
        + "            <Join leftKey=\"line_id\" rightKey=\"line_id\" rightAlias=\"line_line_class\">\n"
        + "                <Table name=\"line\"/>\n"
        + "                <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class\">\n"
        + "                    <Table name=\"line_line_class\"/>\n"
        + "                    <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class_network\">\n"
        + "                        <Table name=\"line_class\"/>\n"
        + "                        <Join leftKey=\"network_id\" rightKey=\"network_id\">\n"
        + "                            <Table name=\"line_class_network\"/>\n"
        + "                            <Table name=\"network\"/>\n"
        + "                        </Join>\n"
        + "                    </Join>\n"
        + "                </Join>\n"
        + "            </Join>\n"
        + "            <Level name=\"network\" table=\"network\" column=\"network_id\" nameColumn=\"network_name\"/>\n"
        + "            <Level name=\"line class\" table=\"line_class\" column=\"line_class_id\" nameColumn=\"line_class_name\" uniqueMembers=\"true\"/>\n"
        + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
        + "        </Hierarchy>\n"
        + " </Dimension>\n"
        + "   <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\" />\n"
        + "</Cube>\n"
        + "<Cube name=\"foo2\">\n"
        + "    <Table name=\"foo_fact\">\n"
        + "    </Table>\n"
        + "    <Dimension name=\"dimension\" foreignKey=\"line_id\">\n"
        + "        <Hierarchy name=\"tenant\" hasAll=\"true\" allMemberName=\"All tenants\"\n"
        + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
        + "            <Join leftKey=\"line_id\" rightKey=\"line_id\"\n"
        + "                rightAlias=\"line_tenant\">\n"
        + "                <Table name=\"line\"/>\n"
        + "                <Join leftKey=\"tenant_id\" rightKey=\"tenant_id\">\n"
        + "                    <Table name=\"line_tenant\"/>\n"
        + "                    <Table name=\"tenant\"/>\n"
        + "                </Join>\n"
        + "            </Join>\n"
        + "            <Level name=\"tenant\" table=\"tenant\" column=\"tenant_id\" nameColumn=\"tenant_name\" uniqueMembers=\"true\"/>\n"
        + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
        + "        </Hierarchy>\n"
        + "        <Hierarchy name=\"distributor\" hasAll=\"true\" allMemberName=\"All distributors\"\n"
        + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
        + "            <Join leftKey=\"line_id\" rightKey=\"line_id\" rightAlias=\"line_line_class\">\n"
        + "                <Table name=\"line\"/>\n"
        + "                <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class\">\n"
        + "                    <Table name=\"line_line_class\"/>\n"
        + "                    <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class_distributor\">\n"
        + "                        <Table name=\"line_class\"/>\n"
        + "                        <Join leftKey=\"distributor_id\" rightKey=\"distributor_id\">\n"
        + "                            <Table name=\"line_class_distributor\"/>\n"
        + "                            <Table name=\"distributor\"/>\n"
        + "                        </Join>\n"
        + "                    </Join>\n"
        + "                </Join>\n"
        + "            </Join>\n"
        + "            <Level name=\"distributor\" table=\"distributor\" column=\"distributor_id\" nameColumn=\"distributor_name\"/>\n"
        + "            <Level name=\"line class\" table=\"line_class\" column=\"line_class_id\" nameColumn=\"line_class_name\" uniqueMembers=\"true\"/>\n"
        + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
        + "        </Hierarchy>\n"
        + "        <Hierarchy name=\"network\" hasAll=\"true\" allMemberName=\"All networks\"\n"
        + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
        + "            <Join leftKey=\"line_id\" rightKey=\"line_id\" rightAlias=\"line_line_class\">\n"
        + "                <Table name=\"line\"/>\n"
        + "                <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class\">\n"
        + "                    <Table name=\"line_line_class\"/>\n"
        + "                    <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class_network\">\n"
        + "                        <Table name=\"line_class\"/>\n"
        + "                        <Join leftKey=\"network_id\" rightKey=\"network_id\">\n"
        + "                            <Table name=\"line_class_network\"/>\n"
        + "                            <Table name=\"network\"/>\n"
        + "                        </Join>\n"
        + "                    </Join>\n"
        + "                </Join>\n"
        + "            </Join>\n"
        + "            <Level name=\"network\" table=\"network\" column=\"network_id\" nameColumn=\"network_name\"/>\n"
        + "            <Level name=\"line class\" table=\"line_class\" column=\"line_class_id\" nameColumn=\"line_class_name\" uniqueMembers=\"true\"/>\n"
        + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
        + "        </Hierarchy>\n"
        + " </Dimension>\n"
        + "   <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\" />\n"
        + "</Cube>\n";
    */
    /*
    private static final String SSAS_COMPAT_CUBE = "<Cube name=\"testSsas\">\n"
            + "    <Table name=\"foo_fact\">\n"
            + "        <AggName name=\"agg_tenant\">\n"
            + "            <AggFactCount column=\"fact_count\"/>\n"
            + "            <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\"/>\n"
            + "            <AggLevel name=\"[dimension].[tenant].[tenant]\"\n"
            + "                column=\"tenant_id\" collapsed=\"false\"/>\n"
            + "        </AggName>\n"
            + "        <AggName name=\"agg_line_class\">\n"
            + "            <AggFactCount column=\"fact_count\"/>\n"
            + "            <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\"/>\n"
            + "            <AggLevel name=\"[dimension].[distributor].[line class]\"\n"
            + "                column=\"line_class_id\" collapsed=\"false\"/>\n"
            + "        </AggName>\n"
            + "        <AggName name=\"agg_line_class\">\n"
            + "            <AggFactCount column=\"fact_count\"/>\n"
            + "            <AggMeasure name=\"[Measures].[Unit Sales]\" column=\"unit_sales\"/>\n"
            + "            <AggLevel name=\"[dimension].[network].[line class]\"\n"
            + "                column=\"line_class_id\" collapsed=\"false\"/>\n"
            + "        </AggName>\n"
            + "    </Table>\n"
            + "    <Dimension name=\"dimension\" foreignKey=\"line_id\">\n"
            + "        <Hierarchy name=\"tenant\" hasAll=\"true\" allMemberName=\"All tenants\"\n"
            + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
            + "            <Join leftKey=\"line_id\" rightKey=\"line_id\"\n"
            + "                rightAlias=\"line_tenant\">\n"
            + "                <Table name=\"line\"/>\n"
            + "                <Join leftKey=\"tenant_id\" rightKey=\"tenant_id\">\n"
            + "                    <Table name=\"line_tenant\"/>\n"
            + "                    <Table name=\"tenant\"/>\n"
            + "                </Join>\n"
            + "            </Join>\n"
            + "            <Level name=\"tenant\" table=\"tenant\" column=\"tenant_id\" nameColumn=\"tenant_name\" uniqueMembers=\"true\"/>\n"
            + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
            + "        </Hierarchy>\n"
            + "        <Hierarchy name=\"distributor\" hasAll=\"true\" allMemberName=\"All distributors\"\n"
            + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
            + "            <Join leftKey=\"line_id\" rightKey=\"line_id\" rightAlias=\"line_line_class\">\n"
            + "                <Table name=\"line\"/>\n"
            + "                <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class\">\n"
            + "                    <Table name=\"line_line_class\"/>\n"
            + "                    <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class_distributor\">\n"
            + "                        <Table name=\"line_class\"/>\n"
            + "                        <Join leftKey=\"distributor_id\" rightKey=\"distributor_id\">\n"
            + "                            <Table name=\"line_class_distributor\"/>\n"
            + "                            <Table name=\"distributor\"/>\n"
            + "                        </Join>\n"
            + "                    </Join>\n"
            + "                </Join>\n"
            + "            </Join>\n"
            + "            <Level name=\"distributor\" table=\"distributor\" column=\"distributor_id\" nameColumn=\"distributor_name\"/>\n"
            + "            <Level name=\"line class\" table=\"line_class\" column=\"line_class_id\" nameColumn=\"line_class_name\" uniqueMembers=\"true\"/>\n"
            + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
            + "        </Hierarchy>\n"
            + "        <Hierarchy name=\"network\" hasAll=\"true\" allMemberName=\"All networks\"\n"
            + "            primaryKey=\"line_id\" primaryKeyTable=\"line\">\n"
            + "            <Join leftKey=\"line_id\" rightKey=\"line_id\" rightAlias=\"line_line_class\">\n"
            + "                <Table name=\"line\"/>\n"
            + "                <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class\">\n"
            + "                    <Table name=\"line_line_class\"/>\n"
            + "                    <Join leftKey=\"line_class_id\" rightKey=\"line_class_id\" rightAlias=\"line_class_network\">\n"
            + "                        <Table name=\"line_class\"/>\n"
            + "                        <Join leftKey=\"network_id\" rightKey=\"network_id\">\n"
            + "                            <Table name=\"line_class_network\"/>\n"
            + "                            <Table name=\"network\"/>\n"
            + "                        </Join>\n"
            + "                    </Join>\n"
            + "                </Join>\n"
            + "            </Join>\n"
            + "            <Level name=\"network\" table=\"network\" column=\"network_id\" nameColumn=\"network_name\"/>\n"
            + "            <Level name=\"line class\" table=\"line_class\" column=\"line_class_id\" nameColumn=\"line_class_name\" uniqueMembers=\"true\"/>\n"
            + "            <Level name=\"line\" table=\"line\" column=\"line_id\" nameColumn=\"line_name\"/>\n"
            + "        </Hierarchy>\n"
            + " </Dimension>\n"
            + "   <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\" />\n"
            + "</Cube>\n";
     */

    @Override
	protected String getFileName() {
        return "non_collapsed_agg_test.csv";
    }



    @Override
	protected void prepareContext(Context context) {
        try {
            super.prepareContext(context);
        }
        catch (Exception e) {
            throw  new RuntimeException("Prepare context for csv tests failed");
        }


    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testSingleJoin(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
    	super.prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        final String mdx =
            "select {[Measures].[Unit Sales]} on columns, {[dimension.tenant].[tenant].Members} on rows from [foo]";



        // We expect the correct cell value + 1 if the agg table is used.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[dimension.tenant].[tenant one]}\n"
            + "{[dimension.tenant].[tenant two]}\n"
            + "Row #0: 31\n"
            + "Row #1: 121\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testComplexJoin(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        final String mdx =
            "select {[Measures].[Unit Sales]} on columns, {[dimension.distributor].[line class].Members} on rows from [foo]";



        // We expect the correct cell value + 1 if the agg table is used.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[dimension.distributor].[distributor one].[line class one]}\n"
            + "{[dimension.distributor].[distributor two].[line class two]}\n"
            + "Row #0: 31\n"
            + "Row #1: 121\n");

        final String mdx2 =
            "select {[Measures].[Unit Sales]} on columns, {[dimension.network].[line class].Members} on rows from [foo]";
        // We expect the correct cell value + 1 if the agg table is used.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[dimension.network].[network one].[line class one]}\n"
            + "{[dimension.network].[network two].[line class two]}\n"
            + "Row #0: 31\n"
            + "Row #1: 121\n");
    }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testComplexJoinDefaultRecognizer(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        if (!isApplicable(context.getConnectionWithDefaultRole())) {
            return;
        }

        // We expect the correct cell value + 2 if the agg table is used.
        final String mdx =
            "select {[Measures].[Unit Sales]} on columns, {[dimension.distributor].[line class].Members} on rows from [foo2]";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[dimension.distributor].[distributor one].[line class one]}\n"
            + "{[dimension.distributor].[distributor two].[line class two]}\n"
            + "Row #0: 32\n"
            + "Row #1: 122\n");

        final String mdx2 =
            "select {[Measures].[Unit Sales]} on columns, {[dimension.network].[line class].Members} on rows from [foo2]";
        // We expect the correct cell value + 2 if the agg table is used.
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx2,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[dimension.network].[network one].[line class one]}\n"
            + "{[dimension.network].[network two].[line class two]}\n"
            + "Row #0: 32\n"
            + "Row #1: 122\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testSsasCompatNamingInAgg(Context context) throws Exception {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        // MONDRIAN-1085
        if (!SystemWideProperties.instance().SsasCompatibleNaming) {
            return;
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null, SSAS_COMPAT_CUBE, null, null, null, null);
         */
        class TestSsasCompatNamingInAggModifier extends PojoMappingModifier {

            public TestSsasCompatNamingInAggModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
                //## ColumnNames: line_id,unit_sales
                //## ColumnTypes: INTEGER,INTEGER
            	ColumnMappingImpl lineIdFooFact = ColumnMappingImpl.builder().withName("line_id").withType("INTEGER").build();
            	ColumnMappingImpl unitSalesFooFact = ColumnMappingImpl.builder().withName("unit_sales").withType("INTEGER").build();
                PhysicalTableMappingImpl fooFact = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("foo_fact")
                        .withColumns(List.of(lineIdFooFact, unitSalesFooFact))).build();
                //## TableName: line
                //## ColumnNames: line_id,line_name
                //## ColumnTypes: INTEGER,VARCHAR(30)
                ColumnMappingImpl lineIdLine = ColumnMappingImpl.builder().withName("line_id").withType("INTEGER").build();
                ColumnMappingImpl lineNameLine = ColumnMappingImpl.builder().withName("line_name").withType("VARCHAR").withCharOctetLength(30).build();
                PhysicalTableMappingImpl line = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("line")
                        .withColumns(List.of(lineIdLine))).build();
                //## TableName: line_tenant
                //## ColumnNames: line_id,tenant_id
                //## ColumnTypes: INTEGER,INTEGER
                ColumnMappingImpl lineIdLineTenant = ColumnMappingImpl.builder().withName("line_id").withType("INTEGER").build();
                ColumnMappingImpl tenantIdLineTenant = ColumnMappingImpl.builder().withName("tenant_id").withType("INTEGER").build();
                PhysicalTableMappingImpl lineTenant = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("line_tenant")
                        .withColumns(List.of(lineIdLineTenant, tenantIdLineTenant))).build();
                //## TableName: tenant
                //## ColumnNames: tenant_id,tenant_name
                //## ColumnTypes: INTEGER,VARCHAR(30)
                ColumnMappingImpl tenantIdTenant = ColumnMappingImpl.builder().withName("tenant_id").withType("INTEGER").build();
                ColumnMappingImpl tenantNameTenant = ColumnMappingImpl.builder().withName("tenant_name").withType("VARCHAR").withCharOctetLength(30).build();
                PhysicalTableMappingImpl tenant = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("tenant")
                        .withColumns(List.of(lineIdLineTenant, tenantIdLineTenant))).build();
                //## TableName: line_line_class
                //## ColumnNames: line_id,line_class_id
                //## ColumnTypes: INTEGER,INTEGER
                ColumnMappingImpl lineIdLineLineClass = ColumnMappingImpl.builder().withName("line_id").withType("INTEGER").build();
                ColumnMappingImpl lineClassIdLineLineClass = ColumnMappingImpl.builder().withName("line_class_id").withType("INTEGER").build();
                PhysicalTableMappingImpl lineLineClass = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("line_line_class")
                        .withColumns(List.of(lineIdLineLineClass, lineClassIdLineLineClass))).build();
                //## TableName: distributor
                //## ColumnNames: distributor_id,distributor_name
                //## ColumnTypes: INTEGER,VARCHAR(30)
                ColumnMappingImpl distributorIdDistributor = ColumnMappingImpl.builder().withName("distributor_id").withType("INTEGER").build();
                ColumnMappingImpl distributorNameDistributor = ColumnMappingImpl.builder().withName("distributor_name").withType("VARCHAR").withCharOctetLength(30).build();
                PhysicalTableMappingImpl distributor = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("distributor")
                        .withColumns(List.of(distributorIdDistributor, distributorNameDistributor))).build();
                //## TableName: line_class_distributor
                //## ColumnNames: line_class_id,distributor_id
                //## ColumnTypes: INTEGER,INTEGER
                ColumnMappingImpl lineClassIdLineClassDistributor = ColumnMappingImpl.builder().withName("line_class_id").withType("INTEGER").build();
                ColumnMappingImpl distributorIdLineClassDistributor = ColumnMappingImpl.builder().withName("distributor_id").withType("INTEGER").build();
                PhysicalTableMappingImpl lineClassDistributor = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("line_class_distributor")
                        .withColumns(List.of(lineClassIdLineClassDistributor, distributorIdLineClassDistributor))).build();
                //## TableName: line_class
                //## ColumnNames: line_class_id,line_class_name
                //## ColumnTypes: INTEGER,VARCHAR(30)
                ColumnMappingImpl lineClassIdLineClass = ColumnMappingImpl.builder().withName("line_class_id").withType("INTEGER").build();
                ColumnMappingImpl lineClassNameLineClass = ColumnMappingImpl.builder().withName("line_class_name").withType("VARCHAR").withCharOctetLength(30).build();
                PhysicalTableMappingImpl lineClass = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("line_class")
                        .withColumns(List.of(lineClassIdLineClass, lineClassNameLineClass))).build();
                //## TableName: network
                //## ColumnNames: network_id,network_name
                //## ColumnTypes: INTEGER,VARCHAR(30)
                ColumnMappingImpl networkIdNetwork = ColumnMappingImpl.builder().withName("network_id").withType("INTEGER").build();
                ColumnMappingImpl networkNameNetwork = ColumnMappingImpl.builder().withName("network_name").withType("VARCHAR").withCharOctetLength(30).build();
                PhysicalTableMappingImpl network = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("network")
                        .withColumns(List.of(networkIdNetwork, networkNameNetwork))).build();
                //## TableName: line_class_network
                //## ColumnNames: line_class_id,network_id
                //## ColumnTypes: INTEGER,INTEGER
                ColumnMappingImpl lineClassIdLineClassNetwork = ColumnMappingImpl.builder().withName("line_class_id").withType("INTEGER").build();
                ColumnMappingImpl networkIdLineClassNetwork = ColumnMappingImpl.builder().withName("network_id").withType("INTEGER").build();
                PhysicalTableMappingImpl lineClassNetwork = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("line_class_network")
                        .withColumns(List.of(lineClassIdLineClassNetwork, networkIdLineClassNetwork))).build();

                //## TableName: agg_tenant
                //## ColumnNames: tenant_id,unit_sales,fact_count
                //## ColumnTypes: INTEGER,INTEGER,INTEGER
                ColumnMappingImpl tenantIdAggTenant = ColumnMappingImpl.builder().withName("tenant_id").withType("INTEGER").build();
                ColumnMappingImpl unitSalesAggTenant = ColumnMappingImpl.builder().withName("unit_sales").withType("INTEGER").build();
                ColumnMappingImpl factCountAggTenant = ColumnMappingImpl.builder().withName("fact_count").withType("INTEGER").build();
                PhysicalTableMappingImpl aggTenant = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("agg_tenant")
                        .withColumns(List.of(tenantIdAggTenant, unitSalesAggTenant, factCountAggTenant))).build();

                //## TableName: agg_line_class
                //## ColumnNames: line_class_id,unit_sales,fact_count
                //## ColumnTypes: INTEGER,INTEGER,INTEGER
                ColumnMappingImpl lineClassIdAggLineClass = ColumnMappingImpl.builder().withName("line_class_id").withType("INTEGER").build();
                ColumnMappingImpl unitSalesAggLineClass = ColumnMappingImpl.builder().withName("unit_sales").withType("INTEGER").build();
                ColumnMappingImpl factCountAggLineClass = ColumnMappingImpl.builder().withName("fact_count").withType("INTEGER").build();
                PhysicalTableMappingImpl aggLineClass = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("agg_line_class")
                        .withColumns(List.of(lineClassIdAggLineClass, unitSalesAggLineClass, factCountAggLineClass))).build();

                //## TableName: agg_10_foo_fact
                //## ColumnNames: line_class_id,unit_sales,fact_count
                //## ColumnTypes: INTEGER,INTEGER,INTEGER
                ColumnMappingImpl lineClassIdAgg10FooFact = ColumnMappingImpl.builder().withName("line_class_id").withType("INTEGER").build();
                ColumnMappingImpl unitSalesAgg10FooFact = ColumnMappingImpl.builder().withName("unit_sales").withType("INTEGER").build();
                ColumnMappingImpl factCountAgg10FooFact = ColumnMappingImpl.builder().withName("fact_count").withType("INTEGER").build();
                PhysicalTableMappingImpl agg10FooFact = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("agg_10_foo_fact")
                        .withColumns(List.of(lineClassIdAgg10FooFact, unitSalesAgg10FooFact, factCountAgg10FooFact))).build();

                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.catalogCubes(schema));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName("testSsas")
                    .withQuery(TableQueryMappingImpl.builder().withTable(fooFact)
                    	.withAggregationTables(List.of(
                            AggregationNameMappingImpl.builder()
                                .withName(aggTenant)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                                    .withColumn(factCountAggTenant)
                                    .build())
                                .withAggregationMeasures(List.of(
                                    AggregationMeasureMappingImpl.builder()
                                        .withName("[Measures].[Unit Sales]").withColumn(unitSalesAggTenant)
                                        .build()
                                ))
                                .withAggregationLevels(List.of(
                                    AggregationLevelMappingImpl.builder()
                                        .withName("[dimension].[tenant].[tenant]")
                                        .withColumn(tenantIdAggTenant).withCollapsed(false)
                                        .build()
                                ))
                                .build(),
                            AggregationNameMappingImpl.builder()
                                .withName(aggLineClass)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                                    .withColumn(factCountAggLineClass)
                                    .build())
                                .withAggregationMeasures(List.of(
                                    AggregationMeasureMappingImpl.builder()
                                        .withName("[Measures].[Unit Sales]").withColumn(unitSalesAggLineClass)
                                        .build()
                                ))
                                .withAggregationLevels(List.of(
                                	AggregationLevelMappingImpl.builder()
                                        .withName("[dimension].[distributor].[line class]")
                                        .withColumn(lineClassIdAggLineClass).withCollapsed(false)
                                        .build()
                                ))
                                .build(),
                            AggregationNameMappingImpl.builder()
                                .withName(aggLineClass)
                                .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                                    .withColumn(factCountAggLineClass)
                                    .build())
                                .withAggregationMeasures(List.of(
                                    AggregationMeasureMappingImpl.builder()
                                        .withName("[Measures].[Unit Sales]").withColumn(unitSalesAggLineClass)
                                        .build()
                                ))
                                .withAggregationLevels(List.of(
                                    AggregationLevelMappingImpl.builder()
                                        .withName("[dimension].[network].[line class]")
                                        .withColumn(lineClassIdAggLineClass).withCollapsed(false)
                                        .build()
                                ))
                                .build()
                        ))
                    		.build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("dimension")
                            .withForeignKey(lineIdFooFact)
                            .withDimension(StandardDimensionMappingImpl.builder()
                            	.withName("dimension")
                            	.withHierarchies(List.of(
                                HierarchyMappingImpl.builder()
                                    .withName("tenant")
                                    .withHasAll(true)
                                    .withAllMemberName("All tenants")
                                    .withPrimaryKey(lineIdLine)
                                    .withPrimaryKeyTable(line)
                    				.withQuery(JoinQueryMappingImpl.builder()
                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                    							.withKey(lineIdLine)
                    							.withQuery(TableQueryMappingImpl.builder().withTable(line).build())
                    							.build())
                    						.withRight(JoinedQueryElementMappingImpl.builder()
                    								.withAlias("line_tenant")
                        							.withKey(lineIdLineTenant)
                                    				.withQuery(JoinQueryMappingImpl.builder()
                                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                                    							.withKey(tenantIdLineTenant)
                                    							.withQuery(TableQueryMappingImpl.builder().withTable(lineTenant).build())
                                    							.build())
                                    						.withRight(JoinedQueryElementMappingImpl.builder()
                                        							.withKey(tenantIdTenant)
                                        							.withQuery(TableQueryMappingImpl.builder().withTable(tenant).build())
                                        							.build())
                                    						.build()
                                    				)
                        							.build())
                    						.build()
                    				)
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("tenant")
                                            .withTable(tenant)
                                            .withColumn(tenantIdTenant)
                                            .withNameColumn(tenantNameTenant)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("line")
                                            .withTable(line)
                                            .withColumn(lineIdLine)
                                            .withNameColumn(lineNameLine)
                                            .build()

                                    ))
                                    .build(),
                                HierarchyMappingImpl.builder()
                                    .withName("distributor")
                                    .withHasAll(true)
                                    .withAllMemberName("All distributors")
                                    .withPrimaryKey(lineIdLine)
                                    .withPrimaryKeyTable(line)

                    				.withQuery(JoinQueryMappingImpl.builder()
                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                    							.withKey(lineIdLine)
                    							.withQuery(TableQueryMappingImpl.builder().withTable(line).build())
                    							.build())
                    						.withRight(JoinedQueryElementMappingImpl.builder()
                    								.withAlias("line_line_class")
                        							.withKey(lineIdLineLineClass)
                                    				.withQuery(JoinQueryMappingImpl.builder()
                                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                                    							.withKey(lineClassIdLineLineClass)
                                    							.withQuery(TableQueryMappingImpl.builder().withTable(lineLineClass).build())
                                    							.build())
                                    						.withRight(JoinedQueryElementMappingImpl.builder()
                                    								.withAlias("line_class")
                                        							.withKey(lineClassIdLineClassDistributor)
                                                    				.withQuery(JoinQueryMappingImpl.builder()
                                                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                                                    							.withKey(distributorIdLineClassDistributor)
                                                    							.withQuery(TableQueryMappingImpl.builder().withTable(lineClassDistributor).build())
                                                    							.build())
                                                    						.withRight(JoinedQueryElementMappingImpl.builder()
                                                        							.withKey(distributorIdDistributor)
                                                        							.withQuery(TableQueryMappingImpl.builder().withTable(distributor).build())
                                                        							.build())
                                                    						.build()
                                                    				)
                                        							.build())
                                    						.build()
                                    				)
                        							.build())
                    						.build()
                    				)
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("distributor")
                                            .withTable(distributor)
                                            .withColumn(distributorIdDistributor)
                                            .withNameColumn(distributorNameDistributor)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("line class")
                                            .withTable(lineClass)
                                            .withColumn(lineClassIdLineClass)
                                            .withNameColumn(lineClassNameLineClass)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("line")
                                            .withTable(line)
                                            .withColumn(lineIdLine)
                                            .withNameColumn(lineNameLine)
                                            .build()
                                    ))
                                    .build(),
                                HierarchyMappingImpl.builder()
                                    .withName("network")
                                    .withHasAll(true)
                                    .withAllMemberName("All networks")
                                    .withPrimaryKey(lineIdLine)
                                    .withPrimaryKeyTable(line)
                    				.withQuery(JoinQueryMappingImpl.builder()
                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                    							.withKey(lineIdLine)
                    							.withQuery(TableQueryMappingImpl.builder().withTable(line).build())
                    							.build())
                    						.withRight(JoinedQueryElementMappingImpl.builder()
                    								.withAlias("line_line_class")
                        							.withKey(lineIdLineLineClass)
                                    				.withQuery(JoinQueryMappingImpl.builder()
                                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                                    							.withKey(lineClassIdLineLineClass)
                                    							.withQuery(TableQueryMappingImpl.builder().withTable(lineLineClass).build())
                                    							.build())
                                    						.withRight(JoinedQueryElementMappingImpl.builder()
                                    								.withAlias("line_class")
                                        							.withKey(lineClassIdLineClass)
                                                    				.withQuery(JoinQueryMappingImpl.builder()
                                                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                                                    							.withKey(lineClassIdLineClass)
                                                    							.withQuery(TableQueryMappingImpl.builder().withTable(lineClass).build())
                                                    							.build())
                                                    						.withRight(JoinedQueryElementMappingImpl.builder()
                                                    								.withAlias("line_class_network")
                                                        							.withKey(lineClassIdLineClassNetwork)
                                                                    				.withQuery(JoinQueryMappingImpl.builder()
                                                                    						.withLeft(JoinedQueryElementMappingImpl.builder()
                                                                    							.withKey(networkIdLineClassNetwork)
                                                                    							.withQuery(TableQueryMappingImpl.builder().withTable(lineClassNetwork).build())
                                                                    							.build())
                                                                    						.withRight(JoinedQueryElementMappingImpl.builder()
                                                                        							.withKey(networkIdNetwork)
                                                                        							.withQuery(TableQueryMappingImpl.builder().withTable(network).build())
                                                                        							.build())
                                                                    						.build()
                                                                    				)
                                                        							.build())
                                                    						.build()
                                                    				)
                                        							.build())
                                    						.build()
                                    				)
                        							.build())
                    						.build()
                    				)
                    				.withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("network")
                                            .withTable(network)
                                            .withColumn(networkIdNetwork)
                                            .withNameColumn(networkNameNetwork)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("line class")
                                            .withTable(lineClass)
                                            .withColumn(lineClassIdLineClass)
                                            .withNameColumn(lineClassNameLineClass)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("line")
                                            .withTable(line)
                                            .withColumn(lineIdLine)
                                            .withNameColumn(lineNameLine)
                                            .build()
                    				))
                                    .build()
                            )).build())
                            .build()
                    ))
                    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                            MeasureMappingImpl.builder()
                            .withName("Unit Sales")
                            .withColumn(unitSalesFooFact)
                            .withAggregatorType(MeasureAggregatorType.SUM)
                            .withFormatString("Standard")
                            .build()

                    )).build()))
                    .build());

                return result;
            }
        }

        withSchema(context, TestSsasCompatNamingInAggModifier::new);

        final String mdx =
                "select {[Measures].[Unit Sales]} on columns, {[dimension].[tenant].[tenant].Members} on rows from [testSsas]";
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            mdx,
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n"
            + "{[dimension].[tenant].[tenant one]}\n"
            + "{[dimension].[tenant].[tenant two]}\n"
            + "Row #0: 31\n"
            + "Row #1: 121\n");
    }

    /**
     * Test case for cast exception on min/max of an integer measure
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMondrian1325(Context context) {
        ((TestConfig)context.getConfig()).setUseAggregates(true);
        ((TestConfig)context.getConfig()).setReadAggregates(true);
        prepareContext(context);
        final String query1 =
            "SELECT\n"
            + "{ Measures.[Bogus Number]} on 0,\n"
            + "non empty Descendants([Time].[Year].Members, Time.Month, SELF) on 1\n"
            + "FROM [Sales]\n";

        final String query2 =
            "SELECT\n"
            + "{ Measures.[Bogus Number]} on 0,\n"
            + "non empty Descendants([Time].[Year].Members, Time.Month, SELF_AND_BEFORE) on 1\n"
            + "FROM [Sales]";

        class TestMondrian1325Modifier extends PojoMappingModifier {

            public TestMondrian1325Modifier(CatalogMapping catalog) {
                super(catalog);
            }
            @Override
            protected List<? extends MeasureGroupMapping> physicalCubeMeasureGroups(PhysicalCubeMapping cube) {
                List<MeasureGroupMapping> result = new ArrayList<>();
                result.addAll(super.physicalCubeMeasureGroups(cube));
                if ("Sales".equals(cube.getName())) {
                    result.add(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                    	MeasureMappingImpl.builder()
                        .withName("Bogus Number")
                        .withColumn(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDatatype(DataType.NUMERIC)
                        .withAggregatorType(MeasureAggregatorType.MAX)
                        .withVisible(true)
                        .build())).build());
                }
                return result;
            }
        }
        /*
        ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
                "Sales",
                null,
                "<Measure name=\"Bogus Number\" column=\"promotion_id\" datatype=\"Numeric\" aggregator=\"max\" visible=\"true\"/>",
                null,
                null));
        */
        withSchema(context, TestMondrian1325Modifier::new);


        executeQuery(query2, context.getConnectionWithDefaultRole());
    }

    protected Function<CatalogMapping, PojoMappingModifier> getModifierFunction(){
        return NonCollapsedAggTestModifier::new;
    }

}
