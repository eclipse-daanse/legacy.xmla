/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*
* Contributors:
*   SmartCity Jena - major API, docs, code-quality changes
*   Stefan Bischof (bipolis.org)
*/

package org.eclipse.daanse.db.statistics.query;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import javax.sql.DataSource;

import org.eclipse.daanse.db.statistics.api.StatisticsProvider;
import org.eclipse.daanse.jdbc.db.api.schema.CatalogReference;
import org.eclipse.daanse.jdbc.db.api.schema.ColumnReference;
import org.eclipse.daanse.jdbc.db.api.schema.SchemaReference;
import org.eclipse.daanse.jdbc.db.api.schema.TableReference;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.jdbc.db.dialect.api.DialectResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link StatisticsProvider} that generates SQL queries to
 * count rows and distinct values.
 */
@Component(service = StatisticsProvider.class, scope = ServiceScope.PROTOTYPE)
public class SqlStatisticsProvider implements StatisticsProvider {
	private static final int FIRST_ROW = 1;

	public static final Logger LOGGER = LoggerFactory.getLogger(SqlStatisticsProvider.class);

	@Reference
	private DialectResolver dialectResolver;

	@Override
	public long getQueryCardinality(Connection connection, String sql) {
		try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql);) {
			if (resultSet.next()) {
				return resultSet.getLong(FIRST_ROW);
			}
			return CARDINALITY_UNKNOWN;
		} catch (SQLException e) {
			throw new SqlStatisticsProviderException(e);
		}
	}

	@Override
	public long getTableCardinality(DataSource dataSource, TableReference tableReference) {
		try (Connection connection = dataSource.getConnection()) {
			return getTableCardinality(connection, tableReference);
		} catch (SQLException e) {
			throw new SqlStatisticsProviderException(e);
		}
	}

	@Override
	public long getTableCardinality(Connection connection, TableReference tableReference) {
		Optional<Dialect> oDialect = this.dialectResolver.resolve(connection);
		String schema = null;
		String table = null;
		String catalog = null;
		table = tableReference.name();
		Optional<SchemaReference> oSchema = tableReference.schema();
		if (oSchema.isPresent()) {
			schema = oSchema.get().name();
			Optional<CatalogReference> oCatalog = oSchema.get().catalog();
			if (oCatalog.isPresent()) {
				catalog = oCatalog.get().name();
			}
		}

		if (oDialect.isPresent()) {
			Dialect dialect = oDialect.get();
			StringBuilder stringBuilder = new StringBuilder("select count(*) from ");
			dialect.quoteIdentifier(stringBuilder, catalog, schema, table);
			final String sql = stringBuilder.toString();
			return getQueryCardinality(connection, sql);
		} else {
			throw new SqlStatisticsProviderException("not able to resolve dialect");
		}

	}

	@Override
	public long getQueryCardinality(DataSource dataSource, String sql) {
		try (Connection connection = dataSource.getConnection()) {
			return getQueryCardinality(connection, sql);
		} catch (SQLException e) {
			throw new SqlStatisticsProviderException(e);
		}
	}

	@Override
	public long getColumnCardinality(DataSource dataSource, ColumnReference column) {
		try (Connection connection = dataSource.getConnection()) {
			return getColumnCardinality(connection, column);
		} catch (SQLException e) {
			throw new SqlStatisticsProviderException(e);
		}
	}

	@Override
	public long getColumnCardinality(Connection connection, ColumnReference column) {
		final Optional<String> oSql = generateColumnCardinalitySql(connection, column);

		return oSql.map(s -> getQueryCardinality(connection, s)).orElse(CARDINALITY_UNKNOWN);
	}

	private Optional<String> generateColumnCardinalitySql(Connection connection, ColumnReference col) {
		final StringBuilder buf = new StringBuilder();
		Optional<Dialect> oDialect = this.dialectResolver.resolve(connection);
		String schema = null;
		String table = null;
		Optional<TableReference> oTable = col.table();
		if (oTable.isPresent()) {
			TableReference tableReference = oTable.get();
			table = tableReference.name();
			Optional<SchemaReference> oSchema = tableReference.schema();
			if (oSchema.isPresent()) {
				schema = oSchema.get().name();
			}
		}
		if (oDialect.isPresent()) {
			Dialect dialect = oDialect.get();
			StringBuilder exprStringBuilder = dialect.quoteIdentifier(col.name());
			if (dialect.allowsCountDistinct()) {
				// e.g. "select count(distinct product_id) from product"
				buf.append("select count(distinct ").append(exprStringBuilder).append(") from ");
				dialect.quoteIdentifier(buf, schema, table);
				return Optional.of(buf.toString());
			} else if (dialect.allowsFromQuery()) {
				// Some databases (e.g. Access) don't like 'count(distinct)',
				// so use, e.g., "select count(*) from (select distinct
				// product_id from product)"
				buf.append("select count(*) from (select distinct ").append(exprStringBuilder).append(" from ");
				dialect.quoteIdentifier(buf, schema, table);
				buf.append(")");
				if (dialect.requiresAliasForFromQuery()) {
					if (dialect.allowsAs()) {
						buf.append(" as ");
					} else {
						buf.append(' ');
					}
					dialect.quoteIdentifier(buf, "init");
				}
				return Optional.of(buf.toString());
			} else {
				LOGGER.warn(
						"Cannot compute cardinality: this database neither supports COUNT DISTINCT nor SELECT in the FROM clause.");
				return Optional.empty();
			}
		} else {
			throw new SqlStatisticsProviderException("not able to resolve dialect");
		}
	}
}
