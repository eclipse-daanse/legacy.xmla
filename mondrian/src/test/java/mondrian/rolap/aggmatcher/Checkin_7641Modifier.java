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
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.ColumnMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
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
    protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
        //## ColumnNames: cust_loc_id,prod_id,first,request_value,shipped_value
        //## ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2),DECIMAL(10,2),DECIMAL(10,2)
        ColumnMappingImpl custLocIdCheckin7641 = ColumnMappingImpl.builder().withName("cust_loc_id").withType("INTEGER").build();
        ColumnMappingImpl prodIdCheckin7641 = ColumnMappingImpl.builder().withName("prod_id").withType("INTEGER").build();
        ColumnMappingImpl firstCheckin7641 = ColumnMappingImpl.builder().withName("first").withType("NUMERIC").withColumnSize(10).withDecimalDigits(2).build();
        ColumnMappingImpl requestValueCheckin7641 = ColumnMappingImpl.builder().withName("request_value").withType("NUMERIC").withColumnSize(10).withDecimalDigits(2).build();
        ColumnMappingImpl shippedValueCheckin7641 = ColumnMappingImpl.builder().withName("request_value").withType("NUMERIC").withColumnSize(10).withDecimalDigits(2).build();
        PhysicalTableMappingImpl checkin7641 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("checkin7641")
                .withColumns(List.of(custLocIdCheckin7641, prodIdCheckin7641, firstCheckin7641, requestValueCheckin7641, shippedValueCheckin7641))).build();
        //## ColumnNames: cust_loc_id,state_cd,city_nm,zip_cd
        //## ColumnTypes: INTEGER,VARCHAR(20),VARCHAR(20),VARCHAR(20)
        ColumnMappingImpl custLocIdGeography7641 = ColumnMappingImpl.builder().withName("cust_loc_id").withType("INTEGER").build();
        ColumnMappingImpl stateCdGeography7641 = ColumnMappingImpl.builder().withName("state_cd").withType("VARCHAR").withCharOctetLength(20).build();
        ColumnMappingImpl cityNmGeography7641 = ColumnMappingImpl.builder().withName("city_nm").withType("VARCHAR").withCharOctetLength(20).build();
        ColumnMappingImpl zipCdGeography7641 = ColumnMappingImpl.builder().withName("zip_cd").withType("VARCHAR").withCharOctetLength(20).build();
        PhysicalTableMappingImpl geography7641 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("geography7641")
                .withColumns(List.of(custLocIdCheckin7641, stateCdGeography7641, cityNmGeography7641, zipCdGeography7641))).build();
        //## ColumnNames: prod_id,class,brand,item
        //## ColumnTypes: INTEGER,VARCHAR(30),VARCHAR(30),VARCHAR(30)
        ColumnMappingImpl prodIdProd7611 = ColumnMappingImpl.builder().withName("prod_id").withType("INTEGER").build();
        ColumnMappingImpl classProd7611 = ColumnMappingImpl.builder().withName("class").withType("VARCHAR").withCharOctetLength(30).build();
        ColumnMappingImpl brandProd7611 = ColumnMappingImpl.builder().withName("brand").withType("VARCHAR").withCharOctetLength(30).build();
        ColumnMappingImpl itemProd7611 = ColumnMappingImpl.builder().withName("item").withType("VARCHAR").withCharOctetLength(30).build();
        PhysicalTableMappingImpl prod7611 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("prod7611")
                .withColumns(List.of(classProd7611, brandProd7611, itemProd7611))).build();

        List<CubeMapping> result = new ArrayList<>();
        result.addAll(super.catalogCubes(schema));
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
