/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import mondrian.olap.exceptions.NotANamedSetException;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedTupleCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>&lt;Named Set&gt;.Current</code> MDX
 * builtin function.
 *
 * @author jhyde
 * @since Oct 19, 2008
 */
public class NamedSetCurrentFunDef extends AbstractFunctionDefinition {

    static OperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("Current");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the current member or tuple of a named set.",
            "<SET>.Current", DataType.TUPLE, new DataType[] { DataType.SET });

    static final NamedSetCurrentFunDef instance =
        new NamedSetCurrentFunDef();

    private NamedSetCurrentFunDef() {
        super(functionMetaData);
    }

    @Override
	public Expression createCall(Validator validator, Expression[] args) {
        assert args.length == 1;
        final Expression arg0 = args[0];
        if (!(arg0 instanceof NamedSetExpression)) {
            throw new NotANamedSetException();
        }
        return super.createCall(validator, args);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression arg0 = call.getArg(0);
        assert arg0 instanceof NamedSetExpression : "checked this in createCall";
        final NamedSetExpression namedSetExpr = (NamedSetExpression) arg0;
        if (arg0.getType().getArity() == 1) {
            return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[0]) {
                @Override
				public Member evaluate(Evaluator evaluator) {
                    return namedSetExpr.getEval(evaluator).currentMember();
                }
            };
        } else {
            return new AbstractProfilingNestedTupleCalc(call.getType(), new Calc[0]) {
                @Override
				public Member[] evaluate(Evaluator evaluator) {
                    return namedSetExpr.getEval(evaluator).currentTuple();
                }
            };
        }
    }
}
