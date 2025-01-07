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
package mondrian.olap;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.query.component.StringLiteral;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantStringCalc;

public class StringLiteralImpl extends AbstractLiteralImpl<String> implements StringLiteral {
    public static final StringLiteralImpl EMPTY_STRING_LITERAL = new StringLiteralImpl("");
    private static final StringType TYPE=StringType.INSTANCE;
	protected StringLiteralImpl(String text) {
		super(text);
	}

	public static StringLiteralImpl create(String text) {
		return ("".equals(text)) ? EMPTY_STRING_LITERAL : new StringLiteralImpl(text);
	}

	@Override
	public Object accept(QueryComponentVisitor visitor) {
		return visitor.visitLiteral(this);
	}

	@Override
	public DataType getCategory() {
		return DataType.STRING;
	}

	@Override
	public Type getType() {
		return TYPE;
	}

	@Override
	public void unparse(PrintWriter pw) {
		pw.print(Util.quoteForMdx(getValue()));
	}

	@Override
	public Calc<String> accept(ExpressionCompiler compiler) {
		return new ConstantStringCalc(TYPE, getValue());
	}

}
