package mondrian.olap.fun;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.InternalOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.rolap.agg.Aggregator;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.GenericCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.calc.impl.ValueCalc;
import mondrian.olap.Property;

public class AggregateChildrenFunbDef extends AbstractFunctionDefinition{
	static OperationAtom functionAtom$AggregateChildren = new InternalOperationAtom("$AggregateChildren");
	static FunctionMetaData functionMetaData$AggregateChildren = new FunctionMetaDataR(functionAtom$AggregateChildren,
			"Equivalent to 'Aggregate(<Hierarchy>.CurrentMember.Children); for internal use.",
			"$AggregateChildren(<Hierarchy>)", DataType.NUMERIC, new DataType[] { DataType.HIERARCHY });

	public AggregateChildrenFunbDef() {
		super(functionMetaData$AggregateChildren);
		// TODO Auto-generated constructor stub
	}
    
    @Override
	public void unparse(Expression[] args, PrintWriter pw) {
        pw.print(getFunctionMetaData().operationAtom().name());
        pw.print("(");
        args[0].unparse(pw);
        pw.print(")");
    }

    @Override
	public Calc compileCall(
			ResolvedFunCall call, ExpressionCompiler compiler)
    {
        final HierarchyCalc hierarchyCalc =
            compiler.compileHierarchy(call.getArg(0));
        final Calc valueCalc = new ValueCalc(call.getType());
        return new GenericCalc(call.getType()) {
            @Override
			public Object evaluate(Evaluator evaluator) {
                Hierarchy hierarchy =
                    hierarchyCalc.evaluate(evaluator);
                return aggregateChildren(
                    evaluator, hierarchy, valueCalc);
            }

            @Override
			public Calc[] getChildCalcs() {
                return new Calc[] {hierarchyCalc, valueCalc};
            }
        };
    }

    Object aggregateChildren(
        Evaluator evaluator,
        Hierarchy hierarchy,
        final Calc valueFunCall)
    {
        Member member =
            evaluator.getPreviousContext(hierarchy);
        List<Member> members = new ArrayList<>();
        evaluator.getSchemaReader()
            .getParentChildContributingChildren(
                member.getDataMember(),
                hierarchy,
                members);
        Aggregator aggregator =
            (Aggregator) evaluator.getProperty(
                Property.AGGREGATION_TYPE.name, null);
        if (aggregator == null) {
            throw FunUtil.newEvalException(
                null,
                new StringBuilder("Could not find an aggregator in the current ")
                .append("evaluation context").toString());
        }
        Aggregator rollup = aggregator.getRollup();
        if (rollup == null) {
            throw FunUtil.newEvalException(
                null,
                new StringBuilder("Don't know how to rollup aggregator '")
                .append(aggregator).append("'").toString());
        }
        final int savepoint = evaluator.savepoint();
        try {
            return rollup.aggregate(
                evaluator,
                new UnaryTupleList(members),
                valueFunCall);
        } finally {
            evaluator.restore(savepoint);
        }
    }

}
