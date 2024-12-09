/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
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

package org.eclipse.daanse.olap.calc.api;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;

/**
 * <code>Calc</code> is the base class for all calculable expressions.
 *
 * <h3>Logical and physical expression languages</h3>
 *
 * Mondrian has two expression languages:<ul>
 * <li>The logical language of parsed MDX fragments ({@link org.eclipse.daanse.olap.api.query.component.Expression}).
 * <li>The phyiscal language of compiled expressions ({@link Calc}).
 * </ul></p>
 *
 * The two languages allow us to separate logical (how an
 * MDX expression was specified) from physical (how it is to be evaluated).
 * The physical language is more strongly typed, and certain constructs which
 * are implicit in the logical language (such as the addition of calls
 * to the <code>&lt;Member&gt;.CurrentMember</code> function) are made
 * explicit in the physical language.<p/>
 *
 * <h3>Compilation</h3>
 *
 * Expressions are generally created from using an expression compiler
 * ({@link ExpressionCompiler}). There are often more than one evaluation strategy
 * for a given expression, and compilation process gives us an opportunity to
 * choose the optimal one.<p/>
 *
 *
 *
 * @author jhyde
 * @since Sep 26, 2005
 */
public interface Calc<E> {
    /**
     * Evaluates this expression.
     *
     * @param evaluator Provides dimensional context in which to evaluate
     *                  this expression
     * @return Result of expression evaluation
     */
    E evaluate(Evaluator evaluator);


    /**
     * Returns whether this expression depends upon a given hierarchy.
     *
     * <p>If it does not depend on the hierarchy, then re-evaluating the
     * expression with a different member of this context must produce the
     * same answer.<p/>
     *
     * Some examples:<ul>
     *
     * <li>The expression
     * <blockquote><code>[Measures].[Unit Sales]</code></blockquote>
     * depends on all dimensions except <code>[Measures]</code>.
     *
     * <li>The boolean expression
     * <blockquote><code>([Measures].[Unit Sales],
     * [Time].[1997]) &gt; 1000</code></blockquote>
     * depends on all hierarchies except [Measures] and [Time].
     *
     * <li>The list expression
     * <blockquote><code>Filter([Store].[USA].Children,
     * [Measures].[Unit Sales] &lt; 50)</code></pre></blockquote>
     * depends upon all hierarchies <em>except</em> [Store] and [Measures].
     * How so? Normally the scalar expression would depend upon all hierarchies
     * except [Measures], but the <code>Filter</code> function sets the [Store]
     * context before evaluating the scalar expression, so it is not inherited
     * from the surrounding context.
     *
     * </ul><p/>
     *
     * @param hierarchy Hierarchy
     * @return Whether this expression's result depends upon the current member
     *   of the hierarchy
     */
    boolean dependsOn(Hierarchy hierarchy);

    /**
     * Returns the type.
     */
    Type getType();

    /**
     * Returns style in which the result of evaluating this expression is
     * returned.
     *
     * <p>One application of this method is for the compiler to figure out
     * whether the compiled expression is returning a mutable list. If a mutable
     * list is required, the compiler can create a mutable copy.
     *
     * @see ExpressionCompiler#compileList(org.eclipse.daanse.olap.api.query.component.Expression, boolean)
     */
    ResultStyle getResultStyle();

}
