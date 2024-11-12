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

import org.eclipse.daanse.rdb.structure.pojo.ColumnImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationColumnNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;

public class MultipleColsInTupleAggTestModifier extends PojoMappingModifier {

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
    protected List<? extends CubeMapping> schemaCubes(SchemaMapping schemaMappingOriginal) {
        //## ColumnNames: prod_id,store_id,amount
        //## ColumnTypes: INTEGER,INTEGER,INTEGER
        ColumnImpl prodIdFact = ColumnImpl.builder().withName("prod_id").withType("INTEGER").build();
        ColumnImpl storeIdFact = ColumnImpl.builder().withName("store_id").withType("INTEGER").build();
        ColumnImpl amountFact = ColumnImpl.builder().withName("amount").withType("INTEGER").build();
        PhysicalTableImpl fact = ((Builder) PhysicalTableImpl.builder().withName("fact")
                .withColumns(List.of(prodIdFact, storeIdFact, amountFact))).build();
        //## TableName: store_csv
        //## ColumnNames: store_id,value
        //## ColumnTypes: INTEGER,INTEGER
        ColumnImpl storeIdStoreCsv = ColumnImpl.builder().withName("store_id").withType("INTEGER").build();
        ColumnImpl valueStoreCsv = ColumnImpl.builder().withName("value").withType("INTEGER").build();
        PhysicalTableImpl storeCsv = ((Builder) PhysicalTableImpl.builder().withName("store_csv")
                .withColumns(List.of(storeIdStoreCsv, valueStoreCsv))).build();
        //## TableName: product_csv
        //## ColumnNames: prod_id,prod_cat,name1,color
        //## ColumnTypes: INTEGER,INTEGER,VARCHAR(30),VARCHAR(30)
        ColumnImpl prodIdProductCsv = ColumnImpl.builder().withName("prod_id").withType("INTEGER").build();
        ColumnImpl prodCatProductCsv = ColumnImpl.builder().withName("prod_cat").withType("INTEGER").build();
        ColumnImpl name1ProductCsv = ColumnImpl.builder().withName("name1").withType("VARCHAR").withCharOctetLength(30).build();
        ColumnImpl colorProductCsv = ColumnImpl.builder().withName("color").withType("VARCHAR").withCharOctetLength(30).build();
        PhysicalTableImpl productCsv = ((Builder) PhysicalTableImpl.builder().withName("product_csv")
                .withColumns(List.of(prodIdProductCsv, prodCatProductCsv))).build();
        //## TableName: cat
        //## ColumnNames: cat,name3,ord,cap
        //## ColumnTypes: INTEGER,VARCHAR(30),INTEGER,VARCHAR(30)
        ColumnImpl catCat = ColumnImpl.builder().withName("cat").withType("INTEGER").build();
        ColumnImpl name3Cat = ColumnImpl.builder().withName("name3").withType("VARCHAR").withCharOctetLength(30).build();
        ColumnImpl ordCat = ColumnImpl.builder().withName("ord").withType("INTEGER").build();
        ColumnImpl capCat = ColumnImpl.builder().withName("cap").withType("VARCHAR").withCharOctetLength(30).build();
        PhysicalTableImpl cat = ((Builder) PhysicalTableImpl.builder().withName("cat")
                .withColumns(List.of(catCat, name3Cat, ordCat, capCat))).build();
        //## TableName: product_cat
        //## ColumnNames: prod_cat,cat,name2,ord,cap
        //## ColumnTypes: INTEGER,INTEGER,VARCHAR(30),INTEGER,VARCHAR(30)
        ColumnImpl prodCatProductCat = ColumnImpl.builder().withName("prod_cat").withType("INTEGER").build();
        ColumnImpl catProductCat = ColumnImpl.builder().withName("cat").withType("INTEGER").build();
        ColumnImpl name2ProductCat = ColumnImpl.builder().withName("name2").withType("VARCHAR").withCharOctetLength(30).build();
        ColumnImpl ordProductCat = ColumnImpl.builder().withName("ord").withType("INTEGER").build();
        ColumnImpl capProductCat = ColumnImpl.builder().withName("cap").withType("INTEGER").build();
        PhysicalTableImpl productCat = ((Builder) PhysicalTableImpl.builder().withName("product_cat")
                .withColumns(List.of(catCat, name3Cat, ordCat, capCat))).build();
        
        List<CubeMapping> result = new ArrayList<>();
        result.addAll(super.schemaCubes(schemaMappingOriginal));
        result.add(PhysicalCubeMappingImpl.builder()
            .withName("Fact")
            .withQuery(TableQueryMappingImpl.builder().withTable(fact).withAggregationTables(
                List.of(
                    AggregationNameMappingImpl.builder()
                        .withName("test_lp_xxx_fact")
                        .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                            .withColumn("fact_count")
                            .build())
                        .withAggregationMeasures(List.of(
                            AggregationMeasureMappingImpl.builder()
                                .withColumn("amount")
                                .withName("[Measures].[Total]")
                                .build()
                        ))
                        .withAggregationLevels(List.of(
                            AggregationLevelMappingImpl.builder()
                                .withColumn("category")
                                .withName("[Product].[Category]")
                                .build(),
                            AggregationLevelMappingImpl.builder()
                                .withColumn("product_category")
                                .withName("[Product].[Product Category]")
                                .build()
                        ))
                        .build(),
                    AggregationNameMappingImpl.builder()
                        .withName("test_lp_xx2_fact")
                        .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                            .withColumn("fact_count")
                            .build())
                        .withAggregationMeasures(List.of(
                            AggregationMeasureMappingImpl.builder()
                                .withColumn("amount")
                                .withName("[Measures].[Total]")
                                .build()
                        ))
                        .withAggregationLevels(List.of(
                            AggregationLevelMappingImpl.builder()
                                .withColumn("prodname")
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
