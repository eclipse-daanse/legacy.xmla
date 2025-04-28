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

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.query.component.NullLiteral;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.constant.ConstantStringCalc;

import mondrian.olap.AbstractLiteralImpl;

public class NullLiteralImpl extends AbstractLiteralImpl<Object> implements NullLiteral {

	public static final NullLiteralImpl nullValue = new NullLiteralImpl();
	private static final ConstantStringCalc NULL_CALC = new ConstantStringCalc(NullType.INSTANCE, null);

	private NullLiteralImpl() {
		super(null);
	}

	@Override
	public Object accept(QueryComponentVisitor visitor) {
		return visitor.visitLiteral(this);
	}

	@Override
	public DataType getCategory() {
		return DataType.NULL;
	}

	@Override
	public Type getType() {
		return NullType.INSTANCE;
	}

	@Override
	public Calc<?> accept(ExpressionCompiler compiler) {
		return NULL_CALC;
	}

	@Override
	public void unparse(PrintWriter pw) {
		pw.print("NULL");
	}

}
