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
package mondrian.rolap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.ColumnDataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType;
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

public class RolapResultTestModifier extends PojoMappingModifier {

    public RolapResultTestModifier(CatalogMapping catalog) {
        super(catalog);
    }

    /*
                "<Cube name='FTAll'>\n"
            + "<Table name='FT1' />\n"
            + "<Dimension name='D1' foreignKey='d1_id' >\n"
            + " <Hierarchy hasAll='true' primaryKey='d1_id'>\n"
            + " <Table name='D1'/>\n"
            + " <Level name='Name' column='name' type='String' uniqueMembers='true'/>\n"
            + " </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name='D2' foreignKey='d2_id' >\n"
            + " <Hierarchy hasAll='true' primaryKey='d2_id'>\n"
            + " <Table name='D2'/>\n"
            + " <Level name='Name' column='name' type='String' uniqueMembers='true'/>\n"
            + " </Hierarchy>\n"
            + "</Dimension>\n"

            + "<Measure name='Value' \n"
            + "    column='value' aggregator='sum'\n"
            + "   formatString='#,###'/>\n"
            + "</Cube> \n"

            + "<Cube name='FT1'>\n"
            + "<Table name='FT1' />\n"
            + "<Dimension name='D1' foreignKey='d1_id' >\n"
            + " <Hierarchy hasAll='false' defaultMember='[D1].[d]' primaryKey='d1_id'>\n"
            + " <Table name='D1'/>\n"
            + " <Level name='Name' column='name' type='String' uniqueMembers='true'/>\n"
            + " </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name='D2' foreignKey='d2_id' >\n"
            + " <Hierarchy hasAll='false' defaultMember='[D2].[w]' primaryKey='d2_id'>\n"
            + " <Table name='D2'/>\n"
            + " <Level name='Name' column='name' type='String' uniqueMembers='true'/>\n"
            + " </Hierarchy>\n"
            + "</Dimension>\n"

            + "<Measure name='Value' \n"
            + "    column='value' aggregator='sum'\n"
            + "   formatString='#,###'/>\n"
            + "</Cube> \n"

            + "<Cube name='FT2'>\n"
            + "<Table name='FT2'/>\n"
            + "<Dimension name='D1' foreignKey='d1_id' >\n"
            + " <Hierarchy hasAll='false' defaultMember='[D1].[d]' primaryKey='d1_id'>\n"
            + " <Table name='D1'/>\n"
            + " <Level name='Name' column='name' type='String' uniqueMembers='true'/>\n"
            + " </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name='D2' foreignKey='d2_id' >\n"
            + " <Hierarchy hasAll='false' defaultMember='[D2].[w]' primaryKey='d2_id'>\n"
            + " <Table name='D2'/>\n"
            + " <Level name='Name' column='name' type='String' uniqueMembers='true'/>\n"
            + " </Hierarchy>\n"
            + "</Dimension>\n"

            + "<Measure name='Value' \n"
            + "    column='value' aggregator='sum'\n"
            + "   formatString='#,###'/>\n"
            + "</Cube>\n"

            + "<Cube name='FT2Extra'>\n"
            + "<Table name='FT2'/>\n"
            + "<Dimension name='D1' foreignKey='d1_id' >\n"
            + " <Hierarchy hasAll='false' defaultMember='[D1].[d]' primaryKey='d1_id'>\n"
            + " <Table name='D1'/>\n"
            + " <Level name='Name' column='name' type='String' uniqueMembers='true'/>\n"
            + " </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Dimension name='D2' foreignKey='d2_id' >\n"
            + " <Hierarchy hasAll='false' defaultMember='[D2].[w]' primaryKey='d2_id'>\n"
            + " <Table name='D2'/>\n"
            + " <Level name='Name' column='name' type='String' uniqueMembers='true'/>\n"
            + " </Hierarchy>\n"
            + "</Dimension>\n"
            + "<Measure name='VExtra' \n"
            + "    column='vextra' aggregator='sum'\n"
            + "   formatString='#,###'/>\n"
            + "<Measure name='Value' \n"
            + "    column='value' aggregator='sum'\n"
            + "   formatString='#,###'/>\n"
            + "</Cube>";

     */

    @Override
    protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
    	//## TableName: FT1
    	//## ColumnNames: d1_id,d2_id,value
    	//## ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2)
        ColumnMappingImpl d1IdFt1 = ColumnMappingImpl.builder().withName("d1_id").withDataType(ColumnDataType.INTEGER).build();
        ColumnMappingImpl d2IdFt1 = ColumnMappingImpl.builder().withName("d2_id").withDataType(ColumnDataType.INTEGER).build();
        ColumnMappingImpl valueFt1 = ColumnMappingImpl.builder().withName("value").withDataType(ColumnDataType.NUMERIC).withColumnSize(10).withDecimalDigits(2).build();
        PhysicalTableMappingImpl ft1 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("FT1")
                .withColumns(List.of(d1IdFt1, d2IdFt1, valueFt1))).build();
        //## ColumnNames: d1_id,d2_id,value,vextra
        //## ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2),DECIMAL(10,2):null
        ColumnMappingImpl d1IdFt2 = ColumnMappingImpl.builder().withName("d1_id").withDataType(ColumnDataType.INTEGER).build();
        ColumnMappingImpl d2IdFt2 = ColumnMappingImpl.builder().withName("d2_id").withDataType(ColumnDataType.INTEGER).build();
        ColumnMappingImpl valueFt2 = ColumnMappingImpl.builder().withName("value").withDataType(ColumnDataType.NUMERIC).withColumnSize(10).withDecimalDigits(2).build();
        ColumnMappingImpl vextraFt2 = ColumnMappingImpl.builder().withName("vextra").withDataType(ColumnDataType.NUMERIC).withColumnSize(10).withDecimalDigits(2).build();
        PhysicalTableMappingImpl ft2 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("FT2")
                .withColumns(List.of(d1IdFt2, d2IdFt2, valueFt2))).build();

        //## ColumnNames: d1_id,name
        //## ColumnTypes: INTEGER,VARCHAR(20)
        ColumnMappingImpl d1IdD1 = ColumnMappingImpl.builder().withName("d1_id").withDataType(ColumnDataType.INTEGER).build();
        ColumnMappingImpl nameD1 = ColumnMappingImpl.builder().withName("name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
        PhysicalTableMappingImpl d1 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("D1")
                .withColumns(List.of(d1IdD1, nameD1))).build();
        //## ColumnNames: d2_id,name
        //## ColumnTypes: INTEGER,VARCHAR(20)
        ColumnMappingImpl d2IdD2 = ColumnMappingImpl.builder().withName("d2_id").withDataType(ColumnDataType.INTEGER).build();
        ColumnMappingImpl nameD2 = ColumnMappingImpl.builder().withName("name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
        PhysicalTableMappingImpl d2 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("D2")
                .withColumns(List.of(d2IdD2, nameD2))).build();

        List<CubeMapping> result = new ArrayList<>();
        result.addAll(super.catalogCubes(schema));
        result.add(PhysicalCubeMappingImpl.builder()
            .withName("FTAll")
            .withQuery(TableQueryMappingImpl.builder().withTable(ft1).build())
            .withDimensionConnectors(List.of(
                DimensionConnectorMappingImpl.builder()
                	.withOverrideDimensionName("D1")
                    .withForeignKey(d1IdFt1)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(d1IdD1)
                            .withQuery(TableQueryMappingImpl.builder().withTable(d1).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Name")
                                    .withColumn(nameD1)
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build(),
                DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("D2")
                    .withForeignKey(d2IdFt1)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(d2IdD2)
                            .withQuery(TableQueryMappingImpl.builder().withTable(d2).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Name")
                                    .withColumn(nameD2)
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build()
            ))
            .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                MeasureMappingImpl.builder()
                    .withName("Value")
                    .withColumn(valueFt1)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build()
            )).build()))
            .build());

        result.add(PhysicalCubeMappingImpl.builder()
            .withName("FT1")
            .withQuery(TableQueryMappingImpl.builder().withTable(ft1).build())
            .withDimensionConnectors(List.of(
                DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("D1")
                    .withForeignKey(d1IdFt1)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("D1")
                    	.withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(false)
                            .withDefaultMember("[D1].[d]")
                            .withPrimaryKey(d1IdD1)
                            .withQuery(TableQueryMappingImpl.builder().withTable(d1).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Name")
                                    .withColumn(nameD1)
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build(),
                DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("D2")
                    .withForeignKey(d2IdFt1)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("D2")
                    	.withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(false)
                            .withDefaultMember("[D2].[w]")
                            .withPrimaryKey(d2IdD2)
                            .withQuery(TableQueryMappingImpl.builder().withTable(d2).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Name")
                                    .withColumn(nameD2)
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build()
            ))
            .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                 MeasureMappingImpl.builder()
                    .withName("Value")
                    .withColumn(valueFt1)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build()
            )).build()))
            .build());

        result.add(PhysicalCubeMappingImpl.builder()
            .withName("FT2")
            .withQuery(TableQueryMappingImpl.builder().withTable(ft2).build())
            .withDimensionConnectors(List.of(
                DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("D1")
                    .withForeignKey(d1IdFt2)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("D1")
                    	.withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withDefaultMember("[D1].[d]")
                            .withPrimaryKey(d1IdD1)
                            .withQuery(TableQueryMappingImpl.builder().withTable(d1).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Name")
                                    .withColumn(nameD1)
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build(),
                DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("D2")
                    .withForeignKey(d2IdFt2)
                    .withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("D2")
                    	.withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withDefaultMember("[D2].[w]")
                            .withPrimaryKey(d2IdD2)
                            .withQuery(TableQueryMappingImpl.builder().withTable(d2).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Name")
                                    .withColumn(nameD2)
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build()
            ))
            .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                MeasureMappingImpl.builder()
                    .withName("Value")
                    .withColumn(valueFt2)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build()
            )).build()))
            .build());

        result.add(PhysicalCubeMappingImpl.builder()
            .withName("FT2Extra")
            .withQuery(TableQueryMappingImpl.builder().withTable(ft2).build())
            .withDimensionConnectors(List.of(
                DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("D1")
                    .withForeignKey(d1IdFt2)
                    .withDimension(StandardDimensionMappingImpl.builder()
                        .withName("D1")
                        .withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withDefaultMember("[D1].[d]")
                            .withPrimaryKey(d1IdD1)
                            .withQuery(TableQueryMappingImpl.builder().withTable(d1).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Name")
                                    .withColumn(nameD1)
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build(),
                DimensionConnectorMappingImpl.builder()
                    .withOverrideDimensionName("D2")
                    .withForeignKey(d2IdFt2)
                    	.withDimension(StandardDimensionMappingImpl.builder()
                    	.withName("D2")
                    	.withHierarchies(List.of(
                        HierarchyMappingImpl.builder()
                            .withHasAll(false)
                            .withDefaultMember("[D2].[w]")
                            .withPrimaryKey(d2IdD2)
                            .withQuery(TableQueryMappingImpl.builder().withTable(d2).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Name")
                                    .withColumn(nameD2)
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()
                    )).build())
                    .build()
            ))
            .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                MeasureMappingImpl.builder()
                    .withName("VExtra")
                    .withColumn(vextraFt2)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build(),
                MeasureMappingImpl.builder()
                    .withName("Value")
                    .withColumn(valueFt2)
                    .withAggregatorType(MeasureAggregatorType.SUM)
                    .withFormatString("#,###")
                    .build()
            )).build()))
            .build());
        return result;

    }
}
