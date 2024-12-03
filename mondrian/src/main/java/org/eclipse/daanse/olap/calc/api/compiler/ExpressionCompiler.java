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
 *   Stefan Bischof (bipolis.org) - initial
 */


package org.eclipse.daanse.olap.calc.api.compiler;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DateTimeCalc;
import org.eclipse.daanse.olap.calc.api.DimensionCalc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;

/**
 * Mediates the compilation of an expression ({@link org.eclipse.daanse.olap.api.query.component.Expression})
 * into a compiled expression ({@link Calc}).
 *
 * @author jhyde
 * @since Sep 28, 2005
 */
public interface ExpressionCompiler {

    /**
     * Returns the evaluator to be used for evaluating expressions during the
     * compilation process.
     */
    Evaluator getEvaluator();

    /**
     * Returns the validator which was used to validate this expression.
     *
     * @return validator
     */
    Validator getValidator();

    /**
     * Compiles an expression.
     *
     * @param exp Expression
     * @return Compiled expression
     */
    Calc<?> compile(Expression exp);

    /**
     * Compiles an expression to a given result type.
     *
     * <p>If <code>resultType</code> is not null, casts the expression to that
     * type. Throws an exception if that conversion is not allowed by the
     * type system.
     *
     * <p>The <code>preferredResultStyles</code> parameter specifies a list
     * of desired result styles. It must not be null, but may be empty.
     *
     * @param exp Expression
     *
     * @param resultType Desired result type, or null to use expression's
     *                   current type
     *
     * @param preferredResultStyles List of result types, in descending order
     *                   of preference. Never null.
     *
     * @return Compiled expression, or null if none can satisfy
     */
    Calc<?> compileAs(
            Expression exp,
            Type resultType,
            List<ResultStyle> preferredResultStyles);

    /**
     * Compiles an expression which yields a {@link Member} result.
     */
    MemberCalc compileMember(Expression exp);

    /**
     * Compiles an expression which yields a {@link Level} result.
     */
    LevelCalc compileLevel(Expression exp);

    /**
     * Compiles an expression which yields a {@link Dimension} result.
     */
    DimensionCalc compileDimension(Expression exp);

    /**
     * Compiles an expression which yields a {@link Hierarchy} result.
     */
    HierarchyCalc compileHierarchy(Expression exp);

    /**
     * Compiles an expression which yields an <code>int</code> result.
     * The expression is implicitly converted into a scalar.
     */
    IntegerCalc compileInteger(Expression exp);

    /**
     * Compiles an expression which yields a {@link String} result.
     * The expression is implicitly converted into a scalar.
     */
    StringCalc compileString(Expression exp);

    /**
     * Compiles an expression which yields a {@link java.util.Date} result.
     * The expression is implicitly converted into a scalar.
     */
    DateTimeCalc compileDateTime(Expression exp);

    /**
     * Compiles an expression which yields an immutable {@link TupleList}
     * result.
     *
     * <p>Always equivalent to <code>{@link #compileList}(exp, false)</code>.
     */
    TupleListCalc compileList(Expression exp);

    /**
     * Compiles an expression which yields {@link TupleList} result.
     *
     * <p>Such an expression is generally a list of {@link Member} objects or a
     * list of tuples (each represented by a {@link Member} array).
     *
     * <p>See {@link #compileList(org.eclipse.daanse.olap.api.query.component.Expression)}.
     *
     * @param exp Expression
     * @param mutable Whether resulting list is mutable
     */
    TupleListCalc compileList(Expression exp, boolean mutable);

    /**
     * Compiles an expression which yields an immutable {@link Iterable} result.
     *
     * @param exp Expression
     * @return Calculator which yields an Iterable
     */
    TupleIteratorCalc compileIter(Expression exp);

    /**
     * Compiles an expression which yields a <code>boolean</code> result.
     *
     * @param exp Expression
     * @return Calculator which yields a boolean
     */
    BooleanCalc compileBoolean(Expression exp);

    /**
     * Compiles an expression which yields a <code>double</code> result.
     *
     * @param exp Expression
     * @return Calculator which yields a double
     */
    DoubleCalc compileDouble(Expression exp);

    /**
     * Compiles an expression which yields a tuple result.
     *
     * @param exp Expression
     * @return Calculator which yields a tuple
     */
    TupleCalc compileTuple(Expression exp);

    /**
     * Compiles an expression to yield a scalar result.
     *
     * <p>If the expression yields a member or tuple, the calculator will
     * automatically apply that member or tuple to the current dimensional
     * context and return the value of the current measure.
     *
     * @param exp Expression
     * @param specific Whether to try to use the specific compile method for
     *   scalar types. For example, if <code>specific</code> is true and
     *   <code>exp</code> is a string expression, calls
     *   {@link #compileString(org.eclipse.daanse.olap.api.query.component.Expression)}
     * @return Calculation which returns the scalar value of the expression
     */
    Calc<?> compileScalar(Expression exp, boolean specific);

    /**
     * Implements a parameter, returning a unique slot which will hold the
     * parameter's value.
     *
     * @param parameter Parameter
     * @return Slot
     */
    ParameterSlot registerParameter(Parameter parameter);

    /**
     * Returns a list of the {@link ResultStyle}s
     * acceptable to the caller.
     */
    List<ResultStyle> getAcceptableResultStyles();

}
