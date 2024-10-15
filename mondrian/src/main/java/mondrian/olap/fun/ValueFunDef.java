/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.io.PrintWriter;

import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;
import org.eclipse.daanse.olap.query.base.Expressions;

/**
 * A <code>ValueFunDef</code> is a pseudo-function to evaluate a member or a
 * tuple. Similar to {@link TupleFunDef}.
 *
 * @author jhyde
 * @since Jun 14, 2002
 */
class ValueFunDef extends AbstractFunctionDefinition {
	private final DataType[] argTypes;

	ValueFunDef(DataType[] argTypes) {
		super(
//				new ParenthesesOperationAtom("Value")
				new FunctionMetaDataR(new ParenthesesOperationAtom(), "Pseudo-function which evaluates a tuple.",
						"_Value([<Member>, ...])", DataType.NUMERIC, argTypes));
		this.argTypes = argTypes;
	}

	@Override
	public void unparse(Expression[] args, PrintWriter pw) {
		Expressions.unparseExpressions(pw, args, "(", ", ", ")");
	}

	@Override
	public Type getResultType(Validator validator, Expression[] args) {
		return null;
	}

	@Override
	public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		throw new UnsupportedOperationException();
	}

}
