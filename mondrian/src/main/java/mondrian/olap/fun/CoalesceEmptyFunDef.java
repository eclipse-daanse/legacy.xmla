/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;

import mondrian.calc.impl.GenericCalc;

/**
 * Definition of the <code>CoalesceEmpty</code> MDX function.
 *
 * <p>It evaluates each of the arguments to the function, returning the
 * first such argument that does not return a null value.
 *
 * @author gjohnson
 */
public class CoalesceEmptyFunDef extends AbstractFunctionDefinition {
	static final OperationAtom functionAtom = new FunctionOperationAtom("CoalesceEmpty");
    static final FunctionResolver Resolver = new ResolverImpl();



	public CoalesceEmptyFunDef(FunctionMetaData functionMetaData) {
		super(functionMetaData);
	}

	@Override
	public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        final Calc[] calcs = new Calc[args.length];
        for (int i = 0; i < args.length; i++) {
            calcs[i] = compiler.compileScalar(args[i], true);
        }
        return new GenericCalc(call.getType()) {
            @Override
			public Object evaluate(Evaluator evaluator) {
                for (Calc calc : calcs) {
                    final Object o = calc.evaluate(evaluator);
                    if (o != null) {
                        return o;
                    }
                }
                return null;
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
            final DataType[] types = {DataType.NUMERIC, DataType.STRING};
            final DataType[] argTypes = new DataType[args.length];
            for (DataType type : types) {
                int matchingArgs = 0;
                conversions.clear();
                for (int i = 0; i < args.length; i++) {
                    if (validator.canConvert(i, args[i], type, conversions)) {
                        matchingArgs++;
                    }
                    argTypes[i] = type;
                }
                if (matchingArgs == args.length) {

                	FunctionMetaData functionMetaData=new FunctionMetaDataR( functionAtom, "Coalesces an empty cell value to a different value. All of the expressions must be of the same type (number or string).", "CoalesceEmpty(<Value Expression>[, <Value Expression>...])",  type, argTypes);
                    return new CoalesceEmptyFunDef(functionMetaData);
                }
            }
            return null;
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
