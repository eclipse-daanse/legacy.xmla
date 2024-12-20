package org.eclipse.daanse.olap.function.def.parameter;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.Validator;
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
import mondrian.olap.type.StringType;

public class ParameterFunDef  extends AbstractFunctionDefinition {
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

    static boolean isConstant(Expression typeArg) {
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
        if (args[0] instanceof Literal firstArgAsLiteral //TODO: maybe StringLiteral Only One that has String ad DataType
            && args[0].getCategory() == DataType.STRING)
        {
            return (String) firstArgAsLiteral.getValue();
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

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        throw new UnsupportedOperationException();
    }
}
