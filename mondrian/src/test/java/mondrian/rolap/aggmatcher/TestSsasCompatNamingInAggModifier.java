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
package mondrian.rolap.aggmatcher;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.model.AggregationColumnName;
import org.eclipse.daanse.rolap.mapping.model.AggregationLevel;
import org.eclipse.daanse.rolap.mapping.model.AggregationMeasure;
import org.eclipse.daanse.rolap.mapping.model.AggregationName;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.JoinQuery;
import org.eclipse.daanse.rolap.mapping.model.JoinedQueryElement;
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

/**
 * EMF version of TestSsasCompatNamingInAggModifier from NonCollapsedAggTest.
 */
public class TestSsasCompatNamingInAggModifier implements CatalogMappingSupplier {

    private final Catalog catalog;

    public TestSsasCompatNamingInAggModifier(Catalog baseCatalog) {
        // Copy the base catalog using EcoreUtil
        this.catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) baseCatalog);

        // Create all physical tables and columns using RolapMappingFactory
        RolapMappingFactory factory = RolapMappingFactory.eINSTANCE;

        // Create foo_fact table with columns
        PhysicalColumn lineIdFooFact = factory.createPhysicalColumn();
        lineIdFooFact.setName("line_id");
        lineIdFooFact.setType(ColumnType.INTEGER);

        PhysicalColumn unitSalesFooFact = factory.createPhysicalColumn();
        unitSalesFooFact.setName("unit_sales");
        unitSalesFooFact.setType(ColumnType.INTEGER);

        PhysicalTable fooFact = factory.createPhysicalTable();
        fooFact.setName("foo_fact");
        fooFact.getColumns().add(lineIdFooFact);
        fooFact.getColumns().add(unitSalesFooFact);

        // Create line table
        PhysicalColumn lineIdLine = factory.createPhysicalColumn();
        lineIdLine.setName("line_id");
        lineIdLine.setType(ColumnType.INTEGER);

        PhysicalColumn lineNameLine = factory.createPhysicalColumn();
        lineNameLine.setName("line_name");
        lineNameLine.setType(ColumnType.VARCHAR);
        lineNameLine.setCharOctetLength(30);

        PhysicalTable line = factory.createPhysicalTable();
        line.setName("line");
        line.getColumns().add(lineIdLine);
        line.getColumns().add(lineNameLine);

        // Create line_tenant table
        PhysicalColumn lineIdLineTenant = factory.createPhysicalColumn();
        lineIdLineTenant.setName("line_id");
        lineIdLineTenant.setType(ColumnType.INTEGER);

        PhysicalColumn tenantIdLineTenant = factory.createPhysicalColumn();
        tenantIdLineTenant.setName("tenant_id");
        tenantIdLineTenant.setType(ColumnType.INTEGER);

        PhysicalTable lineTenant = factory.createPhysicalTable();
        lineTenant.setName("line_tenant");
        lineTenant.getColumns().add(lineIdLineTenant);
        lineTenant.getColumns().add(tenantIdLineTenant);

        // Create tenant table
        PhysicalColumn tenantIdTenant = factory.createPhysicalColumn();
        tenantIdTenant.setName("tenant_id");
        tenantIdTenant.setType(ColumnType.INTEGER);

        PhysicalColumn tenantNameTenant = factory.createPhysicalColumn();
        tenantNameTenant.setName("tenant_name");
        tenantNameTenant.setType(ColumnType.VARCHAR);
        tenantNameTenant.setCharOctetLength(30);

        PhysicalTable tenant = factory.createPhysicalTable();
        tenant.setName("tenant");
        tenant.getColumns().add(tenantIdTenant);
        tenant.getColumns().add(tenantNameTenant);

        // Create line_line_class table
        PhysicalColumn lineIdLineLineClass = factory.createPhysicalColumn();
        lineIdLineLineClass.setName("line_id");
        lineIdLineLineClass.setType(ColumnType.INTEGER);

        PhysicalColumn lineClassIdLineLineClass = factory.createPhysicalColumn();
        lineClassIdLineLineClass.setName("line_class_id");
        lineClassIdLineLineClass.setType(ColumnType.INTEGER);

        PhysicalTable lineLineClass = factory.createPhysicalTable();
        lineLineClass.setName("line_line_class");
        lineLineClass.getColumns().add(lineIdLineLineClass);
        lineLineClass.getColumns().add(lineClassIdLineLineClass);

        // Create distributor table
        PhysicalColumn distributorIdDistributor = factory.createPhysicalColumn();
        distributorIdDistributor.setName("distributor_id");
        distributorIdDistributor.setType(ColumnType.INTEGER);

        PhysicalColumn distributorNameDistributor = factory.createPhysicalColumn();
        distributorNameDistributor.setName("distributor_name");
        distributorNameDistributor.setType(ColumnType.VARCHAR);
        distributorNameDistributor.setCharOctetLength(30);

        PhysicalTable distributor = factory.createPhysicalTable();
        distributor.setName("distributor");
        distributor.getColumns().add(distributorIdDistributor);
        distributor.getColumns().add(distributorNameDistributor);

        // Create line_class_distributor table
        PhysicalColumn lineClassIdLineClassDistributor = factory.createPhysicalColumn();
        lineClassIdLineClassDistributor.setName("line_class_id");
        lineClassIdLineClassDistributor.setType(ColumnType.INTEGER);

        PhysicalColumn distributorIdLineClassDistributor = factory.createPhysicalColumn();
        distributorIdLineClassDistributor.setName("distributor_id");
        distributorIdLineClassDistributor.setType(ColumnType.INTEGER);

        PhysicalTable lineClassDistributor = factory.createPhysicalTable();
        lineClassDistributor.setName("line_class_distributor");
        lineClassDistributor.getColumns().add(lineClassIdLineClassDistributor);
        lineClassDistributor.getColumns().add(distributorIdLineClassDistributor);

        // Create line_class table
        PhysicalColumn lineClassIdLineClass = factory.createPhysicalColumn();
        lineClassIdLineClass.setName("line_class_id");
        lineClassIdLineClass.setType(ColumnType.INTEGER);

        PhysicalColumn lineClassNameLineClass = factory.createPhysicalColumn();
        lineClassNameLineClass.setName("line_class_name");
        lineClassNameLineClass.setType(ColumnType.VARCHAR);
        lineClassNameLineClass.setCharOctetLength(30);

        PhysicalTable lineClass = factory.createPhysicalTable();
        lineClass.setName("line_class");
        lineClass.getColumns().add(lineClassIdLineClass);
        lineClass.getColumns().add(lineClassNameLineClass);

        // Create network table
        PhysicalColumn networkIdNetwork = factory.createPhysicalColumn();
        networkIdNetwork.setName("network_id");
        networkIdNetwork.setType(ColumnType.INTEGER);

        PhysicalColumn networkNameNetwork = factory.createPhysicalColumn();
        networkNameNetwork.setName("network_name");
        networkNameNetwork.setType(ColumnType.VARCHAR);
        networkNameNetwork.setCharOctetLength(30);

        PhysicalTable network = factory.createPhysicalTable();
        network.setName("network");
        network.getColumns().add(networkIdNetwork);
        network.getColumns().add(networkNameNetwork);

        // Create line_class_network table
        PhysicalColumn lineClassIdLineClassNetwork = factory.createPhysicalColumn();
        lineClassIdLineClassNetwork.setName("line_class_id");
        lineClassIdLineClassNetwork.setType(ColumnType.INTEGER);

        PhysicalColumn networkIdLineClassNetwork = factory.createPhysicalColumn();
        networkIdLineClassNetwork.setName("network_id");
        networkIdLineClassNetwork.setType(ColumnType.INTEGER);

        PhysicalTable lineClassNetwork = factory.createPhysicalTable();
        lineClassNetwork.setName("line_class_network");
        lineClassNetwork.getColumns().add(lineClassIdLineClassNetwork);
        lineClassNetwork.getColumns().add(networkIdLineClassNetwork);

        // Create aggregation tables
        // agg_tenant
        PhysicalColumn tenantIdAggTenant = factory.createPhysicalColumn();
        tenantIdAggTenant.setName("tenant_id");
        tenantIdAggTenant.setType(ColumnType.INTEGER);

        PhysicalColumn unitSalesAggTenant = factory.createPhysicalColumn();
        unitSalesAggTenant.setName("unit_sales");
        unitSalesAggTenant.setType(ColumnType.INTEGER);

        PhysicalColumn factCountAggTenant = factory.createPhysicalColumn();
        factCountAggTenant.setName("fact_count");
        factCountAggTenant.setType(ColumnType.INTEGER);

        PhysicalTable aggTenant = factory.createPhysicalTable();
        aggTenant.setName("agg_tenant");
        aggTenant.getColumns().add(tenantIdAggTenant);
        aggTenant.getColumns().add(unitSalesAggTenant);
        aggTenant.getColumns().add(factCountAggTenant);

        // agg_line_class
        PhysicalColumn lineClassIdAggLineClass = factory.createPhysicalColumn();
        lineClassIdAggLineClass.setName("line_class_id");
        lineClassIdAggLineClass.setType(ColumnType.INTEGER);

        PhysicalColumn unitSalesAggLineClass = factory.createPhysicalColumn();
        unitSalesAggLineClass.setName("unit_sales");
        unitSalesAggLineClass.setType(ColumnType.INTEGER);

        PhysicalColumn factCountAggLineClass = factory.createPhysicalColumn();
        factCountAggLineClass.setName("fact_count");
        factCountAggLineClass.setType(ColumnType.INTEGER);

        PhysicalTable aggLineClass = factory.createPhysicalTable();
        aggLineClass.setName("agg_line_class");
        aggLineClass.getColumns().add(lineClassIdAggLineClass);
        aggLineClass.getColumns().add(unitSalesAggLineClass);
        aggLineClass.getColumns().add(factCountAggLineClass);

        // Create TableQuery for fooFact with aggregation tables
        TableQuery fooFactQuery = factory.createTableQuery();
        fooFactQuery.setTable(fooFact);

        // Create aggregation definitions
        // Aggregation 1: agg_tenant
        AggregationColumnName aggTenantFactCount = factory.createAggregationColumnName();
        aggTenantFactCount.setColumn(factCountAggTenant);

        AggregationMeasure aggTenantMeasure = factory.createAggregationMeasure();
        aggTenantMeasure.setName("[Measures].[Unit Sales]");
        aggTenantMeasure.setColumn(unitSalesAggTenant);

        AggregationLevel aggTenantLevel = factory.createAggregationLevel();
        aggTenantLevel.setName("[dimension].[tenant].[tenant]");
        aggTenantLevel.setColumn(tenantIdAggTenant);
        aggTenantLevel.setCollapsed(false);

        AggregationName aggName1 = factory.createAggregationName();
        aggName1.setName(aggTenant);
        aggName1.setAggregationFactCount(aggTenantFactCount);
        aggName1.getAggregationMeasures().add(aggTenantMeasure);
        aggName1.getAggregationLevels().add(aggTenantLevel);

        // Aggregation 2: agg_line_class for distributor
        AggregationColumnName aggLineClassFactCount = factory.createAggregationColumnName();
        aggLineClassFactCount.setColumn(factCountAggLineClass);

        AggregationMeasure aggLineClassMeasure1 = factory.createAggregationMeasure();
        aggLineClassMeasure1.setName("[Measures].[Unit Sales]");
        aggLineClassMeasure1.setColumn(unitSalesAggLineClass);

        AggregationLevel aggLineClassLevel1 = factory.createAggregationLevel();
        aggLineClassLevel1.setName("[dimension].[distributor].[line class]");
        aggLineClassLevel1.setColumn(lineClassIdAggLineClass);
        aggLineClassLevel1.setCollapsed(false);

        AggregationName aggName2 = factory.createAggregationName();
        aggName2.setName(aggLineClass);
        aggName2.setAggregationFactCount(aggLineClassFactCount);
        aggName2.getAggregationMeasures().add(aggLineClassMeasure1);
        aggName2.getAggregationLevels().add(aggLineClassLevel1);

        // Aggregation 3: agg_line_class for network
        AggregationColumnName aggLineClassFactCount2 = factory.createAggregationColumnName();
        aggLineClassFactCount2.setColumn(factCountAggLineClass);

        AggregationMeasure aggLineClassMeasure2 = factory.createAggregationMeasure();
        aggLineClassMeasure2.setName("[Measures].[Unit Sales]");
        aggLineClassMeasure2.setColumn(unitSalesAggLineClass);

        AggregationLevel aggLineClassLevel2 = factory.createAggregationLevel();
        aggLineClassLevel2.setName("[dimension].[network].[line class]");
        aggLineClassLevel2.setColumn(lineClassIdAggLineClass);
        aggLineClassLevel2.setCollapsed(false);

        AggregationName aggName3 = factory.createAggregationName();
        aggName3.setName(aggLineClass);
        aggName3.setAggregationFactCount(aggLineClassFactCount2);
        aggName3.getAggregationMeasures().add(aggLineClassMeasure2);
        aggName3.getAggregationLevels().add(aggLineClassLevel2);

        fooFactQuery.getAggregationTables().add(aggName1);
        fooFactQuery.getAggregationTables().add(aggName2);
        fooFactQuery.getAggregationTables().add(aggName3);

        // Create hierarchies
        // Tenant hierarchy
        TableQuery lineTenantQuery = factory.createTableQuery();
        lineTenantQuery.setTable(lineTenant);

        TableQuery tenantQuery = factory.createTableQuery();
        tenantQuery.setTable(tenant);

        JoinedQueryElement tenantJoinRight = factory.createJoinedQueryElement();
        tenantJoinRight.setKey(tenantIdTenant);
        tenantJoinRight.setQuery(tenantQuery);

        JoinedQueryElement tenantLineJoinLeft = factory.createJoinedQueryElement();
        tenantLineJoinLeft.setKey(tenantIdLineTenant);
        tenantLineJoinLeft.setQuery(lineTenantQuery);

        JoinQuery lineTenantToTenant = factory.createJoinQuery();
        lineTenantToTenant.setLeft(tenantLineJoinLeft);
        lineTenantToTenant.setRight(tenantJoinRight);

        JoinedQueryElement lineTenantJoinRight = factory.createJoinedQueryElement();
        lineTenantJoinRight.setKey(lineIdLineTenant);
        lineTenantJoinRight.setAlias("line_tenant");
        lineTenantJoinRight.setQuery(lineTenantToTenant);

        TableQuery lineQuery1 = factory.createTableQuery();
        lineQuery1.setTable(line);

        JoinedQueryElement lineJoinLeft = factory.createJoinedQueryElement();
        lineJoinLeft.setKey(lineIdLine);
        lineJoinLeft.setQuery(lineQuery1);

        JoinQuery tenantHierarchyJoin = factory.createJoinQuery();
        tenantHierarchyJoin.setLeft(lineJoinLeft);
        tenantHierarchyJoin.setRight(lineTenantJoinRight);

        Level tenantLevel = factory.createLevel();
        tenantLevel.setName("tenant");
        tenantLevel.setColumn(tenantIdTenant);
        tenantLevel.setNameColumn(tenantNameTenant);
        tenantLevel.setUniqueMembers(true);

        Level lineLevel1 = factory.createLevel();
        lineLevel1.setName("line");
        lineLevel1.setColumn(lineIdLine);
        lineLevel1.setNameColumn(lineNameLine);

        ExplicitHierarchy tenantHierarchy = factory.createExplicitHierarchy();
        tenantHierarchy.setName("tenant");
        tenantHierarchy.setHasAll(true);
        tenantHierarchy.setAllMemberName("All tenants");
        tenantHierarchy.setPrimaryKey(lineIdLine);
        tenantHierarchy.setQuery(tenantHierarchyJoin);
        tenantHierarchy.getLevels().add(tenantLevel);
        tenantHierarchy.getLevels().add(lineLevel1);

        // Distributor hierarchy (complex multi-level join)
        TableQuery lineQuery2 = factory.createTableQuery();
        lineQuery2.setTable(line);

        TableQuery lineLineClassQuery = factory.createTableQuery();
        lineLineClassQuery.setTable(lineLineClass);

        TableQuery lineClassQuery1 = factory.createTableQuery();
        lineClassQuery1.setTable(lineClass);

        TableQuery lineClassDistributorQuery = factory.createTableQuery();
        lineClassDistributorQuery.setTable(lineClassDistributor);

        TableQuery distributorQuery = factory.createTableQuery();
        distributorQuery.setTable(distributor);

        // Build distributor hierarchy join (innermost to outermost)
        JoinedQueryElement distributorJoinLeft = factory.createJoinedQueryElement();
        distributorJoinLeft.setKey(distributorIdLineClassDistributor);
        distributorJoinLeft.setQuery(lineClassDistributorQuery);

        JoinedQueryElement distributorJoinRight = factory.createJoinedQueryElement();
        distributorJoinRight.setKey(distributorIdDistributor);
        distributorJoinRight.setQuery(distributorQuery);

        JoinQuery lineClassDistributorToDistributor = factory.createJoinQuery();
        lineClassDistributorToDistributor.setLeft(distributorJoinLeft);
        lineClassDistributorToDistributor.setRight(distributorJoinRight);

        JoinedQueryElement lineClassJoinLeft = factory.createJoinedQueryElement();
        lineClassJoinLeft.setKey(lineClassIdLineClass);
        lineClassJoinLeft.setQuery(lineClassQuery1);

        JoinedQueryElement lineClassDistributorJoinRight = factory.createJoinedQueryElement();
        lineClassDistributorJoinRight.setKey(lineClassIdLineClassDistributor);
        lineClassDistributorJoinRight.setAlias("line_class_distributor");
        lineClassDistributorJoinRight.setQuery(lineClassDistributorToDistributor);

        JoinQuery lineClassToDistributor = factory.createJoinQuery();
        lineClassToDistributor.setLeft(lineClassJoinLeft);
        lineClassToDistributor.setRight(lineClassDistributorJoinRight);

        JoinedQueryElement lineLineClassJoinLeft = factory.createJoinedQueryElement();
        lineLineClassJoinLeft.setKey(lineClassIdLineLineClass);
        lineLineClassJoinLeft.setQuery(lineLineClassQuery);

        JoinedQueryElement lineClassJoinRight = factory.createJoinedQueryElement();
        lineClassJoinRight.setKey(lineClassIdLineClassDistributor);
        lineClassJoinRight.setAlias("line_class");
        lineClassJoinRight.setQuery(lineClassToDistributor);

        JoinQuery lineLineClassToLineClass = factory.createJoinQuery();
        lineLineClassToLineClass.setLeft(lineLineClassJoinLeft);
        lineLineClassToLineClass.setRight(lineClassJoinRight);

        JoinedQueryElement lineJoinLeft2 = factory.createJoinedQueryElement();
        lineJoinLeft2.setKey(lineIdLine);
        lineJoinLeft2.setQuery(lineQuery2);

        JoinedQueryElement lineLineClassJoinRight = factory.createJoinedQueryElement();
        lineLineClassJoinRight.setKey(lineIdLineLineClass);
        lineLineClassJoinRight.setAlias("line_line_class");
        lineLineClassJoinRight.setQuery(lineLineClassToLineClass);

        JoinQuery distributorHierarchyJoin = factory.createJoinQuery();
        distributorHierarchyJoin.setLeft(lineJoinLeft2);
        distributorHierarchyJoin.setRight(lineLineClassJoinRight);

        Level distributorLevel = factory.createLevel();
        distributorLevel.setName("distributor");
        distributorLevel.setColumn(distributorIdDistributor);
        distributorLevel.setNameColumn(distributorNameDistributor);

        Level lineClassLevel1 = factory.createLevel();
        lineClassLevel1.setName("line class");
        lineClassLevel1.setColumn(lineClassIdLineClass);
        lineClassLevel1.setNameColumn(lineClassNameLineClass);
        lineClassLevel1.setUniqueMembers(true);

        Level lineLevel2 = factory.createLevel();
        lineLevel2.setName("line");
        lineLevel2.setColumn(lineIdLine);
        lineLevel2.setNameColumn(lineNameLine);

        ExplicitHierarchy distributorHierarchy = factory.createExplicitHierarchy();
        distributorHierarchy.setName("distributor");
        distributorHierarchy.setHasAll(true);
        distributorHierarchy.setAllMemberName("All distributors");
        distributorHierarchy.setPrimaryKey(lineIdLine);
        distributorHierarchy.setQuery(distributorHierarchyJoin);
        distributorHierarchy.getLevels().add(distributorLevel);
        distributorHierarchy.getLevels().add(lineClassLevel1);
        distributorHierarchy.getLevels().add(lineLevel2);

        // Network hierarchy (complex multi-level join)
        TableQuery lineQuery3 = factory.createTableQuery();
        lineQuery3.setTable(line);

        TableQuery lineLineClassQuery2 = factory.createTableQuery();
        lineLineClassQuery2.setTable(lineLineClass);

        TableQuery lineClassQuery2 = factory.createTableQuery();
        lineClassQuery2.setTable(lineClass);

        TableQuery lineClassNetworkQuery = factory.createTableQuery();
        lineClassNetworkQuery.setTable(lineClassNetwork);

        TableQuery networkQuery = factory.createTableQuery();
        networkQuery.setTable(network);

        // Build network hierarchy join
        JoinedQueryElement lineClassNetworkJoinLeft = factory.createJoinedQueryElement();
        lineClassNetworkJoinLeft.setKey(networkIdLineClassNetwork);
        lineClassNetworkJoinLeft.setQuery(lineClassNetworkQuery);

        JoinedQueryElement networkJoinRight = factory.createJoinedQueryElement();
        networkJoinRight.setKey(networkIdNetwork);
        networkJoinRight.setQuery(networkQuery);

        JoinQuery lineClassNetworkToNetwork = factory.createJoinQuery();
        lineClassNetworkToNetwork.setLeft(lineClassNetworkJoinLeft);
        lineClassNetworkToNetwork.setRight(networkJoinRight);

        JoinedQueryElement lineClassJoinLeft2 = factory.createJoinedQueryElement();
        lineClassJoinLeft2.setKey(lineClassIdLineClass);
        lineClassJoinLeft2.setQuery(lineClassQuery2);

        JoinedQueryElement lineClassNetworkJoinRight = factory.createJoinedQueryElement();
        lineClassNetworkJoinRight.setKey(lineClassIdLineClassNetwork);
        lineClassNetworkJoinRight.setAlias("line_class_network");
        lineClassNetworkJoinRight.setQuery(lineClassNetworkToNetwork);

        JoinQuery lineClassToNetwork = factory.createJoinQuery();
        lineClassToNetwork.setLeft(lineClassJoinLeft2);
        lineClassToNetwork.setRight(lineClassNetworkJoinRight);

        JoinedQueryElement lineLineClassJoinLeft2 = factory.createJoinedQueryElement();
        lineLineClassJoinLeft2.setKey(lineClassIdLineLineClass);
        lineLineClassJoinLeft2.setQuery(lineLineClassQuery2);

        JoinedQueryElement lineClassJoinRight2 = factory.createJoinedQueryElement();
        lineClassJoinRight2.setKey(lineClassIdLineClass);
        lineClassJoinRight2.setAlias("line_class");
        lineClassJoinRight2.setQuery(lineClassToNetwork);

        JoinQuery lineLineClassToLineClass2 = factory.createJoinQuery();
        lineLineClassToLineClass2.setLeft(lineLineClassJoinLeft2);
        lineLineClassToLineClass2.setRight(lineClassJoinRight2);

        JoinedQueryElement lineJoinLeft3 = factory.createJoinedQueryElement();
        lineJoinLeft3.setKey(lineIdLine);
        lineJoinLeft3.setQuery(lineQuery3);

        JoinedQueryElement lineLineClassJoinRight2 = factory.createJoinedQueryElement();
        lineLineClassJoinRight2.setKey(lineIdLineLineClass);
        lineLineClassJoinRight2.setAlias("line_line_class");
        lineLineClassJoinRight2.setQuery(lineLineClassToLineClass2);

        JoinQuery networkHierarchyJoin = factory.createJoinQuery();
        networkHierarchyJoin.setLeft(lineJoinLeft3);
        networkHierarchyJoin.setRight(lineLineClassJoinRight2);

        Level networkLevel = factory.createLevel();
        networkLevel.setName("network");
        networkLevel.setColumn(networkIdNetwork);
        networkLevel.setNameColumn(networkNameNetwork);

        Level lineClassLevel2 = factory.createLevel();
        lineClassLevel2.setName("line class");
        lineClassLevel2.setColumn(lineClassIdLineClass);
        lineClassLevel2.setNameColumn(lineClassNameLineClass);
        lineClassLevel2.setUniqueMembers(true);

        Level lineLevel3 = factory.createLevel();
        lineLevel3.setName("line");
        lineLevel3.setColumn(lineIdLine);
        lineLevel3.setNameColumn(lineNameLine);

        ExplicitHierarchy networkHierarchy = factory.createExplicitHierarchy();
        networkHierarchy.setName("network");
        networkHierarchy.setHasAll(true);
        networkHierarchy.setAllMemberName("All networks");
        networkHierarchy.setPrimaryKey(lineIdLine);
        networkHierarchy.setQuery(networkHierarchyJoin);
        networkHierarchy.getLevels().add(networkLevel);
        networkHierarchy.getLevels().add(lineClassLevel2);
        networkHierarchy.getLevels().add(lineLevel3);

        // Create dimension
        StandardDimension dimension = factory.createStandardDimension();
        dimension.setName("dimension");
        dimension.getHierarchies().add(tenantHierarchy);
        dimension.getHierarchies().add(distributorHierarchy);
        dimension.getHierarchies().add(networkHierarchy);

        // Create dimension connector
        DimensionConnector dimensionConnector = factory.createDimensionConnector();
        dimensionConnector.setOverrideDimensionName("dimension");
        dimensionConnector.setForeignKey(lineIdFooFact);
        dimensionConnector.setDimension(dimension);

        // Create measure
        SumMeasure unitSalesMeasure = factory.createSumMeasure();
        unitSalesMeasure.setName("Unit Sales");
        unitSalesMeasure.setColumn(unitSalesFooFact);
        unitSalesMeasure.setFormatString("Standard");

        // Create measure group
        MeasureGroup measureGroup = factory.createMeasureGroup();
        measureGroup.getMeasures().add(unitSalesMeasure);

        // Create cube
        PhysicalCube testSsasCube = factory.createPhysicalCube();
        testSsasCube.setName("testSsas");
        testSsasCube.setQuery(fooFactQuery);
        testSsasCube.getDimensionConnectors().add(dimensionConnector);
        testSsasCube.getMeasureGroups().add(measureGroup);

        // Add cube to catalog
        this.catalog.getCubes().add(testSsasCube);
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
