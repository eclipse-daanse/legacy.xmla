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
import org.eclipse.daanse.rolap.mapping.api.model.SchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCube;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessHierarchy;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessMember;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessSchema;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.HideMemberIfType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCubeGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessHierarchyGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessMemberGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessRoleMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessSchemaGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationColumnNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationLevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
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

    private static LevelMappingImpl firstNameLevel = LevelMappingImpl.builder()
            .withName("First Name")
            .withVisible(true)
            .withColumn("customer_name").withType(DataType.STRING)
            .withUniqueMembers(false)
            .withLevelType(LevelType.REGULAR)
            .withHideMemberIfType(HideMemberIfType.NEVER)
            .build();

    private static HierarchyMappingImpl customerHierarchy = HierarchyMappingImpl.builder()
            .withName("Customer")
            .withVisible(true)
            .withHasAll(true)
            .withPrimaryKey("customer_id")
            .withQuery(TableQueryMappingImpl.builder().withName("mondrian2225_customer").build())
            .withLevels(List.of(
            	firstNameLevel
            ))
            .build();

    private static PhysicalCubeMappingImpl mondrian2225 = PhysicalCubeMappingImpl.builder()
    .withName("mondrian2225")
    .withVisible(true)
    .withCache(true)
    .withEnabled(true)
    .withQuery(TableQueryMappingImpl.builder().withName("mondrian2225_fact").withAggregationTables(List.of(
        AggregationNameMappingImpl.builder()
            .withName("mondrian2225_agg")
            .withIgnorecase(true)
            .withAggregationFactCount(AggregationColumnNameMappingImpl
                .builder()
                .withColumn("fact_count")
                .build())
            .withAggregationMeasures(List.of(
            	AggregationMeasureMappingImpl.builder()
                    .withColumn("fact_Measure")
                    .withName("[Measures].[Measure]")
                    .build()
            ))
            .withAggregationLevels(List.of(
            	AggregationLevelMappingImpl.builder()
                    .withColumn("dim_code")
                    .withName("[Product Code].[Code]")
                    .withCollapsed(true)
                    .build()
            ))
            .build()
    )).build())
    .withDimensionConnectors(List.of(
    	DimensionConnectorMappingImpl.builder()
            .withVisible(true)
            .withForeignKey("customer_id")
            .withOverrideDimensionName("Customer")
            .withDimension(StandardDimensionMappingImpl.builder()
            	.withName("Customer")
            	.withHierarchies(List.of(
            		customerHierarchy
            )).build())
            .build(),
        DimensionConnectorMappingImpl.builder()
            .withVisible(true)
            .withForeignKey("product_ID")
            .withOverrideDimensionName("Product Code")
            .withDimension(StandardDimensionMappingImpl.builder()
            	.withName("Product Code")
            	.withHierarchies(List.of(
                HierarchyMappingImpl.builder()
                    .withName("Product Code")
                    .withVisible(true)
                    .withHasAll(true)
                    .withPrimaryKey("product_id")
                    .withQuery(TableQueryMappingImpl.builder().withName("mondrian2225_dim").build())
                    .withLevels(List.of(
                        LevelMappingImpl.builder()
                            .withName("Code")
                            .withVisible(true)
                            .withColumn("product_code")
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
            .withColumn("fact")
            .withAggregatorType(MeasureAggregatorType.SUM)
            .withVisible(true)
            .build()
    )).build()))
    .build();

    @Override
    protected List<? extends CubeMapping> schemaCubes(SchemaMapping schema) {
        List<CubeMapping> result = new ArrayList<>();
        result.addAll(super.schemaCubes(schema));
        result.add(mondrian2225);
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

    protected List<? extends AccessRoleMapping> schemaAccessRoles(SchemaMapping schema) {
        List<AccessRoleMapping> result = new ArrayList<>();
        result.addAll(super.schemaAccessRoles(schema));
        result.add(AccessRoleMappingImpl.builder()
            .withName("Test")
            .withAccessSchemaGrants(List.of(
            	AccessSchemaGrantMappingImpl.builder()
                    .withAccess(AccessSchema.NONE)
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
