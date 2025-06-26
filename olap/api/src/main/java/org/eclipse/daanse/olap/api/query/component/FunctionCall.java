/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1998-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
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

package org.eclipse.daanse.olap.api.query.component;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;

/**
 * A <code>FunCall</code> is a function applied to a list of operands.
 *
 * <p>The parser creates function calls as an
 * {@link mondrian.mdx.UnresolvedFunCallImpl unresolved  function call}.
 * The validator converts it to a
 * {@link  mondrian.mdx.ResolvedFunCallImpl resolved function call},
 * which has a {@link FunctionDefinition function definition} and extra type information.
 *
 * @author jhyde
 * @since Jan 6, 2006
 */
public interface FunctionCall extends Expression {
    /**
     * Returns the <code>index</code><sup>th</sup> argument to this function
     * call.
     *
     * @param index Ordinal of the argument
     * @return <code>index</code><sup>th</sup> argument to this function call
     */
    Expression getArg(int index);

    /**
     * Returns the arguments to this function.
     *
     * @return array of arguments
     */
    Expression[] getArgs();

    /**
     * Returns the number of arguments to this function.
     *
     * @return number of arguments
     */
    int getArgCount();

    /**
     * Returns the OperationAtom.
     */
    OperationAtom getOperationAtom();


}
