/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.flushSchemaCache;
import static org.opencube.junit5.TestUtil.withSchema;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

//Disabled by reason log4j. log4j is not using any more
@Disabled
class OrderKeyOneToOneCheckTest {

  //private TestAppender memberSourceAppender;
  //private TestAppender sqlReaderAppender;

  @BeforeEach
  public void beforeEach() throws Exception {
    //Logger memberSourceLogger = (Logger) LoggerFactory.getLogger(SqlMemberSource.class);
    //Logger sqlReaderLogger = (Logger) LoggerFactory.getLogger(SqlTupleReader.class);

    //memberSourceAppender = new TestAppender();
    //sqlReaderAppender = new TestAppender();
    //memberSourceLogger.addAppender(memberSourceAppender);
    //sqlReaderLogger.addAppender(sqlReaderAppender);
  }

  @AfterEach
  protected void afterEach() throws Exception {
    //Logger memberSourceLogger = (Logger) LoggerFactory.getLogger(SqlMemberSource.class);
    //Logger sqlReaderLogger = (Logger) LoggerFactory.getLogger(SqlTupleReader.class);
    //memberSourceLogger.removeAppender(memberSourceAppender);
    //sqlReaderLogger.removeAppender(sqlReaderAppender);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  public void prepareContext(Context<?> context) {
    //TestContext<?> testContext<?> = super.getTestContext()
    //        .withFreshConnection();
    flushSchemaCache(context.getConnectionWithDefaultRole());
    /*
    withSchema(context,
            ""
                    + "<?xml version=\"1.0\"?>\n"
                    + "<Schema name=\"FoodMart 2358\">\n"
                    + "  <Dimension name=\"Time\" type=\"TimeDimension\">\n"
                    + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
                    + "      <Table name=\"time_by_day\"/>\n"
                    + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                    + "          levelType=\"TimeYears\"/> \n"
                    + "      <Level name=\"Quarter\" column=\"quarter\" ordinalColumn=\"month_of_year\" uniqueMembers=\"false\" levelType=\"TimeQuarters\"/>  \n"
                    + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
                    + "          levelType=\"TimeMonths\"/>\n"
                    + "    </Hierarchy>\n"
                    + "  </Dimension>\n"
                    + "<Cube name=\"Sales\" defaultMeasure=\"Unit Sales\">\n"
                    + "  <Table name=\"sales_fact_1997\"/>\n"
                    + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
                    + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
                    + "      formatString=\"Standard\"/>\n"
                    + "</Cube>\n"
                    + "</Schema>");
     */
    withSchema(context, SchemaModifiers.OrderKeyOneToOneCheckTestModifier::new);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMemberSource(Context<?> context) {
    String mdx =
            "with member [Measures].[Count Month] as 'Count(Descendants(Time.CurrentMember, [Time].[Month]))' \n"
                    + "select [Measures].[Count Month] on 0,\n"
                    + "[Time].[1997] on 1 \n"
                    + "from [Sales]";
    prepareContext(context);
    executeQuery(context.getConnectionWithDefaultRole(), mdx);
    fail("need slf4j implementation");
    //TODO need slf4j implementation
    //assertEquals(
    //        8,
    //        sqlReaderAppender.getLogEvents().size(),
    //        "Running with modified schema should log 8 error");
    //assertEquals(
    //        8,
    //        memberSourceAppender.getLogEvents().size(),
    //        "Running with modified schema should log 8 error");
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSqlReader(Context<?> context) {
    String mdx = ""
            + "select [Time].[Quarter].Members on 0"
            + "from [Sales]";

    prepareContext(context);
    executeQuery(context.getConnectionWithDefaultRole(), mdx);
    fail("need slf4j implementation");
    //TODO need slf4j implementation
    //assertEquals(
    //        16,
    //        sqlReaderAppender.getLogEvents().size(),
    //        "Running with modified schema should log 16 error");
  }
}
