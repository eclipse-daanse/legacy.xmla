/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2019 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap;

import mondrian.olap.Util;
import mondrian.olap.Util.PropertyList;
import mondrian.spi.DynamicSchemaProcessor;
import mondrian.test.FoodMartTestCase;
import mondrian.test.TestContext;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.sql.DataSource;

/**
 * Test for {@link RolapCatalogPool}.
 */
class RolapCatalogPoolTest extends FoodMartTestCase {

    public RolapCatalogPoolTest(String name) {
        super(name);
    }

    void testBasicSchemaFetch() {
        RolapCatalogPool schemaPool = RolapCatalogPool.instance();
        schemaPool.clear();

        String catalogUrl = getFoodmartCatalogUrl().toString();
        Util.PropertyList connectInfo =
            Util.parseConnectString(TestContext.getDefaultConnectString());

        RolapCatalog schema =
            schemaPool.get(
                catalogUrl,
                "connectionKeyA",
                "joeTheUser",
                "aDataSource",
                connectInfo);
        RolapCatalog schemaA =
            schemaPool.get(
                catalogUrl,
                "connectionKeyA",
                "joeTheUser",
                "aDataSource",
                connectInfo);
        //same arguments, same object
        assertTrue(schema == schemaA);
    }

    void testSchemaFetchCatalogUrlJdbcUuid() {
        RolapCatalogPool schemaPool = RolapCatalogPool.instance();
        schemaPool.clear();
        final String uuid = "UUID-1";

        String catalogUrl = getFoodmartCatalogUrl().toString();
        Util.PropertyList connectInfo =
            Util.parseConnectString(TestContext.getDefaultConnectString());
        connectInfo.put(
            RolapConnectionProperties.JdbcConnectionUuid.name(),
            uuid);

        // Put in pool
        RolapCatalog schema =
            schemaPool.get(
                catalogUrl,
                "connectionKeyA",
                "joeTheUser",
                "aDataSource",
                connectInfo);

        // Same catalogUrl, same JdbcUuid
        Util.PropertyList connectInfoA =
            Util.parseConnectString(TestContext.getDefaultConnectString());
        connectInfoA.put(
            RolapConnectionProperties.JdbcConnectionUuid.name(),
            uuid);
        RolapCatalog sameSchema =
            schemaPool.get(
                catalogUrl,
                "aDifferentConnectionKey",
                "mrDoeTheOtherUser",
                "someDataSource",
                connectInfoA);
        //must fetch the same object
        assertTrue(schema == sameSchema);

        connectInfo.put(
            RolapConnectionProperties.JdbcConnectionUuid.name(),
            "SomethingCompletelyDifferent");
        RolapCatalog aNewSchema =
            schemaPool.get(
                catalogUrl,
                "connectionKeyA",
                "joeTheUser",
                "aDataSource",
                connectInfo);
        //must create a new object
        assertTrue(schema != aNewSchema);
    }

    /**
     * Test using JdbcConnectionUUID and useSchemaChecksum
     * fetches the same schema in all scenarios.
     */
    void testSchemaFetchMd5JdbcUid() throws IOException {
        RolapCatalogPool pool = RolapCatalogPool.instance();
        pool.clear();
        final String uuid = "UUID-1";
        String catalogUrl = getFoodmartCatalogUrl().toString();
        Util.PropertyList connectInfo =
            Util.parseConnectString(TestContext.getDefaultConnectString());
        connectInfo.put(
            RolapConnectionProperties.JdbcConnectionUuid.name(),
            uuid);
        connectInfo.put(
            RolapConnectionProperties.UseContentChecksum.name(),
            "true");

        RolapCatalog schema =
            pool.get(
                catalogUrl,
                "connectionKeyA",
                "joeTheUser",
                "aDataSource",
                connectInfo);

        Util.PropertyList connectInfoDyn = connectInfo.clone();
        connectInfoDyn.put(
            RolapConnectionProperties.DynamicSchemaProcessor.name(),
            NotReallyDynamicSchemaProcessor.class.getName());
        RolapCatalog schemaDyn =
            pool.get(
                catalogUrl,
                "connectionKeyB",
                "jed",
                "dsName",
                connectInfo);

        assertTrue(schema == schemaDyn);

        String catalogContent = Util.readVirtualFileAsString(catalogUrl);
        Util.PropertyList connectInfoCont = connectInfo.clone();
        connectInfoCont.remove(RolapConnectionProperties.Catalog.name());
        connectInfoCont.put(
            RolapConnectionProperties.CatalogContent.name(),
            catalogContent);
        RolapCatalog schemaCont = pool.get(
            catalogUrl,
            "connectionKeyC", "--", "--", connectInfo);

        assertTrue(schema == schemaCont);

        Util.PropertyList connectInfoDS = connectInfo.clone();
        final StringBuilder buf = new StringBuilder();
        DataSource dataSource =
            RolapConnection.createDataSource(null, connectInfoDS, buf);
        RolapCatalog schemaDS = pool.get(catalogUrl, dataSource, connectInfoDS);

        assertTrue(schema == schemaDS);
    }


    protected URL getFoodmartCatalogUrl() {
        // Works if we are running in root directory of source tree
        File file = new File("demo/FoodMart.xml");
        if (!file.exists()) {
            // Works if we are running in bin directory of runtime env
            file = new File("../demo/FoodMart.xml");
        }
        try {
            return Util.toURL(file);
        } catch (MalformedURLException e) {
            throw new Error(e.getMessage());
        }
    }

    public static class NotReallyDynamicSchemaProcessor
        implements DynamicSchemaProcessor
    {
        public String processSchema(String schemaUrl, PropertyList connectInfo)
            throws Exception
        {
            return Util.readVirtualFileAsString(schemaUrl);
        }
    }
}
