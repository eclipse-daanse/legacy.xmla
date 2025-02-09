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

import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.parser.api.MdxParser;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.ExpressionProvider;
import org.eclipse.daanse.olap.api.query.QueryProvider;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.query.base.ExpressionProviderImpl;
import org.eclipse.daanse.olap.query.base.QueryProviderImpl;
import org.slf4j.Logger;

import mondrian.olap.exceptions.FailedToParseQueryException;


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

    protected ConnectionBase() {
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
     * @param query MDX query that requires special parsing
     * @param funTable Customized function table to use in parsing
     * @param strictValidation If true, do not ignore invalid members
     * @return Query the corresponding Query object if parsing is successful
     * @throws OlapRuntimeException if parsing fails
     */
    public QueryComponent parseStatement(
        Statement statement,
        String query,
        FunctionService funTable,
        boolean strictValidation)
    {
        boolean debug = false;

        if (getLogger().isDebugEnabled()) {
            String s = new StringBuilder().append(Util.NL).append(query.replaceAll("[\n\r]", "_")).toString();
            getLogger().debug(s);
        }

        try {
            MdxParser parser = getContext().getMdxParserProvider().newParser(query, funTable.getPropertyWords());
            MdxStatement mdxStatement  = parser.parseMdxStatement();
            return getQueryProvider().createQuery(statement, mdxStatement, strictValidation);
        } catch (Exception e) {
            throw new FailedToParseQueryException(query, e);
        }
    }

    public QueryProvider getQueryProvider() {
        return queryProvider;
    }

    public ExpressionProvider getExpressionProvider() {
        return expressionProvider;
    }

}
