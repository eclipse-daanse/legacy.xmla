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
import org.eclipse.daanse.rolap.mapping.model.AggregationForeignKey;
import org.eclipse.daanse.rolap.mapping.model.AggregationMeasure;
import org.eclipse.daanse.rolap.mapping.model.AggregationName;
import org.eclipse.daanse.rolap.mapping.model.AvgMeasure;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.CountMeasure;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.PhysicalTable;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

/*
return "<Cube name='Cheques'>\n"
       + "<Table name='cheques'>\n"
       + "<AggName name='agg_lp_xxx_cheques'>\n"
       + "<AggFactCount column='FACT_COUNT'/>\n"
       + "<AggForeignKey factColumn='store_id' aggColumn='store_id' />\n"
       + "<AggMeasure name='[Measures].[Avg Amount]'\n"
       + "   column='amount_AVG' />\n"
       + "</AggName>\n"
           + "</Table>\n"
           + "<Dimension name='StoreX' foreignKey='store_id'>\n"
           + " <Hierarchy hasAll='true' primaryKey='store_id'>\n"
           + " <Table name='store_x'/>\n"
           + " <Level name='Store Value' column='value' uniqueMembers='true'/>\n"
           + " </Hierarchy>\n"
           + "</Dimension>\n"
           + "<Dimension name='ProductX' foreignKey='prod_id'>\n"
           + " <Hierarchy hasAll='true' primaryKey='prod_id'>\n"
           + " <Table name='product_x'/>\n"
           + " <Level name='Store Name' column='name' uniqueMembers='true'/>\n"
           + " </Hierarchy>\n"
           + "</Dimension>\n"

           + "<Measure name='Sales Count' \n"
           + "    column='prod_id' aggregator='count'\n"
           + "   formatString='#,###'/>\n"
           + "<Measure name='Store Count' \n"
           + "    column='store_id' aggregator='distinct-count'\n"
           + "   formatString='#,###'/>\n"
           + "<Measure name='Total Amount' \n"
           + "    column='amount' aggregator='sum'\n"
           + "   formatString='#,###'/>\n"
           + "<Measure name='Avg Amount' \n"
           + "    column='amount' aggregator='avg'\n"
           + "   formatString='00.0'/>\n"
           + "</Cube>";

*/
/*
public class BUG_1541077Modifier extends PojoMappingModifier {

    public BUG_1541077Modifier(CatalogMapping c) {
        super(c);
    }


    protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
    	//## ColumnNames: prod_id,store_id,amount
    	//## ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2)
        PhysicalColumnMappingImpl store_id_cheques = PhysicalColumnMappingImpl.builder().withName("store_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl prod_id_cheques = PhysicalColumnMappingImpl.builder().withName("prod_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl amount_cheques = PhysicalColumnMappingImpl.builder().withName("amount").withDataType(ColumnDataType.DECIMAL).withColumnSize(10).withDecimalDigits(2).build();
        PhysicalTableMappingImpl cheques = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("cheques")
                .withColumns(List.of(
                        store_id_cheques, prod_id_cheques, amount_cheques
                        ))).build();
        //## ColumnNames: store_id,value
        //## ColumnTypes: INTEGER,DECIMAL(10,2)
        PhysicalColumnMappingImpl store_id_store_x = PhysicalColumnMappingImpl.builder().withName("store_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl value_store_x = PhysicalColumnMappingImpl.builder().withName("store_id").withDataType(ColumnDataType.DECIMAL).withColumnSize(10).withDecimalDigits(2).build();
        PhysicalTableMappingImpl store_x = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("store_x")
                .withColumns(List.of(
                        store_id_store_x, value_store_x
                        ))).build();
        //## ColumnNames: prod_id,name
        //## ColumnTypes: INTEGER,VARCHAR(30)
        PhysicalColumnMappingImpl prod_id_product_x = PhysicalColumnMappingImpl.builder().withName("prod_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl name_product_x = PhysicalColumnMappingImpl.builder().withName("name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
        PhysicalTableMappingImpl product_x = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("product_x")
                .withColumns(List.of(
                        prod_id_product_x, name_product_x
                        ))).build();
        //## TableName: agg_lp_xxx_cheques
        //## ColumnNames: store_id,amount_AVG,FACT_COUNT
        //## ColumnTypes: INTEGER,DECIMAL(10,2),INTEGER
        PhysicalColumnMappingImpl storeId = PhysicalColumnMappingImpl.builder().withName("store_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl amountAvg = PhysicalColumnMappingImpl.builder().withName("amount_AVG").withDataType(ColumnDataType.DECIMAL).withCharOctetLength(10).withDecimalDigits(2).build();
        PhysicalColumnMappingImpl factCount = PhysicalColumnMappingImpl.builder().withName("FACT_COUNT").withDataType(ColumnDataType.INTEGER).build();
        PhysicalTableMappingImpl aggLpXxxCheques = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("agg_lp_xxx_cheques")
                .withColumns(List.of(
                		storeId, amountAvg, factCount
                        ))).build();

        List<CubeMapping> result = new ArrayList<>();
        result.addAll(super.catalogCubes(schema));
        result.add(PhysicalCubeMappingImpl.builder()
            .withName("Cheques")
            .withQuery(TableQueryMappingImpl.builder().withTable(cheques)
            		.withAggregationTables(List.of(
                            AggregationNameMappingImpl.builder()
                            .withName(aggLpXxxCheques)
                            .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                                .withColumn(factCount)
                                .build())
                            .withAggregationForeignKeys(List.of(
                            	AggregationForeignKeyMappingImpl.builder()
                                    .withFactColumn(store_id_cheques)
                                    .withAggregationColumn(storeId)
                                    .build()
                            ))
                            .withAggregationMeasures(List.of(
                                AggregationMeasureMappingImpl.builder()
                                    .withName("[Measures].[Avg Amount]")
                                    .withColumn(amountAvg)
                                    .build()
                            ))
                            .build()
            		))
            		.build())
            .withDimensionConnectors(List.of(
            	DimensionConnectorMappingImpl.builder()
            		.withOverrideDimensionName("StoreX")
                    .withForeignKey(store_id_cheques)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("StoreX")
                    	.withHierarchies(List.of(
                        ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(store_id_store_x)
                            .withQuery(TableQueryMappingImpl.builder().withTable(store_x).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Store Value")
                                    .withColumn(value_store_x)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build(),
                DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("ProductX")
                    .withForeignKey(prod_id_cheques)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("ProductX")
                        .withHierarchies(List.of(
                        ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(prod_id_product_x)
                            .withQuery(TableQueryMappingImpl.builder().withTable(product_x).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Store Name")
                                    .withColumn(name_product_x)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build()
            ))
            .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                CountMeasureMappingImpl.builder()
                    .withName("Sales Count")
                    .withColumn(prod_id_cheques)
                    .withFormatString("#,###")
                    .build(),
                CountMeasureMappingImpl.builder()
                    .withName("Store Count")
                    .withColumn(store_id_cheques)
                    .withDistinct(true)
                    .withFormatString("#,###")
                    .build(),
                SumMeasureMappingImpl.builder()
                    .withName("Total Amount")
                    .withColumn(amount_cheques)
                    .withFormatString("#,###")
                    .build(),
                AvgMeasureMappingImpl.builder()
                    .withName("Avg Amount")
                    .withColumn(amount_cheques)
                    .withFormatString("00.0")
                    .build()
            )).build()))
            .build());
        return result;

    }
}
*/
public class BUG_1541077Modifier implements CatalogMappingSupplier {

    private final Catalog originalCatalog;

    public BUG_1541077Modifier(Catalog catalog) {
        this.originalCatalog = catalog;
    }

    /*
        return "<Cube name='Cheques'>\n"
               + "<Table name='cheques'>\n"
               + "<AggName name='agg_lp_xxx_cheques'>\n"
               + "<AggFactCount column='FACT_COUNT'/>\n"
               + "<AggForeignKey factColumn='store_id' aggColumn='store_id' />\n"
               + "<AggMeasure name='[Measures].[Avg Amount]'\n"
               + "   column='amount_AVG' />\n"
               + "</AggName>\n"
                   + "</Table>\n"
                   + "<Dimension name='StoreX' foreignKey='store_id'>\n"
                   + " <Hierarchy hasAll='true' primaryKey='store_id'>\n"
                   + " <Table name='store_x'/>\n"
                   + " <Level name='Store Value' column='value' uniqueMembers='true'/>\n"
                   + " </Hierarchy>\n"
                   + "</Dimension>\n"
                   + "<Dimension name='ProductX' foreignKey='prod_id'>\n"
                   + " <Hierarchy hasAll='true' primaryKey='prod_id'>\n"
                   + " <Table name='product_x'/>\n"
                   + " <Level name='Store Name' column='name' uniqueMembers='true'/>\n"
                   + " </Hierarchy>\n"
                   + "</Dimension>\n"

                   + "<Measure name='Sales Count' \n"
                   + "    column='prod_id' aggregator='count'\n"
                   + "   formatString='#,###'/>\n"
                   + "<Measure name='Store Count' \n"
                   + "    column='store_id' aggregator='distinct-count'\n"
                   + "   formatString='#,###'/>\n"
                   + "<Measure name='Total Amount' \n"
                   + "    column='amount' aggregator='sum'\n"
                   + "   formatString='#,###'/>\n"
                   + "<Measure name='Avg Amount' \n"
                   + "    column='amount' aggregator='avg'\n"
                   + "   formatString='00.0'/>\n"
                   + "</Cube>";

     */

    @Override
    public Catalog get() {
        Catalog catalogCopy = org.opencube.junit5.EmfUtil.copy( (CatalogImpl) originalCatalog);

        // Create columns for cheques table using RolapMappingFactory
        // ColumnNames: prod_id,store_id,amount
        // ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2)
        PhysicalColumn store_id_cheques = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        store_id_cheques.setName("store_id");
        store_id_cheques.setType(ColumnType.INTEGER);

        PhysicalColumn prod_id_cheques = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prod_id_cheques.setName("prod_id");
        prod_id_cheques.setType(ColumnType.INTEGER);

        PhysicalColumn amount_cheques = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        amount_cheques.setName("amount");
        amount_cheques.setType(ColumnType.DECIMAL);
        amount_cheques.setColumnSize(10);
        amount_cheques.setDecimalDigits(2);

        // Create cheques table using RolapMappingFactory
        PhysicalTable cheques = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        cheques.setName("cheques");
        cheques.getColumns().add(store_id_cheques);
        cheques.getColumns().add(prod_id_cheques);
        cheques.getColumns().add(amount_cheques);

        // Create columns for store_x table using RolapMappingFactory
        // ColumnNames: store_id,value
        // ColumnTypes: INTEGER,DECIMAL(10,2)
        PhysicalColumn store_id_store_x = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        store_id_store_x.setName("store_id");
        store_id_store_x.setType(ColumnType.INTEGER);

        PhysicalColumn value_store_x = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        value_store_x.setName("value");
        value_store_x.setType(ColumnType.DECIMAL);
        value_store_x.setColumnSize(10);
        value_store_x.setDecimalDigits(2);

        // Create store_x table using RolapMappingFactory
        PhysicalTable store_x = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        store_x.setName("store_x");
        store_x.getColumns().add(store_id_store_x);
        store_x.getColumns().add(value_store_x);

        // Create columns for product_x table using RolapMappingFactory
        // ColumnNames: prod_id,name
        // ColumnTypes: INTEGER,VARCHAR(30)
        PhysicalColumn prod_id_product_x = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prod_id_product_x.setName("prod_id");
        prod_id_product_x.setType(ColumnType.INTEGER);

        PhysicalColumn name_product_x = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        name_product_x.setName("name");
        name_product_x.setType(ColumnType.VARCHAR);
        name_product_x.setCharOctetLength(30);

        // Create product_x table using RolapMappingFactory
        PhysicalTable product_x = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        product_x.setName("product_x");
        product_x.getColumns().add(prod_id_product_x);
        product_x.getColumns().add(name_product_x);

        // Create columns for agg_lp_xxx_cheques table using RolapMappingFactory
        // TableName: agg_lp_xxx_cheques
        // ColumnNames: store_id,amount_AVG,FACT_COUNT
        // ColumnTypes: INTEGER,DECIMAL(10,2),INTEGER
        PhysicalColumn storeId = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        storeId.setName("store_id");
        storeId.setType(ColumnType.INTEGER);

        PhysicalColumn amountAvg = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        amountAvg.setName("amount_AVG");
        amountAvg.setType(ColumnType.DECIMAL);
        amountAvg.setColumnSize(10);
        amountAvg.setDecimalDigits(2);

        PhysicalColumn factCount = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCount.setName("FACT_COUNT");
        factCount.setType(ColumnType.INTEGER);

        // Create agg_lp_xxx_cheques table using RolapMappingFactory
        PhysicalTable aggLpXxxCheques = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        aggLpXxxCheques.setName("agg_lp_xxx_cheques");
        aggLpXxxCheques.getColumns().add(storeId);
        aggLpXxxCheques.getColumns().add(amountAvg);
        aggLpXxxCheques.getColumns().add(factCount);

        // Create aggregation column name for fact count using RolapMappingFactory
        AggregationColumnName aggFactCount = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCount);

        // Create aggregation foreign key using RolapMappingFactory
        AggregationForeignKey aggForeignKey = RolapMappingFactory.eINSTANCE.createAggregationForeignKey();
        aggForeignKey.setFactColumn(store_id_cheques);
        aggForeignKey.setAggregationColumn(storeId);

        // Create aggregation measure using RolapMappingFactory
        AggregationMeasure aggMeasure = RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setName("[Measures].[Avg Amount]");
        aggMeasure.setColumn(amountAvg);

        // Create aggregation name using RolapMappingFactory
        AggregationName aggregationName = RolapMappingFactory.eINSTANCE.createAggregationName();
        aggregationName.setName(aggLpXxxCheques);
        aggregationName.setAggregationFactCount(aggFactCount);
        aggregationName.getAggregationForeignKeys().add(aggForeignKey);
        aggregationName.getAggregationMeasures().add(aggMeasure);

        // Create table query with aggregation using RolapMappingFactory
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(cheques);
        tableQuery.getAggregationTables().add(aggregationName);

        // Create StoreX hierarchy using RolapMappingFactory
        Level storeValueLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeValueLevel.setName("Store Value");
        storeValueLevel.setColumn(value_store_x);
        storeValueLevel.setUniqueMembers(true);

        TableQuery storeQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        storeQuery.setTable(store_x);

        ExplicitHierarchy storeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        storeHierarchy.setHasAll(true);
        storeHierarchy.setPrimaryKey(store_id_store_x);
        storeHierarchy.setQuery(storeQuery);
        storeHierarchy.getLevels().add(storeValueLevel);

        StandardDimension storeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        storeDimension.setName("StoreX");
        storeDimension.getHierarchies().add(storeHierarchy);

        DimensionConnector storeConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeConnector.setOverrideDimensionName("StoreX");
        storeConnector.setForeignKey(store_id_cheques);
        storeConnector.setDimension(storeDimension);

        // Create ProductX hierarchy using RolapMappingFactory
        Level storeNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeNameLevel.setName("Store Name");
        storeNameLevel.setColumn(name_product_x);
        storeNameLevel.setUniqueMembers(true);

        TableQuery productQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productQuery.setTable(product_x);

        ExplicitHierarchy productHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        productHierarchy.setHasAll(true);
        productHierarchy.setPrimaryKey(prod_id_product_x);
        productHierarchy.setQuery(productQuery);
        productHierarchy.getLevels().add(storeNameLevel);

        StandardDimension productDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        productDimension.setName("ProductX");
        productDimension.getHierarchies().add(productHierarchy);

        DimensionConnector productConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        productConnector.setOverrideDimensionName("ProductX");
        productConnector.setForeignKey(prod_id_cheques);
        productConnector.setDimension(productDimension);

        // Create measures using RolapMappingFactory
        CountMeasure salesCountMeasure = RolapMappingFactory.eINSTANCE.createCountMeasure();
        salesCountMeasure.setName("Sales Count");
        salesCountMeasure.setColumn(prod_id_cheques);
        salesCountMeasure.setFormatString("#,###");

        CountMeasure storeCountMeasure = RolapMappingFactory.eINSTANCE.createCountMeasure();
        storeCountMeasure.setName("Store Count");
        storeCountMeasure.setColumn(store_id_cheques);
        storeCountMeasure.setDistinct(true);
        storeCountMeasure.setFormatString("#,###");

        SumMeasure totalAmountMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        totalAmountMeasure.setName("Total Amount");
        totalAmountMeasure.setColumn(amount_cheques);
        totalAmountMeasure.setFormatString("#,###");

        AvgMeasure avgAmountMeasure = RolapMappingFactory.eINSTANCE.createAvgMeasure();
        avgAmountMeasure.setName("Avg Amount");
        avgAmountMeasure.setColumn(amount_cheques);
        avgAmountMeasure.setFormatString("00.0");

        // Create measure group using RolapMappingFactory
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(salesCountMeasure);
        measureGroup.getMeasures().add(storeCountMeasure);
        measureGroup.getMeasures().add(totalAmountMeasure);
        measureGroup.getMeasures().add(avgAmountMeasure);

        // Create cube using RolapMappingFactory
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("Cheques");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(storeConnector);
        cube.getDimensionConnectors().add(productConnector);
        cube.getMeasureGroups().add(measureGroup);

        // Add the cube to the catalog copy
        catalogCopy.getCubes().add(cube);

        return catalogCopy;
    }
}
