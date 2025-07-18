/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2011-2017 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.monitor.EventBus;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.impl.RectangularCellSetFormatter;
import org.eclipse.daanse.olap.impl.StatementImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Unit test for monitoring, including {@link EventBus}.
 *
 * @author jhyde
 */
class MonitorTest {
    private void println(Object x) {
        // Enable for debugging, but not for checked-in code.
        if (false) {
            System.out.println(x);
        }
    }

    /**
     * Exercises as many fields of the monitoring stats classes as possible.
     * So that we can check that they are being populated.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMe(Context<?> context) throws SQLException {
        String queryString =
            "WITH MEMBER [Measures].[Foo] AS\n"
            + " [Measures].[Unit Sales]"
            + " + case when [Measures].[Unit Sales] > 0\n"
            + "   then CInt( ([Measures].[Foo], [Time].[Time].PrevMember) )\n"
            + "   end\n"
            + "SELECT [Measures].[Foo] on 0\n"
            + "from [Sales]\n"
            + "where [Time].[Time].[1997].[Q3].[9]";

        final Statement statement =
            context.getConnectionWithDefaultRole().createStatement();
        CellSet cellSet = statement.executeQuery(queryString);
        StringWriter stringWriter = new StringWriter();
        new RectangularCellSetFormatter(true).format(
            cellSet,
            new PrintWriter(stringWriter));
        ((StatementImpl)statement).close();
        println(stringWriter);


        final EventBus monitor = context.getMonitor();
//        final ServerInfo server = monitor.getServer();
//
//        println(
//            "# stmts open: "
//            + server.getStatementCurrentlyOpenCount());
//
//        println(
//            "# connections open: "
//            + server.getConnectionCurrentlyOpenCount());
//
//        println("# rows fetched: " + server.sqlStatementRowFetchCount);
//
//        println(
//            "# sql stmts open: "
//            + server.getSqlStatementCurrentlyOpenCount());
//
//        // # sql stmts by category (cell query, member query, other)
//        //  -- if you want to do this, capture sql statement events
//
//        // cell cache requests
//        // cell cache misses
//        // cell cache hits
////        final List<ConnectionInfo> connections = monitor.getConnections();
////        ConnectionInfo lastConnection = connections.get(connections.size() - 1);
//
//        // Cannot reliably retrieve the last statement, since statements are
//        // removed from the map on completion.
//        //final List<StatementInfo> statements = monitor.getStatements();
//        //StatementInfo lastStatement = statements.get(statements.size() - 1);
//
//        println(
//            "# cell cache requests, misses, hits; "
//            + "by server, connection, mdx statement: "
//            + server.cellCacheRequestCount
//            + ", " + server.getCellCacheMissCount()
//            + ", " + server.cellCacheHitCount
////            + "; " + lastConnection.cellCacheRequestCount
////            + ", " + (lastConnection.cellCacheRequestCount
////                      - lastConnection.cellCacheHitCount)
////            + ", " + lastConnection.cellCacheHitCount
//        		);
//
//        // cache misses in the last minute
//        // cache hits in the last minute
//        // -- build a layer on top of monitor that polls say every 15 seconds,
//        //    and keeps results for a few minutes
//
//        println(
//            "number of mdx statements currently open: "
//            + server.getStatementCurrentlyOpenCount());
//        println(
//            "number of mdx statements currently executing: "
//            + server.getStatementCurrentlyExecutingCount());
//
//        println(
//            "jvm memory: " + server.jvmHeapBytesUsed
//            + ", max: " + server.jvmHeapBytesMax
//            + ", committed: " + server.jvmHeapBytesCommitted);
//
//        println(
//            "number of segments: " + server.segmentCount
//            + ", ever created: " + server.segmentCreateCount
//            + ", number of cells: " + server.cellCount
//            + ", number of cell coordinates: " + server.cellCoordinateCount
//            + ", average cell dimensionality: "
//            + ((float) server.cellCoordinateCount / (float) server.cellCount));
//
////        println("Connection: " + lastConnection);
//        println("Server: " + server);

        // number of mdx function calls cumulative
        // how many operations have been evaluated in sql?
        // number of members in cache
        // number of cells in segments
        // mdx query time
        // sql query time
        // sql rows
        // olap4j connection pool size
        // sql connection pool size
        // thread count
        // # schemas in schema cache
        // cells fulfilled by sql statements
        // mondrian server count (other stats relate to just one server)
        //
        // Events:
        //
        // SQL statement start
        // SQL statment stop
        // external cache call
        // sort
        // (other expensive operations similar to sort?)
    }
}
