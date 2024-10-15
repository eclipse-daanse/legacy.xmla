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

import org.eclipse.daanse.mdx.model.api.expression.operation.CaseOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantCalcs;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;
import org.eclipse.daanse.olap.query.base.Expressions;

import mondrian.calc.impl.GenericCalc;
import mondrian.olap.Util;
import mondrian.olap.type.BooleanType;

/**
 * Definition of the tested <code>CASE</code> MDX operator.
 *
 * Syntax is:
 * <blockquote><pre><code>Case
 * When &lt;Logical Expression&gt; Then &lt;Expression&gt;
 * [...]
 * [Else &lt;Expression&gt;]
 * End</code></blockquote>.
 *
 * @see CaseMatchFunDef
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class CaseTestFunDef extends AbstractFunctionDefinition {
	static final String NAME = "_CaseTest";
	static final OperationAtom functionAtom = new CaseOperationAtom(NAME);

	static final String DESCRIPTION = "Evaluates various conditions, and returns the corresponding expression for the first which evaluates to true.";
	static final String SIGNATURE = "Case When <Logical Expression> Then <Expression> [...] [Else <Expression>] End";

	static final ResolverImpl Resolver = new ResolverImpl();
    public CaseTestFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        final BooleanCalc[] conditionCalcs =
                new BooleanCalc[args.length / 2];
        final Calc[] exprCalcs =
                new Calc[args.length / 2];
        final List<Calc> calcList = new ArrayList<>();
        for (int i = 0, j = 0; i < exprCalcs.length; i++) {
            conditionCalcs[i] =
                    compiler.compileBoolean(args[j++]);
            calcList.add(conditionCalcs[i]);
            exprCalcs[i] = compiler.compile(args[j++]);
            calcList.add(exprCalcs[i]);
        }
        final Calc defaultCalc =
            args.length % 2 == 1
            ? compiler.compileScalar(args[args.length - 1], true)
            : ConstantCalcs.nullCalcOf(call.getType());
        calcList.add(defaultCalc);
        final Calc[] calcs = calcList.toArray(new Calc[calcList.size()]);

        
        if ( call.getType() instanceof BooleanType){
        	return	new AbstractProfilingNestedBooleanCalc(call.getType(),calcList.stream().toArray(Calc[]::new)) {
				
				@Override
				public Boolean evaluate(Evaluator evaluator) {
					for (int i = 0; i < conditionCalcs.length; i++) {
	                    if (conditionCalcs[i].evaluate(evaluator)) {
	                        return (Boolean) exprCalcs[i].evaluate(evaluator);
	                    }
	                }
	                return (Boolean) defaultCalc.evaluate(evaluator);
				}
			};
        }
        return new GenericCalc(call.getType()) {
            @Override
			public Object evaluate(Evaluator evaluator) {
                for (int i = 0; i < conditionCalcs.length; i++) {
                    if (conditionCalcs[i].evaluate(evaluator)) {
                        return exprCalcs[i].evaluate(evaluator);
                    }
                }
                return defaultCalc.evaluate(evaluator);
            }

            @Override
			public Calc[] getChildCalcs() {
                return calcs;
            }
        };
    }

    private static class ResolverImpl implements FunctionResolver {


        @Override
		public FunctionDefinition resolve(
            Expression[] args,
            Validator validator,
            List<Conversion> conversions)
        {
            if (args.length < 1) {
                return null;
            }
            int j = 0;
            int clauseCount = args.length / 2;
            int mismatchingArgs = 0;
            DataType returnType = args[1].getCategory();
            for (int i = 0; i < clauseCount; i++) {
                if (!validator.canConvert(
                        j, args[j++], DataType.LOGICAL, conversions))
                {
                    mismatchingArgs++;
                }
                if (!validator.canConvert(
                        j, args[j++], returnType, conversions))
                {
                    mismatchingArgs++;
                }
            }

            if (j < args.length && !validator.canConvert(
                        j, args[j++], returnType, conversions)) {
                    mismatchingArgs++;
            }
            Util.assertTrue(j == args.length);
            if (mismatchingArgs != 0) {
                return null;
            }
			FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, DESCRIPTION, SIGNATURE, returnType,
					Expressions.categoriesOf(args));
			
			return new CaseTestFunDef(functionMetaData);
        }

        @Override
		public boolean requiresScalarExpressionOnArgument(int k) {
            return true;
        }

		@Override
		public OperationAtom getFunctionAtom() {

			return functionAtom;
		}
    }
}
