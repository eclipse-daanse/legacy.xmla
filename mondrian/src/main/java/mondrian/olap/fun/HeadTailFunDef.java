/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.olap.type.DecimalType;

/**
 * Definition of the <code>Head</code> and <code>Tail</code>
 * MDX builtin functions.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class HeadTailFunDef extends AbstractFunctionDefinition {
    static final FunctionResolver TailResolver =
        new ReflectiveMultiResolver(
            "Tail",
            "Tail(<Set>[, <Count>])",
            "Returns a subset from the end of a set.",
            new String[] {"fxx", "fxxn"},
            HeadTailFunDef.class);

    static final FunctionResolver HeadResolver =
        new ReflectiveMultiResolver(
            "Head",
            "Head(<Set>[, < Numeric Expression >])",
            "Returns the first specified number of elements in a set.",
            new String[] {"fxx", "fxxn"},
            HeadTailFunDef.class);

    private final boolean head;

    public HeadTailFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
        head = functionMetaData.operationAtom().name().equals("Head");
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc =
            compiler.compileList(call.getArg(0));
        final IntegerCalc integerCalc =
            call.getArgCount() > 1
            ? compiler.compileInteger(call.getArg(1))
            : new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 1);
        if (head) {
            return new AbstractListCalc(
            		call.getType(), new Calc[] {tupleListCalc, integerCalc})
            {
                @Override
				public TupleList evaluateList(Evaluator evaluator) {
                    final int savepoint = evaluator.savepoint();
                    try {
                        evaluator.setNonEmpty(false);
                        TupleList list = tupleListCalc.evaluateList(evaluator);
                        Integer count = integerCalc.evaluate(evaluator);
                        return HeadTailFunDef.head(count, list);
                    } finally {
                        evaluator.restore(savepoint);
                    }
                }
            };
        } else {
            return new AbstractListCalc(
            		call.getType(), new Calc[] {tupleListCalc, integerCalc})
            {
                @Override
				public TupleList evaluateList(Evaluator evaluator) {
                    final int savepoint = evaluator.savepoint();
                    try {
                        evaluator.setNonEmpty(false);
                        TupleList list = tupleListCalc.evaluateList(evaluator);
                        Integer count = integerCalc.evaluate(evaluator);
                        return HeadTailFunDef.tail(count, list);
                    } finally {
                        evaluator.restore(savepoint);
                    }
                }
            };
        }
    }

    static TupleList tail(final Integer count, final TupleList members) {
        assert members != null;
        final int memberCount = members.size();
        if (count >= memberCount) {
            return members;
        }
        if (count <= 0) {
            return TupleCollections.emptyList(members.getArity());
        }
        return members.subList(members.size() - count, members.size());
    }

    static TupleList head(final Integer count, final TupleList members) {
        assert members != null;
        if (count <= 0) {
            return TupleCollections.emptyList(members.getArity());
        }
        return members.subList(0, Math.min(count, members.size()));
    }
}
