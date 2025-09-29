/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package mondrian.rolap;

import java.util.Collection;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.CalculatedMember;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.DatabaseSchema;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.VirtualCube;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

/**
 * EMF version of
 * TestCalculatedDefaultMeasureOnVirtualCubeNoThrowExceptionModifier from
 * NonEmptyTest. Creates a Sales cube with a calculated member and a virtual
 * cube referencing it.
 *
 * <Schema name="FoodMart"> <Dimension name="Store">...</Dimension>
 * <Cube name="Sales" defaultMeasure="Unit Sales">
 * <Table name="sales_fact_1997"/>
 * <DimensionUsage name="Store" source="Store" foreignKey="store_id"/>
 * <Measure name="Unit Sales" column="unit_sales" aggregator="sum" formatString=
 * "Standard"/> <CalculatedMember name="dummyMeasure" dimension="Measures">
 * <Formula>1</Formula> </CalculatedMember> </Cube>
 * <VirtualCube defaultMeasure="dummyMeasure" name="virtual">
 * <VirtualCubeDimension name="Store"/>
 * <VirtualCubeMeasure cubeName="Sales" name="[Measures].[Unit Sales]"/>
 * <VirtualCubeMeasure name="[Measures].[dummyMeasure]" cubeName="Sales"/>
 * </VirtualCube> </Schema>
 */
public class TestCalculatedDefaultMeasureOnVirtualCubeNoThrowExceptionModifier implements CatalogMappingSupplier {

    private final Catalog catalog;

    public TestCalculatedDefaultMeasureOnVirtualCubeNoThrowExceptionModifier(Catalog baseCatalog) {
        // Copy the base catalog using EcoreUtil
        this.catalog = RolapMappingFactory.eINSTANCE.createCatalog();

        // Static table query for fact table
        TableQuery TABLE_QUERY_SALES_FACT;

        // Static measure
        SumMeasure MEASURE_UNIT_SALES;

        // Static measure group
        MeasureGroup MEASURE_GROUP;

        // Static calculated member
        CalculatedMember CALCULATED_MEMBER_DUMMY;

        // Static dimension connector for Store
        DimensionConnector CONNECTOR_STORE;

        // Static physical cube Sales
        PhysicalCube CUBE_SALES;

        // Static dimension connector for virtual cube
        DimensionConnector VIRTUAL_CONNECTOR_STORE;

        // Static virtual cube
        VirtualCube VIRTUAL_CUBE;

        // Create fact table query
        TABLE_QUERY_SALES_FACT = RolapMappingFactory.eINSTANCE.createTableQuery();
        TABLE_QUERY_SALES_FACT.setTable(CatalogSupplier.TABLE_SALES_FACT);

        // Create measure
        MEASURE_UNIT_SALES = RolapMappingFactory.eINSTANCE.createSumMeasure();
        MEASURE_UNIT_SALES.setName("Unit Sales");
        MEASURE_UNIT_SALES.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
        MEASURE_UNIT_SALES.setFormatString("Standard");

        // Create measure group
        MEASURE_GROUP = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        MEASURE_GROUP.getMeasures().add(MEASURE_UNIT_SALES);

        // Create calculated member
        CALCULATED_MEMBER_DUMMY = RolapMappingFactory.eINSTANCE.createCalculatedMember();
        CALCULATED_MEMBER_DUMMY.setName("dummyMeasure");
        CALCULATED_MEMBER_DUMMY.setFormula("1");
        // Note: dimension is set to Measures by default for calculated members

        // Create dimension connector for Store
        CONNECTOR_STORE = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        CONNECTOR_STORE.setOverrideDimensionName("Store");
        CONNECTOR_STORE.setDimension(CatalogSupplier.DIMENSION_STORE);
        CONNECTOR_STORE.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);

        // Create physical cube
        CUBE_SALES = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        CUBE_SALES.setName("Sales");
        CUBE_SALES.setQuery(TABLE_QUERY_SALES_FACT);
        CUBE_SALES.getDimensionConnectors().add(CONNECTOR_STORE);
        CUBE_SALES.getMeasureGroups().add(MEASURE_GROUP);
        CUBE_SALES.getCalculatedMembers().add(CALCULATED_MEMBER_DUMMY);
        CUBE_SALES.setDefaultMeasure(MEASURE_UNIT_SALES);

        // Create dimension connector for virtual cube
        //VIRTUAL_CONNECTOR_STORE = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        //VIRTUAL_CONNECTOR_STORE.setOverrideDimensionName("Store");
        //VIRTUAL_CONNECTOR_STORE.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);
        //VIRTUAL_CONNECTOR_STORE.setDimension((Dimension) copier.get(CatalogSupplier.DIMENSION_STORE));
        //VIRTUAL_CONNECTOR_STORE.setPhysicalCube(CUBE_SALES);

        // Create virtual cube
        VIRTUAL_CUBE = RolapMappingFactory.eINSTANCE.createVirtualCube();
        VIRTUAL_CUBE.setName("virtual");
        VIRTUAL_CUBE.getDimensionConnectors().add(CONNECTOR_STORE);
        VIRTUAL_CUBE.getReferencedMeasures().add(MEASURE_UNIT_SALES);
        VIRTUAL_CUBE.getReferencedCalculatedMembers().add(CALCULATED_MEMBER_DUMMY);

        // Add both cubes to the catalog
        this.catalog.setName("FoodMart");
        this.catalog.getDbschemas().addAll((Collection<? extends DatabaseSchema>) baseCatalog.getDbschemas());
        this.catalog.getCubes().add(CUBE_SALES);
        this.catalog.getCubes().add(VIRTUAL_CUBE);
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
