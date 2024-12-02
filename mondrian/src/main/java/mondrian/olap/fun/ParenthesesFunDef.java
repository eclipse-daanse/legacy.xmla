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

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.query.base.Expressions;

import mondrian.olap.Util;

/**
 * <code>ParenthesesFunDef</code> implements the parentheses operator as if it
 * were a function.
 *
 * @author jhyde
 * @since 3 March, 2002
 */
public class ParenthesesFunDef extends AbstractFunctionDefinition {
    
	static OperationAtom functionAtom = new ParenthesesOperationAtom();

	public ParenthesesFunDef(DataType argType) {
		super(new FunctionMetaDataR(functionAtom, "Parenthesis enclose an expression and indicate precedence.",
				"(<Expression>)",  argType, new DataType[] { argType }));

	}
	
    @Override
	public void unparse(Expression[] args, PrintWriter pw) {
        if (args.length != 1) {
        	Expressions.unparseExpressions(pw, args, "(", ",", ")");
        } else {
            // Don't use parentheses unless necessary. We add parentheses around
            // expressions because we're not sure of operator precedence, so if
            // we're not careful, the parentheses tend to multiply ad infinitum.
            args[0].unparse(pw);
        }
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        Util.assertTrue(args.length == 1);
        return args[0].getType();
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        return compiler.compile(call.getArg(0));
    }
}
