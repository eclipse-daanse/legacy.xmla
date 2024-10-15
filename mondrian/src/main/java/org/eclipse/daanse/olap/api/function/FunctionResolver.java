/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2002-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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

package org.eclipse.daanse.olap.api.function;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;

/**
 * A {@link FunctionResolver} converts a function name, invocation type, and set
 * of arguments into a {@link FunctionDefinition}.
 */
public interface FunctionResolver {

    public OperationAtom getFunctionAtom();

    /**
     * Given a particular set of arguments the function is applied to, returns the
     * correct overloaded form of the function.
     *
     * <p>
     * The method adds an item to <code>conversions</code> every time it performs an
     * implicit type-conversion. If there are several candidate functions with the
     * same signature, the validator will choose the one which used the fewest
     * implicit conversions.
     * </p>
     *
     * @param args        Expressions which this function call is applied to.
     * @param validator   Validator
     * @param conversions List of implicit conversions performed (out)
     *
     * @return The function definition which matches these arguments, or null if no
     *         function definition that this resolver knows about matches.
     */
    FunctionDefinition resolve(Expression[] args, Validator validator, List<Conversion> conversions);

    /**
     * iIndicated whether a argument with a given <code>positionOfArgument</code>
     * must be a scalar expression. Returns <code>false</code> if any of the
     * variants of this resolver allows a set as its
     * <code>positionOfArgument</code>th argument; true otherwise.
     * 
     * if the ( positionOfArgument < 1 ) implementations must return false;
     * 
     */
    boolean requiresScalarExpressionOnArgument(int positionOfArgument);

    /**
     * Returns a {@link List} of symbolic constants which can appear as arguments to
     * this function.
     *
     * <p>
     * For example, the <code>DrilldownMember</code> may take the symbol
     * <code>RECURSIVE</code> as an argument. Most functions do not define any
     * symbolic constants.
     *
     * @return An {@link List} of the names of the symbolic constants
     */
    default List<String> getReservedWords() {
        return List.of();
    }

    default List<FunctionMetaData> getRepresentativeFunctionMetaDatas() {
        return List.of();
    }

    /**
     * Description of an implicit conversion that occurred while resolving an
     * operator call.
     */
    public interface Conversion {
        /**
         * Returns the cost of the conversion. If there are several matching overloads,
         * the one with the lowest overall cost will be preferred.
         *
         * @return Cost of conversion
         */
        int getCost();

        /**
         * Checks the viability of implicit conversions. Converting from a dimension to
         * a hierarchy is valid if is only one hierarchy.
         */
        void checkValid();

        /**
         * Applies this conversion to its argument, modifying the argument list in
         * place.
         *
         * @param validator Validator
         * @param args      Argument list
         */
        void apply(Validator validator, List<Expression> args);
    }
}
