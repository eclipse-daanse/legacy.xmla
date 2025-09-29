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

import org.eclipse.daanse.rolap.mapping.model.AggregationColumnName;
import org.eclipse.daanse.rolap.mapping.model.AggregationLevel;
import org.eclipse.daanse.rolap.mapping.model.AggregationMeasure;
import org.eclipse.daanse.rolap.mapping.model.AggregationName;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.DatabaseSchema;
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

public class NonCollapsedAggTestModifierEmf implements CatalogMappingSupplier {

    protected final Catalog catalog;

    // foo_fact table columns
    protected PhysicalColumn lineIdFooFact;
    protected PhysicalColumn unitSalesFooFact;
    protected PhysicalTable fooFact;

    // line table columns
    protected PhysicalColumn lineIdLine;
    protected PhysicalColumn lineNameLine;
    protected PhysicalTable line;

    // line_tenant table columns
    protected PhysicalColumn lineIdLineTenant;
    protected PhysicalColumn tenantIdLineTenant;
    protected PhysicalTable lineTenant;

    // tenant table columns
    protected PhysicalColumn tenantIdTenant;
    protected PhysicalColumn tenantNameTenant;
    protected PhysicalTable tenant;

    // line_line_class table columns
    protected PhysicalColumn lineIdLineLineClass;
    protected PhysicalColumn lineClassIdLineLineClass;
    protected PhysicalTable lineLineClass;

    // distributor table columns
    protected PhysicalColumn distributorIdDistributor;
    protected PhysicalColumn distributorNameDistributor;
    protected PhysicalTable distributor;

    // line_class_distributor table columns
    protected PhysicalColumn lineClassIdLineClassDistributor;
    protected PhysicalColumn distributorIdLineClassDistributor;
    protected PhysicalTable lineClassDistributor;

    // line_class table columns
    protected PhysicalColumn lineClassIdLineClass;
    protected PhysicalColumn lineClassNameLineClass;
    protected PhysicalTable lineClass;

    // network table columns
    protected PhysicalColumn networkIdNetwork;
    protected PhysicalColumn networkNameNetwork;
    protected PhysicalTable network;

    // line_class_network table columns
    protected PhysicalColumn lineClassIdLineClassNetwork;
    protected PhysicalColumn networkIdLineClassNetwork;
    protected PhysicalTable lineClassNetwork;

    // agg_tenant table columns
    protected PhysicalColumn tenantIdAggTenant;
    protected PhysicalColumn unitSalesAggTenant;
    protected PhysicalColumn factCountAggTenant;
    protected PhysicalTable aggTenant;

    // agg_line_class table columns
    protected PhysicalColumn lineClassIdAggLineClass;
    protected PhysicalColumn unitSalesAggLineClass;
    protected PhysicalColumn factCountAggLineClass;
    protected PhysicalTable aggLineClass;

    // agg_10_foo_fact table columns
    protected PhysicalColumn lineClassIdAgg10FooFact;
    protected PhysicalColumn unitSalesAgg10FooFact;
    protected PhysicalColumn factCountAgg10FooFact;
    protected PhysicalTable agg10FooFact;

    public NonCollapsedAggTestModifierEmf(Catalog catalogMapping) {
        this.catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) catalogMapping);

        createTables();
        createCubes();
    }

    protected void createTables() {
        // Create foo_fact table
        lineIdFooFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineIdFooFact.setName("line_id");
        lineIdFooFact.setType(ColumnType.INTEGER);

        unitSalesFooFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesFooFact.setName("unit_sales");
        unitSalesFooFact.setType(ColumnType.INTEGER);

        fooFact = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        fooFact.setName("foo_fact");
        fooFact.getColumns().add(lineIdFooFact);
        fooFact.getColumns().add(unitSalesFooFact);

        // Create line table
        lineIdLine = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineIdLine.setName("line_id");
        lineIdLine.setType(ColumnType.INTEGER);

        lineNameLine = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineNameLine.setName("line_name");
        lineNameLine.setType(ColumnType.VARCHAR);
        lineNameLine.setCharOctetLength(30);

        line = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        line.setName("line");
        line.getColumns().add(lineIdLine);
        line.getColumns().add(lineNameLine);

        // Create line_tenant table
        lineIdLineTenant = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineIdLineTenant.setName("line_id");
        lineIdLineTenant.setType(ColumnType.INTEGER);

        tenantIdLineTenant = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        tenantIdLineTenant.setName("tenant_id");
        tenantIdLineTenant.setType(ColumnType.INTEGER);

        lineTenant = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        lineTenant.setName("line_tenant");
        lineTenant.getColumns().add(lineIdLineTenant);
        lineTenant.getColumns().add(tenantIdLineTenant);

        // Create tenant table
        tenantIdTenant = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        tenantIdTenant.setName("tenant_id");
        tenantIdTenant.setType(ColumnType.INTEGER);

        tenantNameTenant = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        tenantNameTenant.setName("tenant_name");
        tenantNameTenant.setType(ColumnType.VARCHAR);
        tenantNameTenant.setCharOctetLength(30);

        tenant = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        tenant.setName("tenant");
        tenant.getColumns().add(tenantIdTenant);
        tenant.getColumns().add(tenantNameTenant);

        // Create line_line_class table
        lineIdLineLineClass = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineIdLineLineClass.setName("line_id");
        lineIdLineLineClass.setType(ColumnType.INTEGER);

        lineClassIdLineLineClass = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineClassIdLineLineClass.setName("line_class_id");
        lineClassIdLineLineClass.setType(ColumnType.INTEGER);

        lineLineClass = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        lineLineClass.setName("line_line_class");
        lineLineClass.getColumns().add(lineIdLineLineClass);
        lineLineClass.getColumns().add(lineClassIdLineLineClass);

        // Create distributor table
        distributorIdDistributor = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        distributorIdDistributor.setName("distributor_id");
        distributorIdDistributor.setType(ColumnType.INTEGER);

        distributorNameDistributor = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        distributorNameDistributor.setName("distributor_name");
        distributorNameDistributor.setType(ColumnType.VARCHAR);
        distributorNameDistributor.setCharOctetLength(30);

        distributor = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        distributor.setName("distributor");
        distributor.getColumns().add(distributorIdDistributor);
        distributor.getColumns().add(distributorNameDistributor);

        // Create line_class_distributor table
        lineClassIdLineClassDistributor = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineClassIdLineClassDistributor.setName("line_class_id");
        lineClassIdLineClassDistributor.setType(ColumnType.INTEGER);

        distributorIdLineClassDistributor = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        distributorIdLineClassDistributor.setName("distributor_id");
        distributorIdLineClassDistributor.setType(ColumnType.INTEGER);

        lineClassDistributor = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        lineClassDistributor.setName("line_class_distributor");
        lineClassDistributor.getColumns().add(lineClassIdLineClassDistributor);
        lineClassDistributor.getColumns().add(distributorIdLineClassDistributor);

        // Create line_class table
        lineClassIdLineClass = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineClassIdLineClass.setName("line_class_id");
        lineClassIdLineClass.setType(ColumnType.INTEGER);

        lineClassNameLineClass = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineClassNameLineClass.setName("line_class_name");
        lineClassNameLineClass.setType(ColumnType.VARCHAR);
        lineClassNameLineClass.setCharOctetLength(30);

        lineClass = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        lineClass.setName("line_class");
        lineClass.getColumns().add(lineClassIdLineClass);
        lineClass.getColumns().add(lineClassNameLineClass);

        // Create network table
        networkIdNetwork = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        networkIdNetwork.setName("network_id");
        networkIdNetwork.setType(ColumnType.INTEGER);

        networkNameNetwork = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        networkNameNetwork.setName("network_name");
        networkNameNetwork.setType(ColumnType.VARCHAR);
        networkNameNetwork.setCharOctetLength(30);

        network = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        network.setName("network");
        network.getColumns().add(networkIdNetwork);
        network.getColumns().add(networkNameNetwork);

        // Create line_class_network table
        lineClassIdLineClassNetwork = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineClassIdLineClassNetwork.setName("line_class_id");
        lineClassIdLineClassNetwork.setType(ColumnType.INTEGER);

        networkIdLineClassNetwork = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        networkIdLineClassNetwork.setName("network_id");
        networkIdLineClassNetwork.setType(ColumnType.INTEGER);

        lineClassNetwork = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        lineClassNetwork.setName("line_class_network");
        lineClassNetwork.getColumns().add(lineClassIdLineClassNetwork);
        lineClassNetwork.getColumns().add(networkIdLineClassNetwork);

        // Create agg_tenant table
        tenantIdAggTenant = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        tenantIdAggTenant.setName("tenant_id");
        tenantIdAggTenant.setType(ColumnType.INTEGER);

        unitSalesAggTenant = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesAggTenant.setName("unit_sales");
        unitSalesAggTenant.setType(ColumnType.INTEGER);

        factCountAggTenant = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountAggTenant.setName("fact_count");
        factCountAggTenant.setType(ColumnType.INTEGER);

        aggTenant = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        aggTenant.setName("agg_tenant");
        aggTenant.getColumns().add(tenantIdAggTenant);
        aggTenant.getColumns().add(unitSalesAggTenant);
        aggTenant.getColumns().add(factCountAggTenant);

        // Create agg_line_class table
        lineClassIdAggLineClass = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineClassIdAggLineClass.setName("line_class_id");
        lineClassIdAggLineClass.setType(ColumnType.INTEGER);

        unitSalesAggLineClass = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesAggLineClass.setName("unit_sales");
        unitSalesAggLineClass.setType(ColumnType.INTEGER);

        factCountAggLineClass = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountAggLineClass.setName("fact_count");
        factCountAggLineClass.setType(ColumnType.INTEGER);

        aggLineClass = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        aggLineClass.setName("agg_line_class");
        aggLineClass.getColumns().add(lineClassIdAggLineClass);
        aggLineClass.getColumns().add(unitSalesAggLineClass);
        aggLineClass.getColumns().add(factCountAggLineClass);

        // Create agg_10_foo_fact table
        lineClassIdAgg10FooFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        lineClassIdAgg10FooFact.setName("line_class_id");
        lineClassIdAgg10FooFact.setType(ColumnType.INTEGER);

        unitSalesAgg10FooFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        unitSalesAgg10FooFact.setName("unit_sales");
        unitSalesAgg10FooFact.setType(ColumnType.INTEGER);

        factCountAgg10FooFact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountAgg10FooFact.setName("fact_count");
        factCountAgg10FooFact.setType(ColumnType.INTEGER);

        agg10FooFact = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        agg10FooFact.setName("agg_10_foo_fact");
        agg10FooFact.getColumns().add(lineClassIdAgg10FooFact);
        agg10FooFact.getColumns().add(unitSalesAgg10FooFact);
        agg10FooFact.getColumns().add(factCountAgg10FooFact);

        // Add tables to database schema
        if (catalog.getDbschemas().size() > 0) {
            DatabaseSchema dbSchema = catalog.getDbschemas().get(0);
            dbSchema.getTables().add(fooFact);
            dbSchema.getTables().add(line);
            dbSchema.getTables().add(lineTenant);
            dbSchema.getTables().add(tenant);
            dbSchema.getTables().add(lineLineClass);
            dbSchema.getTables().add(distributor);
            dbSchema.getTables().add(lineClassDistributor);
            dbSchema.getTables().add(lineClass);
            dbSchema.getTables().add(network);
            dbSchema.getTables().add(lineClassNetwork);
            dbSchema.getTables().add(aggTenant);
            dbSchema.getTables().add(aggLineClass);
            dbSchema.getTables().add(agg10FooFact);
        }
    }

    protected void createCubes() {
        // Create foo cube
        PhysicalCube fooCube = createFooCube();
        catalog.getCubes().add(fooCube);

        // Create foo2 cube
        PhysicalCube foo2Cube = createFoo2Cube();
        catalog.getCubes().add(foo2Cube);
    }

    protected PhysicalCube createFooCube() {
        // Create table query with aggregations
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(fooFact);

        // Add aggregation tables
        tableQuery.getAggregationTables().add(createAggTenantAggregation());
        tableQuery.getAggregationTables().add(createAggLineClassDistributorAggregation());
        tableQuery.getAggregationTables().add(createAggLineClassNetworkAggregation());

        // Create dimension
        StandardDimension dimension = createDimension();

        // Create dimension connector
        DimensionConnector dimensionConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        dimensionConnector.setOverrideDimensionName("dimension");
        dimensionConnector.setForeignKey(lineIdFooFact);
        dimensionConnector.setDimension(dimension);

        // Create measure
        SumMeasure unitSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        unitSalesMeasure.setName("Unit Sales");
        unitSalesMeasure.setColumn(unitSalesFooFact);
        unitSalesMeasure.setFormatString("Standard");

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(unitSalesMeasure);

        // Create cube
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("foo");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(dimensionConnector);
        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    protected PhysicalCube createFoo2Cube() {
        // Create table query without aggregations
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(fooFact);

        // Create dimension
        StandardDimension dimension = createDimension();

        // Create dimension connector
        DimensionConnector dimensionConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        dimensionConnector.setOverrideDimensionName("dimension");
        dimensionConnector.setForeignKey(lineIdFooFact);
        dimensionConnector.setDimension(dimension);

        // Create measure
        SumMeasure unitSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        unitSalesMeasure.setName("Unit Sales");
        unitSalesMeasure.setColumn(unitSalesFooFact);
        unitSalesMeasure.setFormatString("Standard");

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(unitSalesMeasure);

        // Create cube
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("foo2");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(dimensionConnector);
        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    protected AggregationName createAggTenantAggregation() {
        AggregationName aggName = RolapMappingFactory.eINSTANCE.createAggregationName();
        aggName.setName(aggTenant);
        //aggName.setIgnorecase(false);

        // Aggregation fact count
        AggregationColumnName aggFactCount = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCountAggTenant);
        aggName.setAggregationFactCount(aggFactCount);

        // Aggregation measure
        AggregationMeasure aggMeasure = RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setName("[Measures].[Unit Sales]");
        aggMeasure.setColumn(unitSalesAggTenant);
        aggName.getAggregationMeasures().add(aggMeasure);

        // Aggregation level
        AggregationLevel aggLevel = RolapMappingFactory.eINSTANCE.createAggregationLevel();
        aggLevel.setName("[dimension].[tenant].[tenant]");
        aggLevel.setColumn(tenantIdAggTenant);
        aggLevel.setCollapsed(false);
        aggName.getAggregationLevels().add(aggLevel);

        return aggName;
    }

    protected AggregationName createAggLineClassDistributorAggregation() {
        AggregationName aggName = RolapMappingFactory.eINSTANCE.createAggregationName();
        aggName.setName(aggLineClass);
        //aggName.setIgnorecase(false);

        // Aggregation fact count
        AggregationColumnName aggFactCount = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCountAggLineClass);
        aggName.setAggregationFactCount(aggFactCount);

        // Aggregation measure
        AggregationMeasure aggMeasure = RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setName("[Measures].[Unit Sales]");
        aggMeasure.setColumn(unitSalesAggLineClass);
        aggName.getAggregationMeasures().add(aggMeasure);

        // Aggregation level
        AggregationLevel aggLevel = RolapMappingFactory.eINSTANCE.createAggregationLevel();
        aggLevel.setName("[dimension].[distributor].[line class]");
        aggLevel.setColumn(lineClassIdAggLineClass);
        aggLevel.setCollapsed(false);
        aggName.getAggregationLevels().add(aggLevel);

        return aggName;
    }

    protected AggregationName createAggLineClassNetworkAggregation() {
        AggregationName aggName = RolapMappingFactory.eINSTANCE.createAggregationName();
        aggName.setName(aggLineClass);
        //aggName.setIgnorecase(false);

        // Aggregation fact count
        AggregationColumnName aggFactCount = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCountAggLineClass);
        aggName.setAggregationFactCount(aggFactCount);

        // Aggregation measure
        AggregationMeasure aggMeasure = RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setName("[Measures].[Unit Sales]");
        aggMeasure.setColumn(unitSalesAggLineClass);
        aggName.getAggregationMeasures().add(aggMeasure);

        // Aggregation level
        AggregationLevel aggLevel = RolapMappingFactory.eINSTANCE.createAggregationLevel();
        aggLevel.setName("[dimension].[network].[line class]");
        aggLevel.setColumn(lineClassIdAggLineClass);
        aggLevel.setCollapsed(false);
        aggName.getAggregationLevels().add(aggLevel);

        return aggName;
    }

    protected StandardDimension createDimension() {
        StandardDimension dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        dimension.setName("dimension");

        // Create tenant hierarchy
        dimension.getHierarchies().add(createTenantHierarchy());

        // Create distributor hierarchy
        dimension.getHierarchies().add(createDistributorHierarchy());

        // Create network hierarchy
        dimension.getHierarchies().add(createNetworkHierarchy());

        return dimension;
    }

    protected ExplicitHierarchy createTenantHierarchy() {
        // Create join: line_tenant JOIN tenant
        TableQuery tenantQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tenantQuery.setTable(tenant);

        JoinedQueryElement tenantJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        tenantJoinedElement.setKey(tenantIdTenant);
        tenantJoinedElement.setQuery(tenantQuery);

        TableQuery lineTenantQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineTenantQuery.setTable(lineTenant);

        JoinedQueryElement lineTenantJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineTenantJoinedElement.setKey(tenantIdLineTenant);
        lineTenantJoinedElement.setQuery(lineTenantQuery);

        JoinQuery tenantJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        tenantJoin.setLeft(lineTenantJoinedElement);
        tenantJoin.setRight(tenantJoinedElement);

        // Create join: line JOIN (line_tenant JOIN tenant)
        TableQuery lineQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineQuery.setTable(line);

        JoinedQueryElement lineJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineJoinedElement.setKey(lineIdLine);
        lineJoinedElement.setQuery(lineQuery);

        JoinedQueryElement tenantJoinElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        tenantJoinElement.setAlias("line_tenant");
        tenantJoinElement.setKey(lineIdLineTenant);
        tenantJoinElement.setQuery(tenantJoin);

        JoinQuery fullJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        fullJoin.setLeft(lineJoinedElement);
        fullJoin.setRight(tenantJoinElement);

        // Create levels
        Level tenantLevel = RolapMappingFactory.eINSTANCE.createLevel();
        tenantLevel.setName("tenant");
        tenantLevel.setColumn(tenantIdTenant);
        tenantLevel.setNameColumn(tenantNameTenant);
        tenantLevel.setUniqueMembers(true);

        Level lineLevel = RolapMappingFactory.eINSTANCE.createLevel();
        lineLevel.setName("line");
        lineLevel.setColumn(lineIdLine);
        lineLevel.setNameColumn(lineNameLine);

        // Create hierarchy
        ExplicitHierarchy hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        hierarchy.setName("tenant");
        hierarchy.setHasAll(true);
        hierarchy.setAllMemberName("All tenants");
        hierarchy.setPrimaryKey(lineIdLine);
        hierarchy.setQuery(fullJoin);
        hierarchy.getLevels().add(tenantLevel);
        hierarchy.getLevels().add(lineLevel);

        return hierarchy;
    }

    protected ExplicitHierarchy createDistributorHierarchy() {
        // Create innermost join: line_class_distributor JOIN distributor
        TableQuery distributorQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        distributorQuery.setTable(distributor);

        JoinedQueryElement distributorJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        distributorJoinedElement.setKey(distributorIdDistributor);
        distributorJoinedElement.setQuery(distributorQuery);

        TableQuery lineClassDistributorQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineClassDistributorQuery.setTable(lineClassDistributor);

        JoinedQueryElement lineClassDistributorJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineClassDistributorJoinedElement.setKey(distributorIdLineClassDistributor);
        lineClassDistributorJoinedElement.setQuery(lineClassDistributorQuery);

        JoinQuery distributorJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        distributorJoin.setLeft(lineClassDistributorJoinedElement);
        distributorJoin.setRight(distributorJoinedElement);

        // Create middle join: line_class JOIN (line_class_distributor JOIN distributor)
        TableQuery lineClassQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineClassQuery.setTable(lineClass);

        JoinedQueryElement lineClassJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineClassJoinedElement.setKey(lineClassIdLineClass);
        lineClassJoinedElement.setQuery(lineClassQuery);

        JoinedQueryElement distributorJoinElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        distributorJoinElement.setAlias("line_class_distributor");
        distributorJoinElement.setKey(lineClassIdLineClassDistributor);
        distributorJoinElement.setQuery(distributorJoin);

        JoinQuery lineClassJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        lineClassJoin.setLeft(lineClassJoinedElement);
        lineClassJoin.setRight(distributorJoinElement);

        // Create another middle join: line_line_class JOIN (line_class JOIN ...)
        TableQuery lineLineClassQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineLineClassQuery.setTable(lineLineClass);

        JoinedQueryElement lineLineClassJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineLineClassJoinedElement.setKey(lineClassIdLineLineClass);
        lineLineClassJoinedElement.setQuery(lineLineClassQuery);

        JoinedQueryElement lineClassJoinElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineClassJoinElement.setAlias("line_class");
        lineClassJoinElement.setKey(lineClassIdLineClass);
        lineClassJoinElement.setQuery(lineClassJoin);

        JoinQuery lineLineClassJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        lineLineClassJoin.setLeft(lineLineClassJoinedElement);
        lineLineClassJoin.setRight(lineClassJoinElement);

        // Create outermost join: line JOIN (line_line_class JOIN ...)
        TableQuery lineQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineQuery.setTable(line);

        JoinedQueryElement lineJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineJoinedElement.setKey(lineIdLine);
        lineJoinedElement.setQuery(lineQuery);

        JoinedQueryElement lineLineClassJoinElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineLineClassJoinElement.setAlias("line_line_class");
        lineLineClassJoinElement.setKey(lineIdLineLineClass);
        lineLineClassJoinElement.setQuery(lineLineClassJoin);

        JoinQuery fullJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        fullJoin.setLeft(lineJoinedElement);
        fullJoin.setRight(lineLineClassJoinElement);

        // Create levels
        Level distributorLevel = RolapMappingFactory.eINSTANCE.createLevel();
        distributorLevel.setName("distributor");
        distributorLevel.setColumn(distributorIdDistributor);
        distributorLevel.setNameColumn(distributorNameDistributor);

        Level lineClassLevel = RolapMappingFactory.eINSTANCE.createLevel();
        lineClassLevel.setName("line class");
        lineClassLevel.setColumn(lineClassIdLineClass);
        lineClassLevel.setNameColumn(lineClassNameLineClass);
        lineClassLevel.setUniqueMembers(true);

        Level lineLevel = RolapMappingFactory.eINSTANCE.createLevel();
        lineLevel.setName("line");
        lineLevel.setColumn(lineIdLine);
        lineLevel.setNameColumn(lineNameLine);

        // Create hierarchy
        ExplicitHierarchy hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        hierarchy.setName("distributor");
        hierarchy.setHasAll(true);
        hierarchy.setAllMemberName("All distributors");
        hierarchy.setPrimaryKey(lineIdLine);
        hierarchy.setQuery(fullJoin);
        hierarchy.getLevels().add(distributorLevel);
        hierarchy.getLevels().add(lineClassLevel);
        hierarchy.getLevels().add(lineLevel);

        return hierarchy;
    }

    protected ExplicitHierarchy createNetworkHierarchy() {
        // Create innermost join: line_class_network JOIN network
        TableQuery networkQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        networkQuery.setTable(network);

        JoinedQueryElement networkJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        networkJoinedElement.setKey(networkIdNetwork);
        networkJoinedElement.setQuery(networkQuery);

        TableQuery lineClassNetworkQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineClassNetworkQuery.setTable(lineClassNetwork);

        JoinedQueryElement lineClassNetworkJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineClassNetworkJoinedElement.setKey(networkIdLineClassNetwork);
        lineClassNetworkJoinedElement.setQuery(lineClassNetworkQuery);

        JoinQuery networkJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        networkJoin.setLeft(lineClassNetworkJoinedElement);
        networkJoin.setRight(networkJoinedElement);

        // Create middle join: line_class JOIN (line_class_network JOIN network)
        TableQuery lineClassQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineClassQuery.setTable(lineClass);

        JoinedQueryElement lineClassJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineClassJoinedElement.setKey(lineClassIdLineClass);
        lineClassJoinedElement.setQuery(lineClassQuery);

        JoinedQueryElement networkJoinElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        networkJoinElement.setAlias("line_class_network");
        networkJoinElement.setKey(lineClassIdLineClassNetwork);
        networkJoinElement.setQuery(networkJoin);

        JoinQuery lineClassJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        lineClassJoin.setLeft(lineClassJoinedElement);
        lineClassJoin.setRight(networkJoinElement);

        // Create another middle join: line_line_class JOIN (line_class JOIN ...)
        TableQuery lineLineClassQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineLineClassQuery.setTable(lineLineClass);

        JoinedQueryElement lineLineClassJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineLineClassJoinedElement.setKey(lineClassIdLineLineClass);
        lineLineClassJoinedElement.setQuery(lineLineClassQuery);

        JoinedQueryElement lineClassJoinElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineClassJoinElement.setAlias("line_class");
        lineClassJoinElement.setKey(lineClassIdLineClass);
        lineClassJoinElement.setQuery(lineClassJoin);

        JoinQuery lineLineClassJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        lineLineClassJoin.setLeft(lineLineClassJoinedElement);
        lineLineClassJoin.setRight(lineClassJoinElement);

        // Create outermost join: line JOIN (line_line_class JOIN ...)
        TableQuery lineQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        lineQuery.setTable(line);

        JoinedQueryElement lineJoinedElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineJoinedElement.setKey(lineIdLine);
        lineJoinedElement.setQuery(lineQuery);

        JoinedQueryElement lineLineClassJoinElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        lineLineClassJoinElement.setAlias("line_line_class");
        lineLineClassJoinElement.setKey(lineIdLineLineClass);
        lineLineClassJoinElement.setQuery(lineLineClassJoin);

        JoinQuery fullJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();
        fullJoin.setLeft(lineJoinedElement);
        fullJoin.setRight(lineLineClassJoinElement);

        // Create levels
        Level networkLevel = RolapMappingFactory.eINSTANCE.createLevel();
        networkLevel.setName("network");
        networkLevel.setColumn(networkIdNetwork);
        networkLevel.setNameColumn(networkNameNetwork);

        Level lineClassLevel = RolapMappingFactory.eINSTANCE.createLevel();
        lineClassLevel.setName("line class");
        lineClassLevel.setColumn(lineClassIdLineClass);
        lineClassLevel.setNameColumn(lineClassNameLineClass);
        lineClassLevel.setUniqueMembers(true);

        Level lineLevel = RolapMappingFactory.eINSTANCE.createLevel();
        lineLevel.setName("line");
        lineLevel.setColumn(lineIdLine);
        lineLevel.setNameColumn(lineNameLine);

        // Create hierarchy
        ExplicitHierarchy hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        hierarchy.setName("network");
        hierarchy.setHasAll(true);
        hierarchy.setAllMemberName("All networks");
        hierarchy.setPrimaryKey(lineIdLine);
        hierarchy.setQuery(fullJoin);
        hierarchy.getLevels().add(networkLevel);
        hierarchy.getLevels().add(lineClassLevel);
        hierarchy.getLevels().add(lineLevel);

        return hierarchy;
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
