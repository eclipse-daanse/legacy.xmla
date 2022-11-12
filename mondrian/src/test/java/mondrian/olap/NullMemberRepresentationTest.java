/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap;

import mondrian.rolap.RolapUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import java.io.IOException;

import static org.opencube.junit5.TestUtil.assertExprReturns;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

/**
 * <code>NullMemberRepresentationTest</code> tests the null member
 * custom representation feature supported via
 * {@link MondrianProperties#NullMemberRepresentation} property.
 * @author ajogleka
 */
public class NullMemberRepresentationTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testClosingPeriodMemberLeafWithCustomNullRepresentation(Context context) {
        assertQueryReturns(context.createConnection(),
            "with member [Measures].[Foo] as ' ClosingPeriod().uniquename '\n"
            + "select {[Measures].[Foo]} on columns,\n"
            + "  {[Time].[1997],\n"
            + "   [Time].[1997].[Q2],\n"
            + "   [Time].[1997].[Q2].[4]} on rows\n"
            + "from Sales",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Foo]}\n"
            + "Axis #2:\n"
            + "{[Time].[1997]}\n"
            + "{[Time].[1997].[Q2]}\n"
            + "{[Time].[1997].[Q2].[4]}\n"
            + "Row #0: [Time].[1997].[Q4]\n"
            + "Row #1: [Time].[1997].[Q2].[6]\n"
            + "Row #2: [Time].["
            + getNullMemberRepresentation()
            + "]\n"
            + "");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testItemMemberWithCustomNullMemberRepresentation(Context context)
        throws IOException
    {
        Connection connection = context.createConnection();
        assertExprReturns(connection,
            "[Time].[1997].Children.Item(6).UniqueName",
            "[Time].[" + getNullMemberRepresentation() + "]");
        assertExprReturns(connection,
            "[Time].[1997].Children.Item(-1).UniqueName",
            "[Time].[" + getNullMemberRepresentation() + "]");
    }

    public void testNullMemberWithCustomRepresentation(Context context) throws IOException {
        Connection connection = context.createConnection();
        assertExprReturns(connection,
            "[Gender].[All Gender].Parent.UniqueName",
            "[Gender].[" + getNullMemberRepresentation() + "]");

        assertExprReturns(connection,
            "[Gender].[All Gender].Parent.Name", getNullMemberRepresentation());
    }

    private String getNullMemberRepresentation() {
        return RolapUtil.mdxNullLiteral();
    }

}

// End NullMemberRepresentationTest.java
