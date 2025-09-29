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
package mondrian.rolap.agg;

import org.eclipse.daanse.rolap.mapping.model.AccessCatalogGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessCubeGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessHierarchyGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessMemberGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessRole;
import org.eclipse.daanse.rolap.mapping.model.AggregationColumnName;
import org.eclipse.daanse.rolap.mapping.model.AggregationLevel;
import org.eclipse.daanse.rolap.mapping.model.AggregationMeasure;
import org.eclipse.daanse.rolap.mapping.model.AggregationName;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.CatalogAccess;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.CubeAccess;
import org.eclipse.daanse.rolap.mapping.model.DatabaseSchema;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.HideMemberIf;
import org.eclipse.daanse.rolap.mapping.model.HierarchyAccess;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.MemberAccess;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.PhysicalTable;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

public class AggregationOnInvalidRoleTestModifierEmf implements CatalogMappingSupplier {

    protected final Catalog catalog;

    // mondrian2225_customer table columns
    protected PhysicalColumn customerIdMondrian2225Customer;
    protected PhysicalColumn customerNameMondrian2225Customer;
    protected PhysicalTable mondrian2225Customer;

    // mondrian2225_fact table columns
    protected PhysicalColumn productIdMondrian2225Fact;
    protected PhysicalColumn customerIdMondrian2225Fact;
    protected PhysicalColumn factMondrian2225Fact;
    protected PhysicalTable mondrian2225Fact;

    // mondrian2225_dim table columns
    protected PhysicalColumn productIdMondrian2225Dim;
    protected PhysicalColumn productCodeMondrian2225Dim;
    protected PhysicalTable mondrian2225Dim;

    // mondrian2225_agg table columns
    protected PhysicalColumn dimCodeMondrian2225Agg;
    protected PhysicalColumn factMeasureMondrian2225Agg;
    protected PhysicalColumn factCountMondrian2225Agg;
    protected PhysicalTable mondrian2225Agg;

    // Dimensions
    protected StandardDimension customerDimension;
    protected StandardDimension productCodeDimension;

    // Hierarchies and levels
    protected ExplicitHierarchy customerHierarchy;
    protected Level firstNameLevel;

    // Measures
    protected SumMeasure measureMeasure;

    // Cube
    protected PhysicalCube mondrian2225Cube;

    public AggregationOnInvalidRoleTestModifierEmf(Catalog catalogMapping) {
        this.catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) catalogMapping);
        createTables();
        createDimensions();
        createMeasures();
        createCube();
        createAccessRoles();
    }

    /*
      + "<Cube name=\"mondrian2225\" visible=\"true\" cache=\"true\" enabled=\"true\">"
        + "  <Table name=\"mondrian2225_fact\">"
        + "    <AggName name=\"mondrian2225_agg\" ignorecase=\"true\">"
        + "      <AggFactCount column=\"fact_count\"/>"
        + "      <AggMeasure column=\"fact_Measure\" name=\"[Measures].[Measure]\"/>"
        + "      <AggLevel column=\"dim_code\" name=\"[Product Code].[Code]\" collapsed=\"true\"/>"
        + "    </AggName>"
        + "  </Table>"
        + "  <Dimension type=\"StandardDimension\" visible=\"true\" foreignKey=\"customer_id\" highCardinality=\"false\" name=\"Customer\">"
        + "    <Hierarchy name=\"Customer\" visible=\"true\" hasAll=\"true\" primaryKey=\"customer_id\">"
        + "      <Table name=\"mondrian2225_customer\"/>"
        + "        <Level name=\"First Name\" visible=\"true\" column=\"customer_name\" type=\"String\" uniqueMembers=\"false\" levelType=\"Regular\" hideMemberIf=\"Never\"/>"
        + "    </Hierarchy>"
        + "  </Dimension>"
        + "  <Dimension type=\"StandardDimension\" visible=\"true\" foreignKey=\"product_ID\" highCardinality=\"false\" name=\"Product Code\">"
        + "    <Hierarchy name=\"Product Code\" visible=\"true\" hasAll=\"true\" primaryKey=\"product_id\">"
        + "      <Table name=\"mondrian2225_dim\"/>"
        + "      <Level name=\"Code\" visible=\"true\" column=\"product_code\" type=\"String\" uniqueMembers=\"false\" levelType=\"Regular\" hideMemberIf=\"Never\"/>"
        + "    </Hierarchy>"
        + "  </Dimension>"
        + "  <Measure name=\"Measure\" column=\"fact\" aggregator=\"sum\" visible=\"true\"/>"
        + "</Cube>";
     */

    protected void createTables() {
        // Create mondrian2225_customer table columns
        customerIdMondrian2225Customer = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        customerIdMondrian2225Customer.setName("customer_id");
        customerIdMondrian2225Customer.setType(ColumnType.INTEGER);

        customerNameMondrian2225Customer = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        customerNameMondrian2225Customer.setName("customer_name");
        customerNameMondrian2225Customer.setType(ColumnType.VARCHAR);
        customerNameMondrian2225Customer.setCharOctetLength(45);

        mondrian2225Customer = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        mondrian2225Customer.setName("mondrian2225_customer");
        mondrian2225Customer.getColumns().add(customerIdMondrian2225Customer);
        mondrian2225Customer.getColumns().add(customerNameMondrian2225Customer);

        // Create mondrian2225_fact table columns
        productIdMondrian2225Fact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        productIdMondrian2225Fact.setName("product_ID");
        productIdMondrian2225Fact.setType(ColumnType.INTEGER);

        customerIdMondrian2225Fact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        customerIdMondrian2225Fact.setName("customer_id");
        customerIdMondrian2225Fact.setType(ColumnType.INTEGER);

        factMondrian2225Fact = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factMondrian2225Fact.setName("fact");
        factMondrian2225Fact.setType(ColumnType.INTEGER);

        mondrian2225Fact = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        mondrian2225Fact.setName("mondrian2225_fact");
        mondrian2225Fact.getColumns().add(productIdMondrian2225Fact);
        mondrian2225Fact.getColumns().add(customerIdMondrian2225Fact);
        mondrian2225Fact.getColumns().add(factMondrian2225Fact);

        // Create mondrian2225_dim table columns
        productIdMondrian2225Dim = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        productIdMondrian2225Dim.setName("product_id");
        productIdMondrian2225Dim.setType(ColumnType.INTEGER);

        productCodeMondrian2225Dim = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        productCodeMondrian2225Dim.setName("product_code");
        productCodeMondrian2225Dim.setType(ColumnType.VARCHAR);
        productCodeMondrian2225Dim.setCharOctetLength(45);

        mondrian2225Dim = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        mondrian2225Dim.setName("mondrian2225_dim");
        mondrian2225Dim.getColumns().add(productIdMondrian2225Dim);
        mondrian2225Dim.getColumns().add(productCodeMondrian2225Dim);

        // Create mondrian2225_agg table columns
        dimCodeMondrian2225Agg = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        dimCodeMondrian2225Agg.setName("dim_code");
        dimCodeMondrian2225Agg.setType(ColumnType.VARCHAR);
        dimCodeMondrian2225Agg.setCharOctetLength(45);
        dimCodeMondrian2225Agg.setNullable(true);

        factMeasureMondrian2225Agg = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factMeasureMondrian2225Agg.setName("fact_measure");
        factMeasureMondrian2225Agg.setType(ColumnType.DECIMAL);
        factMeasureMondrian2225Agg.setColumnSize(10);
        factMeasureMondrian2225Agg.setDecimalDigits(2);

        factCountMondrian2225Agg = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountMondrian2225Agg.setName("fact_count");
        factCountMondrian2225Agg.setType(ColumnType.INTEGER);

        mondrian2225Agg = RolapMappingFactory.eINSTANCE.createPhysicalTable();
        mondrian2225Agg.setName("mondrian2225_agg");
        mondrian2225Agg.getColumns().add(dimCodeMondrian2225Agg);
        mondrian2225Agg.getColumns().add(factMeasureMondrian2225Agg);
        mondrian2225Agg.getColumns().add(factCountMondrian2225Agg);

        // Add tables to database schema
        if (catalog.getDbschemas().size() > 0) {
            DatabaseSchema dbSchema = catalog.getDbschemas().get(0);
            dbSchema.getTables().add(mondrian2225Customer);
            dbSchema.getTables().add(mondrian2225Fact);
            dbSchema.getTables().add(mondrian2225Dim);
            dbSchema.getTables().add(mondrian2225Agg);
        }
    }

    protected void createDimensions() {
        // Create Customer dimension
        customerDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        customerDimension.setName("Customer");

        // Create Customer hierarchy
        TableQuery customerQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        customerQuery.setTable(mondrian2225Customer);

        firstNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        firstNameLevel.setName("First Name");
        firstNameLevel.setVisible(true);
        firstNameLevel.setColumn(customerNameMondrian2225Customer);
        firstNameLevel.setColumnType(ColumnInternalDataType.STRING);
        firstNameLevel.setUniqueMembers(false);
        firstNameLevel.setType(LevelDefinition.REGULAR);
        firstNameLevel.setHideMemberIf(HideMemberIf.NEVER);

        customerHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        customerHierarchy.setName("Customer");
        customerHierarchy.setVisible(true);
        customerHierarchy.setHasAll(true);
        customerHierarchy.setPrimaryKey(customerIdMondrian2225Customer);
        customerHierarchy.setQuery(customerQuery);
        customerHierarchy.getLevels().add(firstNameLevel);

        customerDimension.getHierarchies().add(customerHierarchy);

        // Create Product Code dimension
        productCodeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        productCodeDimension.setName("Product Code");

        // Create Product Code hierarchy
        TableQuery productCodeQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productCodeQuery.setTable(mondrian2225Dim);

        Level codeLevel = RolapMappingFactory.eINSTANCE.createLevel();
        codeLevel.setName("Code");
        codeLevel.setVisible(true);
        codeLevel.setColumn(productCodeMondrian2225Dim);
        codeLevel.setColumnType(ColumnInternalDataType.STRING);
        codeLevel.setUniqueMembers(false);
        codeLevel.setType(LevelDefinition.REGULAR);
        codeLevel.setHideMemberIf(HideMemberIf.NEVER);

        ExplicitHierarchy productCodeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        productCodeHierarchy.setName("Product Code");
        productCodeHierarchy.setVisible(true);
        productCodeHierarchy.setHasAll(true);
        productCodeHierarchy.setPrimaryKey(productIdMondrian2225Dim);
        productCodeHierarchy.setQuery(productCodeQuery);
        productCodeHierarchy.getLevels().add(codeLevel);

        productCodeDimension.getHierarchies().add(productCodeHierarchy);
    }

    protected void createMeasures() {
        measureMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        measureMeasure.setName("Measure");
        measureMeasure.setColumn(factMondrian2225Fact);
        measureMeasure.setVisible(true);
    }

    protected void createCube() {
        // Create table query with aggregation
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable(mondrian2225Fact);

        // Create aggregation
        AggregationName aggName = RolapMappingFactory.eINSTANCE.createAggregationName();
        aggName.setName(mondrian2225Agg);
        aggName.setIgnorecase(true);

        // Aggregation fact count
        AggregationColumnName aggFactCount = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCountMondrian2225Agg);
        aggName.setAggregationFactCount(aggFactCount);

        // Aggregation measure
        AggregationMeasure aggMeasure = RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setColumn(factMeasureMondrian2225Agg);
        aggMeasure.setName("[Measures].[Measure]");
        aggName.getAggregationMeasures().add(aggMeasure);

        // Aggregation level
        AggregationLevel aggLevel = RolapMappingFactory.eINSTANCE.createAggregationLevel();
        aggLevel.setColumn(dimCodeMondrian2225Agg);
        aggLevel.setName("[Product Code].[Product Code].[Code]");
        aggLevel.setCollapsed(true);
        aggName.getAggregationLevels().add(aggLevel);

        tableQuery.getAggregationTables().add(aggName);

        // Create dimension connectors
        DimensionConnector customerConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        customerConnector.setDimension(customerDimension);
        customerConnector.setOverrideDimensionName("Customer");
        customerConnector.setVisible(true);
        customerConnector.setForeignKey(customerIdMondrian2225Fact);

        DimensionConnector productCodeConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        productCodeConnector.setDimension(productCodeDimension);
        productCodeConnector.setOverrideDimensionName("Product Code");
        productCodeConnector.setVisible(true);
        productCodeConnector.setForeignKey(productIdMondrian2225Fact);

        // Create measure group
        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(measureMeasure);

        // Create cube
        mondrian2225Cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        mondrian2225Cube.setName("mondrian2225");
        mondrian2225Cube.setVisible(true);
        mondrian2225Cube.setCache(true);
        mondrian2225Cube.setEnabled(true);
        mondrian2225Cube.setQuery(tableQuery);
        mondrian2225Cube.getDimensionConnectors().add(customerConnector);
        mondrian2225Cube.getDimensionConnectors().add(productCodeConnector);
        mondrian2225Cube.getMeasureGroups().add(measureGroup);

        // Add cube to catalog
        catalog.getCubes().add(mondrian2225Cube);
    }

    /*
            + "<Role name=\"Test\">"
        + "  <SchemaGrant access=\"none\">"
        + "    <CubeGrant cube=\"mondrian2225\" access=\"all\">"
        + "      <HierarchyGrant hierarchy=\"[Customer.Customer]\" topLevel=\"[Customer.Customer].[First Name]\" access=\"custom\">"
        + "        <MemberGrant member=\"[Customer.Customer].[NonExistingName]\" access=\"all\"/>"
        + "      </HierarchyGrant>"
        + "    </CubeGrant>"
        + "  </SchemaGrant>"
        + "</Role>";

     */

    protected void createAccessRoles() {
        // Create member grant
        AccessMemberGrant memberGrant = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant.setMember("[Customer].[Customer].[NonExistingName]");
        memberGrant.setMemberAccess(MemberAccess.ALL);

        // Create hierarchy grant
        AccessHierarchyGrant hierarchyGrant = RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
        hierarchyGrant.setHierarchy(customerHierarchy);
        hierarchyGrant.setTopLevel(firstNameLevel);
        hierarchyGrant.setHierarchyAccess(HierarchyAccess.CUSTOM);
        hierarchyGrant.getMemberGrants().add(memberGrant);

        // Create cube grant
        AccessCubeGrant cubeGrant = RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
        cubeGrant.setCube(mondrian2225Cube);
        cubeGrant.setCubeAccess(CubeAccess.ALL);
        cubeGrant.getHierarchyGrants().add(hierarchyGrant);

        // Create catalog grant
        AccessCatalogGrant catalogGrant = RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        catalogGrant.setCatalogAccess(CatalogAccess.NONE);
        catalogGrant.getCubeGrants().add(cubeGrant);

        // Create role
        AccessRole role = RolapMappingFactory.eINSTANCE.createAccessRole();
        role.setName("Test");
        role.getAccessCatalogGrants().add(catalogGrant);

        // Add role to catalog
        catalog.getAccessRoles().add(role);
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
