/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.member.defaultmember;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.Column;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.Cube;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.Table;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.TimeDimension;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class DefaultMemberFunDefTest {

    /**
     * EMF version of TestDefaultMemberModifier
     * Creates Time2 dimension with two hierarchies, where Weekly hierarchy has explicit defaultMember
     */
    public static class TestDefaultMemberModifierEmf implements CatalogMappingSupplier {

        private CatalogImpl catalog;

        public TestDefaultMemberModifierEmf(Catalog cat) {
            // Copy catalog using EcoreUtil
            EcoreUtil.Copier copier  = org.opencube.junit5.EmfUtil.copier((CatalogImpl) cat);
            catalog = (CatalogImpl) copier.get(cat);

            // Find Sales cube
            PhysicalCube salesCube = null;
            for (Cube cube : catalog.getCubes()) {
                if ("Sales".equals(cube.getName()) && cube instanceof PhysicalCube) {
                    salesCube = (PhysicalCube) cube;
                    break;
                }
            }

            if (salesCube != null) {
                // Create first hierarchy (no hasAll) using RolapMappingFactory
                Level yearLevel1 =
                    RolapMappingFactory.eINSTANCE.createLevel();
                yearLevel1.setName("Year");
                yearLevel1.setColumn((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY));
                yearLevel1.setColumnType(ColumnInternalDataType.NUMERIC);
                yearLevel1.setUniqueMembers(true);
                yearLevel1.setType(LevelDefinition.TIME_YEARS);

                Level quarterLevel =
                    RolapMappingFactory.eINSTANCE.createLevel();
                quarterLevel.setName("Quarter");
                quarterLevel.setColumn((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY));
                quarterLevel.setUniqueMembers(false);
                quarterLevel.setType(LevelDefinition.TIME_QUARTERS);

                Level monthLevel =
                    RolapMappingFactory.eINSTANCE.createLevel();
                monthLevel.setName("Month");
                monthLevel.setColumn((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY));
                monthLevel.setUniqueMembers(false);
                monthLevel.setColumnType(ColumnInternalDataType.NUMERIC);
                monthLevel.setType(LevelDefinition.TIME_MONTHS);

                TableQuery tableQuery1 =
                    RolapMappingFactory.eINSTANCE.createTableQuery();
                tableQuery1.setTable((Table) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.TABLE_TIME_BY_DAY));

                ExplicitHierarchy hierarchy1 =
                    RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
                hierarchy1.setHasAll(false);
                hierarchy1.setPrimaryKey((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_TIME_ID_TIME_BY_DAY));
                hierarchy1.setQuery(tableQuery1);
                hierarchy1.getLevels().add(yearLevel1);
                hierarchy1.getLevels().add(quarterLevel);
                hierarchy1.getLevels().add(monthLevel);

                // Create second hierarchy (Weekly with defaultMember) using RolapMappingFactory
                Level yearLevel2 =
                    RolapMappingFactory.eINSTANCE.createLevel();
                yearLevel2.setName("Year");
                yearLevel2.setColumn((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY));
                yearLevel2.setColumnType(ColumnInternalDataType.NUMERIC);
                yearLevel2.setUniqueMembers(true);
                yearLevel2.setType(LevelDefinition.TIME_YEARS);

                Level weekLevel =
                    RolapMappingFactory.eINSTANCE.createLevel();
                weekLevel.setName("Week");
                weekLevel.setColumn((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_WEEK_OF_YEAR_TIME_BY_DAY));
                weekLevel.setColumnType(ColumnInternalDataType.NUMERIC);
                weekLevel.setUniqueMembers(false);
                weekLevel.setType(LevelDefinition.TIME_WEEKS);

                Level dayLevel =
                    RolapMappingFactory.eINSTANCE.createLevel();
                dayLevel.setName("Day");
                dayLevel.setColumn((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_DAY_OF_MONTH_TIME_BY_DAY));
                dayLevel.setUniqueMembers(false);
                dayLevel.setColumnType(ColumnInternalDataType.NUMERIC);
                dayLevel.setType(LevelDefinition.TIME_DAYS);

                TableQuery tableQuery2 =
                    RolapMappingFactory.eINSTANCE.createTableQuery();
                tableQuery2.setTable((Table) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.TABLE_TIME_BY_DAY));

                ExplicitHierarchy hierarchy2 =
                    RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
                hierarchy2.setName("Weekly");
                hierarchy2.setHasAll(true);
                hierarchy2.setPrimaryKey((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_TIME_ID_TIME_BY_DAY));
                hierarchy2.setDefaultMember("[Time2].[Weekly].[1997].[23]"); // Set explicit default member
                hierarchy2.setQuery(tableQuery2);
                hierarchy2.getLevels().add(yearLevel2);
                hierarchy2.getLevels().add(weekLevel);
                hierarchy2.getLevels().add(dayLevel);

                // Create Time2 dimension using RolapMappingFactory
                TimeDimension timeDimension =
                    RolapMappingFactory.eINSTANCE.createTimeDimension();
                timeDimension.setName("Time2");
                timeDimension.getHierarchies().add(hierarchy1);
                timeDimension.getHierarchies().add(hierarchy2);

                // Create dimension connector using RolapMappingFactory
                DimensionConnector dimConnector =
                    RolapMappingFactory.eINSTANCE.createDimensionConnector();
                dimConnector.setOverrideDimensionName("Time2");
                dimConnector.setForeignKey((Column) copier.get(org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier.COLUMN_TIME_ID_SALESFACT));
                dimConnector.setDimension(timeDimension);

                // Add dimension connector to Sales cube
                salesCube.getDimensionConnectors().add(dimConnector);
            }
        }

        @Override
        public Catalog get() {
            return catalog;
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDefaultMember(Context<?> context) {
        // [Time] has no default member and no all, so the default member is
        // the first member of the first level.
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select {[Time].[Time].DefaultMember} on columns\n"
                    + "from Sales" );
        assertEquals(
            "1997",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );

        // [Time].[Weekly] has an all member and no explicit default.
        result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select {[Time].[Weekly].DefaultMember} on columns\n"
                    + "from Sales" );
        assertEquals("All Weeklys",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );

        final String memberUname ="[Time2].[Weekly].[1997].[23]";
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
      "Sales",
      "  <Dimension name=\"Time2\" type=\"TimeDimension\" foreignKey=\"time_id\">\n"
        + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
        + "      <Table name=\"time_by_day\"/>\n"
        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
        + "          levelType=\"TimeYears\"/>\n"
        + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
        + "          levelType=\"TimeQuarters\"/>\n"
        + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
        + "          levelType=\"TimeMonths\"/>\n"
        + "    </Hierarchy>\n"
        + "    <Hierarchy hasAll=\"true\" name=\"Weekly\" primaryKey=\"time_id\"\n"
        + "          defaultMember=\""
        + memberUname
        + "\">\n"
        + "      <Table name=\"time_by_day\"/>\n"
        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
        + "          levelType=\"TimeYears\"/>\n"
        + "      <Level name=\"Week\" column=\"week_of_year\" type=\"Numeric\" uniqueMembers=\"false\"\n"
        + "          levelType=\"TimeWeeks\"/>\n"
        + "      <Level name=\"Day\" column=\"day_of_month\" uniqueMembers=\"false\" type=\"Numeric\"\n"
        + "          levelType=\"TimeDays\"/>\n"
        + "    </Hierarchy>\n"
        + "  </Dimension>" ));
      */
        /*
        class TestDefaultMemberModifier extends PojoMappingModifier {

            public TestDefaultMemberModifier(CatalogMapping catalogMapping) {
                super(catalogMapping);
            }

            protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
                List<DimensionConnectorMapping> result = new ArrayList<>();
                result.addAll(super.cubeDimensionConnectors(cube));
                if ("Sales".equals(cube.getName())) {
                    TimeDimensionMappingImpl dimension = TimeDimensionMappingImpl
                        .builder()
                        .withName("Time2")
                        //.withForeignKey("time_id")
                        .withHierarchies(List.of(
                            ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(false)
                                .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("Year")
                                        .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                        .withType(InternalDataType.NUMERIC)
                                        .withUniqueMembers(true)
                                        .withLevelType(LevelType.TIME_YEARS)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("Quarter")
                                        .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY)
                                        .withUniqueMembers(false)
                                        .withLevelType(LevelType.TIME_QUARTERS)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("Month")
                                        .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                                        .withUniqueMembers(false)
                                        .withType(InternalDataType.NUMERIC)
                                        .withLevelType(LevelType.TIME_MONTHS)
                                        .build()
                                ))
                                .build(),
                            ExplicitHierarchyMappingImpl.builder()
                                .withHasAll(true)
                                .withName("Weekly")
                                .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                                .withDefaultMember(memberUname)
                                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("Year")
                                        .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                        .withType(InternalDataType.NUMERIC)
                                        .withUniqueMembers(true)
                                        .withLevelType(LevelType.TIME_YEARS)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("Week")
                                        .withColumn(FoodmartMappingSupplier.WEEK_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                                        .withType(InternalDataType.NUMERIC)
                                        .withUniqueMembers(false)
                                        .withLevelType(LevelType.TIME_WEEKS)
                                        .build(),
                                    LevelMappingImpl.builder()
                                        .withName("Day")
                                        .withColumn(FoodmartMappingSupplier.DAY_OF_MONTH_COLUMN_TIME_BY_DAY)
                                        .withUniqueMembers(false)
                                        .withType(InternalDataType.NUMERIC)
                                        .withLevelType(LevelType.TIME_DAYS)
                                        .build()
                                ))
                                .build()
                        ))
                        .build();
                    result.add(DimensionConnectorMappingImpl.builder()
                        .withOverrideDimensionName("Time2")
                        .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                        .withDimension(dimension)
                        .build());
                }
                return result;
            }
        }
        */

        withSchemaEmf(context, TestDefaultMemberModifierEmf::new);

        // In this variant of the schema, Time2.Weekly has an explicit default
        // member.
        result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select {[Time2].[Weekly].DefaultMember} on columns\n"
                    + "from Sales" );
        assertEquals(
            "23",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDefaultMember(Context<?> context) {
      Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Measures].DefaultMember", "Sales");
      assertEquals( "Unit Sales", member.getName() );
    }

}
