/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.resolver.NoExpressionRequiredFunctionResolver;

import mondrian.calc.impl.GenericCalc;
import mondrian.calc.impl.GenericIterCalc;
import mondrian.olap.ExpCacheDescriptorImpl;
import mondrian.olap.type.SetType;

/**
 * Definition of the <code>Cache</code> system function, which is smart enough
 * to evaluate its argument only once.
 *
 * @author jhyde
 * @since 2005/8/14
 */
public class CacheFunDef extends AbstractFunctionDefinition {
    static final String NAME = "Cache";
    private static final String SIGNATURE_VALUE = "Cache(<<Exp>>)";
    private static final String DESCRIPTION =
        "Evaluates and returns its sole argument, applying statement-level caching";
    static final CacheFunResolver Resolver = new CacheFunResolver();

	public CacheFunDef(FunctionMetaData functionMetaData) {
		super(functionMetaData);
	}

	@Override
	public void unparse(Expression[] args, PrintWriter pw) {
        args[0].unparse(pw);
    }

    @Override
	public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression exp = call.getArg(0);
        final ExpCacheDescriptor cacheDescriptor =
                new ExpCacheDescriptorImpl(exp, compiler);
        if (call.getType() instanceof SetType) {
            return new GenericIterCalc(call.getType()) {
                @Override
				public Object evaluate(Evaluator evaluator) {
                    return evaluator.getCachedResult(cacheDescriptor);
                }

                @Override
				public Calc[] getChildCalcs() {
                    return new Calc[] {cacheDescriptor.getCalc()};
                }

                @Override
				public ResultStyle getResultStyle() {
                    // cached lists are immutable
                    return ResultStyle.LIST;
                }
            };
        } else {
            return new GenericCalc(call.getType()) {
                @Override
				public Object evaluate(Evaluator evaluator) {
                    return evaluator.getCachedResult(cacheDescriptor);
                }

                @Override
				public Calc[] getChildCalcs() {
                    return new Calc[] {cacheDescriptor.getCalc()};
                }

                @Override
				public ResultStyle getResultStyle() {
                    return ResultStyle.VALUE;
                }
            };
        }
    }

    public static class CacheFunResolver extends NoExpressionRequiredFunctionResolver {

    	static OperationAtom functionAtom = new FunctionOperationAtom(NAME);

        @Override
		public FunctionDefinition resolve(
            Expression[] args,
            Validator validator,
            List<Conversion> conversions)
        {
            if (args.length != 1) {
                return null;
            }
            final Expression exp = args[0];
            final DataType category = exp.getCategory();
            final Type type = exp.getType();

            FunctionMetaData functionMetaData=   new FunctionMetaDataR(functionAtom,DESCRIPTION, SIGNATURE_VALUE,  category, new DataType[] {category});
            return new CacheFunDef(functionMetaData);
        }

		@Override
		public OperationAtom getFunctionAtom() {
			return functionAtom;
		}


    }
}
