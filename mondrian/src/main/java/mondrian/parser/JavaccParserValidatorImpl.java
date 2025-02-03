/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.parser;

import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;

import mondrian.olap.FactoryImpl;

/**
 * Default implementation of {@link MdxParserValidator}, using the
 * <a href="http://java.net/projects/javacc/">JavaCC</a> parser generator.
 *
 * @author jhyde
 */
public class JavaccParserValidatorImpl implements MdxParserValidator {
    private final QueryComponentFactory factory;

    /**
     * Creates a JavaccParserValidatorImpl.
     */
    public JavaccParserValidatorImpl() {
        this(new FactoryImpl());
    }

    /**
     * Creates a JavaccParserValidatorImpl with an explicit factory for parse
     * tree nodes.
     *
     * @param factory Factory for parse tree nodes
     */
    public JavaccParserValidatorImpl(QueryComponentFactory factory) {
        this.factory = factory;
    }

    @Override
    public QueryComponent parseInternal(
        Statement statement,
        String queryString,
        boolean debug,
        FunctionService functionService,
        boolean strictValidation
    ) {
        return null;
    }

    @Override
    public Expression parseExpression(
        Statement statement,
        String queryString,
        boolean debug,
        FunctionService functionService
    ) {
        return null;
    }
}
