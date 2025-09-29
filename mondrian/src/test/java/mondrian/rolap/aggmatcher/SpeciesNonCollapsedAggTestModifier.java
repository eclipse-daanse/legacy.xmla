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
import org.eclipse.daanse.rolap.mapping.model.HierarchyAccess;
import org.eclipse.daanse.rolap.mapping.model.JoinQuery;
import org.eclipse.daanse.rolap.mapping.model.JoinedQueryElement;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.MemberAccess;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.PhysicalTable;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.RollupPolicy;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

/*
public class SpeciesNonCollapsedAggTestModifier extends PojoMappingModifier {

	//## TableName: DIM_SPECIES
	//## ColumnNames: FAMILY_ID,GENUS_ID,SPECIES_ID,SPECIES_NAME
	//## ColumnTypes: INTEGER,INTEGER,INTEGER,VARCHAR(30)
	PhysicalColumnMappingImpl familyIdDimSpecies = PhysicalColumnMappingImpl.builder().withName("FAMILY_ID").withDataType(ColumnDataType.INTEGER).build();
	PhysicalColumnMappingImpl genisIdDimSpecies = PhysicalColumnMappingImpl.builder().withName("GENUS_ID").withDataType(ColumnDataType.INTEGER).build();
	PhysicalColumnMappingImpl speciesIdDimSpecies = PhysicalColumnMappingImpl.builder().withName("SPECIES_ID").withDataType(ColumnDataType.INTEGER).build();
	PhysicalColumnMappingImpl speciesNameDimSpecies = PhysicalColumnMappingImpl.builder().withName("SPECIES_NAME").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
    PhysicalTableMappingImpl dimSpecies = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("DIM_SPECIES")
            .withColumns(List.of(familyIdDimSpecies, genisIdDimSpecies, speciesIdDimSpecies, speciesNameDimSpecies))).build();
    //## TableName: DIM_FAMILY
    //## ColumnNames: FAMILY_ID,FAMILY_NAME
    //## ColumnTypes: INTEGER,VARCHAR(30)
    PhysicalColumnMappingImpl familyIdDimFamily = PhysicalColumnMappingImpl.builder().withName("FAMILY_ID").withDataType(ColumnDataType.INTEGER).build();
    PhysicalColumnMappingImpl familyNameDimFamily = PhysicalColumnMappingImpl.builder().withName("FAMILY_NAME").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
    PhysicalTableMappingImpl dimFamily = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("DIM_FAMILY")
            .withColumns(List.of(familyIdDimFamily, familyNameDimFamily))).build();
    //## TableName: DIM_GENUS
    //## ColumnNames: FAMILY_ID,GENUS_ID,GENUS_NAME
    //## ColumnTypes: INTEGER,INTEGER,VARCHAR(30)
    PhysicalColumnMappingImpl familyIdDimGenus = PhysicalColumnMappingImpl.builder().withName("FAMILY_ID").withDataType(ColumnDataType.INTEGER).build();
    PhysicalColumnMappingImpl genusIdDimGenus = PhysicalColumnMappingImpl.builder().withName("GENUS_ID").withDataType(ColumnDataType.INTEGER).build();
    PhysicalColumnMappingImpl genusNameDimGenus = PhysicalColumnMappingImpl.builder().withName("GENUS_NAME").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(30).build();
    PhysicalTableMappingImpl dimGenus = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("DIM_GENUS")
            .withColumns(List.of(familyIdDimGenus, genusIdDimGenus, genusNameDimGenus))).build();
    //## TableName: species_mart
    //## ColumnNames: SPECIES_ID,POPULATION
    //## ColumnTypes: INTEGER,INTEGER
    PhysicalColumnMappingImpl speciesIdSpeciesMart = PhysicalColumnMappingImpl.builder().withName("SPECIES_ID").withDataType(ColumnDataType.INTEGER).build();
    PhysicalColumnMappingImpl populationSpeciesMart = PhysicalColumnMappingImpl.builder().withName("POPULATION").withDataType(ColumnDataType.INTEGER).build();
    PhysicalTableMappingImpl speciesMart = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("species_mart")
            .withColumns(List.of(speciesIdSpeciesMart, populationSpeciesMart))).build();

    //## TableName: AGG_SPECIES_MART
    //## ColumnNames: GEN_ID,POPULATION,FACT_COUNT
    //## ColumnTypes: INTEGER,INTEGER,INTEGER
    PhysicalColumnMappingImpl genIdAggSpeciesMart = PhysicalColumnMappingImpl.builder().withName("GEN_ID").withDataType(ColumnDataType.INTEGER).build();
    PhysicalColumnMappingImpl populationAggSpeciesMart = PhysicalColumnMappingImpl.builder().withName("POPULATION").withDataType(ColumnDataType.INTEGER).build();
    PhysicalColumnMappingImpl factCountAggSpeciesMart = PhysicalColumnMappingImpl.builder().withName("FACT_COUNT").withDataType(ColumnDataType.INTEGER).build();
    PhysicalTableMappingImpl aggSpeciesMart = ((PhysicalTableMappingImpl.Builder) PhysicalTableMappingImpl.builder().withName("AGG_SPECIES_MART")
            .withColumns(List.of(genIdAggSpeciesMart, populationAggSpeciesMart, factCountAggSpeciesMart))).build();

    public SpeciesNonCollapsedAggTestModifier(CatalogMapping catalog) {
        super(catalog);
    }
*/
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
/*
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
        	animalsHierarchy = ExplicitHierarchyMappingImpl.builder()
                .withName("Animals")
                .withHasAll(true)
                .withAllMemberName("All Animals")
                .withPrimaryKey(speciesIdDimSpecies)
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
                        .withColumn(familyIdDimFamily)
                        .withNameColumn(familyNameDimFamily)
                        .withUniqueMembers(true)
                        .withType(InternalDataType.NUMERIC)
                        .withApproxRowCount("2")
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Genus")
                        .withColumn(genusIdDimGenus)
                        .withNameColumn(genusNameDimGenus)
                        .withUniqueMembers(true)
                        .withType(InternalDataType.NUMERIC)
                        .withApproxRowCount("4")
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Species")
                        .withColumn(speciesIdDimSpecies)
                        .withNameColumn(speciesNameDimSpecies)
                        .withUniqueMembers(true)
                        .withType(InternalDataType.NUMERIC)
                        .withApproxRowCount("8")
                        .build()
                ))
                .build()
        ))
        .build();

        SumMeasureMappingImpl populationMeasure = SumMeasureMappingImpl.builder()
        .withName("Population")
        .withColumn(populationSpeciesMart)
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
                                    .withName("[Animal].[Animals].[Genus]")
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
                                                .withMember("[Animal].[Animals].[Family].[Loricariidae]")
                                                .withAccess(AccessMember.ALL)
                                                .build(),
                                            AccessMemberGrantMappingImpl.builder()
                                                .withMember("[Animal].[Animals].[Family].[Cichlidae]")
                                                .withAccess(AccessMember.ALL)
                                                .build(),
                                            AccessMemberGrantMappingImpl.builder()
                                                .withMember("[Animal].[Animals].[Family].[Cyprinidae]")
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
*/

/**
 * EMF version of SpeciesNonCollapsedAggTestModifier
 * Creates Testmart catalog with Animal dimension, Test cube, aggregation tables and access roles
 */
public class SpeciesNonCollapsedAggTestModifier implements CatalogMappingSupplier {

    private Catalog catalog;

    public SpeciesNonCollapsedAggTestModifier(Catalog cat) {
        // Create new catalog from scratch (not copying the existing one)
        catalog = RolapMappingFactory.eINSTANCE.createCatalog();
        catalog.setName("Testmart");


        // Create database schema
        DatabaseSchema dbSchema =
            RolapMappingFactory.eINSTANCE.createDatabaseSchema();

        // Create tables and columns - DIM_SPECIES
        PhysicalColumn familyIdDimSpecies =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        familyIdDimSpecies.setName("FAMILY_ID");
        familyIdDimSpecies.setType(ColumnType.INTEGER);

        PhysicalColumn genusIdDimSpecies =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        genusIdDimSpecies.setName("GENUS_ID");
        genusIdDimSpecies.setType(ColumnType.INTEGER);

        PhysicalColumn speciesIdDimSpecies =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        speciesIdDimSpecies.setName("SPECIES_ID");
        speciesIdDimSpecies.setType(ColumnType.INTEGER);

        PhysicalColumn speciesNameDimSpecies =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        speciesNameDimSpecies.setName("SPECIES_NAME");
        speciesNameDimSpecies.setType(ColumnType.VARCHAR);
        speciesNameDimSpecies.setCharOctetLength(30);

        PhysicalTable dimSpecies =
            RolapMappingFactory.eINSTANCE.createPhysicalTable();
        dimSpecies.setName("DIM_SPECIES");
        dimSpecies.getColumns().add(familyIdDimSpecies);
        dimSpecies.getColumns().add(genusIdDimSpecies);
        dimSpecies.getColumns().add(speciesIdDimSpecies);
        dimSpecies.getColumns().add(speciesNameDimSpecies);

        // DIM_FAMILY table
        PhysicalColumn familyIdDimFamily =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        familyIdDimFamily.setName("FAMILY_ID");
        familyIdDimFamily.setType(ColumnType.INTEGER);

        PhysicalColumn familyNameDimFamily =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        familyNameDimFamily.setName("FAMILY_NAME");
        familyNameDimFamily.setType(ColumnType.VARCHAR);
        familyNameDimFamily.setCharOctetLength(30);

        PhysicalTable dimFamily =
            RolapMappingFactory.eINSTANCE.createPhysicalTable();
        dimFamily.setName("DIM_FAMILY");
        dimFamily.getColumns().add(familyIdDimFamily);
        dimFamily.getColumns().add(familyNameDimFamily);

        // DIM_GENUS table
        PhysicalColumn familyIdDimGenus =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        familyIdDimGenus.setName("FAMILY_ID");
        familyIdDimGenus.setType(ColumnType.INTEGER);

        PhysicalColumn genusIdDimGenus =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        genusIdDimGenus.setName("GENUS_ID");
        genusIdDimGenus.setType(ColumnType.INTEGER);

        PhysicalColumn genusNameDimGenus =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        genusNameDimGenus.setName("GENUS_NAME");
        genusNameDimGenus.setType(ColumnType.VARCHAR);
        genusNameDimGenus.setCharOctetLength(30);

        PhysicalTable dimGenus =
            RolapMappingFactory.eINSTANCE.createPhysicalTable();
        dimGenus.setName("DIM_GENUS");
        dimGenus.getColumns().add(familyIdDimGenus);
        dimGenus.getColumns().add(genusIdDimGenus);
        dimGenus.getColumns().add(genusNameDimGenus);

        // species_mart fact table
        PhysicalColumn speciesIdSpeciesMart =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        speciesIdSpeciesMart.setName("SPECIES_ID");
        speciesIdSpeciesMart.setType(ColumnType.INTEGER);

        PhysicalColumn populationSpeciesMart =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        populationSpeciesMart.setName("POPULATION");
        populationSpeciesMart.setType(ColumnType.INTEGER);

        PhysicalTable speciesMart =
            RolapMappingFactory.eINSTANCE.createPhysicalTable();
        speciesMart.setName("species_mart");
        speciesMart.getColumns().add(speciesIdSpeciesMart);
        speciesMart.getColumns().add(populationSpeciesMart);

        // AGG_SPECIES_MART aggregation table
        PhysicalColumn genIdAggSpeciesMart =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        genIdAggSpeciesMart.setName("GEN_ID");
        genIdAggSpeciesMart.setType(ColumnType.INTEGER);

        PhysicalColumn populationAggSpeciesMart =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        populationAggSpeciesMart.setName("POPULATION");
        populationAggSpeciesMart.setType(ColumnType.INTEGER);

        PhysicalColumn factCountAggSpeciesMart =
            RolapMappingFactory.eINSTANCE.createPhysicalColumn();
        factCountAggSpeciesMart.setName("FACT_COUNT");
        factCountAggSpeciesMart.setType(ColumnType.INTEGER);

        PhysicalTable aggSpeciesMart =
            RolapMappingFactory.eINSTANCE.createPhysicalTable();
        aggSpeciesMart.setName("AGG_SPECIES_MART");
        aggSpeciesMart.getColumns().add(genIdAggSpeciesMart);
        aggSpeciesMart.getColumns().add(populationAggSpeciesMart);
        aggSpeciesMart.getColumns().add(factCountAggSpeciesMart);

        // Add tables to database schema
        dbSchema.getTables().add(dimSpecies);
        dbSchema.getTables().add(dimFamily);
        dbSchema.getTables().add(dimGenus);
        dbSchema.getTables().add(speciesMart);
        dbSchema.getTables().add(aggSpeciesMart);

        // Create levels for Animal hierarchy
        Level familyLevel =
            RolapMappingFactory.eINSTANCE.createLevel();
        familyLevel.setName("Family");
        familyLevel.setColumn(familyIdDimFamily);
        familyLevel.setNameColumn(familyNameDimFamily);
        familyLevel.setUniqueMembers(true);
        familyLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        familyLevel.setApproxRowCount("2");

        Level genusLevel =
            RolapMappingFactory.eINSTANCE.createLevel();
        genusLevel.setName("Genus");
        genusLevel.setColumn(genusIdDimGenus);
        genusLevel.setNameColumn(genusNameDimGenus);
        genusLevel.setUniqueMembers(true);
        genusLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        genusLevel.setApproxRowCount("4");

        Level speciesLevel =
            RolapMappingFactory.eINSTANCE.createLevel();
        speciesLevel.setName("Species");
        speciesLevel.setColumn(speciesIdDimSpecies);
        speciesLevel.setNameColumn(speciesNameDimSpecies);
        speciesLevel.setUniqueMembers(true);
        speciesLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        speciesLevel.setApproxRowCount("8");

        // Create join query for hierarchy (DIM_SPECIES -> DIM_GENUS -> DIM_FAMILY)
        TableQuery dimGenusQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        dimGenusQuery.setTable(dimGenus);

        JoinedQueryElement left = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        left.setKey(familyIdDimGenus);
        left.setQuery(dimGenusQuery);

        TableQuery dimFamilyQuery =  RolapMappingFactory.eINSTANCE.createTableQuery();
        dimFamilyQuery.setTable(dimFamily);

        JoinedQueryElement right = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        right.setKey(familyIdDimFamily);
        right.setQuery(dimFamilyQuery);

        JoinQuery innerJoin =
            RolapMappingFactory.eINSTANCE.createJoinQuery();
        innerJoin.setLeft(left);
        innerJoin.setRight(right);

        TableQuery dimSpeciesQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        dimSpeciesQuery.setTable(dimSpecies);

        JoinedQueryElement left1 = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        left1.setKey(genusIdDimSpecies);
        left1.setQuery(dimSpeciesQuery);

        JoinedQueryElement right1 = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        right1.setKey(genusIdDimGenus);
        right1.setAlias("DIM_GENUS");
        right1.setQuery(innerJoin);

        JoinQuery outerJoin =
            RolapMappingFactory.eINSTANCE.createJoinQuery();
        outerJoin.setLeft(left1);
        outerJoin.setRight(right1);

        // Create Animals hierarchy
        ExplicitHierarchy animalsHierarchy =
            RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        animalsHierarchy.setName("Animals");
        animalsHierarchy.setHasAll(true);
        animalsHierarchy.setAllMemberName("All Animals");
        animalsHierarchy.setPrimaryKey(speciesIdDimSpecies);
        animalsHierarchy.setQuery(outerJoin);
        animalsHierarchy.getLevels().add(familyLevel);
        animalsHierarchy.getLevels().add(genusLevel);
        animalsHierarchy.getLevels().add(speciesLevel);

        // Create Animal dimension
        StandardDimension animalDimension =
            RolapMappingFactory.eINSTANCE.createStandardDimension();
        animalDimension.setName("Animal");
        animalDimension.getHierarchies().add(animalsHierarchy);

        // Create Population measure
        SumMeasure populationMeasure =
            RolapMappingFactory.eINSTANCE.createSumMeasure();
        populationMeasure.setName("Population");
        populationMeasure.setColumn(populationSpeciesMart);


        // Create aggregation
        AggregationColumnName aggFactCount =
            RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        aggFactCount.setColumn(factCountAggSpeciesMart);

        AggregationMeasure aggMeasure =
            RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        aggMeasure.setName("Measures.[Population]");
        aggMeasure.setColumn(populationAggSpeciesMart);

        AggregationLevel aggLevel =
            RolapMappingFactory.eINSTANCE.createAggregationLevel();
        aggLevel.setName("[Animal].[Animals].[Genus]");
        aggLevel.setColumn(genIdAggSpeciesMart);
        aggLevel.setCollapsed(false);

        AggregationName aggregation =
            RolapMappingFactory.eINSTANCE.createAggregationName();
        aggregation.setName(aggSpeciesMart);
        aggregation.setAggregationFactCount(aggFactCount);
        aggregation.getAggregationMeasures().add(aggMeasure);
        aggregation.getAggregationLevels().add(aggLevel);

        // Create table query with aggregation for cube
        TableQuery cubeQuery =
            RolapMappingFactory.eINSTANCE.createTableQuery();
        cubeQuery.setTable(speciesMart);
        cubeQuery.getAggregationTables().add(aggregation);

        // Create dimension connector
        DimensionConnector dimConnector =
            RolapMappingFactory.eINSTANCE.createDimensionConnector();
        dimConnector.setOverrideDimensionName("Animal");
        dimConnector.setDimension(animalDimension);
        dimConnector.setForeignKey(speciesIdSpeciesMart);

        // Create measure group
        MeasureGroup measureGroup =
            RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(populationMeasure);

        // Create Test cube
        PhysicalCube testCube =
            RolapMappingFactory.eINSTANCE.createPhysicalCube();
        testCube.setName("Test");
        testCube.setDefaultMeasure(populationMeasure);
        testCube.setQuery(cubeQuery);
        testCube.getDimensionConnectors().add(dimConnector);
        testCube.getMeasureGroups().add(measureGroup);

        // Create access member grants
        AccessMemberGrant memberGrant1 =
            RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant1.setMember("[Animal].[Animals].[Family].[Loricariidae]");
        memberGrant1.setMemberAccess(MemberAccess.ALL);

        AccessMemberGrant memberGrant2 =
            RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant2.setMember("[Animal].[Animals].[Family].[Cichlidae]");
        memberGrant2.setMemberAccess(MemberAccess.ALL);

        AccessMemberGrant memberGrant3 =
            RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        memberGrant3.setMember("[Animal].[Animals].[Family].[Cyprinidae]");
        memberGrant3.setMemberAccess(MemberAccess.NONE);

        // Create hierarchy grant
        AccessHierarchyGrant hierarchyGrant =
            RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
        hierarchyGrant.setHierarchy(animalsHierarchy);
        hierarchyGrant.setHierarchyAccess(HierarchyAccess.CUSTOM);
        hierarchyGrant.setRollupPolicy(RollupPolicy.PARTIAL);
        hierarchyGrant.getMemberGrants().add(memberGrant1);
        hierarchyGrant.getMemberGrants().add(memberGrant2);
        hierarchyGrant.getMemberGrants().add(memberGrant3);

        // Create cube grant
        AccessCubeGrant cubeGrant =
            RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
        cubeGrant.setCube(testCube);
        cubeGrant.setCubeAccess(CubeAccess.ALL);
        cubeGrant.getHierarchyGrants().add(hierarchyGrant);

        // Create catalog grant
        AccessCatalogGrant catalogGrant =
            RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        catalogGrant.setCatalogAccess(CatalogAccess.NONE);
        catalogGrant.getCubeGrants().add(cubeGrant);

        // Create access role
        AccessRole accessRole =
            RolapMappingFactory.eINSTANCE.createAccessRole();
        accessRole.setName("Test role");
        accessRole.getAccessCatalogGrants().add(catalogGrant);

        // Add everything to schema and catalog

        catalog.getDbschemas().add(dbSchema);
        catalog.getCubes().add(testCube);
        catalog.getAccessRoles().add(accessRole);
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}

