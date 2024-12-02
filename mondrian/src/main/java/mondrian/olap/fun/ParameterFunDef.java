/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.FunctionCall;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.mdx.ParameterExpressionImpl;
import mondrian.olap.Util;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.NumericType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.StringType;

/**
 * A <code>ParameterFunDef</code> is a pseudo-function describing calls to
 * <code>Parameter</code> and <code>ParamRef</code> functions. It exists only
 * fleetingly, and is then converted into a {@link org.eclipse.daanse.olap.api.Parameter}.
 * For internal use only.
 *
 * @author jhyde
 * @since Feb 14, 2003
 */
public class ParameterFunDef extends AbstractFunctionDefinition {
    public final String parameterName;
    private final Type type;
    public final Expression exp;
    public final String parameterDescription;

    ParameterFunDef(
    		FunctionMetaData functionMetaData ,
        String parameterName,
        Type type,
        DataType returnCategory,
        Expression exp,
        String description)
    {
        super(
            functionMetaData);
        Util.assertPrecondition(
        		getFunctionMetaData().operationAtom().name().equals("Parameter")
            || getFunctionMetaData().operationAtom().name().equals("ParamRef"));
        this.parameterName = parameterName;
        this.type = type;
        this.exp = exp;
        this.parameterDescription = description;
    }

    @Override
	public Expression createCall(Validator validator, Expression[] args) {
        Parameter parameter = validator.createOrLookupParam(
            this.getFunctionMetaData().operationAtom().name().equals("Parameter"),
            parameterName, type, exp, parameterDescription);
        return new ParameterExpressionImpl(parameter);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        return type;
    }

    private static boolean isConstant(Expression typeArg) {
        if (typeArg instanceof LevelExpression) {
            // e.g. "[Time].[Quarter]"
            return true;
        }
        if (typeArg instanceof HierarchyExpressionImpl) {
            // e.g. "[Time].[By Week]"
            return true;
        }
        if (typeArg instanceof DimensionExpression) {
            // e.g. "[Time]"
            return true;
        }
        if (typeArg instanceof FunctionCall hierarchyCall) {
            if (hierarchyCall.getOperationAtom().name().equals("Hierarchy")
                && hierarchyCall.getArgCount() > 0
                && hierarchyCall.getArg(0) instanceof FunctionCall)
            {
                FunctionCall currentMemberCall = (FunctionCall) hierarchyCall.getArg(0);
                if (currentMemberCall.getOperationAtom().name().equals("CurrentMember")
                    && currentMemberCall.getArgCount() > 0
                    && currentMemberCall.getArg(0) instanceof DimensionExpression)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getParameterName(Expression[] args) {
        if (args[0] instanceof Literal
            && args[0].getCategory() == DataType.STRING)
        {
            return (String) ((Literal) args[0]).getValue();
        } else {
            throw Util.newInternal("Parameter name must be a string constant");
        }
    }

    /**
     * Returns an approximate type for a parameter, based upon the 1'th
     * argument. Does not use the default value expression, so this method
     * can safely be used before the expression has been validated.
     */
    public static Type getParameterType(Expression[] args) {
        if (args[1] instanceof Id id) {
            String[] names = id.toStringArray();
            if (names.length == 1) {
                final String name = names[0];
                if (name.equals("NUMERIC")) {
                    return NumericType.INSTANCE;
                }
                if (name.equals("STRING")) {
                    return StringType.INSTANCE;
                }
            }
        } else if (args[1] instanceof Literal literal) {
            if (literal.getValue().equals("NUMERIC")) {
                return NumericType.INSTANCE;
            } else if (literal.getValue().equals("STRING")) {
                return StringType.INSTANCE;
            }
        } else if (args[1] instanceof MemberExpression) {
            return new MemberType(null, null, null, null);
        }
        return StringType.INSTANCE;
    }

    /**
     * Resolves calls to the <code>Parameter</code> MDX function.
     */
    public static class ParameterResolver extends MultiResolver {
        private static final List<String> RESERVED_WORDS = List.of("NUMERIC", "STRING");
		private static final String[] SIGNATURES = {
            // Parameter(string const, symbol, string[, string const]): string
            "fS#yS#", "fS#yS",
            // Parameter(string const, symbol, numeric[, string const]): numeric
            "fn#yn#", "fn#yn",
            // Parameter(string const, hierarchy constant, member[, string
            // const[, symbol]]): member
            "fm#hm#", "fm#hm",
            // Parameter(string const, hierarchy constant, set[, string
            // const]): set
            "fx#hx#", "fx#hx",
        };

        public ParameterResolver() {
            super(
                "Parameter",
                "Parameter(<Name>, <Type>, <DefaultValue>, <Description>, <Set>)",
                "Returns default value of parameter.",
                ParameterResolver.SIGNATURES);
        }

        @Override
		public List<String> getReservedWords() {
            return RESERVED_WORDS;
        }

        @Override
		protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData ) {
            String parameterName = ParameterFunDef.getParameterName(args);
            Expression typeArg = args[1];
            DataType category;
            Type type = typeArg.getType();
            switch (typeArg.getCategory()) {
            case DIMENSION:
            case HIERARCHY:
            case LEVEL:
                Dimension dimension = type.getDimension();
                if (!ParameterFunDef.isConstant(typeArg)) {
                    throw FunUtil.newEvalException(
                        functionMetaData,
                        new StringBuilder("Invalid parameter '").append(parameterName)
                        .append("'. Type must be a NUMERIC, STRING, or a dimension, ")
                        .append("hierarchy or level").toString());
                }
                if (dimension == null) {
                    throw FunUtil.newEvalException(
                        functionMetaData,
                        new StringBuilder("Invalid dimension for parameter '")
                        .append(parameterName).append("'").toString());
                }
                type =
                    new MemberType(
                        type.getDimension(),
                        type.getHierarchy(),
                        type.getLevel(),
                        null);
                category = DataType.MEMBER;
                break;

            case SYMBOL:
                String s = (String) ((Literal) typeArg).getValue();
                if (s.equalsIgnoreCase("NUMERIC")) {
                    category = DataType.NUMERIC;
                    type = NumericType.INSTANCE;
                    break;
                } else if (s.equalsIgnoreCase("STRING")) {
                    category = DataType.STRING;
                    type = StringType.INSTANCE;
                    break;
                }
                // fall through and throw error
            default:
                // Error is internal because the function call has already been
                // type-checked.
                throw FunUtil.newEvalException(
                    functionMetaData,
                    new StringBuilder("Invalid type for parameter '").append(parameterName)
                    .append("'; expecting NUMERIC, STRING or a hierarchy").toString());
            }

            // Default value
            Expression exp = args[2];
            Validator validator =
                Util.createSimpleValidator(BuiltinFunTable.instance());
            final List<Conversion> conversionList = new ArrayList<>();
            String typeName = category.getName().toUpperCase();
            if (!validator.canConvert(2, exp, category, conversionList)) {
                throw FunUtil.newEvalException(
                    functionMetaData,
                    new StringBuilder("Default value of parameter '").append(parameterName)
                    .append("' is inconsistent with its type, ").append(typeName).toString());
            }
            if (exp.getCategory() == DataType.SET
                && category == DataType.MEMBER)
            {
                // Default value is a set; take this an indication that
                // the type is 'set of <member type>'.
                type = new SetType(type);
            }
            if (category == DataType.MEMBER) {
                Type expType = exp.getType();
                if (expType instanceof SetType) {
                    expType = ((SetType) expType).getElementType();
                }
                if (ParameterResolver.distinctFrom(type.getDimension(), expType.getDimension())
                    || ParameterResolver.distinctFrom(type.getHierarchy(), expType.getHierarchy())
                    || ParameterResolver.distinctFrom(type.getLevel(), expType.getLevel()))
                {
                    throw FunUtil.newEvalException(
                        functionMetaData,
                        new StringBuilder("Default value of parameter '").append(parameterName)
                        .append("' is not consistent with the parameter type '")
                        .append(type).toString());
                }
            }

            String parameterDescription = null;
            if (args.length > 3) {
                if (args[3] instanceof Literal
                    && args[3].getCategory() == DataType.STRING)
                {
                    parameterDescription =
                        (String) ((Literal) args[3]).getValue();
                } else {
                    throw FunUtil.newEvalException(
                        functionMetaData,
                        new StringBuilder("Description of parameter '").append(parameterName)
                        .append("' must be a string constant").toString());
                }
            }

            return new ParameterFunDef(
                functionMetaData, parameterName, type, category,
                exp, parameterDescription);
        }

        private static <T> boolean distinctFrom(T t1, T t2) {
            return t1 != null
               && t2 != null
               && !t1.equals(t2);
        }
    }

    /**
     * Resolves calls to the <code>ParamRef</code> MDX function.
     */
    public static class ParamRefResolver extends MultiResolver {
        public ParamRefResolver() {
            super(
                "ParamRef",
                "ParamRef(<Name>)",
                "Returns the current value of this parameter. If it is null, returns the default value.",
                new String[]{"fv#"});
        }

        @Override
		protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData ) {
            String parameterName = ParameterFunDef.getParameterName(args);
            return new ParameterFunDef(
                functionMetaData, parameterName, null, DataType.UNKNOWN, null,
                null);
        }
    }

	@Override
	public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		throw new UnsupportedOperationException();
	}
}
