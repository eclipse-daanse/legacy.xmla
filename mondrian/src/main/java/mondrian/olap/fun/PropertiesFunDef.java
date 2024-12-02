/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2018 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.GenericCalc;
import mondrian.olap.Property;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;

/**
 * Definition of the <code>Properties</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class PropertiesFunDef extends AbstractFunctionDefinition {
	static OperationAtom functionAtom = new MethodOperationAtom("Properties");

    static final ResolverImpl Resolver = new ResolverImpl();


    public PropertiesFunDef(
        DataType returnType)
    {
        super(new FunctionMetaDataR(functionAtom, "Returns the value of a member property.", "<Member>.Properties(<String Expression>)",  returnType, ResolverImpl.PARAMETER_TYPES));
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        final StringCalc stringCalc = compiler.compileString(call.getArg(1));
        return new GenericCalc(call.getType()) {
            @Override
			public Object evaluate(Evaluator evaluator) {
                return PropertiesFunDef.properties(
                    memberCalc.evaluate(evaluator),
                        stringCalc.evaluate(evaluator));
            }

            @Override
			public Calc[] getChildCalcs() {
                return new Calc[] {memberCalc, stringCalc};
            }
        };
    }

    static Object properties(Member member, String s) {
        boolean matchCase = SystemWideProperties.instance().CaseSensitive;
        Object o = member.getPropertyValue(s, matchCase);
        if (o == null) {
            if (!Util.isValidProperty(s, member.getLevel())) {
                throw new MondrianEvaluationException(
                    new StringBuilder("Property '").append(s)
                    .append("' is not valid for member '").append(member).append("'").toString());
            }
        }
        return o;
    }

    /**
     * Resolves calls to the <code>PROPERTIES</code> MDX function.
     */
    private static class ResolverImpl implements FunctionResolver {
        public static final DataType[] PARAMETER_TYPES = {
            DataType.MEMBER, DataType.STRING
        };


        private boolean matches(
            Expression[] args,
            DataType[] parameterTypes,
            Validator validator,
            List<Conversion> conversions)
        {
            if (parameterTypes.length != args.length) {
                return false;
            }
            for (int i = 0; i < args.length; i++) {
                if (!validator.canConvert(
                        i, args[i], parameterTypes[i], conversions))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
		public FunctionDefinition resolve(
            Expression[] args,
            Validator validator,
            List<Conversion> conversions)
        {
            if (!matches(args, ResolverImpl.PARAMETER_TYPES, validator, conversions)) {
                return null;
            }
            DataType returnType = deducePropertyCategory(args[0], args[1]);


            return new PropertiesFunDef(
                returnType);
        }

        /**
         * Deduces the category of a property. This is possible only if the
         * name is a string literal, and the member's hierarchy is unambigous.
         * If the type cannot be deduced, returns {@link DataType#VALUE}.
         *
         * @param memberExp Expression for the member
         * @param propertyNameExp Expression for the name of the property
         * @return Category of the property
         */
        private DataType deducePropertyCategory(
            Expression memberExp,
            Expression propertyNameExp)
        {
            if (!(propertyNameExp instanceof Literal)) {
                return DataType.VALUE;
            }
            String propertyName =
                (String) ((Literal) propertyNameExp).getValue();
            Hierarchy hierarchy = memberExp.getType().getHierarchy();
            if (hierarchy == null) {
                return DataType.VALUE;
            }
            Level[] levels = hierarchy.getLevels();
            Property property = Util.lookupProperty(
                levels[levels.length - 1], propertyName);
            if (property == null) {
                // we'll likely get a runtime error
                return DataType.VALUE;
            } else {
                switch (property.getType()) {
                case TYPE_BOOLEAN:
                    return DataType.LOGICAL;
                case TYPE_NUMERIC:
                case TYPE_INTEGER:
                case TYPE_LONG:
                    return DataType.NUMERIC;
                case TYPE_STRING:
                    return DataType.STRING;
                case TYPE_DATE:
                case TYPE_TIME:
                case TYPE_TIMESTAMP:
                    return DataType.DATE_TIME;
                default:
                    throw Util.badValue(property.getType());
                }
            }
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
