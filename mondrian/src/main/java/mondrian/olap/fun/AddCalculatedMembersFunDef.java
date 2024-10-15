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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.resolver.ParametersCheckingFunctionDefinitionResolver;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;

/**
 * Definition of the <code>AddCalculatedMembers</code> MDX function.
 *
 * <p>AddCalculatedMembers adds calculated members that are siblings
 * of the members in the set. The set is limited to one dimension.
 *
 * <p>Syntax:
 *
 * <blockquote><pre>AddCalculatedMembers(&lt;Set&gt;)</pre></blockquote>

 * @author jhyde
 * @since Mar 23, 2006
 */
class AddCalculatedMembersFunDef extends AbstractFunctionDefinition {

	static OperationAtom functionAtom = new FunctionOperationAtom("AddCalculatedMembers");

	static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Adds calculated members to a set.",
			"", DataType.SET, new DataType[] { DataType.SET });
	

    public static final FunctionResolver resolver = new AddCalculatedMembersFunctionResolver();

	private AddCalculatedMembersFunDef() {
		super(functionMetaData);
	}

    @Override
	public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        return new AbstractListCalc(call.getType(), new Calc[] {tupleListCalc}) {
            @Override
			public TupleList evaluateList(Evaluator evaluator) {
                final TupleList list =
                    tupleListCalc.evaluateList(evaluator);
                return new UnaryTupleList(
                    addCalculatedMembers(list.slice(0), evaluator));
            }
        };
    }

	private List<Member> addCalculatedMembers(List<Member> memberList, Evaluator evaluator) {
		// Determine unique levels in the set
		final Set<Level> levels = new LinkedHashSet<>();
		Hierarchy hierarchy = null;

		for (Member member : memberList) {
			if (hierarchy == null) {
				hierarchy = member.getHierarchy();
			} else if (hierarchy != member.getHierarchy()) {
				throw new MondrianEvaluationException(
						new StringBuilder("Only members from the same hierarchy are allowed in the ")
								.append("AddCalculatedMembers set: ").append(hierarchy).append(" vs ")
								.append(member.getHierarchy()).toString());
			}
			levels.add(member.getLevel());
		}

        // For each level, add the calculated members from both
        // the schema and the query
        List<Member> workingList = new ArrayList<>(memberList);
        final SchemaReader schemaReader =
                evaluator.getQuery().getSchemaReader(true);
        for (Level level : levels) {
            List<Member> calcMemberList =
                schemaReader.getCalculatedMembers(level);
            for (Member calcMember : calcMemberList) {
                Member parentMember = calcMember.getParentMember();
                if (parentMember == null || memberList.stream().anyMatch(m -> m.getParentMember() != null && m.getParentMember().getUniqueName().equals(parentMember.getUniqueName()))) {
                    workingList.add(calcMember);
                }
            }
        }
        return workingList;
    }

	private static class AddCalculatedMembersFunctionResolver extends ParametersCheckingFunctionDefinitionResolver {
		public AddCalculatedMembersFunctionResolver() {
			super(new AddCalculatedMembersFunDef());
		}

		@Override
		protected boolean checkExpressions(Expression[] expressions) {
			if (expressions.length == 1) {
				Expression firstExpression = expressions[0];
				final Type expressionType = firstExpression.getType();
				if (expressionType instanceof SetType setType) {
					if (setType.getElementType() instanceof MemberType) {
						return true;
					} else {
						throw new IllegalArgumentException(
								"Only single dimension members allowed in Set for AddCalculatedMembers");
					}
				}
			}
			return false;
		}
	}
}
