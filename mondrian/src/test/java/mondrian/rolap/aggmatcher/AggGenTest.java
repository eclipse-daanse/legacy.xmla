/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */
package mondrian.rolap.aggmatcher;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.query.component.QueryImpl;
import org.eclipse.daanse.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.common.RolapConnection;
import org.eclipse.daanse.rolap.common.aggmatcher.AggGen;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DatabaseSchemaMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test if lookup columns are there after loading them in
 * AggGen#addCollapsedColumn(...).
 *
 * @author Sherman Wood
 */
class AggGenTest {
    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testCallingLoadColumnsInAddCollapsedColumnOrAddzSpecialCollapsedColumn(Context<?> context) throws Exception
    {
        Logger logger = LoggerFactory.getLogger(AggGen.class);
        StringWriter writer = new StringWriter();

        //TODO use log in tests?
        //final Appender appender =
        //    Util.makeAppender("testMdcContext", writer, null);

        //Util.addAppender(appender, logger, org.apache.logging.log4j.Level.DEBUG);

        // If run in Ant and with mondrian.jar, please comment out this line:
//        ((TestContextImpl)context).setAggregateRules("/DefaultRules.xml");
        ((TestContextImpl)context).setUseAggregates(true);
        ((TestContextImpl)context).setReadAggregates(true);
        ((TestContextImpl)context).setGenerateAggregateSql(true);

        final RolapConnection rolapConn = (RolapConnection) context.getConnectionWithDefaultRole();
        Query query =
            rolapConn.parseQuery(
                "select {[Measures].[Count]} on columns from [HR]");
        rolapConn.execute(query);

        //Util.removeAppender(appender, logger);

        final DataSource dataSource = rolapConn.getDataSource();
        Connection sqlConnection = null;
        try {
            sqlConnection = dataSource.getConnection();
            DatabaseMetaData dbmeta = sqlConnection.getMetaData();
            CatalogMapping catalogMapping = ((RolapContext) context).getCatalogMapping();
            List<? extends DatabaseSchemaMapping> schemas = catalogMapping.getDbschemas();
            DatabaseSchemaMapping databaseSchema = schemas.getFirst();


            String log = writer.toString();
            Pattern p = Pattern.compile(
                "DEBUG - Init: Column: [^:]+: `(\\w+)`.`(\\w+)`"
                + Util.NL
                + "WARN - Can not find column: \\2");
            Matcher m = p.matcher(log);

            while (m.find()) {
                ResultSet rs =
                    dbmeta.getColumns(
                    		catalogMapping.getName(), databaseSchema.getName(), m.group(1), m.group(2));
                assertTrue(!rs.next());
            }
        } finally {
            if (sqlConnection != null) {
                try {
                    sqlConnection.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
    }

}
