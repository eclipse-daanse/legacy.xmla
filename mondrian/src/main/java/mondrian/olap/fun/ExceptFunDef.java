/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.ArrayTupleList;

/**
 * Definition of the <code>Except</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class ExceptFunDef extends AbstractFunctionDefinition {
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Except",
            "Except(<Set1>, <Set2>[, ALL])",
            "Finds the difference between two sets, optionally retaining duplicates.",
            new String[]{"fxxx", "fxxxy"},
            ExceptFunDef.class);

    public ExceptFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        // todo: implement ALL
        final TupleListCalc listCalc0 = compiler.compileList(call.getArg(0));
        final TupleListCalc listCalc1 = compiler.compileList(call.getArg(1));
        return new AbstractListCalc(call.getType(), new Calc[] {listCalc0, listCalc1})
        {
            @Override
			public TupleList evaluateList(Evaluator evaluator) {
                TupleList list0 = listCalc0.evaluateList(evaluator);
                if (list0.isEmpty()) {
                    return list0;
                }
                TupleList list1 = listCalc1.evaluateList(evaluator);
                if (list1.isEmpty()) {
                    return list0;
                }
                final Set<List<Member>> set1 = new HashSet<>(list1);
                final TupleList result =
                    new ArrayTupleList(list0.getArity(), list0.size());
                for (List<Member> tuple1 : list0) {
                    if (!set1.contains(tuple1)) {
                        result.add(tuple1);
                    }
                }
                return result;
            }
        };
    }
}
