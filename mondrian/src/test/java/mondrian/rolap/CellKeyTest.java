/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.rolap;

import mondrian.olap.MondrianProperties;
import mondrian.test.PropertySaver5;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.SchemaUtil;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;
import static org.opencube.junit5.TestUtil.withSchema;

/**
 * Test that the implementations of the CellKey interface are correct.
 *
 * @author Richard M. Emberson
 */
public class CellKeyTest  {

    private PropertySaver5 propSaver;
    @BeforeEach
    public void beforeEach() {
        propSaver = new PropertySaver5();
    }

    @AfterEach
    public void afterEach() {
        propSaver.reset();
    }

    @Test
    public void testMany() {
        CellKey key = CellKey.Generator.newManyCellKey(5);

        assertTrue(key.size() == 5, "CellKey size");

        CellKey copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        int[] ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");

        boolean gotException = false;
        try {
            key.setAxis(6, 1);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey axis too big");

        gotException = false;
        try {
            key.setOrdinals(new int[6]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too big");

        gotException = false;
        try {
            key.setOrdinals(new int[4]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too small");

        key.setAxis(0, 1);
        key.setAxis(1, 3);
        key.setAxis(2, 5);
        key.setAxis(3, 7);
        key.setAxis(4, 13);
        assertTrue(!key.equals(copy), "CellKey not equals");

        copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");
    }

    @Test
    public void testZero() {
        CellKey key = CellKey.Generator.newCellKey(new int[0]);
        CellKey key2 = CellKey.Generator.newCellKey(new int[0]);
        assertTrue(key == key2); // all 0-dimensional keys have same singleton
        assertEquals(0, key.size());

        CellKey copy = key.copy();
        assertEquals(copy, key);

        boolean gotException = false;
        try {
            key.setAxis(0, 0);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey axis too big");

        int[] ordinals = key.getOrdinals();
        assertEquals(ordinals.length, 0);
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");
    }

    @Test
    public void testOne() {
        CellKey key = CellKey.Generator.newCellKey(1);
        assertTrue(key.size() == 1, "CellKey size");

        CellKey copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        int[] ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");

        boolean gotException = false;
        try {
            key.setAxis(3, 1);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey axis too big");

        gotException = false;
        try {
            key.setOrdinals(new int[3]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too big");

        gotException = false;
        try {
            key.setOrdinals(new int[0]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too small");

        key.setAxis(0, 1);

        copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");
    }

    @Test
    public void testTwo() {
        CellKey key = CellKey.Generator.newCellKey(2);

        assertTrue(key.size() == 2, "CellKey size");

        CellKey copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        int[] ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");

        boolean gotException = false;
        try {
            key.setAxis(3, 1);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey axis too big");

        gotException = false;
        try {
            key.setOrdinals(new int[3]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too big");

        gotException = false;
        try {
            key.setOrdinals(new int[1]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too small");

        key.setAxis(0, 1);
        key.setAxis(1, 3);

        copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");
    }

    @Test
    public void testThree() {
        CellKey key = CellKey.Generator.newCellKey(3);

        assertTrue(key.size() == 3, "CellKey size");

        CellKey copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        int[] ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");

        boolean gotException = false;
        try {
            key.setAxis(3, 1);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey axis too big");

        gotException = false;
        try {
            key.setOrdinals(new int[4]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too big");

        gotException = false;
        try {
            key.setOrdinals(new int[1]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too small");

        key.setAxis(0, 1);
        key.setAxis(1, 3);
        key.setAxis(2, 5);

        copy = key.copy();
        assertTrue(key.equals(copy), "CellKey array too small");

        ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");
    }

    @Test
    public void testFour() {
        CellKey key = CellKey.Generator.newCellKey(4);

        assertTrue(key.size() == 4, "CellKey size");

        CellKey copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        int[] ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");

        boolean gotException = false;
        try {
            key.setAxis(4, 1);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey axis too big");

        gotException = false;
        try {
            key.setOrdinals(new int[5]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too big");

        gotException = false;
        try {
            key.setOrdinals(new int[1]);
        } catch (Exception ex) {
            gotException = true;
        }
        assertTrue(gotException, "CellKey array too small");

        key.setAxis(0, 1);
        key.setAxis(1, 3);
        key.setAxis(2, 5);
        key.setAxis(3, 7);

        copy = key.copy();
        assertTrue(key.equals(copy), "CellKey equals");

        ordinals = key.getOrdinals();
        copy = CellKey.Generator.newCellKey(ordinals);
        assertTrue(key.equals(copy), "CellKey equals");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testCellLookup(Context context) {
        if (!isDefaultNullMemberRepresentation()) {
            return;
        }
        String cubeDef =
            "<Cube name = \"SalesTest\" defaultMeasure=\"Unit Sales\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <Dimension name=\"City\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"city\" column=\"city\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"gender\" column=\"gender\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Address2\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "      <Table name=\"customer\"/>\n"
            + "      <Level name=\"addr\" column=\"address2\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\" formatString=\"Standard\"/>\n"
            + "</Cube>";

        String query =
            "With Set [*NATIVE_CJ_SET] as NonEmptyCrossJoin([Gender].Children, [Address2].Children) "
            + "Select Generate([*NATIVE_CJ_SET], {([Gender].CurrentMember, [Address2].CurrentMember)}) on columns "
            + "From [SalesTest] where ([City].[Redwood City])";

        String result =
            "Axis #0:\n"
            + "{[City].[Redwood City]}\n"
            + "Axis #1:\n"
            + "{[Gender].[F], [Address2].[#null]}\n"
            + "{[Gender].[F], [Address2].[#2]}\n"
            + "{[Gender].[F], [Address2].[Unit H103]}\n"
            + "{[Gender].[M], [Address2].[#null]}\n"
            + "{[Gender].[M], [Address2].[#208]}\n"
            + "Row #0: 71\n"
            + "Row #0: 10\n"
            + "Row #0: 3\n"
            + "Row #0: 52\n"
            + "Row #0: 8\n";

        /*
         * Make sure ExpandNonNative is not set. Otherwise, the query is
         * evaluated natively. For the given data set(which contains NULL
         * members), native evaluation produces results in a different order
         * from the non-native evaluation.
         */
        propSaver.set(
            MondrianProperties.instance().ExpandNonNative,
            false);

        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
                null,
                cubeDef,
                null,
                null,
                null,
                null);
        withSchema(context, schema);
        assertQueryReturns(context.createConnection(), query, result);
    }

    public void testSize() {
        for (int i = 1; i < 20; i++) {
            assertEquals(i, CellKey.Generator.newCellKey(new int[i]).size());
            assertEquals(i, CellKey.Generator.newCellKey(i).size());
        }
    }
}

// End CellKeyTest.java
