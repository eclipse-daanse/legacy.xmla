/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.eclipse.daanse.db.dialect.db.infobright;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.db.dialect.db.mysql.MySqlDialect;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Implementation of {@link Dialect} for the Infobright database.
 *
 * @author jhyde
 * @since Nov 23, 2008
 */
@ServiceProvider(value = Dialect.class, attribute = { "database.dialect.type:String='MYSQL'",
        "database.product:String='INFOBRIGHT'" })
@Component(service = Dialect.class, scope = ServiceScope.PROTOTYPE)
public class InfobrightDialect extends MySqlDialect {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfobrightDialect.class);
    private static final String SUPPORTED_PRODUCT_NAME = "INFOBRIGHT";

    @Override
    protected boolean isSupportedProduct(String productName, String productVersion) {
        return SUPPORTED_PRODUCT_NAME.equalsIgnoreCase(productVersion);
    }

    @Override
    public boolean initialize(Connection connection) {
        try {
            return super.initialize(connection) && isInfobright(connection.getMetaData());
        } catch (SQLException e) {
            LOGGER.warn("", e);
        }
        return false;
    }

    @Override
	public boolean allowsCompoundCountDistinct() {
        return false;
    }

    @Override
    public StringBuilder generateOrderItem(CharSequence expr, boolean nullable, boolean ascending, boolean collateNullsLast) {
        // Like MySQL, Infobright collates NULL values as negative-infinity
        // (first in ASC, last in DESC). But we can't generate ISNULL to
        // correct the NULL ordering, as we do for MySQL, because Infobright
        // does not support this function.
        if (ascending) {
            return new StringBuilder(expr).append(" ASC");
        } else {
            return new StringBuilder(expr).append(" DESC");
        }
    }

    @Override
    public boolean supportsGroupByExpressions() {
        return false;
    }

    @Override
    public boolean requiresGroupByAlias() {
        return true;
    }

    @Override
    public boolean allowsOrderByAlias() {
        return false;
    }

    @Override
    public boolean requiresOrderByAlias() {
        // Actually, Infobright doesn't ALLOW aliases to be used in the ORDER BY
        // clause, let alone REQUIRE them. Infobright doesn't allow expressions
        // in the ORDER BY clause, so returning true gives the right effect.
        return true;
    }

    @Override
    public boolean supportsMultiValueInExpr() {
        // Infobright supports multi-value IN by falling through to MySQL,
        // which is very slow (see for example
        // PredicateFilterTest.testFilterAtSameLevel) so we pretend that it
        // does not.
        return false;
    }

    @Override
    public String getDialectName() {
        return SUPPORTED_PRODUCT_NAME.toLowerCase();
    }
}
