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
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;

public class Checkin_7641Modifier  extends PojoMappingModifier {

    public Checkin_7641Modifier(CatalogMapping catalog) {
        super(catalog);
    }

    /*
                "<Cube name='ImplicitMember'>\n"
            + "<Table name='checkin7641'/>\n"
            + "<Dimension name='Geography' foreignKey='cust_loc_id'>\n"
            + "    <Hierarchy hasAll='true' allMemberName='All Regions' primaryKey='cust_loc_id'>\n"
            + "    <Table name='geography7641'/>\n"
            + "    <Level column='state_cd' name='State' type='String' uniqueMembers='true'/>\n"
            + "    <Level column='city_nm' name='City' type='String' uniqueMembers='true'/>\n"
            + "    <Level column='zip_cd' name='Zip Code' type='String' uniqueMembers='true'/>\n"
            + "    </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name='Product' foreignKey='prod_id'>\n"
            + "    <Hierarchy hasAll='false' defaultMember='Class2' primaryKey='prod_id'>\n"
            + "    <Table name='prod7611'/>\n"
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
    protected List<? extends CubeMapping> schemaCubes(SchemaMapping schema) {
        //## ColumnNames: cust_loc_id,prod_id,first,request_value,shipped_value
        //## ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2),DECIMAL(10,2),DECIMAL(10,2)
        ColumnImpl custLocIdCheckin7641 = ColumnImpl.builder().withName("cust_loc_id").withType("INTEGER").build();
        ColumnImpl prodIdCheckin7641 = ColumnImpl.builder().withName("prod_id").withType("INTEGER").build();
        ColumnImpl firstCheckin7641 = ColumnImpl.builder().withName("first").withType("NUMERIC").withTypeQualifiers(List.of("10", "2")).build();
        ColumnImpl requestValueCheckin7641 = ColumnImpl.builder().withName("request_value").withType("NUMERIC").withTypeQualifiers(List.of("10", "2")).build();
        ColumnImpl shippedValueCheckin7641 = ColumnImpl.builder().withName("request_value").withType("NUMERIC").withTypeQualifiers(List.of("10", "2")).build();
        PhysicalTableImpl checkin7641 = ((Builder) PhysicalTableImpl.builder().withName("checkin7641")
                .withColumns(List.of(custLocIdCheckin7641, prodIdCheckin7641, firstCheckin7641, requestValueCheckin7641, shippedValueCheckin7641))).build();
        //## ColumnNames: cust_loc_id,state_cd,city_nm,zip_cd
        //## ColumnTypes: INTEGER,VARCHAR(20),VARCHAR(20),VARCHAR(20)
        ColumnImpl custLocIdGeography7641 = ColumnImpl.builder().withName("cust_loc_id").withType("INTEGER").build();
        ColumnImpl stateCdGeography7641 = ColumnImpl.builder().withName("state_cd").withType("VARCHAR").withTypeQualifiers(List.of("20")).build();
        ColumnImpl cityNmGeography7641 = ColumnImpl.builder().withName("city_nm").withType("VARCHAR").withTypeQualifiers(List.of("20")).build();
        ColumnImpl zipCdGeography7641 = ColumnImpl.builder().withName("zip_cd").withType("VARCHAR").withTypeQualifiers(List.of("20")).build();
        PhysicalTableImpl geography7641 = ((Builder) PhysicalTableImpl.builder().withName("geography7641")
                .withColumns(List.of(custLocIdCheckin7641, stateCdGeography7641, cityNmGeography7641, zipCdGeography7641))).build();
        //## ColumnNames: prod_id,class,brand,item
        //## ColumnTypes: INTEGER,VARCHAR(30),VARCHAR(30),VARCHAR(30)
        ColumnImpl prodIdProd7611 = ColumnImpl.builder().withName("prod_id").withType("INTEGER").build();
        ColumnImpl classProd7611 = ColumnImpl.builder().withName("class").withType("VARCHAR").withTypeQualifiers(List.of("30")).build();
        ColumnImpl brandProd7611 = ColumnImpl.builder().withName("brand").withType("VARCHAR").withTypeQualifiers(List.of("30")).build();
        ColumnImpl itemProd7611 = ColumnImpl.builder().withName("item").withType("VARCHAR").withTypeQualifiers(List.of("30")).build();
        PhysicalTableImpl prod7611 = ((Builder) PhysicalTableImpl.builder().withName("prod7611")
                .withColumns(List.of(classProd7611, brandProd7611, itemProd7611))).build();
        
        List<CubeMapping> result = new ArrayList<>();
        result.addAll(super.schemaCubes(schema));
        result.add(PhysicalCubeMappingImpl.builder()
            .withName("ImplicitMember")
            .withQuery(TableQueryMappingImpl.builder().withTable(checkin7641).build())
            .withDimensionConnectors(List.of(
                DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("Geography")
                    .withForeignKey(custLocIdCheckin7641)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("Geography")
                    	.withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withAllMemberName("All Regions")
                            .withPrimaryKey(custLocIdGeography7641)
                            .withQuery(TableQueryMappingImpl.builder().withTable(geography7641).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withColumn(stateCdGeography7641)
                                    .withName("State")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(cityNmGeography7641)
                                    .withName("City")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(zipCdGeography7641)
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
                    .withForeignKey(prodIdCheckin7641)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("Product")
                        .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(false)
                            .withDefaultMember("Class2")
                            .withPrimaryKey(prodIdProd7611)
                            .withQuery(TableQueryMappingImpl.builder().withTable(prod7611).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withColumn(classProd7611)
                                    .withName("Class")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(brandProd7611)
                                    .withName("Brand")
                                    .withType(DataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(itemProd7611)
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
                    .withColumn(firstCheckin7641)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build(),
                MeasureMappingImpl.builder()
                    .withName("Requested Value")
                    .withColumn(requestValueCheckin7641)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build(),
                MeasureMappingImpl.builder()
                    .withName("Shipped Value")
                    .withColumn(shippedValueCheckin7641)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build()
            )).build()))
            .build());
        return result;

    }
}
