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

import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
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
/*
public class Checkin_7641Modifier  extends PojoMappingModifier {

    public Checkin_7641Modifier(CatalogMapping catalog) {
        super(catalog);
    }


    @Override
    protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
        //## ColumnNames: cust_loc_id,prod_id,first,request_value,shipped_value
        //## ColumnTypes: INTEGER,INTEGER,DECIMAL(10,2),DECIMAL(10,2),DECIMAL(10,2)
        PhysicalColumnMappingImpl custLocIdCheckin7641 = PhysicalColumnMappingImpl.builder().withName("cust_loc_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl prodIdCheckin7641 = PhysicalColumnMappingImpl.builder().withName("prod_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl firstCheckin7641 = PhysicalColumnMappingImpl.builder().withName("first").withDataType(ColumnDataType.NUMERIC).withColumnSize(10).withDecimalDigits(2).build();
        PhysicalColumnMappingImpl requestValueCheckin7641 = PhysicalColumnMappingImpl.builder().withName("request_value").withDataType(ColumnDataType.NUMERIC).withColumnSize(10).withDecimalDigits(2).build();
        PhysicalColumnMappingImpl shippedValueCheckin7641 = PhysicalColumnMappingImpl.builder().withName("request_value").withDataType(ColumnDataType.NUMERIC).withColumnSize(10).withDecimalDigits(2).build();
        PhysicalTableMappingImpl checkin7641 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("checkin7641")
                .withColumns(List.of(custLocIdCheckin7641, prodIdCheckin7641, firstCheckin7641, requestValueCheckin7641, shippedValueCheckin7641))).build();
        //## ColumnNames: cust_loc_id,state_cd,city_nm,zip_cd
        //## ColumnTypes: INTEGER,VARCHAR(20),VARCHAR(20),VARCHAR(20)
        PhysicalColumnMappingImpl custLocIdGeography7641 = PhysicalColumnMappingImpl.builder().withName("cust_loc_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl stateCdGeography7641 = PhysicalColumnMappingImpl.builder().withName("state_cd").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
        PhysicalColumnMappingImpl cityNmGeography7641 = PhysicalColumnMappingImpl.builder().withName("city_nm").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
        PhysicalColumnMappingImpl zipCdGeography7641 = PhysicalColumnMappingImpl.builder().withName("zip_cd").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
        PhysicalTableMappingImpl geography7641 = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("geography7641")
                .withColumns(List.of(custLocIdCheckin7641, stateCdGeography7641, cityNmGeography7641, zipCdGeography7641))).build();
        //## ColumnNames: prod_id,class,brand,item
        //## ColumnTypes: INTEGER,VARCHAR(30),VARCHAR(30),VARCHAR(30)
        PhysicalColumnMappingImpl prodIdProd7611 = PhysicalColumnMappingImpl.builder().withName("prod_id").withDataType(ColumnDataType.INTEGER).build();
        PhysicalColumnMappingImpl classProd7611 = PhysicalColumnMappingImpl.builder().withName("class").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
        PhysicalColumnMappingImpl brandProd7611 = PhysicalColumnMappingImpl.builder().withName("brand").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
        PhysicalColumnMappingImpl itemProd7611 = PhysicalColumnMappingImpl.builder().withName("item").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
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
                        ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withAllMemberName("All Regions")
                            .withPrimaryKey(custLocIdGeography7641)
                            .withQuery(TableQueryMappingImpl.builder().withTable(geography7641).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withColumn(stateCdGeography7641)
                                    .withName("State")
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(cityNmGeography7641)
                                    .withName("City")
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(zipCdGeography7641)
                                    .withName("Zip Code")
                                    .withType(InternalDataType.STRING)
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
                        ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(false)
                            .withDefaultMember("Class2")
                            .withPrimaryKey(prodIdProd7611)
                            .withQuery(TableQueryMappingImpl.builder().withTable(prod7611).build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withColumn(classProd7611)
                                    .withName("Class")
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(brandProd7611)
                                    .withName("Brand")
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build(),
                                LevelMappingImpl.builder()
                                    .withColumn(itemProd7611)
                                    .withName("Item")
                                    .withType(InternalDataType.STRING)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build()

                    )).build())
                    .build()
            ))
            .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                SumMeasureMappingImpl.builder()
                    .withName("First Measure")
                    .withColumn(firstCheckin7641)
                    .withFormatString("#,###")
                    .build(),
                SumMeasureMappingImpl.builder()
                    .withName("Requested Value")
                    .withColumn(requestValueCheckin7641)
                    .withFormatString("#,###")
                    .build(),
                SumMeasureMappingImpl.builder()
                    .withName("Shipped Value")
                    .withColumn(shippedValueCheckin7641)
                    .withFormatString("#,###")
                    .build()
            )).build()))
            .build());
        return result;

    }
}
*/
public class Checkin_7641Modifier implements CatalogMappingSupplier {

    private final CatalogImpl originalCatalog;

    public Checkin_7641Modifier(Catalog catalog) {
        this.originalCatalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) catalog);
        PhysicalColumn custLocIdCheckin7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        custLocIdCheckin7641.setName("cust_loc_id");
        custLocIdCheckin7641.setType(ColumnType.INTEGER);

        PhysicalColumn prodIdCheckin7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prodIdCheckin7641.setName("prod_id");
        prodIdCheckin7641.setType(ColumnType.INTEGER);

        PhysicalColumn firstCheckin7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        firstCheckin7641.setName("first");
        firstCheckin7641.setType(ColumnType.NUMERIC);
        firstCheckin7641.setColumnSize(10);
        firstCheckin7641.setDecimalDigits(2);

        PhysicalColumn requestValueCheckin7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        requestValueCheckin7641.setName("request_value");
        requestValueCheckin7641.setType(ColumnType.NUMERIC);
        requestValueCheckin7641.setColumnSize(10);
        requestValueCheckin7641.setDecimalDigits(2);

        PhysicalColumn shippedValueCheckin7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        shippedValueCheckin7641.setName("shipped_value");
        shippedValueCheckin7641.setType(ColumnType.NUMERIC);
        shippedValueCheckin7641.setColumnSize(10);
        shippedValueCheckin7641.setDecimalDigits(2);

        // Create checkin7641 table using RolapMappingFactory
        PhysicalTable checkin7641 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        checkin7641.setName("checkin7641");
        checkin7641.getColumns().add(custLocIdCheckin7641);
        checkin7641.getColumns().add(prodIdCheckin7641);
        checkin7641.getColumns().add(firstCheckin7641);
        checkin7641.getColumns().add(requestValueCheckin7641);
        checkin7641.getColumns().add(shippedValueCheckin7641);

        // Create columns for geography7641 table using RolapMappingFactory
        // ColumnNames: cust_loc_id,state_cd,city_nm,zip_cd
        // ColumnTypes: INTEGER,VARCHAR(20),VARCHAR(20),VARCHAR(20)
        PhysicalColumn custLocIdGeography7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        custLocIdGeography7641.setName("cust_loc_id");
        custLocIdGeography7641.setType(ColumnType.INTEGER);

        PhysicalColumn stateCdGeography7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        stateCdGeography7641.setName("state_cd");
        stateCdGeography7641.setType(ColumnType.VARCHAR);
        stateCdGeography7641.setCharOctetLength(20);

        PhysicalColumn cityNmGeography7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        cityNmGeography7641.setName("city_nm");
        cityNmGeography7641.setType(ColumnType.VARCHAR);
        cityNmGeography7641.setCharOctetLength(20);

        PhysicalColumn zipCdGeography7641 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        zipCdGeography7641.setName("zip_cd");
        zipCdGeography7641.setType(ColumnType.VARCHAR);
        zipCdGeography7641.setCharOctetLength(20);

        // Create geography7641 table using RolapMappingFactory
        PhysicalTable geography7641 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        geography7641.setName("geography7641");
        geography7641.getColumns().add(custLocIdGeography7641);
        geography7641.getColumns().add(stateCdGeography7641);
        geography7641.getColumns().add(cityNmGeography7641);
        geography7641.getColumns().add(zipCdGeography7641);

        // Create columns for prod7611 table using RolapMappingFactory
        // ColumnNames: prod_id,class,brand,item
        // ColumnTypes: INTEGER,VARCHAR(30),VARCHAR(30),VARCHAR(30)
        PhysicalColumn prodIdProd7611 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        prodIdProd7611.setName("prod_id");
        prodIdProd7611.setType(ColumnType.INTEGER);

        PhysicalColumn classProd7611 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        classProd7611.setName("class");
        classProd7611.setType(ColumnType.VARCHAR);
        classProd7611.setCharOctetLength(30);

        PhysicalColumn brandProd7611 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        brandProd7611.setName("brand");
        brandProd7611.setType(ColumnType.VARCHAR);
        brandProd7611.setCharOctetLength(30);

        PhysicalColumn itemProd7611 = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        itemProd7611.setName("item");
        itemProd7611.setType(ColumnType.VARCHAR);
        itemProd7611.setCharOctetLength(30);

        // Create prod7611 table using RolapMappingFactory
        PhysicalTable prod7611 = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        prod7611.setName("prod7611");
        prod7611.getColumns().add(prodIdProd7611);
        prod7611.getColumns().add(classProd7611);
        prod7611.getColumns().add(brandProd7611);
        prod7611.getColumns().add(itemProd7611);

        // Create Geography hierarchy using RolapMappingFactory
        Level stateLevel = RolapMappingFactory.eINSTANCE.createLevel();
        stateLevel.setColumn(stateCdGeography7641);
        stateLevel.setName("State");
        stateLevel.setColumnType(ColumnInternalDataType.STRING);
        stateLevel.setUniqueMembers(true);

        Level cityLevel = RolapMappingFactory.eINSTANCE.createLevel();
        cityLevel.setColumn(cityNmGeography7641);
        cityLevel.setName("City");
        cityLevel.setColumnType(ColumnInternalDataType.STRING);
        cityLevel.setUniqueMembers(true);

        Level zipCodeLevel = RolapMappingFactory.eINSTANCE.createLevel();
        zipCodeLevel.setColumn(zipCdGeography7641);
        zipCodeLevel.setName("Zip Code");
        zipCodeLevel.setColumnType(ColumnInternalDataType.STRING);
        zipCodeLevel.setUniqueMembers(true);

        TableQuery geographyQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        geographyQuery.setTable(geography7641);

        ExplicitHierarchy geographyHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        geographyHierarchy.setHasAll(true);
        geographyHierarchy.setAllMemberName("All Regions");
        geographyHierarchy.setPrimaryKey(custLocIdGeography7641);
        geographyHierarchy.setQuery(geographyQuery);
        geographyHierarchy.getLevels().add(stateLevel);
        geographyHierarchy.getLevels().add(cityLevel);
        geographyHierarchy.getLevels().add(zipCodeLevel);

        StandardDimension geographyDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        geographyDimension.setName("Geography");
        geographyDimension.getHierarchies().add(geographyHierarchy);

        DimensionConnector geographyConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        geographyConnector.setOverrideDimensionName("Geography");
        geographyConnector.setForeignKey(custLocIdCheckin7641);
        geographyConnector.setDimension(geographyDimension);

        // Create Product hierarchy using RolapMappingFactory
        Level classLevel = RolapMappingFactory.eINSTANCE.createLevel();
        classLevel.setColumn(classProd7611);
        classLevel.setName("Class");
        classLevel.setColumnType(ColumnInternalDataType.STRING);
        classLevel.setUniqueMembers(true);

        Level brandLevel = RolapMappingFactory.eINSTANCE.createLevel();
        brandLevel.setColumn(brandProd7611);
        brandLevel.setName("Brand");
        brandLevel.setColumnType(ColumnInternalDataType.STRING);
        brandLevel.setUniqueMembers(true);

        Level itemLevel = RolapMappingFactory.eINSTANCE.createLevel();
        itemLevel.setColumn(itemProd7611);
        itemLevel.setName("Item");
        itemLevel.setColumnType(ColumnInternalDataType.STRING);
        itemLevel.setUniqueMembers(true);

        TableQuery productQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productQuery.setTable(prod7611);

        ExplicitHierarchy productHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        productHierarchy.setHasAll(false);
        productHierarchy.setDefaultMember("Class2");
        productHierarchy.setPrimaryKey(prodIdProd7611);
        productHierarchy.setQuery(productQuery);
        productHierarchy.getLevels().add(classLevel);
        productHierarchy.getLevels().add(brandLevel);
        productHierarchy.getLevels().add(itemLevel);

        StandardDimension productDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        productDimension.setName("Product");
        productDimension.getHierarchies().add(productHierarchy);

        DimensionConnector productConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        productConnector.setOverrideDimensionName("Product");
        productConnector.setForeignKey(prodIdCheckin7641);
        productConnector.setDimension(productDimension);

        // Create measures using RolapMappingFactory
        SumMeasure firstMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        firstMeasure.setName("First Measure");
        firstMeasure.setColumn(firstCheckin7641);
        firstMeasure.setFormatString("#,###");

        SumMeasure requestedValueMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        requestedValueMeasure.setName("Requested Value");
        requestedValueMeasure.setColumn(requestValueCheckin7641);
        requestedValueMeasure.setFormatString("#,###");

        SumMeasure shippedValueMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        shippedValueMeasure.setName("Shipped Value");
        shippedValueMeasure.setColumn(shippedValueCheckin7641);
        shippedValueMeasure.setFormatString("#,###");

        // Create measure group using RolapMappingFactory
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(firstMeasure);
        measureGroup.getMeasures().add(requestedValueMeasure);
        measureGroup.getMeasures().add(shippedValueMeasure);

        // Create cube query using RolapMappingFactory
        TableQuery cubeQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        cubeQuery.setTable(checkin7641);

        // Create cube using RolapMappingFactory
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("ImplicitMember");
        cube.setQuery(cubeQuery);
        cube.getDimensionConnectors().add(geographyConnector);
        cube.getDimensionConnectors().add(productConnector);
        cube.getMeasureGroups().add(measureGroup);

        // Add the cube to the catalog copy
        originalCatalog.getCubes().add(cube);

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
    public Catalog get() {
        return originalCatalog;
    }
}

