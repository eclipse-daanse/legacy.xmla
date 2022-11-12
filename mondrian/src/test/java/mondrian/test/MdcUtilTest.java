/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2021 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.test;

import mondrian.olap.Connection;
import mondrian.rolap.RolapUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.flushSchemaCache;

/**
 * Verifies that MDC context is passed between threads.
 * 
 * @author Benny
 */
//disabled by reason of we don't plan use log4j
@Disabled
public class MdcUtilTest {

  private static Logger rolapUtilLogger = LogManager.getLogger( RolapUtil.class );

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
  public void testMdcContext(Context context) throws Exception {

    Connection connection = context.createConnection();
    flushSchemaCache(connection);

    ThreadContext.put( "sessionName", "hello-world" );
    StringWriter writer = new StringWriter();

    //final Appender appender =
    //    Util.makeAppender(
    //        "testMdcContext",
    //        writer,
    //        "sessionName:%X{sessionName} %t %m");

    //Util.addAppender(appender, rolapUtilLogger, null);
    //Level oldLevel = rolapUtilLogger.getLevel();
    //Util.setLevel( rolapUtilLogger, Level.DEBUG );
    
    //Util.addAppender(appender, RolapUtil.MONITOR_LOGGER, null);
    //Level oldMonitorLevel = RolapUtil.MONITOR_LOGGER.getLevel();
    //Util.setLevel( RolapUtil.MONITOR_LOGGER, Level.DEBUG );
    
    //Util.addAppender(appender, RolapUtil.MDX_LOGGER, null);
    //Level oldMdxLevel = RolapUtil.MDX_LOGGER.getLevel();
    //Util.setLevel( RolapUtil.MDX_LOGGER, Level.DEBUG );
    
    String log = "";
    try {
      
      String query =
          "SELECT {[Measures].[Unit Sales]} " + "on columns, {[Gender].Members} on rows FROM [Sales]";
  
      String expected =
          "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Unit Sales]}\n" + "Axis #2:\n"
              + "{[Gender].[All Gender]}\n" + "{[Gender].[F]}\n" + "{[Gender].[M]}\n" + "Row #0: 266,773\n"
              + "Row #1: 131,558\n" + "Row #2: 135,215\n";
  
      assertQueryReturns(context.createConnection(), query, expected );
      log = writer.toString();

    } finally {
      
      //Util.removeAppender(appender, rolapUtilLogger);
      //Util.setLevel( rolapUtilLogger, oldLevel );
      
      //Util.removeAppender(appender, RolapUtil.MONITOR_LOGGER);
      //Util.setLevel( RolapUtil.MONITOR_LOGGER, oldMonitorLevel );
      
      //Util.removeAppender(appender, RolapUtil.MDX_LOGGER);
      //Util.setLevel( RolapUtil.MDX_LOGGER, oldMdxLevel );
      
    }
    
    // Mondrian uses another thread pool to execute SQL statements.  
    // Verify that sessionName is now present on the SQL log statements
    assertContains(log, "sessionName:hello-world mondrian.rolap.agg.SegmentCacheManager$sqlExecutor_");
    assertContains(log, "sessionName:hello-world Mondrian Monitor StatementStartEvent");
    assertContains(log, "sessionName:hello-world mondrian.rolap.RolapResultShepherd$executor_");
  }
  
  public void assertContains( String actual, String contains ) {
    if ( actual.contains( contains ) ) {
      return;
    }
    fail( "Test String does not contain: " + contains + " Test String: " + actual );
  }

}
// End MdcUtilTest.java
