/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 1998-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package org.eclipse.daanse.olap.query.component;

import java.io.PrintWriter;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;

import mondrian.olap.Util;
import mondrian.olap.fun.FunUtil;

/**
 * A <code>ResolvedFunCall</code> is a function applied to a list of operands,
 * which has been validated and resolved to a
 * {@link FunctionDefinition function definition}.
 *
 * @author jhyde
 * @since Jan 6, 2006
 */
public final class ResolvedFunCallImpl extends AbstractExpression implements  ResolvedFunCall {

    /**
     * The arguments to the function call.  Note that for methods, 0-th arg is
     * 'this'.
     */
    private final Expression[] args;

    /**
     * Return type of this function call.
     */
    private final Type returnType;

    /**
     * Function definition.
     */
    private final FunctionDefinition funDef;

    /**
     * Creates a function call.
     *
     * @param funDef Function definition
     * @param args Arguments
     * @param returnType Return type
     */
    public ResolvedFunCallImpl(FunctionDefinition funDef, Expression[] args, Type returnType) {
        if (funDef == null || args == null || returnType == null) {
            throw new IllegalArgumentException("ResolvedFunCall params be not null");
        }
        this.funDef = funDef;
        this.args = args;
        this.returnType = returnType;
    }

    @Override
	public String toString() {
        return Util.unparse(this);
    }

    @Override
	@SuppressWarnings({"CloneDoesntCallSuperClone"})
    public ResolvedFunCallImpl cloneExp() {
        return new ResolvedFunCallImpl(
            funDef, Expressions.cloneExpressions(args), returnType);
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

    @Override
	public OperationAtom getOperationAtom() {
        return funDef.getFunctionMetaData().operationAtom();
    }


    @SuppressWarnings("java:S4144")
    @Override
	public Object[] getChildren() {
        return args;
    }

    /**
     * Returns the definition of the function which is being called.
     *
     * @return function definition
     */
    @Override
    public FunctionDefinition getFunDef() {
        return funDef;
    }

    @Override
	public final DataType getCategory() {
        return funDef.getFunctionMetaData().returnCategory();
    }

    @Override
	public final Type getType() {
        return returnType;
    }

    @Override
	public Expression accept(Validator validator) {
        // even though the function has already been validated, we need
        // to walk through the arguments to determine which measures are
        // referenced
        Expression[] newArgs = new Expression[args.length];
        FunUtil.resolveFunArgs(
            validator, funDef, args, newArgs, getOperationAtom());

        return this;
    }

    @Override
	public void unparse(PrintWriter pw) {
        funDef.unparse(args, pw);
    }

    @Override
	public Calc accept(ExpressionCompiler compiler) {
        return funDef.compileCall(this, compiler);
    }

    @Override
	public Object accept(QueryComponentVisitor visitor) {
        final Object o = visitor.visitResolvedFunCall(this);
        if (visitor.visitChildren()) {
            // visit the call's arguments
            for (Expression arg : args) {
                arg.accept(visitor);
            }
        }
        return o;
    }
}
