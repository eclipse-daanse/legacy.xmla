/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.mdx;

import java.io.PrintWriter;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.UnresolvedFunCall;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionPrinter;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;

import mondrian.olap.fun.FunUtil;

/**
 * An expression consisting of a named function or operator
 * applied to a set of arguments. The syntax determines whether this is
 * called infix, with function call syntax, and so forth.
 *
 * @author jhyde
 * @since Sep 28, 2005
 */
public class UnresolvedFunCallImpl extends AbstractExpression implements UnresolvedFunCall {
    private final OperationAtom operationAtom;
    private final Expression[] args;


	/**
	 * Creates a function call.
	 */
	public UnresolvedFunCallImpl(OperationAtom operationAtom, Expression[] args) {
		if (operationAtom == null || args == null) {
			throw new IllegalArgumentException("UnresolvedFunCall: params should be not null");
		}
		this.operationAtom = operationAtom;
		this.args = args;

	}

    @Override
	@SuppressWarnings({"CloneDoesntCallSuperClone"})
    public UnresolvedFunCallImpl cloneExp() {
        return new UnresolvedFunCallImpl(operationAtom, Expressions.cloneExpressions(args));
    }

    @Override
	public DataType getCategory() {
        throw new UnsupportedOperationException();
    }

    @Override
	public Type getType() {
        throw new UnsupportedOperationException();
    }

    @Override
	public void unparse(PrintWriter pw) {
    	FunctionPrinter.unparse(operationAtom, args, pw);
    }

    @Override
	public Object accept(QueryComponentVisitor visitor) {
        final Object o = visitor.visitUnresolvedFunCall(this);
        if (visitor.visitChildren()) {
            // visit the call's arguments
            for (Expression arg : args) {
                arg.accept(visitor);
            }
        }
        return o;
    }

    @Override
	public Expression accept(Validator validator) {
        Expression[] newArgs = new Expression[args.length];
        FunctionDefinition funDef =
            FunUtil.resolveFunArgs(
                validator, null, args, newArgs, operationAtom);
        return funDef.createCall(validator, newArgs);
    }

    @Override
	public Calc accept(ExpressionCompiler compiler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the OperationAtom.
     *
     * @return OperationAtom
     */
    @Override
	public OperationAtom getOperationAtom() {
        return operationAtom;
    }


    /**
     * Returns the Exp argument at the specified index.
     *
     * @param      index   the index of the Exp.
     * @return     the Exp at the specified index of this array of Exp.
     *             The first Exp is at index <code>0</code>.
     * @see #getArgs()
     */
    @Override
	public Expression getArg(int index) {
        return args[index];
    }

    /**
     * Returns the internal array of Exp arguments.
     *
     * <p>Note: this does NOT do a copy.
     *
     * @return the array of expressions
     */
    @Override
	public Expression[] getArgs() {
        return args;
    }

    /**
     * Returns the number of arguments.
     *
     * @return number of arguments.
     * @see #getArgs()
     */
    @Override
	public final int getArgCount() {
        return args.length;
    }

    @SuppressWarnings("java:S4144") //we have getArgs method
    @Override
	public Object[] getChildren() {
        return args;
    }
}
