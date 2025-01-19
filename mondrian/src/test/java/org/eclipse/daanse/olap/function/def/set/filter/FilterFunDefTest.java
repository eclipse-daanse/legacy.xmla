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
package org.eclipse.daanse.olap.function.def.set.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeAxis;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.QueryTimeoutException;
import mondrian.olap.SystemWideProperties;


class FilterFunDefTest {

    /**
     * Make sure that slicer is in force when expression is applied on axis, E.g. select filter([Customers].members, [Unit
     * Sales] > 100) from sales where ([Time].[1998])
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFilterWithSlicer(Context context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales]} on columns,\n"
                + " filter([Customers].[USA].children,\n"
                + "        [Measures].[Unit Sales] > 20000) on rows\n"
                + "from Sales\n"
                + "where ([Time].[1997].[Q1])" );
        Axis rows = result.getAxes()[ 1 ];
        // if slicer were ignored, there would be 3 rows
        assertEquals( 1, rows.getPositions().size() );
        Cell cell = result.getCell( new int[] { 0, 0 } );
        assertEquals( "30,114", cell.getFormattedValue() );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFilterCompound(Context context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "select {[Measures].[Unit Sales]} on columns,\n"
                + "  Filter(\n"
                + "    CrossJoin(\n"
                + "      [Gender].Children,\n"
                + "      [Customers].[USA].Children),\n"
                + "    [Measures].[Unit Sales] > 9500) on rows\n"
                + "from Sales\n"
                + "where ([Time].[1997].[Q1])" );
        List<Position> rows = result.getAxes()[ 1 ].getPositions();
        assertEquals( 3, rows.size() );
        assertEquals( "F", rows.get( 0 ).get( 0 ).getName() );
        assertEquals( "WA", rows.get( 0 ).get( 1 ).getName() );
        assertEquals( "M", rows.get( 1 ).get( 0 ).getName() );
        assertEquals( "OR", rows.get( 1 ).get( 1 ).getName() );
        assertEquals( "M", rows.get( 2 ).get( 0 ).getName() );
        assertEquals( "WA", rows.get( 2 ).get( 1 ).getName() );
    }

    //TODO: reanable
    @Disabled //UserDefinedFunction
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFilterWillTimeout(Context context) {
        ((TestConfig)context.getConfig()).setQueryTimeout(3);
        SystemWideProperties.instance().EnableNativeNonEmpty = false;
        try {
            class TestFilterWillTimeoutModifier extends PojoMappingModifier {

                public TestFilterWillTimeoutModifier(CatalogMapping catalog) {
                    super(catalog);
                }
            /* TODO: UserDefinedFunction
            @Override
            protected List<MappingUserDefinedFunction> schemaUserDefinedFunctions(MappingSchema schema) {
                List<MappingUserDefinedFunction> result = new ArrayList<>();
                result.addAll(super.schemaUserDefinedFunctions(schema));
                result.add(UserDefinedFunctionRBuilder.builder()
                    .name("SleepUdf")
                    .className(BasicQueryTest.SleepUdf.class.getName())
                    .build());
                return result;
            }*/
            }
      /*
      String baseSchema = TestUtil.getRawSchema(context);
      String schema = SchemaUtil.getSchema(baseSchema,
        null, null, null, null,
        "<UserDefinedFunction name=\"SleepUdf\" className=\""
          + BasicQueryTest.SleepUdf.class.getName()
          + "\"/>", null );
      TestUtil.withSchema(context, schema);
       */
            withSchema(context, TestFilterWillTimeoutModifier::new);
            executeAxis(context.getConnectionWithDefaultRole(),
                "Filter("
                    + "Filter(CrossJoin([Customers].[Name].members, [Product].[Product Name].members), SleepUdf([Measures]"
                    + ".[Unit Sales]) > 0),"
                    + " SleepUdf([Measures].[Sales Count]) > 5) " );
        } catch ( QueryTimeoutException e ) {
            return;
        }
        fail( "should have timed out" );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFilterEmpty(Context context) {
        // Unlike "Descendants(<set>, ...)", we do not need to know the precise
        // type of the set, therefore it is OK if the set is empty.
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Filter({}, 1=0)",
            "" );
        assertAxisReturns(context.getConnectionWithDefaultRole(),
            "Filter({[Time].[Time].Children}, 1=0)",
            "" );
    }


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testFilterCalcSlicer(Context context) {
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Time].[Time].[Date Range] as \n"
                + "'Aggregate({[Time].[1997].[Q1]:[Time].[1997].[Q3]})'\n"
                + "select\n"
                + "{[Measures].[Unit Sales],[Measures].[Store Cost],\n"
                + "[Measures].[Store Sales]} ON columns,\n"
                + "NON EMPTY Filter ([Store].[Store State].members,\n"
                + "[Measures].[Store Cost] > 75000) ON rows\n"
                + "from [Sales] where [Time].[Date Range]",
            "Axis #0:\n"
                + "{[Time].[Date Range]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Store Cost]}\n"
                + "{[Measures].[Store Sales]}\n"
                + "Axis #2:\n"
                + "{[Store].[USA].[WA]}\n"
                + "Row #0: 90,131\n"
                + "Row #0: 76,151.59\n"
                + "Row #0: 190,776.88\n" );
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "with member [Time].[Time].[Date Range] as \n"
                + "'Aggregate({[Time].[1997].[Q1]:[Time].[1997].[Q3]})'\n"
                + "select\n"
                + "{[Measures].[Unit Sales],[Measures].[Store Cost],\n"
                + "[Measures].[Store Sales]} ON columns,\n"
                + "NON EMPTY Order (Filter ([Store].[Store State].members,\n"
                + "[Measures].[Store Cost] > 100),[Measures].[Store Cost], DESC) ON rows\n"
                + "from [Sales] where [Time].[Date Range]",
            "Axis #0:\n"
                + "{[Time].[Date Range]}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales]}\n"
                + "{[Measures].[Store Cost]}\n"
                + "{[Measures].[Store Sales]}\n"
                + "Axis #2:\n"
                + "{[Store].[USA].[WA]}\n"
                + "{[Store].[USA].[CA]}\n"
                + "{[Store].[USA].[OR]}\n"
                + "Row #0: 90,131\n"
                + "Row #0: 76,151.59\n"
                + "Row #0: 190,776.88\n"
                + "Row #1: 53,312\n"
                + "Row #1: 45,435.93\n"
                + "Row #1: 113,966.00\n"
                + "Row #2: 51,306\n"
                + "Row #2: 43,033.82\n"
                + "Row #2: 107,823.63\n" );
    }

}
