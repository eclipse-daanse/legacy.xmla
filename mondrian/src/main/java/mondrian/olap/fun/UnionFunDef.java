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
import mondrian.calc.impl.TupleCollections;

/**
 * Definition of the <code>Union</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class UnionFunDef extends AbstractFunctionDefinition {
    static final List<String> ReservedWords = List.of("ALL", "DISTINCT");

    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Union",
            "Union(<Set1>, <Set2>[, ALL])",
            "Returns the union of two sets, optionally retaining duplicates.",
            new String[] {"fxxx", "fxxxy"},
            UnionFunDef.class,
            UnionFunDef.ReservedWords);

    public UnionFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        String allString = FunUtil.getLiteralArg(call, 2, "DISTINCT", UnionFunDef.ReservedWords);
        final boolean all = allString.equalsIgnoreCase("ALL");
        // todo: do at validate time
        FunUtil.checkCompatible(call.getArg(0), call.getArg(1), null);
        final TupleListCalc listCalc0 =
            compiler.compileList(call.getArg(0));
        final TupleListCalc listCalc1 =
            compiler.compileList(call.getArg(1));
        return new AbstractListCalc(call.getType(), new Calc[] {listCalc0, listCalc1}) {
            @Override
			public TupleList evaluateList(Evaluator evaluator) {
                TupleList list0 = listCalc0.evaluateList(evaluator);
                TupleList list1 = listCalc1.evaluateList(evaluator);
                return union(list0, list1, all);
            }
        };
    }

    public TupleList union(TupleList list0, TupleList list1, final boolean all) {
        assert list0 != null;
        assert list1 != null;
        if (all) {
            if (list0.isEmpty()) {
                return list1;
            }
            if (list1.isEmpty()) {
                return list0;
            }
            TupleList result = TupleCollections.createList(list0.getArity());
            result.addAll(list0);
            result.addAll(list1);
            return result;
        } else {
            Set<List<Member>> added = new HashSet<>();
            TupleList result = TupleCollections.createList(list0.getArity());
            FunUtil.addUnique(result, list0, added);
            FunUtil.addUnique(result, list1, added);
            return result;
        }
    }
}
