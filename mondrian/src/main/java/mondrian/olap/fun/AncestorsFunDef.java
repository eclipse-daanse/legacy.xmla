/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.olap.type.LevelType;


/**
 * Definition of the <code>Ancestors</code> MDX function.
 *
 * @author lboudreau
 * @since Nov 27 2012
 */
class AncestorsFunDef extends AbstractFunctionDefinition {
    static final ReflectiveMultiResolver Resolver =
        new ReflectiveMultiResolver(
            "Ancestors",
            "Ancestors(<Member>, {<Level>|<Numeric Expression>})",
            "Returns the set of all ancestors of a specified member at a specified level or at a specified distance from the member",
            new String[] {"fxml", "fxmn"},
            AncestorsFunDef.class);

    public AncestorsFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

//    @Override
//	public DataType getReturnCategory() {
//        return DataType.SET;
//    }

    @Override
	public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc =
            compiler.compileMember(call.getArg(0));
        final Type type1 = call.getArg(1).getType();
        if (type1 instanceof LevelType) {
            final LevelCalc levelCalc =
                compiler.compileLevel(call.getArg(1));
            return new AbstractListCalc(
            		call.getType(), new Calc[] {memberCalc, levelCalc})
            {
                @Override
				public TupleList evaluateList(Evaluator evaluator) {
                    Level level = levelCalc.evaluate(evaluator);
                    Member member = memberCalc.evaluate(evaluator);
                    int distance =
                        member.getDepth() - level.getDepth();
                    List<Member> ancestors = new ArrayList<>();
                    for (int curDist = 1; curDist <= distance; curDist++) {
                        ancestors.add(
                            FunUtil.ancestor(evaluator, member, curDist, null));
                    }
                    return TupleCollections.asTupleList(ancestors);
                }
            };
        } else {
            final IntegerCalc distanceCalc =
                compiler.compileInteger(call.getArg(1));
            return new AbstractListCalc(
            		call.getType(), new Calc[] {memberCalc, distanceCalc})
            {
                @Override
				public TupleList evaluateList(Evaluator evaluator) {
                    Member member = memberCalc.evaluate(evaluator);
                    Integer distance = distanceCalc.evaluate(evaluator);
                    List<Member> ancestors = new ArrayList<>();
                    for (int curDist = 1; curDist <= distance; curDist++) {
                        ancestors.add(
                            FunUtil.ancestor(evaluator, member, curDist, null));
                    }
                    return TupleCollections.asTupleList(ancestors);
                }
            };
        }
    }
}
