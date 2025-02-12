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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.rolap.mapping.api.model.AccessRoleMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DatabaseSchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCatalog;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCube;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessHierarchy;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessMember;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.HideMemberIfType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCatalogGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCubeGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessHierarchyGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessMemberGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessRoleMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationColumnNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
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

public class AggregationOnInvalidRoleTestModifier extends PojoMappingModifier {

    public AggregationOnInvalidRoleTestModifier(CatalogMapping c) {
        super(c);
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

    //## ColumnNames: customer_id,customer_name
    //## ColumnTypes: INTEGER,VARCHAR(45):null
    private static ColumnMappingImpl customerIdMondrian2225Customer = ColumnMappingImpl.builder().withName("customer_id").withType("INTEGER").build();
    private static ColumnMappingImpl customerNameMondrian2225Customer = ColumnMappingImpl.builder().withName("customer_name").withType("VARCHAR").withColumnSize(45).build();
    private static PhysicalTableMappingImpl mondrian2225Customer = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("mondrian2225_customer")
            .withColumns(List.of(customerIdMondrian2225Customer, customerNameMondrian2225Customer))).build();

    //## ColumnNames: product_ID,customer_id,fact
    //## ColumnTypes: INTEGER,INTEGER,INTEGER
    private static ColumnMappingImpl productIdMondrian2225Fact = ColumnMappingImpl.builder().withName("product_ID").withType("INTEGER").build();
    private static ColumnMappingImpl customerIdMondrian2225Fact = ColumnMappingImpl.builder().withName("customer_id").withType("INTEGER").build();
    private static ColumnMappingImpl factMondrian2225Fact = ColumnMappingImpl.builder().withName("fact").withType("INTEGER").build();
    private static PhysicalTableMappingImpl mondrian2225Fact = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("mondrian2225_fact")
            .withColumns(List.of(productIdMondrian2225Fact, customerIdMondrian2225Fact, factMondrian2225Fact))).build();

    //## TableName: mondrian2225_dim
    //## ColumnNames: product_id,product_code,product_sub_code
    //## ColumnTypes: INTEGER,VARCHAR(45):null,VARCHAR(45):null
    private static ColumnMappingImpl productIdMondrian2225Dim = ColumnMappingImpl.builder().withName("product_id").withType("INTEGER").build();
    private static ColumnMappingImpl productCodeMondrian2225Dim = ColumnMappingImpl.builder().withName("product_code").withType("VARCHAR").withColumnSize(45).build();
    private static PhysicalTableMappingImpl mondrian2225Dim = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("mondrian2225_dim")
            .withColumns(List.of(productIdMondrian2225Fact, customerIdMondrian2225Fact, factMondrian2225Fact))).build();

    //## TableName: mondrian2225_agg
    //## ColumnNames: dim_code,fact_measure,fact_count
    //## ColumnTypes: VARCHAR(45):null,DECIMAL(10,2),INTEGER
    private static ColumnMappingImpl dimCodeMondrian2225Agg = ColumnMappingImpl.builder().withName("dim_code").withType("VARCHAR").withColumnSize(45).withNullable(true).build();
    private static ColumnMappingImpl factMeasureMondrian2225Agg = ColumnMappingImpl.builder().withName("fact_measure").withType("DECIMAL").withColumnSize(10).withDecimalDigits(2).build();
    private static ColumnMappingImpl factCountMondrian2225Agg = ColumnMappingImpl.builder().withName("fact_count").withType("INTEGER").build();
    private static PhysicalTableMappingImpl mondrian2225Agg = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("mondrian2225_agg")
            .withColumns(List.of(dimCodeMondrian2225Agg, factMeasureMondrian2225Agg, factCountMondrian2225Agg))).build();

    private static LevelMappingImpl firstNameLevel = LevelMappingImpl.builder()
            .withName("First Name")
            .withVisible(true)
            .withColumn(customerNameMondrian2225Customer).withType(DataType.STRING)
            .withUniqueMembers(false)
            .withLevelType(LevelType.REGULAR)
            .withHideMemberIfType(HideMemberIfType.NEVER)
            .build();

    private static HierarchyMappingImpl customerHierarchy = HierarchyMappingImpl.builder()
            .withName("Customer")
            .withVisible(true)
            .withHasAll(true)
            .withPrimaryKey(customerIdMondrian2225Customer)
            .withQuery(TableQueryMappingImpl.builder().withTable(mondrian2225Customer).build())
            .withLevels(List.of(
            	firstNameLevel
            ))
            .build();

    private static PhysicalCubeMappingImpl mondrian2225 = PhysicalCubeMappingImpl.builder()
    .withName("mondrian2225")
    .withVisible(true)
    .withCache(true)
    .withEnabled(true)
    .withQuery(TableQueryMappingImpl.builder().withTable(mondrian2225Fact).withAggregationTables(List.of(
        AggregationNameMappingImpl.builder()
            .withName(mondrian2225Agg)
            .withIgnorecase(true)
            .withAggregationFactCount(AggregationColumnNameMappingImpl
                .builder()
                .withColumn(factCountMondrian2225Agg)
                .build())
            .withAggregationMeasures(List.of(
            	AggregationMeasureMappingImpl.builder()
                    .withColumn(factMeasureMondrian2225Agg)
                    .withName("[Measures].[Measure]")
                    .build()
            ))
            .withAggregationLevels(List.of(
            	AggregationLevelMappingImpl.builder()
                    .withColumn(dimCodeMondrian2225Agg)
                    .withName("[Product Code].[Code]")
                    .withCollapsed(true)
                    .build()
            ))
            .build()
    )).build())
    .withDimensionConnectors(List.of(
    	DimensionConnectorMappingImpl.builder()
            .withVisible(true)
            .withForeignKey(customerIdMondrian2225Fact)
            .withOverrideDimensionName("Customer")
            .withDimension(StandardDimensionMappingImpl.builder()
            	.withName("Customer")
            	.withHierarchies(List.of(
            		customerHierarchy
            )).build())
            .build(),
        DimensionConnectorMappingImpl.builder()
            .withVisible(true)
            .withForeignKey(productIdMondrian2225Fact)
            .withOverrideDimensionName("Product Code")
            .withDimension(StandardDimensionMappingImpl.builder()
            	.withName("Product Code")
            	.withHierarchies(List.of(
                HierarchyMappingImpl.builder()
                    .withName("Product Code")
                    .withVisible(true)
                    .withHasAll(true)
                    .withPrimaryKey(productIdMondrian2225Dim)
                    .withQuery(TableQueryMappingImpl.builder().withTable(mondrian2225Dim).build())
                    .withLevels(List.of(
                        LevelMappingImpl.builder()
                            .withName("Code")
                            .withVisible(true)
                            .withColumn(productCodeMondrian2225Dim)
                            .withType(DataType.STRING)
                            .withUniqueMembers(false)
                            .withLevelType(LevelType.REGULAR)
                            .withHideMemberIfType(HideMemberIfType.NEVER)
                            .build()
                    ))
                    .build()
            )).build())
            .build()
    ))
    .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
        MeasureMappingImpl.builder()
            .withName("Measure")
            .withColumn(factMondrian2225Fact)
            .withAggregatorType(MeasureAggregatorType.SUM)
            .withVisible(true)
            .build()
    )).build()))
    .build();

    @Override
    protected List<? extends CubeMapping> catalogCubes(CatalogMapping schema) {
        List<CubeMapping> result = new ArrayList<>();
        result.addAll(super.catalogCubes(schema));
        result.add(mondrian2225);
        return result;

    }

    @Override
    protected List<? extends TableMapping> databaseSchemaTables(DatabaseSchemaMapping databaseSchema) {
        List<TableMapping> result = new ArrayList();
        result.addAll(super.databaseSchemaTables(databaseSchema));
        result.addAll(List.of(mondrian2225Agg, mondrian2225Customer, mondrian2225Fact, mondrian2225Dim));
        return result;
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

    @Override
    protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping schema) {
        List<AccessRoleMapping> result = new ArrayList<>();
        result.addAll(super.catalogAccessRoles(schema));
        result.add(AccessRoleMappingImpl.builder()
            .withName("Test")
            .withAccessCatalogGrants(List.of(
            	AccessCatalogGrantMappingImpl.builder()
                    .withAccess(AccessCatalog.NONE)
                    .withCubeGrant(List.of(
                    	AccessCubeGrantMappingImpl.builder()
                            .withCube(mondrian2225)
                            .withAccess(AccessCube.ALL)
                            .withHierarchyGrants(List.of(
                            	AccessHierarchyGrantMappingImpl.builder()
                                    .withHierarchy(customerHierarchy)
                                    .withTopLevel(firstNameLevel)
                                    .withAccess(AccessHierarchy.CUSTOM)
                                    .withMemberGrants(List.of(
                                    	AccessMemberGrantMappingImpl.builder()
                                            .withMember("[Customer.Customer].[NonExistingName]")
                                            .withAccess(AccessMember.ALL)
                                            .build()
                                    ))
                                    .build()
                            ))
                            .build()
                    ))
                    .build()
            ))
            .build());
        return result;

    }
}
