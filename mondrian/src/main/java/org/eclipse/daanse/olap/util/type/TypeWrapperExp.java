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
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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

package org.eclipse.daanse.olap.util.type;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.Type;

/**
 * TypeWrappingExp expression which exists only to wrap a
 * {@link org.eclipse.daanse.olap.api.type.Type}.
 *
 * @author jhyde
 * @since Sep 26, 2005
 */
public class TypeWrapperExp implements Expression {
    private final Type type;

    public TypeWrapperExp(Type type) {
        this.type = type;
    }

    @Override
    public TypeWrapperExp cloneExp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataType getCategory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void unparse(PrintWriter pw) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Expression accept(Validator validator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Calc<?> accept(ExpressionCompiler compiler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object accept(QueryComponentVisitor visitor) {
        throw new UnsupportedOperationException();
    }

}
