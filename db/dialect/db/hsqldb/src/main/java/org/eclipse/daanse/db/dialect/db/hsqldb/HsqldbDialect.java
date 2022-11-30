/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.eclipse.daanse.db.dialect.db.hsqldb;

import java.sql.Date;
import java.util.List;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.db.dialect.db.common.JdbcDialectImpl;
import org.eclipse.daanse.db.dialect.db.common.Util;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Implementation of {@link Dialect} for the Hsqldb database.
 *
 * @author wgorman
 * @since Aug 20, 2009
 */
@ServiceProvider(value = Dialect.class, attribute = { "database.dialect.type:String='HSQLDB'",
		"database.product:String='HSQLDB'" })
@Component(service = Dialect.class, scope = ServiceScope.PROTOTYPE)
public class HsqldbDialect extends JdbcDialectImpl {

    private static final String SUPPORTED_PRODUCT_NAME = "HSQLDB";

    @Override
    protected boolean isSupportedProduct(String productName, String productVersion) {
        return SUPPORTED_PRODUCT_NAME.equalsIgnoreCase(productVersion);
    }

    protected void quoteDateLiteral(
        StringBuilder buf,
        String value,
        Date date)
    {
        // Hsqldb accepts '2008-01-23' but not SQL:2003 format.
        Util.singleQuoteString(value, buf);
    }

    public String generateInline(
        List<String> columnNames,
        List<String> columnTypes,
        List<String[]> valueList)
    {
        // Fall back to using the FoodMart 'days' table, because
        // HQLDB's SQL has no way to generate values not from a table.
        // (Same as Access.)
        return generateInlineGeneric(
            columnNames, columnTypes, valueList,
            " from \"days\" where \"day\" = 1", false);
    }
}

// End HsqldbDialect.java
