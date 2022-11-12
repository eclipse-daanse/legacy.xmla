/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 1998-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
//
// Shishir, 08 May, 2007
*/

package mondrian.olap;

import mondrian.server.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import static mondrian.olap.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Query test.
 */
public class QueryTest {
    private QueryPart[] cellProps = {
        new CellProperty(Id.Segment.toList("Value")),
        new CellProperty(Id.Segment.toList("Formatted_Value")),
        new CellProperty(Id.Segment.toList("Format_String")),
    };
    private QueryAxis[] axes = new QueryAxis[0];
    private Formula[] formulas = new Formula[0];
    private Query queryWithCellProps;
    private Query queryWithoutCellProps;


    private void beforeTest(Context context)
    {

        ConnectionBase connection =
                (ConnectionBase) context.createConnection();
        final Statement statement =
                connection.getInternalStatement();

        try {
            queryWithCellProps =
                    new Query(
                            statement, formulas, axes, "Sales",
                            null, cellProps, false);
            queryWithoutCellProps =
                    new Query(
                            statement, formulas, axes, "Sales",
                            null, new QueryPart[0], false);
        } finally {
            statement.close();
        }
    }

    @AfterEach
    public void afterEach() {
        queryWithCellProps = null;
        queryWithoutCellProps = null;
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testHasCellPropertyWhenQueryHasCellProperties(Context context) {
        beforeTest(context);
        assertTrue(queryWithCellProps.hasCellProperty("Value"));
        assertFalse(queryWithCellProps.hasCellProperty("Language"));
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testIsCellPropertyEmpty(Context context) {
        beforeTest(context);
        assertTrue(queryWithoutCellProps.isCellPropertyEmpty());
    }
}

// End QueryTest.java
