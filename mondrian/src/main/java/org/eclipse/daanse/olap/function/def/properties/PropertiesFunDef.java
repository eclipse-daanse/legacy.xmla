/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.properties;

import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.olap.fun.MondrianEvaluationException;

public class PropertiesFunDef extends AbstractFunctionDefinition {
    static OperationAtom functionAtom = new MethodOperationAtom("Properties");
    public static final FunctionParameterR[] PARAMETER_TYPES = { new FunctionParameterR(DataType.MEMBER),
            new FunctionParameterR(DataType.STRING, "String") };

    public PropertiesFunDef(DataType returnType) {
        super(new FunctionMetaDataR(functionAtom, "Returns the value of a member property.",
                returnType, PARAMETER_TYPES));
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        final StringCalc stringCalc = compiler.compileString(call.getArg(1));
        return new PropertiesCalc(call.getType(), memberCalc, stringCalc);
    }

    static Object properties(Member member, String s) {
        boolean matchCase = SystemWideProperties.instance().CaseSensitive;
        Object o = member.getPropertyValue(s, matchCase);
        if (o == null) {
            if (!Util.isValidProperty(s, member.getLevel())) {
                throw new MondrianEvaluationException(new StringBuilder("Property '").append(s)
                        .append("' is not valid for member '").append(member).append("'").toString());
            }
        }
        return o;
    }
}
