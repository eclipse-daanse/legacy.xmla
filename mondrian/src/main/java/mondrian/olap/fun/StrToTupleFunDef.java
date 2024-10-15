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

import mondrian.olap.exceptions.ArgumentFunctionNotHierarchyException;
import mondrian.olap.exceptions.EmptyExpressionWasSpecifiedException;
import mondrian.olap.exceptions.MdxFuncArgumentsNumException;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedTupleCalc;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.resolver.NoExpressionRequiredFunctionResolver;

import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.olap.MondrianException;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.NullType;
import mondrian.olap.type.StringType;
import mondrian.olap.type.TupleType;
import mondrian.olap.type.TypeUtil;

/**
 * Definition of the <code>StrToTuple</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class StrToTupleFunDef extends AbstractFunctionDefinition {

	private static final String NAME = "StrToTuple";
	static OperationAtom functionAtom = new FunctionOperationAtom(NAME);
    static final ResolverImpl Resolver = new ResolverImpl();

    private StrToTupleFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final StringCalc stringCalc = compiler.compileString(call.getArg(0));
        Type elementType = call.getType();
        if (elementType instanceof MemberType) {
            final Hierarchy hierarchy = elementType.getHierarchy();
            return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {stringCalc}) {
                @Override
				public Member evaluate(Evaluator evaluator) {
                    String string = stringCalc.evaluate(evaluator);
                    if (string == null) {
                        throw FunUtil.newEvalException(
                            new EmptyExpressionWasSpecifiedException());
                    }
                    return FunUtil.parseMember(evaluator, string, hierarchy);
                }
            };
        } else {
            TupleType tupleType = (TupleType) elementType;
            final List<Hierarchy> hierarchies = tupleType.getHierarchies();
            return new AbstractProfilingNestedTupleCalc(call.getType(), new Calc[] {stringCalc}) {
                @Override
				public Member[] evaluate(Evaluator evaluator) {
                    String string = stringCalc.evaluate(evaluator);
                    if (string == null) {
                        throw FunUtil.newEvalException(
                            new EmptyExpressionWasSpecifiedException());
                    }
                    return FunUtil.parseTuple(evaluator, string, hierarchies);
                }
            };
        }
    }

    @Override
	public Expression createCall(Validator validator, Expression[] args) {
        final int argCount = args.length;
        if (argCount <= 1) {
            throw new MdxFuncArgumentsNumException( getFunctionMetaData().operationAtom().name() );
        }
        for (int i = 1; i < argCount; i++) {
            final Expression arg = args[i];
            if (arg instanceof DimensionExpression dimensionExpr) {
                Dimension dimension = dimensionExpr.getDimension();
                args[i] = new HierarchyExpressionImpl(dimension.getHierarchy());
            } else if (arg instanceof HierarchyExpressionImpl) {
                // nothing
            } else {
                throw new ArgumentFunctionNotHierarchyException(
                    i + 1, getFunctionMetaData().operationAtom().name());
            }
        }
        return super.createCall(validator, args);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        switch (args.length) {
        case 1:
            // This is a call to the standard version of StrToTuple,
            // which doesn't give us any hints about type.
            return new TupleType(null);

        case 2:
            final Type argType = args[1].getType();
            return new MemberType(
                argType.getDimension(),
                argType.getHierarchy(),
                argType.getLevel(),
                null);

        default: {
            // This is a call to Mondrian's extended version of
            // StrToTuple, of the form
            //   StrToTuple(s, <Hier1>, ... , <HierN>)
            //
            // The result is a tuple
            //  (<Hier1>, ... ,  <HierN>)
            final List<MemberType> list = new ArrayList<>();
            for (int i = 1; i < args.length; i++) {
                Expression arg = args[i];
                final Type type = arg.getType();
                list.add(TypeUtil.toMemberType(type));
            }
            final MemberType[] types =
                list.toArray(new MemberType[list.size()]);
            TupleType.checkHierarchies(types);
            return new TupleType(types);
        }
        }
    }

    private static class ResolverImpl extends NoExpressionRequiredFunctionResolver {


        @Override
		public FunctionDefinition resolve(
            Expression[] args,
            Validator validator,
            List<Conversion> conversions)
        {
            if (args.length < 1) {
                return null;
            }
            Type type = args[0].getType();
            if (!(type instanceof StringType)
                && !(type instanceof NullType))
            {
                return null;
            }
            for (int i = 1; i < args.length; i++) {
                Expression exp = args[i];
                if (!(exp instanceof DimensionExpression
                    || exp instanceof HierarchyExpressionImpl))
                {
                    return null;
                }
            }
            DataType[] argTypes = new DataType[args.length];
            argTypes[0] = DataType.STRING;
            for (int i = 1; i < argTypes.length; i++) {
                argTypes[i] = DataType.HIERARCHY;
            }
            return new StrToTupleFunDef(functionMetaDataFor(argTypes));
        }


        @Override
        public List<FunctionMetaData> getRepresentativeFunctionMetaDatas() {
        	return List.of( functionMetaDataFor(new DataType[] {DataType.STRING}));
        }

		private FunctionMetaData functionMetaDataFor(DataType[] argTypes) {
			FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom,
					"Constructs a tuple from a string.", "StrToTuple(<String Expression>)",
					 DataType.TUPLE, argTypes);
			return functionMetaData;
		}


		@Override
		public OperationAtom getFunctionAtom() {
			return functionAtom;
		}

    }
}
