/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap;

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
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.query.base.ExpressionProviderImpl;
import org.eclipse.daanse.olap.query.base.QueryProviderImpl;
import org.eclipse.daanse.olap.query.component.QueryImpl;
import org.eclipse.daanse.sql.guard.api.SqlGuard;
import org.eclipse.daanse.sql.guard.api.SqlGuardFactory;
import org.eclipse.daanse.sql.guard.api.elements.DatabaseCatalog;
import org.eclipse.daanse.sql.guard.api.exception.GuardException;
import org.eclipse.daanse.sql.guard.api.exception.UnparsableStatementGuardException;
import org.slf4j.Logger;

import mondrian.olap.exceptions.FailedToParseQueryException;
import mondrian.olap.guard.DatabaseCatalogImpl;


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
	public QueryImpl parseQuery(String query) {
        return (QueryImpl) parseStatement(query);
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

        try {
            MdxParser parser = getContext().getMdxParserProvider().newParser(queryToParse, funTable.getPropertyWords());
            try {
               MdxStatement mdxStatement = parser.parseMdxStatement();
               return getQueryProvider().createQuery(statement, mdxStatement, strictValidation);
            } catch (MdxParserException mdxPE) {
                parser = getContext().getMdxParserProvider().newParser(queryToParse, Set.of());
                MdxStatement mdxStatement = parser.parseDMVStatement();
                return getQueryProvider().createQuery(statement, mdxStatement, strictValidation);
            }
        } catch (MdxParserException mdxPE) {
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



    public QueryProvider getQueryProvider() {
        return queryProvider;
    }

    public ExpressionProvider getExpressionProvider() {
        return expressionProvider;
    }

}
