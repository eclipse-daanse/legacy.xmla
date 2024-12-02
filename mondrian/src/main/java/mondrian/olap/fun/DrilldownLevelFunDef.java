/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..
 * Copyright (C) 2021 Sergei Semenkov
 *
 * All rights reserved.
 */

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.calc.impl.UnaryTupleList;

/**
 * Definition of the <code>DrilldownLevel</code> MDX function.
 *
 * <p>Syntax:
 *
 * <blockquote><pre>
 * DrilldownLevel(Set_Expression[, Level_Expression])
 * DrilldownLevel(Set_Expression, , Numeric_Expression)
 * </pre></blockquote>
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class DrilldownLevelFunDef extends AbstractFunctionDefinition {
    private static final String INCLUDE_CALC_MEMBERS = "INCLUDE_CALC_MEMBERS";
    private static final List<String> RESERVED_WORDS=List.of(INCLUDE_CALC_MEMBERS);
    
    static final ReflectiveMultiResolver Resolver =
            new ReflectiveMultiResolver(
                    "DrilldownLevel",
                    "DrilldownLevel(<Set>[, <Level>]) or DrilldownLevel(<Set>, , <Index>)",
                    "Drills down the members of a set, at a specified level, to one level below. Alternatively, drills down on a specified dimension in the set.",
                    new String[]{"fxx", "fxxl", "fxxen", "fxxeny", "fxxeey"},
                    DrilldownLevelFunDef.class,
                    RESERVED_WORDS);

    public DrilldownLevelFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc =
            compiler.compileList(call.getArg(0));
        final LevelCalc levelCalc =
            call.getArgCount() > 1
                && call.getArg(1).getType()
                instanceof mondrian.olap.type.LevelType
                ? compiler.compileLevel(call.getArg(1))
                : null;
        final IntegerCalc indexCalc =
            call.getArgCount() > 2
                && call.getArg(2) != null
                && !(call.getArg(2).getType() instanceof mondrian.olap.type.EmptyType)
                ? compiler.compileInteger(call.getArg(2))
                : null;
        final int arity = tupleListCalc.getType().getArity();
        final boolean includeCalcMembers =
            call.getArgCount() == 4
                && call.getArg(3) != null
                && call.getArg(3) instanceof Literal literal
                && DrilldownLevelFunDef.INCLUDE_CALC_MEMBERS.equals(literal.getValue());
        if (indexCalc == null) {
            return new AbstractListCalc(call.getType(), new Calc[] {tupleListCalc, levelCalc})
            {
                @Override
				public TupleList evaluateList(Evaluator evaluator) {
                    TupleList list = tupleListCalc.evaluateList(evaluator);
                    if (list.isEmpty()) {
                        return list;
                    }
                    int searchDepth = -1;
                    if (levelCalc != null) {
                        Level level = levelCalc.evaluate(evaluator);
                        searchDepth = level.getDepth();
                    }
                    return new UnaryTupleList(
                        drill(searchDepth, list.slice(0), evaluator, includeCalcMembers));
                }
            };
        } else {
            return new AbstractListCalc(call.getType(), new Calc[] {tupleListCalc, indexCalc})
            {
                @Override
				public TupleList evaluateList(Evaluator evaluator) {
                    TupleList list = tupleListCalc.evaluateList(evaluator);
                    if (list.isEmpty()) {
                        return list;
                    }
                    final Integer index = indexCalc.evaluate(evaluator);
                    if (index < 0 || index >= arity) {
                        return list;
                    }
                    HashMap<Member,List<Member>> calcMembersByParent = getCalcMembersByParent(
                            list.get(0).get(index).getHierarchy(),
                            evaluator,
                            includeCalcMembers);
                    TupleList result = TupleCollections.createList(arity);
                    final SchemaReader schemaReader =
                            evaluator.getSchemaReader();
                    final Member[] tupleClone = new Member[arity];
                    for (List<Member> tuple : list) {
                        result.add(tuple);
                        final List<Member> children =
                                schemaReader.getMemberChildren(tuple.get(index));
                        for (Member child : children) {
                            tuple.toArray(tupleClone);
                            tupleClone[index] = child;
                            result.addTuple(tupleClone);
                        }
                        List<Member> childrenCalcMembers = calcMembersByParent.get(tuple.get(index));
                        if(childrenCalcMembers != null) {
                            for (Member childMember : childrenCalcMembers) {
                                tuple.toArray(tupleClone);
                                tupleClone[index] = childMember;
                                result.addTuple(tupleClone);
                            }
                        }
                    }
                    return result;
                }
            };
        }
    }

    HashMap<Member,List<Member>> getCalcMembersByParent(Hierarchy hierarchy, Evaluator evaluator, boolean includeCalcMembers) {
        List<Member> calculatedMembers;
        if(includeCalcMembers) {
            final SchemaReader schemaReader =
                    evaluator.getSchemaReader();
            calculatedMembers = schemaReader.getCalculatedMembers(hierarchy);
        }
        else {
            calculatedMembers = new ArrayList<>();
        }
        HashMap<Member,List<Member>> calcMembersByParent = new HashMap<>();
        for(Member member: calculatedMembers) {
            if(member.getParentMember() != null) {
                List<Member> children = calcMembersByParent.get(member.getParentMember());
                if(children == null) {
                    children = new ArrayList<>();
                    calcMembersByParent.put(member.getParentMember(), children);
                }
                children.add(member);
            }
        }
        return calcMembersByParent;
    }

    List<Member> drill(int searchDepth, List<Member> list, Evaluator evaluator, boolean includeCalcMembers)
    {
        HashMap<Member,List<Member>> calcMembersByParent = getCalcMembersByParent(
                list.get(0).getHierarchy(),
                evaluator,
                includeCalcMembers);

        if (searchDepth == -1) {
            searchDepth = list.get(0).getLevel().getDepth();

            for (int i = 1, m = list.size(); i < m; i++) {
                Member member = list.get(i);
                int memberDepth = member.getLevel().getDepth();

                if (memberDepth > searchDepth) {
                    searchDepth = memberDepth;
                }
            }
        }

        List<Member> drilledSet = new ArrayList<>();

        List<Member> parentMembers = new ArrayList<>();

        for (int i = 0, m = list.size(); i < m; i++) {
            Member member = list.get(i);
            drilledSet.add(member);

            Member nextMember =
                i == (m - 1)
                ? null
                : list.get(i + 1);

            //
            // This member is drilled if it's at the correct depth
            // and if it isn't drilled yet. A member is considered
            // to be "drilled" if it is immediately followed by
            // at least one descendant
            //
            if (member.getLevel().getDepth() == searchDepth
                && !FunUtil.isAncestorOf(member, nextMember, true))
            {
                parentMembers.add(member);
            }
        }

        for(Member parentMember: parentMembers) {
            List<Member> childMembers =
                    evaluator.getSchemaReader().getMemberChildren(parentMember);
            for (Member childMember : childMembers) {
                drilledSet.add(childMember);
            }
            List<Member> childrenCalcMembers = calcMembersByParent.get(parentMember);
            if(childrenCalcMembers != null) {
                for (Member childMember : childrenCalcMembers) {
                    drilledSet.add(childMember);
                }
            }
        }

        return drilledSet;
    }
}
