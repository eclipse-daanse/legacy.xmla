/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package mondrian.rolap.aggmatcher;

import org.eclipse.daanse.rolap.mapping.model.AggregationColumnName;
import org.eclipse.daanse.rolap.mapping.model.AggregationLevel;
import org.eclipse.daanse.rolap.mapping.model.AggregationMeasure;
import org.eclipse.daanse.rolap.mapping.model.AggregationName;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.JoinQuery;
import org.eclipse.daanse.rolap.mapping.model.JoinedQueryElement;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.MemberProperty;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.PhysicalTable;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

public class MultipleColsInTupleAggTestModifierEmf implements CatalogMappingSupplier {

    private final CatalogImpl originalCatalog;

    public MultipleColsInTupleAggTestModifierEmf(Catalog catalog) {
        this.originalCatalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) catalog);
        // Create fact table columns
        PhysicalColumn prodIdFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prodIdFact.setName("prod_id");
        prodIdFact.setType(ColumnType.INTEGER);

        PhysicalColumn storeIdFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeIdFact.setName("store_id");
        storeIdFact.setType(ColumnType.INTEGER);

        PhysicalColumn amountFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        amountFact.setName("amount");
        amountFact.setType(ColumnType.INTEGER);

        // Create fact table
        PhysicalTable fact = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        fact.setName("fact");
        fact.getColumns().add(prodIdFact);
        fact.getColumns().add(storeIdFact);
        fact.getColumns().add(amountFact);

        // Create store_csv table columns
        PhysicalColumn storeIdStoreCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeIdStoreCsv.setName("store_id");
        storeIdStoreCsv.setType(ColumnType.INTEGER);

        PhysicalColumn valueStoreCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        valueStoreCsv.setName("value");
        valueStoreCsv.setType(ColumnType.INTEGER);

        // Create store_csv table
        PhysicalTable storeCsv = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        storeCsv.setName("store_csv");
        storeCsv.getColumns().add(storeIdStoreCsv);
        storeCsv.getColumns().add(valueStoreCsv);

        // Create product_csv table columns
        PhysicalColumn prodIdProductCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prodIdProductCsv.setName("prod_id");
        prodIdProductCsv.setType(ColumnType.INTEGER);

        PhysicalColumn prodCatProductCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prodCatProductCsv.setName("prod_cat");
        prodCatProductCsv.setType(ColumnType.INTEGER);

        PhysicalColumn name1ProductCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        name1ProductCsv.setName("name1");
        name1ProductCsv.setType(ColumnType.VARCHAR);
        name1ProductCsv.setCharOctetLength(30);

        PhysicalColumn colorProductCsv = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        colorProductCsv.setName("color");
        colorProductCsv.setType(ColumnType.VARCHAR);
        colorProductCsv.setCharOctetLength(30);

        // Create product_csv table
        PhysicalTable productCsv = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        productCsv.setName("product_csv");
        productCsv.getColumns().add(prodIdProductCsv);
        productCsv.getColumns().add(prodCatProductCsv);
        productCsv.getColumns().add(name1ProductCsv);
        productCsv.getColumns().add(colorProductCsv);

        // Create cat table columns
        PhysicalColumn catCat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        catCat.setName("cat");
        catCat.setType(ColumnType.INTEGER);

        PhysicalColumn name3Cat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        name3Cat.setName("name3");
        name3Cat.setType(ColumnType.VARCHAR);
        name3Cat.setCharOctetLength(30);

        PhysicalColumn ordCat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        ordCat.setName("ord");
        ordCat.setType(ColumnType.INTEGER);

        PhysicalColumn capCat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        capCat.setName("cap");
        capCat.setType(ColumnType.VARCHAR);
        capCat.setCharOctetLength(30);

        // Create cat table
        PhysicalTable cat = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        cat.setName("cat");
        cat.getColumns().add(catCat);
        cat.getColumns().add(name3Cat);
        cat.getColumns().add(ordCat);
        cat.getColumns().add(capCat);

        // Create product_cat table columns
        PhysicalColumn prodCatProductCat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prodCatProductCat.setName("prod_cat");
        prodCatProductCat.setType(ColumnType.INTEGER);

        PhysicalColumn catProductCat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        catProductCat.setName("cat");
        catProductCat.setType(ColumnType.INTEGER);

        PhysicalColumn name2ProductCat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        name2ProductCat.setName("name2");
        name2ProductCat.setType(ColumnType.VARCHAR);
        name2ProductCat.setCharOctetLength(30);

        PhysicalColumn ordProductCat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        ordProductCat.setName("ord");
        ordProductCat.setType(ColumnType.INTEGER);

        PhysicalColumn capProductCat = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        capProductCat.setName("cap");
        capProductCat.setType(ColumnType.INTEGER);

        // Create product_cat table
        PhysicalTable productCat = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        productCat.setName("product_cat");
        productCat.getColumns().add(prodCatProductCat);
        productCat.getColumns().add(catProductCat);
        productCat.getColumns().add(name2ProductCat);
        productCat.getColumns().add(ordProductCat);
        productCat.getColumns().add(capProductCat);

        // Create test_lp_xxx_fact aggregation table columns
        PhysicalColumn categoryTestLpXxxFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        categoryTestLpXxxFact.setName("category");
        categoryTestLpXxxFact.setType(ColumnType.INTEGER);

        PhysicalColumn productCategoryTestLpXxxFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        productCategoryTestLpXxxFact.setName("product_category");
        productCategoryTestLpXxxFact.setType(ColumnType.VARCHAR);
        productCategoryTestLpXxxFact.setCharOctetLength(30);

        PhysicalColumn amountTestLpXxxFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        amountTestLpXxxFact.setName("amount");
        amountTestLpXxxFact.setType(ColumnType.INTEGER);

        PhysicalColumn factCountTestLpXxxFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountTestLpXxxFact.setName("fact_count");
        factCountTestLpXxxFact.setType(ColumnType.INTEGER);

        // Create test_lp_xxx_fact aggregation table
        PhysicalTable testLpXxxFact = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        testLpXxxFact.setName("test_lp_xxx_fact");
        testLpXxxFact.getColumns().add(categoryTestLpXxxFact);
        testLpXxxFact.getColumns().add(productCategoryTestLpXxxFact);
        testLpXxxFact.getColumns().add(amountTestLpXxxFact);
        testLpXxxFact.getColumns().add(factCountTestLpXxxFact);

        // Create test_lp_xx2_fact aggregation table columns
        PhysicalColumn prodnameTestLpXx2Fact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prodnameTestLpXx2Fact.setName("prodname");
        prodnameTestLpXx2Fact.setType(ColumnType.VARCHAR);
        prodnameTestLpXx2Fact.setCharOctetLength(30);

        PhysicalColumn amountTestLpXx2Fact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        amountTestLpXx2Fact.setName("amount");
        amountTestLpXx2Fact.setType(ColumnType.INTEGER);

        PhysicalColumn factCountTestLpXx2Fact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountTestLpXx2Fact.setName("fact_count");
        factCountTestLpXx2Fact.setType(ColumnType.INTEGER);

        // Create test_lp_xx2_fact aggregation table
        PhysicalTable testLpXx2Fact = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        testLpXx2Fact.setName("test_lp_xx2_fact");
        testLpXx2Fact.getColumns().add(prodnameTestLpXx2Fact);
        testLpXx2Fact.getColumns().add(amountTestLpXx2Fact);
        testLpXx2Fact.getColumns().add(factCountTestLpXx2Fact);

        // Create first aggregation name (test_lp_xxx_fact)
        AggregationColumnName aggFactCount1 = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount1.setColumn(factCountTestLpXxxFact);

        AggregationMeasure aggMeasure1 = RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure1.setColumn(amountTestLpXxxFact);
        aggMeasure1.setName("[Measures].[Total]");

        AggregationLevel aggLevel1 = RolapMappingFactory.eINSTANCE.createAggregationLevel();
        aggLevel1.setColumn(categoryTestLpXxxFact);
        aggLevel1.setName("[Product].[Product].[Category]");

        AggregationLevel aggLevel2 = RolapMappingFactory.eINSTANCE.createAggregationLevel();
        aggLevel2.setColumn(productCategoryTestLpXxxFact);
        aggLevel2.setName("[Product].[Product].[Product Category]");

        AggregationName aggregationName1 = RolapMappingFactory.eINSTANCE.createAggregationName();
        aggregationName1.setName(testLpXxxFact);
        aggregationName1.setAggregationFactCount(aggFactCount1);
        aggregationName1.getAggregationMeasures().add(aggMeasure1);
        aggregationName1.getAggregationLevels().add(aggLevel1);
        aggregationName1.getAggregationLevels().add(aggLevel2);

        // Create second aggregation name (test_lp_xx2_fact)
        AggregationColumnName aggFactCount2 = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount2.setColumn(factCountTestLpXx2Fact);

        AggregationMeasure aggMeasure2 = RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure2.setColumn(amountTestLpXx2Fact);
        aggMeasure2.setName("[Measures].[Total]");

        AggregationLevel aggLevel3 = RolapMappingFactory.eINSTANCE.createAggregationLevel();
        aggLevel3.setColumn(prodnameTestLpXx2Fact);
        aggLevel3.setName("[Product].[Product].[Product Name]");
        aggLevel3.setCollapsed(false);

        AggregationName aggregationName2 = RolapMappingFactory.eINSTANCE.createAggregationName();
        aggregationName2.setName(testLpXx2Fact);
        aggregationName2.setAggregationFactCount(aggFactCount2);
        aggregationName2.getAggregationMeasures().add(aggMeasure2);
        aggregationName2.getAggregationLevels().add(aggLevel3);

        // Create table query with aggregations
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(fact);
        tableQuery.getAggregationTables().add(aggregationName1);
        tableQuery.getAggregationTables().add(aggregationName2);

        // Create Store dimension hierarchy
        Level storeValueLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeValueLevel.setName("Store Value");
        storeValueLevel.setColumn(valueStoreCsv);
        storeValueLevel.setUniqueMembers(true);

        TableQuery storeQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        storeQuery.setTable(storeCsv);

        ExplicitHierarchy storeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        storeHierarchy.setHasAll(true);
        storeHierarchy.setPrimaryKey(storeIdStoreCsv);
        storeHierarchy.setQuery(storeQuery);
        storeHierarchy.getLevels().add(storeValueLevel);

        StandardDimension storeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        storeDimension.setName("Store");
        storeDimension.getHierarchies().add(storeHierarchy);

        DimensionConnector storeConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeConnector.setOverrideDimensionName("Store");
        storeConnector.setForeignKey(storeIdFact);
        storeConnector.setDimension(storeDimension);

        // Create Product dimension with complex joins
        // Create Product Name level with property
        MemberProperty productColorProperty = RolapMappingFactory.eINSTANCE.createMemberProperty();
        productColorProperty.setName("Product Color");
        productColorProperty.setColumn(colorProductCsv);

        Level productNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productNameLevel.setName("Product Name");
        productNameLevel.setColumn(name1ProductCsv);
        productNameLevel.setUniqueMembers(true);
        productNameLevel.getMemberProperties().add(productColorProperty);

        Level productCategoryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productCategoryLevel.setName("Product Category");
        productCategoryLevel.setColumn(name2ProductCat);
        productCategoryLevel.setOrdinalColumn(ordProductCat);
        productCategoryLevel.setCaptionColumn(capProductCat);
        productCategoryLevel.setUniqueMembers(false);

        Level categoryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        categoryLevel.setName("Category");
        categoryLevel.setColumn(catCat);
        categoryLevel.setOrdinalColumn(ordCat);
        categoryLevel.setCaptionColumn(capCat);
        categoryLevel.setNameColumn(name3Cat);
        categoryLevel.setUniqueMembers(false);
        categoryLevel.setColumnType(ColumnInternalDataType.NUMERIC);

        // Create join: product_cat JOIN cat
        TableQuery catQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        catQuery.setTable(cat);

        JoinedQueryElement catJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        catJoinedElement.setKey(catCat);
        catJoinedElement.setQuery(catQuery);

        TableQuery productCatQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productCatQuery.setTable(productCat);

        JoinedQueryElement productCatJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        productCatJoinedElement.setKey(catProductCat);
        productCatJoinedElement.setQuery(productCatQuery);

        JoinQuery innerJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        innerJoin.setLeft(productCatJoinedElement);
        innerJoin.setRight(catJoinedElement);

        // Create join: product_csv JOIN (product_cat JOIN cat)
        TableQuery productCsvQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productCsvQuery.setTable(productCsv);

        JoinedQueryElement productCsvJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        productCsvJoinedElement.setKey(prodCatProductCsv);
        productCsvJoinedElement.setQuery(productCsvQuery);

        JoinedQueryElement innerJoinElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        innerJoinElement.setAlias("product_cat");
        innerJoinElement.setKey(prodCatProductCat);
        innerJoinElement.setQuery(innerJoin);

        JoinQuery outerJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        outerJoin.setLeft(productCsvJoinedElement);
        outerJoin.setRight(innerJoinElement);

        ExplicitHierarchy productHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        productHierarchy.setHasAll(true);
        productHierarchy.setPrimaryKey(prodIdProductCsv);
        productHierarchy.setQuery(outerJoin);
        productHierarchy.getLevels().add(categoryLevel);
        productHierarchy.getLevels().add(productCategoryLevel);
        productHierarchy.getLevels().add(productNameLevel);

        StandardDimension productDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        productDimension.setName("Product");
        productDimension.getHierarchies().add(productHierarchy);

        DimensionConnector productConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        productConnector.setOverrideDimensionName("Product");
        productConnector.setForeignKey(prodIdFact);
        productConnector.setDimension(productDimension);

        // Create measures
        SumMeasure totalMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        totalMeasure.setName("Total");
        totalMeasure.setColumn(amountFact);
        totalMeasure.setFormatString("#,###");

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(totalMeasure);

        // Create cube
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("Fact");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(storeConnector);
        cube.getDimensionConnectors().add(productConnector);
        cube.getMeasureGroups().add(measureGroup);

        // Add tables to database schema
        if (originalCatalog.getDbschemas().size() > 0) {
            originalCatalog.getDbschemas().get(0).getTables().add(fact);
            originalCatalog.getDbschemas().get(0).getTables().add(storeCsv);
            originalCatalog.getDbschemas().get(0).getTables().add(productCsv);
            originalCatalog.getDbschemas().get(0).getTables().add(cat);
            originalCatalog.getDbschemas().get(0).getTables().add(productCat);
            originalCatalog.getDbschemas().get(0).getTables().add(testLpXxxFact);
            originalCatalog.getDbschemas().get(0).getTables().add(testLpXx2Fact);
        }

        // Add the cube to the catalog copy
        originalCatalog.getCubes().add(cube);

    }

    /*
        return "<Cube name='Fact'>\n"
       + "<Table name='fact'>\n"
       + " <AggName name='test_lp_xxx_fact'>\n"
       + "  <AggFactCount column='fact_count'/>\n"
       + "  <AggMeasure column='amount' name='[Measures].[Total]'/>\n"
       + "  <AggLevel column='category' name='[Product].[Category]'/>\n"
       + "  <AggLevel column='product_category' "
       + "            name='[Product].[Product Category]'/>\n"
       + " </AggName>\n"
        + " <AggName name='test_lp_xx2_fact'>\n"
        + "  <AggFactCount column='fact_count'/>\n"
        + "  <AggMeasure column='amount' name='[Measures].[Total]'/>\n"
        + "  <AggLevel column='prodname' name='[Product].[Product Name]' collapsed='false'/>\n"
        + " </AggName>\n"
       + "</Table>"
       + "<Dimension name='Store' foreignKey='store_id'>\n"
       + " <Hierarchy hasAll='true' primaryKey='store_id'>\n"
       + "  <Table name='store_csv'/>\n"
       + "  <Level name='Store Value' column='value' "
       + "         uniqueMembers='true'/>\n"
       + " </Hierarchy>\n"
       + "</Dimension>\n"
       + "<Dimension name='Product' foreignKey='prod_id'>\n"
       + " <Hierarchy hasAll='true' primaryKey='prod_id' "
       + "primaryKeyTable='product_csv'>\n"
       + " <Join leftKey='prod_cat' rightAlias='product_cat' "
       + "rightKey='prod_cat'>\n"
       + "  <Table name='product_csv'/>\n"
       + "  <Join leftKey='cat' rightKey='cat'>\n"
       + "   <Table name='product_cat'/>\n"
       + "   <Table name='cat'/>\n"
       + "  </Join>"
       + " </Join>\n"
       + " <Level name='Category' table='cat' column='cat' "
       + "ordinalColumn='ord' captionColumn='cap' nameColumn='name3' "
       + "uniqueMembers='false' type='Numeric'/>\n"
       + " <Level name='Product Category' table='product_cat' "
       + "column='name2' ordinalColumn='ord' captionColumn='cap' "
       + "uniqueMembers='false'/>\n"
       + " <Level name='Product Name' table='product_csv' column='name1' "
       + "uniqueMembers='true'>\n"
        + "<Property name='Product Color' table='product_csv' column='color' />"
        + "</Level>"
       + " </Hierarchy>\n"
       + "</Dimension>\n"
       + "<Measure name='Total' \n"
       + "    column='amount' aggregator='sum'\n"
       + "   formatString='#,###'/>\n"
       + "</Cube>";

     */

    @Override
    public Catalog get() {

        return originalCatalog;
    }
}
