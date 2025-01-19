/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.withSchema;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.IdImpl;
import mondrian.olap.Property;
import mondrian.olap.SystemWideProperties;
import mondrian.rolap.SchemaModifiers;

/**
 * Tests intrinsic member and cell properties as specified in OLE DB for OLAP
 * specification.
 *
 * @author anikitin
 * @since 5 July, 2005
 */
class PropertiesTest {

    /**
     * Tests existence and values of mandatory member properties.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMandatoryMemberProperties(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        Cube salesCube = connection.getSchema().lookupCube("Sales", true);
        SchemaReader scr = salesCube.getSchemaReader(null).withLocus();
        Member member =
            scr.getMemberByUniqueName(
                IdImpl.toList("Customers", "All Customers", "USA", "CA"),
                true);
        final boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;

        String stringPropValue;
        Integer intPropValue;

        // I'm not sure this property has to store the same value
        // getConnection().getCatalogName() returns.

        // todo:
//        stringPropValue = (String)member.getPropertyValue("CATALOG_NAME");
//        assertEquals(getConnection().getCatalogName(), stringPropValue);

        stringPropValue = (String)member.getPropertyValue("SCHEMA_NAME");
        assertEquals(connection.getSchema().getName(), stringPropValue);

        // todo:
//        stringPropValue = (String)member.getPropertyValue("CUBE_NAME");
//        assertEquals(salesCube.getName(), stringPropValue);

        stringPropValue =
            (String)member.getPropertyValue("DIMENSION_UNIQUE_NAME");
        assertEquals(member.getDimension().getUniqueName(), stringPropValue);

        // Case sensitivity.
        stringPropValue = (String)member.getPropertyValue(
            "dimension_unique_name", caseSensitive);
        if (caseSensitive) {
            assertNull(stringPropValue);
        } else {
            assertEquals(
                member.getDimension().getUniqueName(),
                stringPropValue);
        }

        // Non-existent property.
        stringPropValue =
            (String)member.getPropertyValue("DIMENSION_UNIQUE_NAME_XXXX");
        assertNull(stringPropValue);

        // Leading spaces.
        stringPropValue =
            (String)member.getPropertyValue(" DIMENSION_UNIQUE_NAME");
        assertNull(stringPropValue);

        // Trailing spaces.
        stringPropValue =
            (String)member.getPropertyValue("DIMENSION_UNIQUE_NAME  ");
        assertNull(stringPropValue);

        stringPropValue =
            (String)member.getPropertyValue("HIERARCHY_UNIQUE_NAME");
        assertEquals(member.getHierarchy().getUniqueName(), stringPropValue);

        // This property works in Mondrian 1.1.5 (due to XMLA support)
        stringPropValue = (String)member.getPropertyValue("LEVEL_UNIQUE_NAME");
        assertEquals(member.getLevel().getUniqueName(), stringPropValue);

        // This property works in Mondrian 1.1.5 (due to XMLA support)
        intPropValue = (Integer)member.getPropertyValue("LEVEL_NUMBER");
        assertEquals(
            Integer.valueOf(member.getLevel().getDepth()),
            intPropValue);

        // This property works in Mondrian 1.1.5 (due to XMLA support)
        stringPropValue = (String)member.getPropertyValue("MEMBER_UNIQUE_NAME");
        assertEquals(member.getUniqueName(), stringPropValue);

        stringPropValue = (String)member.getPropertyValue("MEMBER_NAME");
        assertEquals(member.getName(), stringPropValue);

        intPropValue = (Integer)member.getPropertyValue("MEMBER_TYPE");
        assertEquals(
            Integer.valueOf(member.getMemberType().ordinal()),
            intPropValue);

        stringPropValue = (String)member.getPropertyValue("MEMBER_GUID");
        assertNull(stringPropValue);

        // This property works in Mondrian 1.1.5 (due to XMLA support)
        stringPropValue = (String)member.getPropertyValue("MEMBER_CAPTION");
        assertEquals(member.getCaption(), stringPropValue);

        stringPropValue = (String)member.getPropertyValue("CAPTION");
        assertEquals(member.getCaption(), stringPropValue);

        // It's worth checking case-sensitivity for CAPTION because it is a
        // synonym, not a true property.
        stringPropValue = (String) member.getPropertyValue(
            "caption", caseSensitive);
        if (caseSensitive) {
            assertNull(stringPropValue);
        } else {
            assertEquals(member.getCaption(), stringPropValue);
        }

        intPropValue = (Integer)member.getPropertyValue("MEMBER_ORDINAL");
        assertEquals(Integer.valueOf(member.getOrdinal()), intPropValue);

        if (false) {
            intPropValue =
                (Integer)member.getPropertyValue("CHILDREN_CARDINALITY");
            assertEquals(
                Integer.valueOf(scr.getMemberChildren(member).size()),
                intPropValue);
        }

        intPropValue = (Integer)member.getPropertyValue("PARENT_LEVEL");
        assertEquals(
            Integer.valueOf(member.getParentMember().getLevel().getDepth()),
            intPropValue);

        stringPropValue = (String)member.getPropertyValue("PARENT_UNIQUE_NAME");
        assertEquals(member.getParentUniqueName(), stringPropValue);

        intPropValue = (Integer)member.getPropertyValue("PARENT_COUNT");
        assertEquals(Integer.valueOf(1), intPropValue);

        stringPropValue = (String)member.getPropertyValue("DESCRIPTION");
        assertEquals(member.getDescription(), stringPropValue);

        // Case sensitivity.
        stringPropValue =
            (String)member.getPropertyValue("desCription", caseSensitive);
        if (caseSensitive) {
            assertNull(stringPropValue);
        } else {
            assertEquals(member.getDescription(), stringPropValue);
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testGetChildCardinalityPropertyValue(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        Cube salesCube = connection.getSchema().lookupCube("Sales", true);
        SchemaReader scr = salesCube.getSchemaReader(null);
        Member memberForCardinalityTest =
            scr.getMemberByUniqueName(
                IdImpl.toList("Marital Status", "All Marital Status"),
                true);
        Integer intPropValue =
            (Integer) memberForCardinalityTest.getPropertyValue(
                "CHILDREN_CARDINALITY");
        assertEquals(Integer.valueOf(111), intPropValue);
    }

    /**
     * Tests the ability of MDX parser to pass requested member properties
     * to Result object.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPropertiesMDX(Context context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "SELECT {[Customers].[All Customers].[USA].[CA]} DIMENSION PROPERTIES \n"
            + " CATALOG_NAME, SCHEMA_NAME, CUBE_NAME, DIMENSION_UNIQUE_NAME, \n"
            + " HIERARCHY_UNIQUE_NAME, LEVEL_UNIQUE_NAME, LEVEL_NUMBER, MEMBER_UNIQUE_NAME, \n"
            + " MEMBER_NAME, MEMBER_TYPE, MEMBER_GUID, MEMBER_CAPTION, MEMBER_ORDINAL, CHILDREN_CARDINALITY,\n"
            + " PARENT_LEVEL, PARENT_UNIQUE_NAME, PARENT_COUNT, DESCRIPTION ON COLUMNS\n"
            + "FROM [Sales]");
        QueryAxis[] axes = result.getQuery().getAxes();
        Id[] axesProperties = axes[0].getDimensionProperties();
        String[] props = {
            "CATALOG_NAME",
            "SCHEMA_NAME",
            "CUBE_NAME",
            "DIMENSION_UNIQUE_NAME",
            "HIERARCHY_UNIQUE_NAME",
            "LEVEL_UNIQUE_NAME",
            "LEVEL_NUMBER",
            "MEMBER_UNIQUE_NAME",
            "MEMBER_NAME",
            "MEMBER_TYPE",
            "MEMBER_GUID",
            "MEMBER_CAPTION",
            "MEMBER_ORDINAL",
            "CHILDREN_CARDINALITY",
            "PARENT_LEVEL",
            "PARENT_UNIQUE_NAME",
            "PARENT_COUNT",
            "DESCRIPTION"
        };

        assertEquals(axesProperties.length, props.length);
        int i = 0;
        for (String prop : props) {
            assertEquals(prop, axesProperties[i++].toString());
        }
    }

    /**
     * Tests the ability to project non-standard member properties.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberProperties(Context context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "SELECT {[Store].Children} DIMENSION PROPERTIES\n"
            + " CATALOG_NAME, PARENT_UNIQUE_NAME, [Store Type], FORMAT_EXP\n"
            + " ON COLUMNS\n"
            + "FROM [Sales]");
        QueryAxis[] axes = result.getQuery().getAxes();
        Id[] axesProperties = axes[0].getDimensionProperties();

        assertEquals(4, axesProperties.length);
    }

    /**
     * Tests the ability to project non-standard member properties.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMemberPropertiesBad(Context context) {
        Result result = executeQuery(context.getConnectionWithDefaultRole(),
            "SELECT {[Store].Children} DIMENSION PROPERTIES\n"
            + " CATALOG_NAME, PARENT_UNIQUE_NAME, [Store Type], BAD\n"
            + " ON COLUMNS\n"
            + "FROM [Sales]");
        QueryAxis[] axes = result.getQuery().getAxes();
        Id[] axesProperties = axes[0].getDimensionProperties();

        assertEquals(4, axesProperties.length);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMandatoryCellProperties(Context context) {
        Connection connection = context.getConnectionWithDefaultRole();
        Query salesCube = connection.parseQuery(
            "select \n"
            + " {[Measures].[Store Sales], [Measures].[Unit Sales]} on columns, \n"
            + " {[Gender].members} on rows \n"
            + "from [Sales]");
        Result result = connection.execute(salesCube);
        int x = 1;
        int y = 2;
        Cell cell = result.getCell(new int[] {x, y});

        assertNull(cell.getPropertyValue("BACK_COLOR"));
        assertNull(cell.getPropertyValue("CELL_EVALUATION_LIST"));
        assertEquals(y * 2 + x, cell.getPropertyValue("CELL_ORDINAL"));
        assertNull(cell.getPropertyValue("FORE_COLOR"));
        assertNull(cell.getPropertyValue("FONT_NAME"));
        assertNull(cell.getPropertyValue("FONT_SIZE"));
        assertEquals(0, cell.getPropertyValue("FONT_FLAGS"));
        assertEquals("Standard", cell.getPropertyValue("FORMAT_STRING"));
        // FORMAT is a synonym for FORMAT_STRING
        assertEquals("Standard", cell.getPropertyValue("FORMAT"));
        assertEquals("135,215", cell.getPropertyValue("FORMATTED_VALUE"));
        assertNull(cell.getPropertyValue("NON_EMPTY_BEHAVIOR"));
        assertEquals(0, cell.getPropertyValue("SOLVE_ORDER"));
        assertEquals(
            135215.0,
            ((Number) cell.getPropertyValue("VALUE")).doubleValue(),
            0.1);

        // Case sensitivity.
        if (SystemWideProperties.instance().CaseSensitive) {
            assertNull(cell.getPropertyValue("cell_ordinal"));
            assertNull(cell.getPropertyValue("font_flags"));
            assertNull(cell.getPropertyValue("format_string"));
            assertNull(cell.getPropertyValue("format"));
            assertNull(cell.getPropertyValue("formatted_value"));
            assertNull(cell.getPropertyValue("solve_order"));
            assertNull(cell.getPropertyValue("value"));
        } else {
            assertEquals(y * 2 + x, cell.getPropertyValue("cell_ordinal"));
            assertEquals(0, cell.getPropertyValue("font_flags"));
            assertEquals("Standard", cell.getPropertyValue("format_string"));
            assertEquals("Standard", cell.getPropertyValue("format"));
            assertEquals("135,215", cell.getPropertyValue("formatted_value"));
            assertEquals(0, cell.getPropertyValue("solve_order"));
            assertEquals(
                135215.0,
                ((Number) cell.getPropertyValue("value")).doubleValue(),
                0.1);
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testPropertyDescription(Context context) throws Exception {
        withSchema(context, SchemaModifiers.PropertiesTestModifier::new);
        Cube[] cubes = context.getConnectionWithDefaultRole().getSchema()
            .getCubes();
        Optional<Cube> optionalCube = Arrays.stream(cubes).filter(c -> c.getName().equals("Foo")).findFirst();
        Cube cube = optionalCube.orElseThrow(() -> new RuntimeException("Cube with name Foo absent"));
        Optional<Dimension> optionalDimension  = Arrays.stream(cube.getDimensions()).filter(d -> d.getName().equals("Promotions")).findFirst();
        Dimension dimension = optionalDimension.orElseThrow(() -> new RuntimeException("Dimension with name Foo absent"));
        Optional<Property> optionalProperty = Arrays.stream(dimension.getHierarchies()[0].getLevels()[1].getProperties())
            .filter(p -> p.getName().equals("BarProp")).findFirst();
        Property property =  optionalProperty.orElseThrow(() -> new RuntimeException("Property with name BarProp absent"));
        assertEquals(
            "BaconDesc",
            property
                .getDescription());
    }
}
