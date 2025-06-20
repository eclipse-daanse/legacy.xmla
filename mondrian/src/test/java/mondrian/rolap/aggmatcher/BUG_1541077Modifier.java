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
import org.eclipse.daanse.rolap.mapping.api.model.enums.ColumnDataType;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationColumnNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationForeignKeyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AvgMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CountMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalColumnMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ExplicitHierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SumMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;

public class BUG_1541077Modifier extends PojoMappingModifier {

    public BUG_1541077Modifier(CatalogMapping c) {
        super(c);
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
