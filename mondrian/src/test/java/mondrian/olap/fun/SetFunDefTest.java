/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
//
// ajogleka, 19 December, 2007
*/

package mondrian.olap.fun;

import mondrian.olap.Connection;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import static org.opencube.junit5.TestUtil.assertQueryThrows;

/**
 * Unit test for the set constructor function <code>{ ... }</code>,
 * {@link SetFunDef}.
 *
 * @author ajogleka
 * @since 19 December, 2007
 */
public class SetFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testSetWithMembersFromDifferentHierarchies(Context context) {
        assertQueryFailsInSetValidation(context.createConnection(),
            "with member store.x as "
            + "'{[Gender].[M],[Store].[USA].[CA]}' "
            + " SELECT store.x on 0, [measures].[customer count] on 1 from sales");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testSetWith2TuplesWithDifferentHierarchies(Context context) {
        assertQueryFailsInSetValidation(context.createConnection(),
            "with member store.x as '{([Gender].[M],[Store].[All Stores].[USA].[CA]),"
            + "([Store].[USA].[OR],[Gender].[F])}'\n"
            + " SELECT store.x on 0, [measures].[customer count] on 1 from sales");
    }

    private void assertQueryFailsInSetValidation(Connection connection, String query) {
        assertQueryThrows(connection,
            query,
            "Mondrian Error:All arguments to function '{}' "
            + "must have same hierarchy");
    }
}

// End SetFunDefTest.java
