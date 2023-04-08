/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.db.jdbc.util.impl;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.db.dialect.api.DialectResolver;
import org.eclipse.daanse.db.jdbc.util.api.DatabaseCreatorService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component(service = DatabaseCreatorService.class, scope = ServiceScope.SINGLETON)
public class DatabaseCreatorServiceImpl implements DatabaseCreatorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCreatorServiceImpl.class);
    public static final String nl = System.getProperty("line.separator");
    @Reference
    private DialectResolver dialectResolver;

    @Override
    public void createDatabaseSchema(DataSource dataSource, DBStructure dbStructure) throws SQLException {
        Optional<Dialect> optional = dialectResolver.resolve(dataSource);
        if (optional.isPresent()) {
            final Dialect dialect = optional.get();
            try (Connection connection = dataSource.getConnection()) {
                List<String> dropTableList = dbStructure.getTables().parallelStream()
                    .map(t -> dropTableSQL(t, dialect)).toList();
                List<String> createTableList = dbStructure.getTables().parallelStream()
                    .map(t -> createTableSQL(t, dialect)).toList();
                List<String> createIndexList = dbStructure.getTables().parallelStream()
                    .flatMap(t -> createIndexSqls(t, dialect).stream()).toList();

                final Statement statement = connection.createStatement();
                StringBuilder sb = new StringBuilder("CREATE DATABASE IF NOT EXISTS ")
                    .append(dbStructure.getName());
                LOGGER.debug(sb.toString());
                statement.execute(sb.toString());
                connection.setAutoCommit(false);
                for (String sql : dropTableList) {
                    LOGGER.debug(sql);
                    statement.addBatch(sql);
                }
                for (String sql : createTableList) {
                    LOGGER.debug(sql);
                    statement.addBatch(sql);
                }
                for (String sql : createIndexList) {
                    LOGGER.debug(sql);
                    statement.addBatch(sql);
                }
                statement.executeBatch();
                connection.commit();

            }
        } else {
            throw new RuntimeException("Database dialect did not determinate");
        }
    }

    /**
     * Creates a table definition.
     *
     * @param table   Table
     * @param dialect dialect
     */
    private String dropTableSQL(Table table, Dialect dialect) {
        String schemaTable = dialect.quoteIdentifier(table.getSchemaName(), table.getTableName());

        return new StringBuilder("DROP TABLE IF EXISTS ").append(schemaTable).toString();
    }

    /**
     * Creates a table definition.
     *
     * @param table   Table
     * @param dialect dialect
     */
    private static String createTableSQL(Table table, Dialect dialect) {
        String schemaTable = dialect.quoteIdentifier(table.getSchemaName(), table.getTableName());

        // Define the table.
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ").append(schemaTable).append("(");

        boolean first = true;
        for (Column column : table.getColumns()) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }
            buf.append(nl);
            buf.append("    ").append(dialect.quoteIdentifier(column.getName()));
            buf.append(" ").append(column.getType().toPhysical(dialect));
            //if (!column.getConstraint().equals("")) {
            //    buf.append(" ").append(column.getConstraint());
            //}
        }

        buf.append(")");
        switch (dialect.getDialectName()) {
            case "NEOVIEW":
                // no unique keys defined
                buf.append(" NO PARTITION");
        }

        final String ddl = buf.toString();
        return ddl;

    }

    /**
     * Creates an index.
     *
     * <p>
     * If we are outputting to JDBC, executes the CREATE INDEX statement; otherwise,
     * outputs the statement to a file.
     */
    private List<String> createIndexSqls(Table table, Dialect dialect) {

        if (table.getConstraints() == null) {
            return List.of();
        }

        return table.getConstraints().stream().map(constraint -> {

            StringBuilder buf = new StringBuilder();

            buf.append(constraint.isUnique() ? "CREATE UNIQUE INDEX " : "CREATE INDEX ")
                .append(dialect.quoteIdentifier(constraint.getName()));
            if (!"TERADATA".equals(dialect.getDialectName())) {
                buf.append(" ON ").append(dialect.quoteIdentifier(table.getSchemaName(), table.getTableName()));
            }
            buf.append(" (");

            boolean first = true;
            for (String columnName : constraint.getColumnNames()) {

                if (first) {
                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(dialect.quoteIdentifier(columnName));
            }
            buf.append(")");
            if ("TERADATA".equals(dialect.getDialectName())) {
                buf.append(" ON ").append(dialect.quoteIdentifier(table.getSchemaName(), table.getTableName()));
            }
            final String createDDL = buf.toString();
            return createDDL;

        }).toList();
    }
}

