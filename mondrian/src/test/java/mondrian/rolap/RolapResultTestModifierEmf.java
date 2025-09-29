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

import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.DatabaseSchema;
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
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

public class RolapResultTestModifierEmf implements CatalogMappingSupplier {

    protected final Catalog catalog;

    // FT1 table columns
    protected PhysicalColumn d1IdFt1;
    protected PhysicalColumn d2IdFt1;
    protected PhysicalColumn valueFt1;
    protected PhysicalTable ft1;

    // FT2 table columns
    protected PhysicalColumn d1IdFt2;
    protected PhysicalColumn d2IdFt2;
    protected PhysicalColumn valueFt2;
    protected PhysicalColumn vextraFt2;
    protected PhysicalTable ft2;

    // D1 table columns
    protected PhysicalColumn d1IdD1;
    protected PhysicalColumn nameD1;
    protected PhysicalTable d1;

    // D2 table columns
    protected PhysicalColumn d2IdD2;
    protected PhysicalColumn nameD2;
    protected PhysicalTable d2;

    public RolapResultTestModifierEmf(Catalog catalogMapping) {
        this.catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) catalogMapping);
        createTables();
        createCubes();
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

    protected void createTables() {
        // Create FT1 table
        // ColumnNames: d1_id,d2_id,value
        // ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2)
        d1IdFt1 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        d1IdFt1.setName("d1_id");
        d1IdFt1.setType(ColumnType.INTEGER);

        d2IdFt1 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        d2IdFt1.setName("d2_id");
        d2IdFt1.setType(ColumnType.INTEGER);

        valueFt1 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        valueFt1.setName("value");
        valueFt1.setType(ColumnType.DECIMAL);
        valueFt1.setColumnSize(10);
        valueFt1.setDecimalDigits(2);

        ft1 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        ft1.setName("FT1");
        ft1.getColumns().add(d1IdFt1);
        ft1.getColumns().add(d2IdFt1);
        ft1.getColumns().add(valueFt1);

        // Create FT2 table
        // ColumnNames: d1_id,d2_id,value,vextra
        // ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2),DECIMAL(10,2):null
        d1IdFt2 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        d1IdFt2.setName("d1_id");
        d1IdFt2.setType(ColumnType.INTEGER);

        d2IdFt2 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        d2IdFt2.setName("d2_id");
        d2IdFt2.setType(ColumnType.INTEGER);

        valueFt2 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        valueFt2.setName("value");
        valueFt2.setType(ColumnType.DECIMAL);
        valueFt2.setColumnSize(10);
        valueFt2.setDecimalDigits(2);

        vextraFt2 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        vextraFt2.setName("vextra");
        vextraFt2.setType(ColumnType.DECIMAL);
        vextraFt2.setColumnSize(10);
        vextraFt2.setDecimalDigits(2);

        ft2 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        ft2.setName("FT2");
        ft2.getColumns().add(d1IdFt2);
        ft2.getColumns().add(d2IdFt2);
        ft2.getColumns().add(valueFt2);
        ft2.getColumns().add(vextraFt2);

        // Create D1 table
        // ColumnNames: d1_id,name
        // ColumnTypes: INTEGER,VARCHAR(20)
        d1IdD1 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        d1IdD1.setName("d1_id");
        d1IdD1.setType(ColumnType.INTEGER);

        nameD1 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        nameD1.setName("name");
        nameD1.setType(ColumnType.VARCHAR);
        nameD1.setCharOctetLength(20);

        d1 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        d1.setName("D1");
        d1.getColumns().add(d1IdD1);
        d1.getColumns().add(nameD1);

        // Create D2 table
        // ColumnNames: d2_id,name
        // ColumnTypes: INTEGER,VARCHAR(20)
        d2IdD2 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        d2IdD2.setName("d2_id");
        d2IdD2.setType(ColumnType.INTEGER);

        nameD2 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        nameD2.setName("name");
        nameD2.setType(ColumnType.VARCHAR);
        nameD2.setCharOctetLength(20);

        d2 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        d2.setName("D2");
        d2.getColumns().add(d2IdD2);
        d2.getColumns().add(nameD2);

        // Add tables to database schema
        if (catalog.getDbschemas().size() > 0) {
            DatabaseSchema dbSchema = catalog.getDbschemas().get(0);
            dbSchema.getTables().add(ft1);
            dbSchema.getTables().add(ft2);
            dbSchema.getTables().add(d1);
            dbSchema.getTables().add(d2);
        }
    }

    protected void createCubes() {
        // Create FTAll cube
        catalog.getCubes().add(createFTAllCube());

        // Create FT1 cube
        catalog.getCubes().add(createFT1Cube());

        // Create FT2 cube
        catalog.getCubes().add(createFT2Cube());

        // Create FT2Extra cube
        catalog.getCubes().add(createFT2ExtraCube());
    }

    protected PhysicalCube createFTAllCube() {
        // Create table query
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(ft1);

        // Create D1 dimension with hasAll=true
        StandardDimension d1Dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        d1Dimension.setName("D1");

        TableQuery d1Query = RolapMappingFactory.eINSTANCE.createTableQuery();
        d1Query.setTable(d1);

        Level d1NameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        d1NameLevel.setName("Name");
        d1NameLevel.setColumn(nameD1);
        d1NameLevel.setColumnType(ColumnInternalDataType.STRING);
        d1NameLevel.setUniqueMembers(true);

        ExplicitHierarchy d1Hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        d1Hierarchy.setHasAll(true);
        d1Hierarchy.setPrimaryKey(d1IdD1);
        d1Hierarchy.setQuery(d1Query);
        d1Hierarchy.getLevels().add(d1NameLevel);

        d1Dimension.getHierarchies().add(d1Hierarchy);

        // Create D1 connector
        DimensionConnector d1Connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        d1Connector.setOverrideDimensionName("D1");
        d1Connector.setForeignKey(d1IdFt1);
        d1Connector.setDimension(d1Dimension);

        // Create D2 dimension with hasAll=true
        StandardDimension d2Dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        d2Dimension.setName("D2");

        TableQuery d2Query = RolapMappingFactory.eINSTANCE.createTableQuery();
        d2Query.setTable(d2);

        Level d2NameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        d2NameLevel.setName("Name");
        d2NameLevel.setColumn(nameD2);
        d2NameLevel.setColumnType(ColumnInternalDataType.STRING);
        d2NameLevel.setUniqueMembers(true);

        ExplicitHierarchy d2Hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        d2Hierarchy.setHasAll(true);
        d2Hierarchy.setPrimaryKey(d2IdD2);
        d2Hierarchy.setQuery(d2Query);
        d2Hierarchy.getLevels().add(d2NameLevel);

        d2Dimension.getHierarchies().add(d2Hierarchy);

        // Create D2 connector
        DimensionConnector d2Connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        d2Connector.setOverrideDimensionName("D2");
        d2Connector.setForeignKey(d2IdFt1);
        d2Connector.setDimension(d2Dimension);

        // Create measure
        SumMeasure valueMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        valueMeasure.setName("Value");
        valueMeasure.setColumn(valueFt1);
        valueMeasure.setFormatString("#,###");

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(valueMeasure);

        // Create cube
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("FTAll");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(d1Connector);
        cube.getDimensionConnectors().add(d2Connector);
        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    protected PhysicalCube createFT1Cube() {
        // Create table query
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(ft1);

        // Create D1 dimension with hasAll=false and defaultMember
        StandardDimension d1Dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        d1Dimension.setName("D1");

        TableQuery d1Query = RolapMappingFactory.eINSTANCE.createTableQuery();
        d1Query.setTable(d1);

        Level d1NameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        d1NameLevel.setName("Name");
        d1NameLevel.setColumn(nameD1);
        d1NameLevel.setColumnType(ColumnInternalDataType.STRING);
        d1NameLevel.setUniqueMembers(true);

        ExplicitHierarchy d1Hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        d1Hierarchy.setHasAll(false);
        d1Hierarchy.setDefaultMember("[D1].[d]");
        d1Hierarchy.setPrimaryKey(d1IdD1);
        d1Hierarchy.setQuery(d1Query);
        d1Hierarchy.getLevels().add(d1NameLevel);

        d1Dimension.getHierarchies().add(d1Hierarchy);

        // Create D1 connector
        DimensionConnector d1Connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        d1Connector.setOverrideDimensionName("D1");
        d1Connector.setForeignKey(d1IdFt1);
        d1Connector.setDimension(d1Dimension);

        // Create D2 dimension with hasAll=false and defaultMember
        StandardDimension d2Dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        d2Dimension.setName("D2");

        TableQuery d2Query = RolapMappingFactory.eINSTANCE.createTableQuery();
        d2Query.setTable(d2);

        Level d2NameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        d2NameLevel.setName("Name");
        d2NameLevel.setColumn(nameD2);
        d2NameLevel.setColumnType(ColumnInternalDataType.STRING);
        d2NameLevel.setUniqueMembers(true);

        ExplicitHierarchy d2Hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        d2Hierarchy.setHasAll(false);
        d2Hierarchy.setDefaultMember("[D2].[w]");
        d2Hierarchy.setPrimaryKey(d2IdD2);
        d2Hierarchy.setQuery(d2Query);
        d2Hierarchy.getLevels().add(d2NameLevel);

        d2Dimension.getHierarchies().add(d2Hierarchy);

        // Create D2 connector
        DimensionConnector d2Connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        d2Connector.setOverrideDimensionName("D2");
        d2Connector.setForeignKey(d2IdFt1);
        d2Connector.setDimension(d2Dimension);

        // Create measure
        SumMeasure valueMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        valueMeasure.setName("Value");
        valueMeasure.setColumn(valueFt1);
        valueMeasure.setFormatString("#,###");

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(valueMeasure);

        // Create cube
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("FT1");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(d1Connector);
        cube.getDimensionConnectors().add(d2Connector);
        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    protected PhysicalCube createFT2Cube() {
        // Create table query
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(ft2);

        // Create D1 dimension with hasAll=true (note: different from original XML comment which shows false)
        StandardDimension d1Dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        d1Dimension.setName("D1");

        TableQuery d1Query = RolapMappingFactory.eINSTANCE.createTableQuery();
        d1Query.setTable(d1);

        Level d1NameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        d1NameLevel.setName("Name");
        d1NameLevel.setColumn(nameD1);
        d1NameLevel.setColumnType(ColumnInternalDataType.STRING);
        d1NameLevel.setUniqueMembers(true);

        ExplicitHierarchy d1Hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        d1Hierarchy.setHasAll(true);
        d1Hierarchy.setDefaultMember("[D1].[d]");
        d1Hierarchy.setPrimaryKey(d1IdD1);
        d1Hierarchy.setQuery(d1Query);
        d1Hierarchy.getLevels().add(d1NameLevel);

        d1Dimension.getHierarchies().add(d1Hierarchy);

        // Create D1 connector
        DimensionConnector d1Connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        d1Connector.setOverrideDimensionName("D1");
        d1Connector.setForeignKey(d1IdFt2);
        d1Connector.setDimension(d1Dimension);

        // Create D2 dimension with hasAll=true (note: different from original XML comment which shows false)
        StandardDimension d2Dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        d2Dimension.setName("D2");

        TableQuery d2Query = RolapMappingFactory.eINSTANCE.createTableQuery();
        d2Query.setTable(d2);

        Level d2NameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        d2NameLevel.setName("Name");
        d2NameLevel.setColumn(nameD2);
        d2NameLevel.setColumnType(ColumnInternalDataType.STRING);
        d2NameLevel.setUniqueMembers(true);

        ExplicitHierarchy d2Hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        d2Hierarchy.setHasAll(true);
        d2Hierarchy.setDefaultMember("[D2].[w]");
        d2Hierarchy.setPrimaryKey(d2IdD2);
        d2Hierarchy.setQuery(d2Query);
        d2Hierarchy.getLevels().add(d2NameLevel);

        d2Dimension.getHierarchies().add(d2Hierarchy);

        // Create D2 connector
        DimensionConnector d2Connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        d2Connector.setOverrideDimensionName("D2");
        d2Connector.setForeignKey(d2IdFt2);
        d2Connector.setDimension(d2Dimension);

        // Create measure
        SumMeasure valueMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        valueMeasure.setName("Value");
        valueMeasure.setColumn(valueFt2);
        valueMeasure.setFormatString("#,###");

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(valueMeasure);

        // Create cube
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("FT2");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(d1Connector);
        cube.getDimensionConnectors().add(d2Connector);
        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    protected PhysicalCube createFT2ExtraCube() {
        // Create table query
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(ft2);

        // Create D1 dimension with hasAll=true and defaultMember
        StandardDimension d1Dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        d1Dimension.setName("D1");

        TableQuery d1Query = RolapMappingFactory.eINSTANCE.createTableQuery();
        d1Query.setTable(d1);

        Level d1NameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        d1NameLevel.setName("Name");
        d1NameLevel.setColumn(nameD1);
        d1NameLevel.setColumnType(ColumnInternalDataType.STRING);
        d1NameLevel.setUniqueMembers(true);

        ExplicitHierarchy d1Hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        d1Hierarchy.setHasAll(true);
        d1Hierarchy.setDefaultMember("[D1].[d]");
        d1Hierarchy.setPrimaryKey(d1IdD1);
        d1Hierarchy.setQuery(d1Query);
        d1Hierarchy.getLevels().add(d1NameLevel);

        d1Dimension.getHierarchies().add(d1Hierarchy);

        // Create D1 connector
        DimensionConnector d1Connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        d1Connector.setOverrideDimensionName("D1");
        d1Connector.setForeignKey(d1IdFt2);
        d1Connector.setDimension(d1Dimension);

        // Create D2 dimension with hasAll=false and defaultMember
        StandardDimension d2Dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        d2Dimension.setName("D2");

        TableQuery d2Query = RolapMappingFactory.eINSTANCE.createTableQuery();
        d2Query.setTable(d2);

        Level d2NameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        d2NameLevel.setName("Name");
        d2NameLevel.setColumn(nameD2);
        d2NameLevel.setColumnType(ColumnInternalDataType.STRING);
        d2NameLevel.setUniqueMembers(true);

        ExplicitHierarchy d2Hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        d2Hierarchy.setHasAll(false);
        d2Hierarchy.setDefaultMember("[D2].[w]");
        d2Hierarchy.setPrimaryKey(d2IdD2);
        d2Hierarchy.setQuery(d2Query);
        d2Hierarchy.getLevels().add(d2NameLevel);

        d2Dimension.getHierarchies().add(d2Hierarchy);

        // Create D2 connector
        DimensionConnector d2Connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        d2Connector.setOverrideDimensionName("D2");
        d2Connector.setForeignKey(d2IdFt2);
        d2Connector.setDimension(d2Dimension);

        // Create measures
        SumMeasure vextraMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        vextraMeasure.setName("VExtra");
        vextraMeasure.setColumn(vextraFt2);
        vextraMeasure.setFormatString("#,###");

        SumMeasure valueMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        valueMeasure.setName("Value");
        valueMeasure.setColumn(valueFt2);
        valueMeasure.setFormatString("#,###");

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(vextraMeasure);
        measureGroup.getMeasures().add(valueMeasure);

        // Create cube
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("FT2Extra");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(d1Connector);
        cube.getDimensionConnectors().add(d2Connector);
        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
