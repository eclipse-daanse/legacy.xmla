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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DatabaseSchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
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
import org.eclipse.daanse.rolap.mapping.pojo.MemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;

public class MultipleColsInTupleAggTestModifier extends PojoMappingModifier {

    //## ColumnNames: prod_id,store_id,amount
    //## ColumnTypes: INTEGER,INTEGER,INTEGER
    ColumnMappingImpl prodIdFact = ColumnMappingImpl.builder().withName("prod_id").withType("INTEGER").build();
    ColumnMappingImpl storeIdFact = ColumnMappingImpl.builder().withName("store_id").withType("INTEGER").build();
    ColumnMappingImpl amountFact = ColumnMappingImpl.builder().withName("amount").withType("INTEGER").build();
    PhysicalTableMappingImpl fact = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("fact")
            .withColumns(List.of(prodIdFact, storeIdFact, amountFact))).build();
    //## TableName: store_csv
    //## ColumnNames: store_id,value
    //## ColumnTypes: INTEGER,INTEGER
    ColumnMappingImpl storeIdStoreCsv = ColumnMappingImpl.builder().withName("store_id").withType("INTEGER").build();
    ColumnMappingImpl valueStoreCsv = ColumnMappingImpl.builder().withName("value").withType("INTEGER").build();
    PhysicalTableMappingImpl storeCsv = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("store_csv")
            .withColumns(List.of(storeIdStoreCsv, valueStoreCsv))).build();
    //## TableName: product_csv
    //## ColumnNames: prod_id,prod_cat,name1,color
    //## ColumnTypes: INTEGER,INTEGER,VARCHAR(30),VARCHAR(30)
    ColumnMappingImpl prodIdProductCsv = ColumnMappingImpl.builder().withName("prod_id").withType("INTEGER").build();
    ColumnMappingImpl prodCatProductCsv = ColumnMappingImpl.builder().withName("prod_cat").withType("INTEGER").build();
    ColumnMappingImpl name1ProductCsv = ColumnMappingImpl.builder().withName("name1").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl colorProductCsv = ColumnMappingImpl.builder().withName("color").withType("VARCHAR").withCharOctetLength(30).build();
    PhysicalTableMappingImpl productCsv = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("product_csv")
            .withColumns(List.of(prodIdProductCsv, prodCatProductCsv))).build();
    //## TableName: cat
    //## ColumnNames: cat,name3,ord,cap
    //## ColumnTypes: INTEGER,VARCHAR(30),INTEGER,VARCHAR(30)
    ColumnMappingImpl catCat = ColumnMappingImpl.builder().withName("cat").withType("INTEGER").build();
    ColumnMappingImpl name3Cat = ColumnMappingImpl.builder().withName("name3").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl ordCat = ColumnMappingImpl.builder().withName("ord").withType("INTEGER").build();
    ColumnMappingImpl capCat = ColumnMappingImpl.builder().withName("cap").withType("VARCHAR").withCharOctetLength(30).build();
    PhysicalTableMappingImpl cat = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("cat")
            .withColumns(List.of(catCat, name3Cat, ordCat, capCat))).build();
    //## TableName: product_cat
    //## ColumnNames: prod_cat,cat,name2,ord,cap
    //## ColumnTypes: INTEGER,INTEGER,VARCHAR(30),INTEGER,VARCHAR(30)
    ColumnMappingImpl prodCatProductCat = ColumnMappingImpl.builder().withName("prod_cat").withType("INTEGER").build();
    ColumnMappingImpl catProductCat = ColumnMappingImpl.builder().withName("cat").withType("INTEGER").build();
    ColumnMappingImpl name2ProductCat = ColumnMappingImpl.builder().withName("name2").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl ordProductCat = ColumnMappingImpl.builder().withName("ord").withType("INTEGER").build();
    ColumnMappingImpl capProductCat = ColumnMappingImpl.builder().withName("cap").withType("INTEGER").build();
    PhysicalTableMappingImpl productCat = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("product_cat")
            .withColumns(List.of(catCat, name3Cat, ordCat, capCat))).build();

    //## ColumnNames: category,product_category,amount,fact_count
    //## ColumnTypes: INTEGER,VARCHAR(30),INTEGER,INTEGER
    ColumnMappingImpl categoryTestLpXxxFact = ColumnMappingImpl.builder().withName("category").withType("INTEGER").build();
    ColumnMappingImpl productCategoryTestLpXxxFact = ColumnMappingImpl.builder().withName("product_category").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl amountTestLpXxxFact = ColumnMappingImpl.builder().withName("amount").withType("INTEGER").build();
    ColumnMappingImpl factCountTestLpXxxFact = ColumnMappingImpl.builder().withName("fact_count").withType("INTEGER").build();
    PhysicalTableMappingImpl testLpXxxFact = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("test_lp_xxx_fact")
            .withColumns(List.of(categoryTestLpXxxFact, productCategoryTestLpXxxFact, amountTestLpXxxFact, factCountTestLpXxxFact))).build();

    //## TableName: test_lp_xx2_fact
    //## ColumnNames: prodname,amount,fact_count
    //## ColumnTypes: VARCHAR(30),INTEGER,INTEGER
    ColumnMappingImpl prodnameTestLpXx2Fact = ColumnMappingImpl.builder().withName("prodname").withType("VARCHAR").withCharOctetLength(30).build();
    ColumnMappingImpl amountTestLpXx2Fact = ColumnMappingImpl.builder().withName("amount").withType("INTEGER").build();
    ColumnMappingImpl factCountTestLpXx2Fact = ColumnMappingImpl.builder().withName("fact_count").withType("INTEGER").build();
    PhysicalTableMappingImpl testLpXx2Fact = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("test_lp_xx2_fact")
            .withColumns(List.of(prodnameTestLpXx2Fact, amountTestLpXx2Fact, factCountTestLpXx2Fact))).build();

    public MultipleColsInTupleAggTestModifier(CatalogMapping catalog) {
        super(catalog);
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
    protected List<? extends TableMapping> databaseSchemaTables(DatabaseSchemaMapping databaseSchema) {
        List<TableMapping> result = new ArrayList<>();
        result.addAll(super.databaseSchemaTables(databaseSchema));
        result.addAll(List.of(testLpXxxFact, testLpXx2Fact, fact, storeCsv, productCsv, cat, productCat));
        return result;
    }

    @Override
    protected List<? extends CubeMapping> catalogCubes(CatalogMapping schemaMappingOriginal) {
        List<CubeMapping> result = new ArrayList<>();
        result.addAll(super.catalogCubes(schemaMappingOriginal));
        result.add(PhysicalCubeMappingImpl.builder()
            .withName("Fact")
            .withQuery(TableQueryMappingImpl.builder().withTable(fact).withAggregationTables(
                List.of(
                    AggregationNameMappingImpl.builder()
                        .withName(testLpXxxFact)
                        .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                            .withColumn(factCountTestLpXxxFact)
                            .build())
                        .withAggregationMeasures(List.of(
                            AggregationMeasureMappingImpl.builder()
                                .withColumn(amountTestLpXxxFact)
                                .withName("[Measures].[Total]")
                                .build()
                        ))
                        .withAggregationLevels(List.of(
                            AggregationLevelMappingImpl.builder()
                                .withColumn(categoryTestLpXxxFact)
                                .withName("[Product].[Category]")
                                .build(),
                            AggregationLevelMappingImpl.builder()
                                .withColumn(productCategoryTestLpXxxFact)
                                .withName("[Product].[Product Category]")
                                .build()
                        ))
                        .build(),
                    AggregationNameMappingImpl.builder()
                        .withName(testLpXx2Fact)
                        .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                            .withColumn(factCountTestLpXx2Fact)
                            .build())
                        .withAggregationMeasures(List.of(
                            AggregationMeasureMappingImpl.builder()
                                .withColumn(amountTestLpXx2Fact)
                                .withName("[Measures].[Total]")
                                .build()
                        ))
                        .withAggregationLevels(List.of(
                            AggregationLevelMappingImpl.builder()
                                .withColumn(prodnameTestLpXx2Fact)
                                .withName("[Product].[Product Name]")
                                .withCollapsed(false)
                                .build()
                        ))
                        .build()
                )).build())

            .withDimensionConnectors(List.of(
                DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("Store")
                    .withForeignKey(storeIdFact)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("Store")
                        .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(storeIdStoreCsv)
                            .withQuery(TableQueryMappingImpl.builder().withTable(storeCsv).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withColumn(valueStoreCsv)
                                    .withName("Store Value")
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()

                    )).build())
                    .build(),
                DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("Product")
                    .withForeignKey(prodIdFact)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("Product")
                        .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(prodIdProductCsv)
                            .withPrimaryKeyTable(productCsv)
                            .withQuery(JoinQueryMappingImpl.builder()
                            		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(prodCatProductCsv)
                            				.withQuery(TableQueryMappingImpl.builder().withTable(productCsv).build())
                            				.build())
                            		.withRight(JoinedQueryElementMappingImpl.builder().withAlias("product_cat").withKey(prodCatProductCat)
                                            .withQuery(JoinQueryMappingImpl.builder()
                                            		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(catProductCat)
                                            				.withQuery(TableQueryMappingImpl.builder().withTable(productCat).build())
                                            				.build())
                                            		.withRight(JoinedQueryElementMappingImpl.builder().withKey(catCat)
                                            				.withQuery(TableQueryMappingImpl.builder().withTable(cat).build())
                                            				.build())
                                            		.build())
                            				.build())
                            		.build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Category")
                                    .withTable(cat)
                                    .withColumn(catCat)
                                    .withOrdinalColumn(ordCat)
                                    .withCaptionColumn(capCat)
                                    .withNameColumn(name3Cat)
                                    .withUniqueMembers(false)
                                    .withType(DataType.NUMERIC)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withName("Product Category")
                                    .withTable(productCat)
                                    .withColumn(name2ProductCat)
                                    .withOrdinalColumn(ordProductCat)
                                    .withCaptionColumn(capProductCat)
                                    .withUniqueMembers(false)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withName("Product Name")
                                    .withTable(productCsv)
                                    .withColumn(name1ProductCsv)
                                    .withUniqueMembers(true)
                                    .withMemberProperties(List.of(
                                    	MemberPropertyMappingImpl.builder()
                                        .withName("Product Color")
                                        //.table("product_csv")
                                        .withColumn(colorProductCsv)
                                        .build()
                                    ))
                                    .build()
                            ))
                            .build()

                    )).build())
                    .build()
            ))
            .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                MeasureMappingImpl.builder()
                    .withName("Total")
                    .withColumn(amountFact)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build()
            )).build()))
            .build());
        return result;
    }
}
