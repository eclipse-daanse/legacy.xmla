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
package org.eclipse.daanse.olap.query.component;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Map;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.query.component.NumericLiteral;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDoubleCalc;

public class NumericLiteralImpl extends AbstractLiteralImpl<BigDecimal> implements NumericLiteral {
	private static final NumericLiteralImpl negativeOne = new NumericLiteralImpl(BigDecimal.ONE.negate());
	private static final NumericLiteralImpl one = new NumericLiteralImpl(BigDecimal.ONE);
	public static final NumericLiteralImpl zero = new NumericLiteralImpl(BigDecimal.ZERO);
	private static final Map<BigDecimal, NumericLiteralImpl> MAP = Map.of(BigDecimal.ZERO, zero,
			BigDecimal.ONE, one, BigDecimal.ONE.negate(), negativeOne);

	private NumericLiteralImpl(BigDecimal bigDecimal) {
		super(bigDecimal);
	}

	/**
	 * Creates a numeric literal.
	 *
	 * <p>
	 * Using a {@link BigDecimal} allows us to store the precise value that the user
	 * typed. We will have to fit the value into a native {@code double} or
	 * {@code int} later on, but parse time is not the time to be throwing away
	 * information.
	 */
	public static NumericLiteralImpl create(BigDecimal bigDecimal) {
		final NumericLiteralImpl literal = MAP.get(bigDecimal);
		if (literal != null) {
			return literal;
		}
		return new NumericLiteralImpl(bigDecimal);
	}

	@Override
	public Object accept(QueryComponentVisitor visitor) {
		return visitor.visitLiteral(this);
	}

	@Override
	public DataType getCategory() {
		return DataType.NUMERIC;
	}

	@Override
	public Type getType() {
		return NumericType.INSTANCE;
	}

	@Override
	public void unparse(PrintWriter pw) {
		pw.print(getValue());
	}

	@Override
	public Calc<?> accept(ExpressionCompiler compiler) {
		return new ConstantDoubleCalc(NumericType.INSTANCE, getValue().doubleValue());
	}

	@Override
	public int getIntValue() {
		return getValue().intValue();
	}
}
