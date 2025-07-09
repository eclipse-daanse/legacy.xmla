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

package org.eclipse.daanse.olap.common;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.parser.api.MdxParser;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.element.DatabaseSchema;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.ExpressionProvider;
import org.eclipse.daanse.olap.api.query.QueryProvider;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.exceptions.FailedToParseQueryException;
import org.eclipse.daanse.olap.guard.DatabaseCatalogImpl;
import org.eclipse.daanse.olap.query.base.ExpressionProviderImpl;
import org.eclipse.daanse.olap.query.base.QueryProviderImpl;
import org.eclipse.daanse.sql.guard.api.SqlGuard;
import org.eclipse.daanse.sql.guard.api.SqlGuardFactory;
import org.eclipse.daanse.sql.guard.api.exception.GuardException;
import org.eclipse.daanse.sql.guard.api.exception.UnparsableStatementGuardException;
import org.slf4j.Logger;


/**
 * <code>ConnectionBase</code> implements some of the methods in
 * {@link Connection}.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public abstract class ConnectionBase implements Connection {

    QueryProvider queryProvider = new QueryProviderImpl();

    ExpressionProvider expressionProvider = new ExpressionProviderImpl();

    private Optional<SqlGuard> oSqlGuard = Optional.empty();

    protected ConnectionBase() {
//        getContext().getSqlGuardFactory();
    }

    protected abstract Logger getLogger();


    @Override
	public Query parseQuery(String query) {
        return (Query) parseStatement(query);
    }

    /**
     * Parses a query, with specified function table and the mode for strict
     * validation(if true then invalid members are not ignored).
     *
     * <p>This method is only used in testing and by clients that need to
     * support customized parser behavior. That is why this method is not part
     * of the Connection interface.
     *
     * <p>See test case mondrian.olap.CustomizedParserTest.
     *
     * @param statement Evaluation context
     * @param queryToParse MDX query that requires special parsing
     * @param funTable Customized function table to use in parsing
     * @param strictValidation If true, do not ignore invalid members
     * @return Query the corresponding Query object if parsing is successful
     * @throws OlapRuntimeException if parsing fails
     */
    public QueryComponent parseStatement(
        Statement statement,
        String queryToParse,
        FunctionService funTable,
        boolean strictValidation)
    {
        boolean debug = false;

        if (getLogger().isDebugEnabled()) {
            String s = new StringBuilder().append(Util.NL).append(queryToParse.replaceAll("[\n\r]", "_")).toString();
            getLogger().debug(s);
        }

        MdxParser parser;
        try {
           parser = getContext().getMdxParserProvider().newParser(queryToParse, funTable.getPropertyWords());
           MdxStatement mdxStatement = parser.parseMdxStatement();
           return getQueryProvider().createQuery(statement, mdxStatement, strictValidation);
        } catch (MdxParserException mdxPE) {
            try {
                parser = getContext().getMdxParserProvider().newParser(queryToParse, Set.of());
                MdxStatement mdxStatement = parser.parseDMVStatement();
                return getQueryProvider().createQuery(statement, mdxStatement, strictValidation);
            } catch (MdxParserException mdxPE1) {
                Optional<SqlGuardFactory> oSqlGuardFactory = getContext().getSqlGuardFactory();
                if (oSqlGuardFactory.isEmpty()) {
                    throw new FailedToParseQueryException(queryToParse, mdxPE);
                } else {
                    List<DatabaseSchema> ds = (List<DatabaseSchema>) this.getCatalogReader().getDatabaseSchemas();
                    org.eclipse.daanse.sql.guard.api.elements.DatabaseCatalog dc = new DatabaseCatalogImpl("", ds);
                    SqlGuard guard = oSqlGuardFactory.get().create("", "", dc, List.of(), this.getContext().getDialect() ); //TODO add white list functions
                    try {
                        String sanetizedSql= guard.guard(queryToParse);
                        return new SqlQueryImpl(sanetizedSql, getContext().getDataSource());
                    } catch (UnparsableStatementGuardException uex) {
                        // when can´t be parse then we decide to throw the exception of MDX
                        throw new FailedToParseQueryException(queryToParse, mdxPE);
                    } catch (GuardException guasdEx) {
                        throw new FailedToParseQueryException(queryToParse, guasdEx);
                    }
                }
            }
        }
    }



    public QueryProvider getQueryProvider() {
        return queryProvider;
    }

    public ExpressionProvider getExpressionProvider() {
        return expressionProvider;
    }

}
