/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2021 Sergei Semenkov
 */

package mondrian.spi.impl;

import mondrian.olap.Util;
import mondrian.spi.Dialect;

import java.sql.Connection;
import java.sql.SQLException;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Implementation of {@link mondrian.spi.Dialect} for ClickHouse
 */
@ServiceProvider(value = Dialect.class, attribute = { "database.dialect.type:String='CLICKHOUSE'",
		"database.product:String='CLICKHOUSE'" })
public class ClickHouseDialect extends JdbcDialectImpl {

    public static final JdbcDialectFactory FACTORY =
            new JdbcDialectFactory(
                    ClickHouseDialect.class,
                    DatabaseProduct.CLICKHOUSE);

    /**
     * Creates a ClickHouseDialect.
     *
     * @param connection Connection
     */
    public ClickHouseDialect(Connection connection) throws SQLException {
        super(connection);
    }

    public boolean requiresDrillthroughMaxRowsInLimit() {
        return true;
    }

    public void quoteStringLiteral(
            StringBuilder buf,
            String s)
    {
        buf.append('\'');

        String s0 = Util.replace(s, "\\", "\\\\");
        s0 = Util.replace(s0, "'", "\\'");
        buf.append(s0);

        buf.append('\'');
    }
}

