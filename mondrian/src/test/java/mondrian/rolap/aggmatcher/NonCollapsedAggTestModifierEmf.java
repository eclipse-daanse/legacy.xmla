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

import org.eclipse.daanse.cwm.model.cwm.resource.relational.Column;
import org.eclipse.daanse.cwm.model.cwm.resource.relational.Schema;
import org.eclipse.daanse.cwm.model.cwm.resource.relational.Table;
import org.eclipse.daanse.cwm.util.resource.relational.SqlSimpleTypes;
import org.eclipse.daanse.rolap.mapping.model.catalog.Catalog;
import org.eclipse.daanse.rolap.mapping.model.catalog.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationColumnName;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationFactory;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationLevel;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationMeasure;
import org.eclipse.daanse.rolap.mapping.model.database.aggregation.AggregationName;
import org.eclipse.daanse.rolap.mapping.model.database.source.JoinSource;
import org.eclipse.daanse.rolap.mapping.model.database.source.JoinedQueryElement;
import org.eclipse.daanse.rolap.mapping.model.database.source.SourceFactory;
import org.eclipse.daanse.rolap.mapping.model.database.source.TableSource;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.CubeFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.measure.MeasureFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.cube.measure.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.DimensionFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.HierarchyFactory;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.Level;
import org.eclipse.daanse.rolap.mapping.model.olap.dimension.hierarchy.level.LevelFactory;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
public class NonCollapsedAggTestModifierEmf implements CatalogMappingSupplier {

    protected final Catalog catalog;

    // foo_fact table columns
    protected Column lineIdFooFact;
    protected Column unitSalesFooFact;
    protected Table fooFact;

    // line table columns
    protected Column lineIdLine;
    protected Column lineNameLine;
    protected Table line;

    // line_tenant table columns
    protected Column lineIdLineTenant;
    protected Column tenantIdLineTenant;
    protected Table lineTenant;

    // tenant table columns
    protected Column tenantIdTenant;
    protected Column tenantNameTenant;
    protected Table tenant;

    // line_line_class table columns
    protected Column lineIdLineLineClass;
    protected Column lineClassIdLineLineClass;
    protected Table lineLineClass;

    // distributor table columns
    protected Column distributorIdDistributor;
    protected Column distributorNameDistributor;
    protected Table distributor;

    // line_class_distributor table columns
    protected Column lineClassIdLineClassDistributor;
    protected Column distributorIdLineClassDistributor;
    protected Table lineClassDistributor;

    // line_class table columns
    protected Column lineClassIdLineClass;
    protected Column lineClassNameLineClass;
    protected Table lineClass;

    // network table columns
    protected Column networkIdNetwork;
    protected Column networkNameNetwork;
    protected Table network;

    // line_class_network table columns
    protected Column lineClassIdLineClassNetwork;
    protected Column networkIdLineClassNetwork;
    protected Table lineClassNetwork;

    // agg_tenant table columns
    protected Column tenantIdAggTenant;
    protected Column unitSalesAggTenant;
    protected Column factCountAggTenant;
    protected Table aggTenant;

    // agg_line_class table columns
    protected Column lineClassIdAggLineClass;
    protected Column unitSalesAggLineClass;
    protected Column factCountAggLineClass;
    protected Table aggLineClass;

    // agg_10_foo_fact table columns
    protected Column lineClassIdAgg10FooFact;
    protected Column unitSalesAgg10FooFact;
    protected Column factCountAgg10FooFact;
    protected Table agg10FooFact;

    public NonCollapsedAggTestModifierEmf(Catalog catalogMapping) {
        this.catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) catalogMapping);

        createTables();
        createCubes();
    }

    protected void createTables() {
        // Create foo_fact table
        lineIdFooFact = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineIdFooFact.setName("line_id");
        lineIdFooFact.setType(SqlSimpleTypes.Sql99.integerType());

        unitSalesFooFact = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesFooFact.setName("unit_sales");
        unitSalesFooFact.setType(SqlSimpleTypes.Sql99.integerType());

        fooFact = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        fooFact.setName("foo_fact");
        fooFact.getFeature().add(lineIdFooFact);
        fooFact.getFeature().add(unitSalesFooFact);

        // Create line table
        lineIdLine = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineIdLine.setName("line_id");
        lineIdLine.setType(SqlSimpleTypes.Sql99.integerType());

        lineNameLine = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineNameLine.setName("line_name");
        lineNameLine.setType(SqlSimpleTypes.varcharType(255));
        // lineNameLine.setCharOctetLength(30);

        line = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        line.setName("line");
        line.getFeature().add(lineIdLine);
        line.getFeature().add(lineNameLine);

        // Create line_tenant table
        lineIdLineTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineIdLineTenant.setName("line_id");
        lineIdLineTenant.setType(SqlSimpleTypes.Sql99.integerType());

        tenantIdLineTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        tenantIdLineTenant.setName("tenant_id");
        tenantIdLineTenant.setType(SqlSimpleTypes.Sql99.integerType());

        lineTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        lineTenant.setName("line_tenant");
        lineTenant.getFeature().add(lineIdLineTenant);
        lineTenant.getFeature().add(tenantIdLineTenant);

        // Create tenant table
        tenantIdTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        tenantIdTenant.setName("tenant_id");
        tenantIdTenant.setType(SqlSimpleTypes.Sql99.integerType());

        tenantNameTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        tenantNameTenant.setName("tenant_name");
        tenantNameTenant.setType(SqlSimpleTypes.varcharType(255));
        // tenantNameTenant.setCharOctetLength(30);

        tenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        tenant.setName("tenant");
        tenant.getFeature().add(tenantIdTenant);
        tenant.getFeature().add(tenantNameTenant);

        // Create line_line_class table
        lineIdLineLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineIdLineLineClass.setName("line_id");
        lineIdLineLineClass.setType(SqlSimpleTypes.Sql99.integerType());

        lineClassIdLineLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineClassIdLineLineClass.setName("line_class_id");
        lineClassIdLineLineClass.setType(SqlSimpleTypes.Sql99.integerType());

        lineLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        lineLineClass.setName("line_line_class");
        lineLineClass.getFeature().add(lineIdLineLineClass);
        lineLineClass.getFeature().add(lineClassIdLineLineClass);

        // Create distributor table
        distributorIdDistributor = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        distributorIdDistributor.setName("distributor_id");
        distributorIdDistributor.setType(SqlSimpleTypes.Sql99.integerType());

        distributorNameDistributor = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        distributorNameDistributor.setName("distributor_name");
        distributorNameDistributor.setType(SqlSimpleTypes.varcharType(255));
        // distributorNameDistributor.setCharOctetLength(30);

        distributor = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        distributor.setName("distributor");
        distributor.getFeature().add(distributorIdDistributor);
        distributor.getFeature().add(distributorNameDistributor);

        // Create line_class_distributor table
        lineClassIdLineClassDistributor = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineClassIdLineClassDistributor.setName("line_class_id");
        lineClassIdLineClassDistributor.setType(SqlSimpleTypes.Sql99.integerType());

        distributorIdLineClassDistributor = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        distributorIdLineClassDistributor.setName("distributor_id");
        distributorIdLineClassDistributor.setType(SqlSimpleTypes.Sql99.integerType());

        lineClassDistributor = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        lineClassDistributor.setName("line_class_distributor");
        lineClassDistributor.getFeature().add(lineClassIdLineClassDistributor);
        lineClassDistributor.getFeature().add(distributorIdLineClassDistributor);

        // Create line_class table
        lineClassIdLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineClassIdLineClass.setName("line_class_id");
        lineClassIdLineClass.setType(SqlSimpleTypes.Sql99.integerType());

        lineClassNameLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineClassNameLineClass.setName("line_class_name");
        lineClassNameLineClass.setType(SqlSimpleTypes.varcharType(255));
        // lineClassNameLineClass.setCharOctetLength(30);

        lineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        lineClass.setName("line_class");
        lineClass.getFeature().add(lineClassIdLineClass);
        lineClass.getFeature().add(lineClassNameLineClass);

        // Create network table
        networkIdNetwork = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        networkIdNetwork.setName("network_id");
        networkIdNetwork.setType(SqlSimpleTypes.Sql99.integerType());

        networkNameNetwork = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        networkNameNetwork.setName("network_name");
        networkNameNetwork.setType(SqlSimpleTypes.varcharType(255));
        // networkNameNetwork.setCharOctetLength(30);

        network = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        network.setName("network");
        network.getFeature().add(networkIdNetwork);
        network.getFeature().add(networkNameNetwork);

        // Create line_class_network table
        lineClassIdLineClassNetwork = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineClassIdLineClassNetwork.setName("line_class_id");
        lineClassIdLineClassNetwork.setType(SqlSimpleTypes.Sql99.integerType());

        networkIdLineClassNetwork = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        networkIdLineClassNetwork.setName("network_id");
        networkIdLineClassNetwork.setType(SqlSimpleTypes.Sql99.integerType());

        lineClassNetwork = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        lineClassNetwork.setName("line_class_network");
        lineClassNetwork.getFeature().add(lineClassIdLineClassNetwork);
        lineClassNetwork.getFeature().add(networkIdLineClassNetwork);

        // Create agg_tenant table
        tenantIdAggTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        tenantIdAggTenant.setName("tenant_id");
        tenantIdAggTenant.setType(SqlSimpleTypes.Sql99.integerType());

        unitSalesAggTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesAggTenant.setName("unit_sales");
        unitSalesAggTenant.setType(SqlSimpleTypes.Sql99.integerType());

        factCountAggTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        factCountAggTenant.setName("fact_count");
        factCountAggTenant.setType(SqlSimpleTypes.Sql99.integerType());

        aggTenant = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        aggTenant.setName("agg_tenant");
        aggTenant.getFeature().add(tenantIdAggTenant);
        aggTenant.getFeature().add(unitSalesAggTenant);
        aggTenant.getFeature().add(factCountAggTenant);

        // Create agg_line_class table
        lineClassIdAggLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineClassIdAggLineClass.setName("line_class_id");
        lineClassIdAggLineClass.setType(SqlSimpleTypes.Sql99.integerType());

        unitSalesAggLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesAggLineClass.setName("unit_sales");
        unitSalesAggLineClass.setType(SqlSimpleTypes.Sql99.integerType());

        factCountAggLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        factCountAggLineClass.setName("fact_count");
        factCountAggLineClass.setType(SqlSimpleTypes.Sql99.integerType());

        aggLineClass = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        aggLineClass.setName("agg_line_class");
        aggLineClass.getFeature().add(lineClassIdAggLineClass);
        aggLineClass.getFeature().add(unitSalesAggLineClass);
        aggLineClass.getFeature().add(factCountAggLineClass);

        // Create agg_10_foo_fact table
        lineClassIdAgg10FooFact = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        lineClassIdAgg10FooFact.setName("line_class_id");
        lineClassIdAgg10FooFact.setType(SqlSimpleTypes.Sql99.integerType());

        unitSalesAgg10FooFact = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        unitSalesAgg10FooFact.setName("unit_sales");
        unitSalesAgg10FooFact.setType(SqlSimpleTypes.Sql99.integerType());

        factCountAgg10FooFact = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createColumn();
        factCountAgg10FooFact.setName("fact_count");
        factCountAgg10FooFact.setType(SqlSimpleTypes.Sql99.integerType());

        agg10FooFact = org.eclipse.daanse.cwm.model.cwm.resource.relational.RelationalFactory.eINSTANCE.createTable();
        agg10FooFact.setName("agg_10_foo_fact");
        agg10FooFact.getFeature().add(lineClassIdAgg10FooFact);
        agg10FooFact.getFeature().add(unitSalesAgg10FooFact);
        agg10FooFact.getFeature().add(factCountAgg10FooFact);

        // Add tables to database schema
        if (catalog.getDbschemas().size() > 0) {
            Schema dbSchema = catalog.getDbschemas().get(0);
            dbSchema.getOwnedElement().add(fooFact);
            dbSchema.getOwnedElement().add(line);
            dbSchema.getOwnedElement().add(lineTenant);
            dbSchema.getOwnedElement().add(tenant);
            dbSchema.getOwnedElement().add(lineLineClass);
            dbSchema.getOwnedElement().add(distributor);
            dbSchema.getOwnedElement().add(lineClassDistributor);
            dbSchema.getOwnedElement().add(lineClass);
            dbSchema.getOwnedElement().add(network);
            dbSchema.getOwnedElement().add(lineClassNetwork);
            dbSchema.getOwnedElement().add(aggTenant);
            dbSchema.getOwnedElement().add(aggLineClass);
            dbSchema.getOwnedElement().add(agg10FooFact);
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
        TableSource tableQuery = SourceFactory.eINSTANCE.createTableSource();
        tableQuery.setTable(fooFact);

        // Add aggregation tables
        tableQuery.getAggregationTables().add(createAggTenantAggregation());
        tableQuery.getAggregationTables().add(createAggLineClassDistributorAggregation());
        tableQuery.getAggregationTables().add(createAggLineClassNetworkAggregation());

        // Create dimension
        StandardDimension dimension = createDimension();

        // Create dimension connector
        DimensionConnector dimensionConnector = DimensionFactory.eINSTANCE.createDimensionConnector();
        dimensionConnector.setOverrideDimensionName("dimension");
        dimensionConnector.setForeignKey(lineIdFooFact);
        dimensionConnector.setDimension(dimension);

        // Create measure
        SumMeasure unitSalesMeasure = MeasureFactory.eINSTANCE.createSumMeasure();
        unitSalesMeasure.setName("Unit Sales");
        unitSalesMeasure.setColumn(unitSalesFooFact);
        unitSalesMeasure.setFormatString("Standard");

        // Create measure group
        MeasureGroup measureGroup = CubeFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(unitSalesMeasure);

        // Create cube
        PhysicalCube cube = CubeFactory.eINSTANCE.createPhysicalCube();
        cube.setName("foo");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(dimensionConnector);
        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    protected PhysicalCube createFoo2Cube() {
        // Create table query without aggregations
        TableSource tableQuery = SourceFactory.eINSTANCE.createTableSource();
        tableQuery.setTable(fooFact);

        // Create dimension
        StandardDimension dimension = createDimension();

        // Create dimension connector
        DimensionConnector dimensionConnector = DimensionFactory.eINSTANCE.createDimensionConnector();
        dimensionConnector.setOverrideDimensionName("dimension");
        dimensionConnector.setForeignKey(lineIdFooFact);
        dimensionConnector.setDimension(dimension);

        // Create measure
        SumMeasure unitSalesMeasure = MeasureFactory.eINSTANCE.createSumMeasure();
        unitSalesMeasure.setName("Unit Sales");
        unitSalesMeasure.setColumn(unitSalesFooFact);
        unitSalesMeasure.setFormatString("Standard");

        // Create measure group
        MeasureGroup measureGroup = CubeFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(unitSalesMeasure);

        // Create cube
        PhysicalCube cube = CubeFactory.eINSTANCE.createPhysicalCube();
        cube.setName("foo2");
        cube.setQuery(tableQuery);
        cube.getDimensionConnectors().add(dimensionConnector);
        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    protected AggregationName createAggTenantAggregation() {
        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(aggTenant);
        //aggName.setIgnorecase(false);

        // Aggregation fact count
        AggregationColumnName aggFactCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCountAggTenant);
        aggName.setAggregationFactCount(aggFactCount);

        // Aggregation measure
        AggregationMeasure aggMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setName("[Measures].[Unit Sales]");
        aggMeasure.setColumn(unitSalesAggTenant);
        aggName.getAggregationMeasures().add(aggMeasure);

        // Aggregation level
        AggregationLevel aggLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        aggLevel.setName("[dimension].[tenant].[tenant]");
        aggLevel.setColumn(tenantIdAggTenant);
        aggLevel.setCollapsed(false);
        aggName.getAggregationLevels().add(aggLevel);

        return aggName;
    }

    protected AggregationName createAggLineClassDistributorAggregation() {
        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(aggLineClass);
        //aggName.setIgnorecase(false);

        // Aggregation fact count
        AggregationColumnName aggFactCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCountAggLineClass);
        aggName.setAggregationFactCount(aggFactCount);

        // Aggregation measure
        AggregationMeasure aggMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setName("[Measures].[Unit Sales]");
        aggMeasure.setColumn(unitSalesAggLineClass);
        aggName.getAggregationMeasures().add(aggMeasure);

        // Aggregation level
        AggregationLevel aggLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        aggLevel.setName("[dimension].[distributor].[line class]");
        aggLevel.setColumn(lineClassIdAggLineClass);
        aggLevel.setCollapsed(false);
        aggName.getAggregationLevels().add(aggLevel);

        return aggName;
    }

    protected AggregationName createAggLineClassNetworkAggregation() {
        AggregationName aggName = AggregationFactory.eINSTANCE.createAggregationName();
        aggName.setName(aggLineClass);
        //aggName.setIgnorecase(false);

        // Aggregation fact count
        AggregationColumnName aggFactCount = AggregationFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCountAggLineClass);
        aggName.setAggregationFactCount(aggFactCount);

        // Aggregation measure
        AggregationMeasure aggMeasure = AggregationFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setName("[Measures].[Unit Sales]");
        aggMeasure.setColumn(unitSalesAggLineClass);
        aggName.getAggregationMeasures().add(aggMeasure);

        // Aggregation level
        AggregationLevel aggLevel = AggregationFactory.eINSTANCE.createAggregationLevel();
        aggLevel.setName("[dimension].[network].[line class]");
        aggLevel.setColumn(lineClassIdAggLineClass);
        aggLevel.setCollapsed(false);
        aggName.getAggregationLevels().add(aggLevel);

        return aggName;
    }

    protected StandardDimension createDimension() {
        StandardDimension dimension = DimensionFactory.eINSTANCE.createStandardDimension();
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
        TableSource tenantQuery = SourceFactory.eINSTANCE.createTableSource();
        tenantQuery.setTable(tenant);

        JoinedQueryElement tenantJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        tenantJoinedElement.setKey(tenantIdTenant);
        tenantJoinedElement.setQuery(tenantQuery);

        TableSource lineTenantQuery = SourceFactory.eINSTANCE.createTableSource();
        lineTenantQuery.setTable(lineTenant);

        JoinedQueryElement lineTenantJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineTenantJoinedElement.setKey(tenantIdLineTenant);
        lineTenantJoinedElement.setQuery(lineTenantQuery);

        JoinSource tenantJoin = SourceFactory.eINSTANCE.createJoinSource();
        tenantJoin.setLeft(lineTenantJoinedElement);
        tenantJoin.setRight(tenantJoinedElement);

        // Create join: line JOIN (line_tenant JOIN tenant)
        TableSource lineQuery = SourceFactory.eINSTANCE.createTableSource();
        lineQuery.setTable(line);

        JoinedQueryElement lineJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineJoinedElement.setKey(lineIdLine);
        lineJoinedElement.setQuery(lineQuery);

        JoinedQueryElement tenantJoinElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        tenantJoinElement.setAlias("line_tenant");
        tenantJoinElement.setKey(lineIdLineTenant);
        tenantJoinElement.setQuery(tenantJoin);

        JoinSource fullJoin = SourceFactory.eINSTANCE.createJoinSource();
        fullJoin.setLeft(lineJoinedElement);
        fullJoin.setRight(tenantJoinElement);

        // Create levels
        Level tenantLevel = LevelFactory.eINSTANCE.createLevel();
        tenantLevel.setName("tenant");
        tenantLevel.setColumn(tenantIdTenant);
        tenantLevel.setNameColumn(tenantNameTenant);
        tenantLevel.setUniqueMembers(true);

        Level lineLevel = LevelFactory.eINSTANCE.createLevel();
        lineLevel.setName("line");
        lineLevel.setColumn(lineIdLine);
        lineLevel.setNameColumn(lineNameLine);

        // Create hierarchy
        ExplicitHierarchy hierarchy = HierarchyFactory.eINSTANCE.createExplicitHierarchy();
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
        TableSource distributorQuery = SourceFactory.eINSTANCE.createTableSource();
        distributorQuery.setTable(distributor);

        JoinedQueryElement distributorJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        distributorJoinedElement.setKey(distributorIdDistributor);
        distributorJoinedElement.setQuery(distributorQuery);

        TableSource lineClassDistributorQuery = SourceFactory.eINSTANCE.createTableSource();
        lineClassDistributorQuery.setTable(lineClassDistributor);

        JoinedQueryElement lineClassDistributorJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineClassDistributorJoinedElement.setKey(distributorIdLineClassDistributor);
        lineClassDistributorJoinedElement.setQuery(lineClassDistributorQuery);

        JoinSource distributorJoin = SourceFactory.eINSTANCE.createJoinSource();
        distributorJoin.setLeft(lineClassDistributorJoinedElement);
        distributorJoin.setRight(distributorJoinedElement);

        // Create middle join: line_class JOIN (line_class_distributor JOIN distributor)
        TableSource lineClassQuery = SourceFactory.eINSTANCE.createTableSource();
        lineClassQuery.setTable(lineClass);

        JoinedQueryElement lineClassJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineClassJoinedElement.setKey(lineClassIdLineClass);
        lineClassJoinedElement.setQuery(lineClassQuery);

        JoinedQueryElement distributorJoinElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        distributorJoinElement.setAlias("line_class_distributor");
        distributorJoinElement.setKey(lineClassIdLineClassDistributor);
        distributorJoinElement.setQuery(distributorJoin);

        JoinSource lineClassJoin = SourceFactory.eINSTANCE.createJoinSource();
        lineClassJoin.setLeft(lineClassJoinedElement);
        lineClassJoin.setRight(distributorJoinElement);

        // Create another middle join: line_line_class JOIN (line_class JOIN ...)
        TableSource lineLineClassQuery = SourceFactory.eINSTANCE.createTableSource();
        lineLineClassQuery.setTable(lineLineClass);

        JoinedQueryElement lineLineClassJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineLineClassJoinedElement.setKey(lineClassIdLineLineClass);
        lineLineClassJoinedElement.setQuery(lineLineClassQuery);

        JoinedQueryElement lineClassJoinElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineClassJoinElement.setAlias("line_class");
        lineClassJoinElement.setKey(lineClassIdLineClass);
        lineClassJoinElement.setQuery(lineClassJoin);

        JoinSource lineLineClassJoin = SourceFactory.eINSTANCE.createJoinSource();
        lineLineClassJoin.setLeft(lineLineClassJoinedElement);
        lineLineClassJoin.setRight(lineClassJoinElement);

        // Create outermost join: line JOIN (line_line_class JOIN ...)
        TableSource lineQuery = SourceFactory.eINSTANCE.createTableSource();
        lineQuery.setTable(line);

        JoinedQueryElement lineJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineJoinedElement.setKey(lineIdLine);
        lineJoinedElement.setQuery(lineQuery);

        JoinedQueryElement lineLineClassJoinElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineLineClassJoinElement.setAlias("line_line_class");
        lineLineClassJoinElement.setKey(lineIdLineLineClass);
        lineLineClassJoinElement.setQuery(lineLineClassJoin);

        JoinSource fullJoin = SourceFactory.eINSTANCE.createJoinSource();
        fullJoin.setLeft(lineJoinedElement);
        fullJoin.setRight(lineLineClassJoinElement);

        // Create levels
        Level distributorLevel = LevelFactory.eINSTANCE.createLevel();
        distributorLevel.setName("distributor");
        distributorLevel.setColumn(distributorIdDistributor);
        distributorLevel.setNameColumn(distributorNameDistributor);

        Level lineClassLevel = LevelFactory.eINSTANCE.createLevel();
        lineClassLevel.setName("line class");
        lineClassLevel.setColumn(lineClassIdLineClass);
        lineClassLevel.setNameColumn(lineClassNameLineClass);
        lineClassLevel.setUniqueMembers(true);

        Level lineLevel = LevelFactory.eINSTANCE.createLevel();
        lineLevel.setName("line");
        lineLevel.setColumn(lineIdLine);
        lineLevel.setNameColumn(lineNameLine);

        // Create hierarchy
        ExplicitHierarchy hierarchy = HierarchyFactory.eINSTANCE.createExplicitHierarchy();
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
        TableSource networkQuery = SourceFactory.eINSTANCE.createTableSource();
        networkQuery.setTable(network);

        JoinedQueryElement networkJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        networkJoinedElement.setKey(networkIdNetwork);
        networkJoinedElement.setQuery(networkQuery);

        TableSource lineClassNetworkQuery = SourceFactory.eINSTANCE.createTableSource();
        lineClassNetworkQuery.setTable(lineClassNetwork);

        JoinedQueryElement lineClassNetworkJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineClassNetworkJoinedElement.setKey(networkIdLineClassNetwork);
        lineClassNetworkJoinedElement.setQuery(lineClassNetworkQuery);

        JoinSource networkJoin = SourceFactory.eINSTANCE.createJoinSource();
        networkJoin.setLeft(lineClassNetworkJoinedElement);
        networkJoin.setRight(networkJoinedElement);

        // Create middle join: line_class JOIN (line_class_network JOIN network)
        TableSource lineClassQuery = SourceFactory.eINSTANCE.createTableSource();
        lineClassQuery.setTable(lineClass);

        JoinedQueryElement lineClassJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineClassJoinedElement.setKey(lineClassIdLineClass);
        lineClassJoinedElement.setQuery(lineClassQuery);

        JoinedQueryElement networkJoinElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        networkJoinElement.setAlias("line_class_network");
        networkJoinElement.setKey(lineClassIdLineClassNetwork);
        networkJoinElement.setQuery(networkJoin);

        JoinSource lineClassJoin = SourceFactory.eINSTANCE.createJoinSource();
        lineClassJoin.setLeft(lineClassJoinedElement);
        lineClassJoin.setRight(networkJoinElement);

        // Create another middle join: line_line_class JOIN (line_class JOIN ...)
        TableSource lineLineClassQuery = SourceFactory.eINSTANCE.createTableSource();
        lineLineClassQuery.setTable(lineLineClass);

        JoinedQueryElement lineLineClassJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineLineClassJoinedElement.setKey(lineClassIdLineLineClass);
        lineLineClassJoinedElement.setQuery(lineLineClassQuery);

        JoinedQueryElement lineClassJoinElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineClassJoinElement.setAlias("line_class");
        lineClassJoinElement.setKey(lineClassIdLineClass);
        lineClassJoinElement.setQuery(lineClassJoin);

        JoinSource lineLineClassJoin = SourceFactory.eINSTANCE.createJoinSource();
        lineLineClassJoin.setLeft(lineLineClassJoinedElement);
        lineLineClassJoin.setRight(lineClassJoinElement);

        // Create outermost join: line JOIN (line_line_class JOIN ...)
        TableSource lineQuery = SourceFactory.eINSTANCE.createTableSource();
        lineQuery.setTable(line);

        JoinedQueryElement lineJoinedElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineJoinedElement.setKey(lineIdLine);
        lineJoinedElement.setQuery(lineQuery);

        JoinedQueryElement lineLineClassJoinElement = SourceFactory.eINSTANCE.createJoinedQueryElement();
        lineLineClassJoinElement.setAlias("line_line_class");
        lineLineClassJoinElement.setKey(lineIdLineLineClass);
        lineLineClassJoinElement.setQuery(lineLineClassJoin);

        JoinSource fullJoin = SourceFactory.eINSTANCE.createJoinSource();
        fullJoin.setLeft(lineJoinedElement);
        fullJoin.setRight(lineLineClassJoinElement);

        // Create levels
        Level networkLevel = LevelFactory.eINSTANCE.createLevel();
        networkLevel.setName("network");
        networkLevel.setColumn(networkIdNetwork);
        networkLevel.setNameColumn(networkNameNetwork);

        Level lineClassLevel = LevelFactory.eINSTANCE.createLevel();
        lineClassLevel.setName("line class");
        lineClassLevel.setColumn(lineClassIdLineClass);
        lineClassLevel.setNameColumn(lineClassNameLineClass);
        lineClassLevel.setUniqueMembers(true);

        Level lineLevel = LevelFactory.eINSTANCE.createLevel();
        lineLevel.setName("line");
        lineLevel.setColumn(lineIdLine);
        lineLevel.setNameColumn(lineNameLine);

        // Create hierarchy
        ExplicitHierarchy hierarchy = HierarchyFactory.eINSTANCE.createExplicitHierarchy();
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
