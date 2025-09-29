/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.olap.fun;

import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import java.sql.SQLException;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.rolap.SchemaModifiersEmf;

/**
 * Tests for ValidMeasureFunDef
 *
 * Created by Yury_Bakhmutski on 9/2/2015.
 */
class ValidMeasureFunDefTest {

  /**
   * Test for MONDRIAN-1032 issue.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSecondHierarchyInDimension(Context<?> context) throws SQLException {
    /*
    final String schema = "<?xml version=\"1.0\"?>\n"
    + "<Schema name=\"FoodMart\">\n"
    + "  <Dimension name=\"Product\">\n"
    + "    <Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
    + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
    + "        <Table name=\"product\"/>\n"
    + "        <Table name=\"product_class\"/>\n"
    + "      </Join>\n"
    + "      <Level name=\"Product Name\" table=\"product\" column=\"product_name\" uniqueMembers=\"true\"/>\n"
    + "    </Hierarchy>\t\n"
    + "\t<Hierarchy name=\"BrandOnly\" hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
    + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
    + "        <Table name=\"product\"/>\n"
    + "        <Table name=\"product_class\"/>\n"
    + "      </Join>\n"
    + "      <Level name=\"Product\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
    + "    </Hierarchy>\n"
    + "  </Dimension>\n"
    + "  <Cube name=\"Sales\" defaultMeasure=\"Unit Sales\">\n"
    + "    <Table name=\"sales_fact_1997\"/>\n"
    + "    <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
    + "  </Cube>\n"
    + "  <Cube name=\"Sales 1\" cache=\"true\" enabled=\"true\">\n"
    + "    <Table name=\"sales_fact_1997\"/>\n"
    + "\t<Measure name=\"Unit Sales1\" column=\"unit_sales\" aggregator=\"sum\"\n"
    + "      formatString=\"Standard\"/>\n" + "  </Cube>  \n"
    + " \n"
    + "  <VirtualCube enabled=\"true\" name=\"Virtual Cube\">\n"
    + "\t<VirtualCubeDimension cubeName=\"Sales\" highCardinality=\"false\" name=\"Product\">\n"
    + "    </VirtualCubeDimension>\n"
    + "    <VirtualCubeMeasure cubeName=\"Sales 1\" name=\"[Measures].[Unit Sales1]\" visible=\"true\">\n"
    + "    </VirtualCubeMeasure>\n"
    + "  </VirtualCube>\n" + "</Schema>";
    */
    final String query =
        "with member [Measures].[TestValid] as ValidMeasure([Measures].[Unit Sales1])\n"
        + "select [Measures].[TestValid] on columns,\n"
        + "TopCount([Product].[BrandOnly].[Product].members, 1) on rows\n"
        + "from [Virtual Cube]";
    /*
    TestUtil.withSchema(context, schema);
     */
    withSchemaEmf(context, SchemaModifiersEmf.ValidMeasureFunDefTestModifier::new);

    final String expected = "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[TestValid]}\n" + "Axis #2:\n"
        + "{[Product].[BrandOnly].[ADJ]}\n" + "Row #0: 266,773\n";

    assertQueryReturns(context.getConnectionWithDefaultRole(),
        query, expected);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testValidMeasureWithNullTuple(Context<?> context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "with member measures.vm as "
        + "'ValidMeasure((Measures.[Unit Sales], Store.[All Stores].Parent))' "
        + "select measures.vm on 0 from [warehouse and sales]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[vm]}\n"
        + "Row #0: \n");
  }
}
