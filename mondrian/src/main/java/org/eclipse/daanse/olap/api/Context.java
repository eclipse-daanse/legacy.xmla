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
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.olap.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.monitor.EventBus;
import org.eclipse.daanse.olap.api.query.QueryProvider;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.rolap.core.BasicContextConfig;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;

/**
 * The {@link Context} gives access to all resources and configurations that are needed
 * to calculate and Data Cubes
 *
 * @author stbischof
 *
 */
public interface Context {

    /**
     * Gives access to the {@link javax.sql.DataSource} that holds the {@link java.sql.Connection}s to
     * the Database.
     *
     * @return DataSource
     */
    DataSource getDataSource();

    /**
     * Gives access to the {@link Dialect} that must be used to generate SQL querys
     * against the {@link javax.sql.DataSource}.
     *
     * @return DataSource
     */
    Dialect getDialect();

    //TODO REMOVE HERE. Only In RolapContext
    CatalogMapping getCatalogMapping();


    /**
     * Gives access to a {@link QueryProvider}.
     *
     * @return {@link QueryProvider}.
     */
//    QueryProvider getQueryProvider();
	/*
	 * The human readable name of the Context. By default the name of the Schema.
	 * May be overridden.
	 */
	String getName();

	/*
	 * The human readable description of the Context. By default the getDescription
	 * of the Schema. May be overridden.
	 */
	Optional<String> getDescription();

	ExpressionCompilerFactory getExpressionCompilerFactory();

	/*
	 * Gives access to the {@link Connection}.
	 */
	Connection getConnection();

    Connection getConnection(ConnectionProps props);

    Scenario createScenario();

	void addConnection(Connection rolapConnection);

	void removeConnection(Connection rolapConnection);

	ResultShepherd getResultShepherd();

//	AggregationManager getAggregationManager();

	void addStatement(Statement statement);

	void removeStatement(Statement internalStatement);

	EventBus getMonitor();

	List<Statement> getStatements(Connection connection);

    BasicContextConfig getConfig();

    Semaphore getQueryLimitSemaphore();

	Optional<Map<Object, Object>> getSqlMemberSourceValuePool();

    FunctionService getFunctionService();

    MdxParserProvider getMdxParserProvider();

}
