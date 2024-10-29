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
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;

public class Checkin_7634Modifier extends PojoMappingModifier {

    public Checkin_7634Modifier(CatalogMapping c) {
        super(c);
    }

    /*
                "<Cube name='Checkin_7634'>\n"
            + "<Table name='table7634'/>\n"
            + "<Dimension name='Geography' foreignKey='cust_loc_id'>\n"
            + "    <Hierarchy hasAll='true' allMemberName='All Regions' primaryKey='cust_loc_id'>\n"
            + "    <Table name='geography7631'/>\n"
            + "    <Level column='state_cd' name='State' type='String' uniqueMembers='true'/>\n"
            + "    <Level column='city_nm' name='City' type='String' uniqueMembers='true'/>\n"
            + "    <Level column='zip_cd' name='Zip Code' type='String' uniqueMembers='true'/>\n"
            + "    </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name='Product' foreignKey='prod_id'>\n"
            + "    <Hierarchy hasAll='true' allMemberName='All Products' primaryKey='prod_id'>\n"
            + "    <Table name='prod7631'/>\n"
            + "    <Level column='class' name='Class' type='String' uniqueMembers='true'/>\n"
            + "    <Level column='brand' name='Brand' type='String' uniqueMembers='true'/>\n"
            + "    <Level column='item' name='Item' type='String' uniqueMembers='true'/>\n"
            + "    </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name='First Measure' \n"
            + "    column='first' aggregator='sum'\n"
            + "   formatString='#,###'/>\n"
            + "<Measure name='Requested Value' \n"
            + "    column='request_value' aggregator='sum'\n"
            + "   formatString='#,###'/>\n"
            + "<Measure name='Shipped Value' \n"
            + "    column='shipped_value' aggregator='sum'\n"
            + "   formatString='#,###'/>\n"
            + "</Cube>";

     */

    @Override
    protected List<? extends CubeMapping> schemaCubes(SchemaMapping schemaMappingOriginal) {
        List<CubeMapping> result = new ArrayList<>();
        ColumnImpl cust_loc_id_geography7631 = ColumnImpl.builder().withName("cust_loc_id").withType("INTEGER").build();
        ColumnImpl state_cd = ColumnImpl.builder().withName("state_cd").withType("VARCHAR").withTypeQualifiers(List.of("20")).build();
        ColumnImpl city_nm = ColumnImpl.builder().withName("city_nm").withType("VARCHAR").withTypeQualifiers(List.of("20")).build();
        ColumnImpl zip_cd = ColumnImpl.builder().withName("zip_cd").withType("VARCHAR").withTypeQualifiers(List.of("20")).build();
        PhysicalTableImpl geography7631 = ((org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder) PhysicalTableImpl.builder().withName("geography7631")
                .withColumns(List.of(
                        cust_loc_id_geography7631, state_cd, city_nm, zip_cd
                        ))).build();
        ColumnImpl cust_loc_id_table7634 = ColumnImpl.builder().withName("cust_loc_id").withType("INTEGER").build();
        ColumnImpl prod_id_table7634 = ColumnImpl.builder().withName("prod_id").withType("INTEGER").build();
        ColumnImpl first_table7634 = ColumnImpl.builder().withName("first").withType("DECIMAL").withTypeQualifiers(List.of("10", "2")).build();
        ColumnImpl request_value_table7634 = ColumnImpl.builder().withName("request_value").withType("DECIMAL").withTypeQualifiers(List.of("10", "2")).build();
        ColumnImpl shipped_value_table7634 = ColumnImpl.builder().withName("shipped_value").withType("DECIMAL").withTypeQualifiers(List.of("10", "2")).build();
        PhysicalTableImpl table7634 = ((org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder) PhysicalTableImpl.builder().withName("table7634")
                .withColumns(List.of(
                        cust_loc_id_table7634, prod_id_table7634, first_table7634, request_value_table7634, shipped_value_table7634
                        ))).build();
        ColumnImpl prod_id_prod7631 = ColumnImpl.builder().withName("prod_id").withType("INTEGER").build();
        ColumnImpl class_prod7631 = ColumnImpl.builder().withName("class").withType("INTEGER").build();
        ColumnImpl brand_prod7631 = ColumnImpl.builder().withName("brand").withType("INTEGER").build();
        ColumnImpl item_prod7631 = ColumnImpl.builder().withName("item").withType("INTEGER").build();
        PhysicalTableImpl prod7631 = ((org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder) PhysicalTableImpl.builder().withName("prod7631")
                .withColumns(List.of(
                		prod_id_prod7631, class_prod7631, brand_prod7631, item_prod7631
                        ))).build();
        
        result.addAll(super.schemaCubes(schemaMappingOriginal));
        result.add(PhysicalCubeMappingImpl.builder()
            .withName("Checkin_7634")
            .withQuery(TableQueryMappingImpl.builder().withTable(table7634).build())
            .withDimensionConnectors(List.of(
            	DimensionConnectorMappingImpl.builder()
            		.withOverrideDimensionName("Geography")
                    .withForeignKey(cust_loc_id_table7634)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("Geography")
                        .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withAllMemberName("All Regions")
                            .withPrimaryKey(cust_loc_id_geography7631)
                            .withQuery(TableQueryMappingImpl.builder().withTable(geography7631).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withColumn(state_cd)
                                    .withName("State")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(city_nm)
                                    .withName("City")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(zip_cd)
                                    .withName("Zip Code")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()

                    )).build())
                    .build(),
                DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("Product")
                    .withForeignKey(prod_id_table7634)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("Product")
                        .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withAllMemberName("All Products")
                            .withPrimaryKey(prod_id_prod7631)
                            .withQuery(TableQueryMappingImpl.builder().withTable(prod7631).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withColumn(class_prod7631)
                                    .withName("Class")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(brand_prod7631)
                                    .withName("Brand")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(item_prod7631)
                                    .withName("Item")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()

                    )).build())
                    .build()
            ))
            .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                MeasureMappingImpl.builder()
                    .withName("First Measure")
                    .withColumn(first_table7634)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build(),
                MeasureMappingImpl.builder()
                    .withName("Requested Value")
                    .withColumn(request_value_table7634)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build(),
                MeasureMappingImpl.builder()
                    .withName("Shipped Value")
                    .withColumn(shipped_value_table7634)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build()
            )).build()))
            .build());


        return result;
    }
}
