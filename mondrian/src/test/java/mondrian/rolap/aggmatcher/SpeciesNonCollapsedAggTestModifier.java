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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DatabaseSchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCatalog;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCube;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessHierarchy;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessMember;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.RollupPolicyType;
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
import org.eclipse.daanse.rolap.mapping.pojo.CatalogMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ColumnMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DatabaseSchemaMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;

public class SpeciesNonCollapsedAggTestModifier extends PojoMappingModifier {

	//## TableName: DIM_SPECIES
	//## ColumnNames: FAMILY_ID,GENUS_ID,SPECIES_ID,SPECIES_NAME
	//## ColumnTypes: INTEGER,INTEGER,INTEGER,VARCHAR(30)
	ColumnMappingImpl familyIdDimSpecies = ColumnMappingImpl.builder().withName("FAMILY_ID").withType("INTEGER").build();
	ColumnMappingImpl genisIdDimSpecies = ColumnMappingImpl.builder().withName("GENUS_ID").withType("INTEGER").build();
	ColumnMappingImpl speciesIdDimSpecies = ColumnMappingImpl.builder().withName("SPECIES_ID").withType("INTEGER").build();
	ColumnMappingImpl speciesNameDimSpecies = ColumnMappingImpl.builder().withName("SPECIES_NAME").withType("VARCHAR").withCharOctetLength(30).build();
    PhysicalTableMappingImpl dimSpecies = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("DIM_SPECIES")
            .withColumns(List.of(familyIdDimSpecies, genisIdDimSpecies, speciesIdDimSpecies, speciesNameDimSpecies))).build();
    //## TableName: DIM_FAMILY
    //## ColumnNames: FAMILY_ID,FAMILY_NAME
    //## ColumnTypes: INTEGER,VARCHAR(30)
    ColumnMappingImpl familyIdDimFamily = ColumnMappingImpl.builder().withName("FAMILY_ID").withType("INTEGER").build();
    ColumnMappingImpl familyNameDimFamily = ColumnMappingImpl.builder().withName("FAMILY_NAME").withType("VARCHAR").withCharOctetLength(30).build();
    PhysicalTableMappingImpl dimFamily = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("DIM_FAMILY")
            .withColumns(List.of(familyIdDimFamily, familyNameDimFamily))).build();
    //## TableName: DIM_GENUS
    //## ColumnNames: FAMILY_ID,GENUS_ID,GENUS_NAME
    //## ColumnTypes: INTEGER,INTEGER,VARCHAR(30)
    ColumnMappingImpl familyIdDimGenus = ColumnMappingImpl.builder().withName("FAMILY_ID").withType("INTEGER").build();
    ColumnMappingImpl genusIdDimGenus = ColumnMappingImpl.builder().withName("GENUS_ID").withType("INTEGER").build();
    ColumnMappingImpl genusNameDimGenus = ColumnMappingImpl.builder().withName("GENUS_NAME").withType("VARCHAR").withCharOctetLength(30).build();
    PhysicalTableMappingImpl dimGenus = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("DIM_GENUS")
            .withColumns(List.of(familyIdDimGenus, genusIdDimGenus, genusNameDimGenus))).build();
    //## TableName: species_mart
    //## ColumnNames: SPECIES_ID,POPULATION
    //## ColumnTypes: INTEGER,INTEGER
    ColumnMappingImpl speciesIdSpeciesMart = ColumnMappingImpl.builder().withName("SPECIES_ID").withType("INTEGER").build();
    ColumnMappingImpl populationSpeciesMart = ColumnMappingImpl.builder().withName("POPULATION").withType("INTEGER").build();
    PhysicalTableMappingImpl speciesMart = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("species_mart")
            .withColumns(List.of(speciesIdSpeciesMart, populationSpeciesMart))).build();

    //## TableName: AGG_SPECIES_MART
    //## ColumnNames: GEN_ID,POPULATION,FACT_COUNT
    //## ColumnTypes: INTEGER,INTEGER,INTEGER
    ColumnMappingImpl genIdAggSpeciesMart = ColumnMappingImpl.builder().withName("GEN_ID").withType("INTEGER").build();
    ColumnMappingImpl populationAggSpeciesMart = ColumnMappingImpl.builder().withName("POPULATION").withType("INTEGER").build();
    ColumnMappingImpl factCountAggSpeciesMart = ColumnMappingImpl.builder().withName("FACT_COUNT").withType("INTEGER").build();
    PhysicalTableMappingImpl aggSpeciesMart = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("AGG_SPECIES_MART")
            .withColumns(List.of(genIdAggSpeciesMart, populationAggSpeciesMart, factCountAggSpeciesMart))).build();

    public SpeciesNonCollapsedAggTestModifier(CatalogMapping catalog) {
        super(catalog);
    }

    /*
            "<?xml version='1.0'?>\n"
        + "<Schema name='Testmart'>\n"
        + "  <Dimension name='Animal'>\n"
        + "    <Hierarchy name='Animals' hasAll='true' allMemberName='All Animals' primaryKey='SPECIES_ID' primaryKeyTable='DIM_SPECIES'>\n"
        + "      <Join leftKey='GENUS_ID' rightAlias='DIM_GENUS' rightKey='GENUS_ID'>\n"
        + "        <Table name='DIM_SPECIES' />\n"
        + "        <Join leftKey='FAMILY_ID' rightKey='FAMILY_ID'>\n"
        + "          <Table name='DIM_GENUS' />\n"
        + "          <Table name='DIM_FAMILY' />\n"
        + "        </Join>\n"
        + "      </Join>\n"
        + "      <Level name='Family' table='DIM_FAMILY' column='FAMILY_ID' nameColumn='FAMILY_NAME' uniqueMembers='true' type='Numeric' approxRowCount='2' />\n"
        + "      <Level name='Genus' table='DIM_GENUS' column='GENUS_ID' nameColumn='GENUS_NAME' uniqueMembers='true' type='Numeric' approxRowCount='4' />\n"
        + "      <Level name='Species' table='DIM_SPECIES' column='SPECIES_ID' nameColumn='SPECIES_NAME' uniqueMembers='true' type='Numeric' approxRowCount='8' />\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>\n"
        + "  <Cube name='Test' defaultMeasure='Population'>\n"
        + "    <Table name='species_mart'>\n" // See MONDRIAN-2237 - Table name needs to be lower case for embedded Windows MySQL integration testing
        + "      <AggName name='AGG_SPECIES_MART'>\n"
        + "        <AggFactCount column='FACT_COUNT' />\n"
        + "        <AggMeasure name='Measures.[Population]' column='POPULATION' />\n"
        + "        <AggLevel name='[Animal.Animals].[Genus]' column='GEN_ID' collapsed='false' />\n"
        + "      </AggName>\n"
        + "    </Table>\n"
        + "    <DimensionUsage name='Animal' source='Animal' foreignKey='SPECIES_ID'/>\n"
        + "    <Measure name='Population' column='POPULATION' aggregator='sum'/>\n"
        + "  </Cube>\n"
        + "  <Role name='Test role'>\n"
        + "    <SchemaGrant access='none'>\n"
        + "      <CubeGrant cube='Test' access='all'>\n"
        + "        <HierarchyGrant hierarchy='[Animal.Animals]' access='custom' rollupPolicy='partial'>\n"
        + "          <MemberGrant member='[Animal.Animals].[Family].[Loricariidae]' access='all'/>\n"
        + "          <MemberGrant member='[Animal.Animals].[Family].[Cichlidae]' access='all'/>\n"
        + "          <MemberGrant member='[Animal.Animals].[Family].[Cyprinidae]' access='none'/>\n"
        + "        </HierarchyGrant>\n"
        + "      </CubeGrant>\n"
        + "    </SchemaGrant>\n"
        + "  </Role>\n"
        + "</Schema>";
     */

    @Override
    protected List<? extends TableMapping> databaseSchemaTables(DatabaseSchemaMapping databaseSchema) {
        List<TableMapping> result = new ArrayList<TableMapping>();
        result.addAll(super.databaseSchemaTables(databaseSchema));
        result.addAll(List.of(dimSpecies, dimFamily, dimGenus, speciesMart, aggSpeciesMart));
        return result;
    }

    @Override
    protected CatalogMapping modifyCatalog(CatalogMapping schemaMappingOriginal) {
    	HierarchyMappingImpl animalsHierarchy;
        StandardDimensionMappingImpl animal = StandardDimensionMappingImpl.builder()
        .withName("Animal")
        .withHierarchies(List.of(
        	animalsHierarchy = HierarchyMappingImpl.builder()
                .withName("Animals")
                .withHasAll(true)
                .withAllMemberName("All Animals")
                .withPrimaryKey(speciesIdDimSpecies)
                .withPrimaryKeyTable(dimSpecies)
                .withQuery(JoinQueryMappingImpl.builder()
                		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(genisIdDimSpecies)
                				.withQuery(TableQueryMappingImpl.builder().withTable(dimSpecies).build())
                				.build())
                		.withRight(JoinedQueryElementMappingImpl.builder().withAlias("DIM_GENUS").withKey(genusIdDimGenus)
                                .withQuery(JoinQueryMappingImpl.builder()
                                		.withLeft(JoinedQueryElementMappingImpl.builder().withKey(familyIdDimGenus)
                                				.withQuery(TableQueryMappingImpl.builder().withTable(dimGenus).build())
                                				.build())
                                		.withRight(JoinedQueryElementMappingImpl.builder().withKey(familyIdDimFamily)
                                				.withQuery(TableQueryMappingImpl.builder().withTable(dimFamily).build())
                                				.build())
                                		.build())

                				.build())
                		.build())

                .withLevels(List.of(
                    LevelMappingImpl.builder()
                        .withName("Family")
                        .withTable(dimFamily)
                        .withColumn(familyIdDimFamily)
                        .withNameColumn(familyNameDimFamily)
                        .withUniqueMembers(true)
                        .withType(DataType.NUMERIC)
                        .withApproxRowCount("2")
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Genus")
                        .withTable(dimGenus)
                        .withColumn(genusIdDimGenus)
                        .withNameColumn(genusNameDimGenus)
                        .withUniqueMembers(true)
                        .withType(DataType.NUMERIC)
                        .withApproxRowCount("4")
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Species")
                        .withTable(dimSpecies)
                        .withColumn(speciesIdDimSpecies)
                        .withNameColumn(speciesNameDimSpecies)
                        .withUniqueMembers(true)
                        .withType(DataType.NUMERIC)
                        .withApproxRowCount("8")
                        .build()
                ))
                .build()
        ))
        .build();

        MeasureMappingImpl populationMeasure = MeasureMappingImpl.builder()
        .withName("Population")
        .withColumn(populationSpeciesMart)
        .withAggregatorType(MeasureAggregatorType.SUM)
        .build();
        PhysicalCubeMappingImpl testCube;

        return CatalogMappingImpl.builder()
        .withName("Testmart")
        .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas(schemaMappingOriginal))
        .withCubes(List.of(
        	testCube = PhysicalCubeMappingImpl.builder()
                .withName("Test")
                .withDefaultMeasure(populationMeasure)
                .withQuery(TableQueryMappingImpl.builder().withTable(speciesMart).withAggregationTables(
                    List.of(
                        AggregationNameMappingImpl.builder()
                            .withName(aggSpeciesMart)
                            .withAggregationFactCount(AggregationColumnNameMappingImpl.builder()
                                .withColumn(factCountAggSpeciesMart)
                                .build())
                            .withAggregationMeasures(List.of(
                            	AggregationMeasureMappingImpl.builder()
                                    .withName("Measures.[Population]")
                                    .withColumn(populationAggSpeciesMart)
                                    .build()
                            ))
                            .withAggregationLevels(List.of(
                                AggregationLevelMappingImpl.builder()
                                    .withName("[Animal.Animals].[Genus]")
                                    .withColumn(genIdAggSpeciesMart)
                                    .withCollapsed(false)
                                    .build()
                            ))
                            .build()
                    )).build())
                .withDimensionConnectors(List.of(
                    DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Animal")
                        .withDimension(animal)
                        .withForeignKey(speciesIdSpeciesMart)
                        .build()
                ))
                .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(populationMeasure)).build()))
                .build()
        ))
        .withAccessRoles(List.of(
            AccessRoleMappingImpl.builder()
                .withName("Test role")
                .withAccessCatalogGrants(List.of(
                	AccessCatalogGrantMappingImpl.builder()
                        .withAccess(AccessCatalog.NONE)
                        .withCubeGrant(List.of(
                        	AccessCubeGrantMappingImpl.builder()
                        		.withCube(testCube)
                                .withAccess(AccessCube.ALL)
                                .withHierarchyGrants(List.of(
                                	AccessHierarchyGrantMappingImpl.builder()
                                        .withHierarchy(animalsHierarchy)
                                        .withAccess(AccessHierarchy.CUSTOM)
                                        .withRollupPolicyType(RollupPolicyType.PARTIAL)
                                        .withMemberGrants(List.of(
                                        	AccessMemberGrantMappingImpl.builder()
                                                .withMember("[Animal.Animals].[Family].[Loricariidae]")
                                                .withAccess(AccessMember.ALL)
                                                .build(),
                                            AccessMemberGrantMappingImpl.builder()
                                                .withMember("[Animal.Animals].[Family].[Cichlidae]")
                                                .withAccess(AccessMember.ALL)
                                                .build(),
                                            AccessMemberGrantMappingImpl.builder()
                                                .withMember("[Animal.Animals].[Family].[Cyprinidae]")
                                                .withAccess(AccessMember.NONE)
                                                .build()
                                        ))
                                        .build()
                                ))
                                .build()
                        ))
                        .build()
                ))
                .build()
        ))
        .build();
}
}
