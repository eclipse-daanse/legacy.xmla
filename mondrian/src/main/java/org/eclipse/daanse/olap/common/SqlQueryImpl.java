/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.common;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.query.component.SqlQuery;

public class SqlQueryImpl implements SqlQuery{

    private String sql;
    private DataSource dataSource;

    public SqlQueryImpl(String sql, DataSource dataSource) {
        this.sql = sql;
        this.dataSource = dataSource;
    }

    @Override
    public Object[] getChildren() {
        return null;
    }

    @Override
    public void unparse(PrintWriter pw) {
    }

    @Override
    public void explain(PrintWriter pw) {
    }

    @Override
    public String getSql() {
        return this.sql;
    }

    @Override
    public ResultSet execute() {
        //try (java.sql.Connection jdbcConn = dataSource.getConnection(); Statement statement = jdbcConn.createStatement()){
        try {
             java.sql.Connection jdbcConn = dataSource.getConnection(); 
             Statement statement = jdbcConn.createStatement();
             return statement.executeQuery(sql);
        } catch (SQLException e) {
             throw new RuntimeException(e);
        }
    }
}
