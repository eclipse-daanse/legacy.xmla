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
package org.eclipse.daanse.olap.api.query.component;

import org.eclipse.daanse.olap.api.SubtotalVisibility;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;

public non-sealed interface QueryAxis extends QueryComponent {

    String getAxisName();

    AxisOrdinal getAxisOrdinal();

    boolean isNonEmpty();

    void setNonEmpty(boolean nonEmpty);

    Expression getSet();

    void setSet(Expression set);

    Calc compile(ExpressionCompiler compiler, ResultStyle resultStyle);

    Object accept(QueryComponentVisitor visitor);

    Id[] getDimensionProperties();

    SubtotalVisibility getSubtotalVisibility();

    void validate(Validator validator);

    void addLevel(Level level);

    void resolve(Validator validator);

    boolean isOrdered();

    void setOrdered(boolean ordered);

}
