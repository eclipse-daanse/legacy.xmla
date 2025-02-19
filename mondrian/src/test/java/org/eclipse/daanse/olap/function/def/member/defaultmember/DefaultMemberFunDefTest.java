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
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionConnectorMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TimeDimensionMappingImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;

class DefaultMemberFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDefaultMember(Context context) {
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
                "select {[Time.Weekly].DefaultMember} on columns\n"
                    + "from Sales" );
        assertEquals(
            SystemWideProperties.instance().SsasCompatibleNaming
                ? "All Weeklys"
                : "All Time.Weeklys",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );

        final String memberUname =
            SystemWideProperties.instance().SsasCompatibleNaming
                ? "[Time2].[Weekly].[1997].[23]"
                : "[Time2.Weekly].[1997].[23]";
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
                            HierarchyMappingImpl.builder()
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
                            HierarchyMappingImpl.builder()
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
        withSchema(context, TestDefaultMemberModifier::new);

        // In this variant of the schema, Time2.Weekly has an explicit default
        // member.
        result =
            executeQuery(context.getConnectionWithDefaultRole(),
                "select {[Time2.Weekly].DefaultMember} on columns\n"
                    + "from Sales" );
        assertEquals(
            "23",
            result.getAxes()[ 0 ].getPositions().get( 0 ).get( 0 ).getName() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDimensionDefaultMember(Context context) {
      Member member = executeSingletonAxis(context.getConnectionWithDefaultRole(), "[Measures].DefaultMember", "Sales");
      assertEquals( "Unit Sales", member.getName() );
    }

}
