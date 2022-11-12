/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara.  All rights reserved.
*/
package mondrian.udf;

import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.SchemaUtil;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import mondrian.olap.Connection;

/**
 * Tests the CurrentDateMemberUdf class.
 *
 * @author Luc Boudreau
 */
public class CurrentDateMemberUdfTest {

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testCurrentDateMemberUdf(Context context) {
		Connection connection=context.createConnection();
		//TODO: context redesign
		//Assertions.fail("Handle comment , Context redesign nedded");
		String baseSchema = TestUtil.getRawSchema(context);
	    String schema = SchemaUtil.getSchema(baseSchema,
            null,
            null,
            null,
            null,
            "<UserDefinedFunction name=\"MockCurrentDateMember\" "
            + "className=\"mondrian.udf.MockCurrentDateMember\" /> ",
            null);
	    TestUtil.withSchema(context, schema);
	    TestUtil.assertQueryReturns(context.createConnection(),
            "SELECT NON EMPTY {[Measures].[Org Salary]} ON COLUMNS, "
            + "NON EMPTY {MockCurrentDateMember([Time].[Time], \"[yyyy]\")} ON ROWS "
            + "FROM [HR] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Org Salary]}\n"
            + "Axis #2:\n"
            + "{[Time].[1997]}\n"
            + "Row #0: $39,431.67\n");
    }

    /**
     * test for MONDRIAN-2256 issue. Tests if method returns member with
     * dimension info or not. To get a number as a result you should change
     * current year to 1997. In this case expected should be ended with
     * "266,773\n"
    */
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testGetReturnType(Context context) {
		Connection connection=context.createConnection();
        String query = "WITH MEMBER [Time].[YTD] AS SUM( YTD(CurrentDateMember"
             + "([Time], '[\"Time\"]\\.[yyyy]\\.[Qq].[m]', EXACT)), Measures.[Unit Sales]) SELECT Time.YTD on 0 FROM sales";
        String expected = "Axis #0:\n" + "{}\n" + "Axis #1:\n"
             + "{[Time].[YTD]}\n" + "Row #0: \n";
        TestUtil.assertQueryReturns(connection,query, expected);
    }
}

// End CurrentDateMemberUdfTest.java
