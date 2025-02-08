/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2005-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara
 * All Rights Reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.MemberProperty;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.type.Type;

/**
 * Provides context necessary to resolve identifiers to objects, function
 * calls to specific functions.
 *
 * <p>An expression calls {@link #validate} on each of its children,
 * which in turn calls {@link Expression#accept}.
 *
 * @author jhyde
 */
public interface Validator {
    /**
     * Returns the {@link Query} which is being validated.
     */
    Query getQuery();

    /**
     * Validates an expression, and returns the expression it resolves to.
     *
     * @param exp Expression to validate
     * @param scalar Whether the context requires that the expression is
     *   evaluated to a value, as opposed to a tuple
     */
    Expression validate(Expression exp, boolean scalar);

    /**
     * Validates a usage of a parameter.
     *
     * <p>It must resolve to the same object (although sub-objects may change).
     */
    void validate(ParameterExpression parameterExpr);

    /**
     * Validates a child member property.
     *
     * <p>It must resolve to the same object (although sub-objects may change).
     */
    void validate(MemberProperty memberProperty);

    /**
     * Validates an axis.
     *
     * It must resolve to the same object (although sub-objects may change).
     */
    void validate(QueryAxis axis);

    /**
     * Validates a formula.
     *
     * It must resolve to the same object (although sub-objects may change).
     */
    void validate(Formula formula);

    /**
     * Returns whether the current context requires an expression.
     */
    boolean requiresExpression();

    /**
     * Returns whether we can convert an argument to a parameter type.
     *
     * @param ordinal argument ordinal
     * @param fromExp argument type
     * @param to   parameter type
     * @param conversions List of conversions performed;
     *             method adds an element for each non-trivial conversion (for
     *             example, converting a member to a level).
     * @return Whether we can convert an argument to a parameter type
     */
    boolean canConvert(
        int ordinal,
        Expression fromExp,
        DataType to,
        List<FunctionResolver.Conversion> conversions);

    /**
     * Returns the table of function and operator definitions.
     */
    FunctionService getFunctionService();

    /**
     * Creates or retrieves the parameter corresponding to a "Parameter" or
     * "ParamRef" function call.
     */
    Parameter createOrLookupParam(
        boolean definition,
        String name,
        Type type,
        Expression defaultExp,
        String description);

    /**
     * Resolves a function call to a particular function. If the function is
     * overloaded, returns as precise a match to the argument types as
     * possible.
     */
    FunctionDefinition getDef(
        Expression[] args,
        OperationAtom operationAtom );

    /**
     * Whether to resolve function name and arguments to a function definition
     * each time a node is validated, not just the first time.
     *
     * <p>Default implementation returns {@code false}.
     *
     * @return whether to resolve function each time
     */
    boolean alwaysResolveFunDef();

    /**
     * Returns the schema reader with which to resolve names of MDX objects
     * (dimensions, hierarchies, levels, members, named sets).
     *
     * <p>The schema reader is initially in the context of the query's cube,
     * and during a traversal it may change if named sets are introduced using
     * the 'expr AS alias' construct.
     *
     * @return Schema reader
     */
    CatalogReader getCatalogReader();
}
