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
package org.eclipse.daanse.olap.function.def.set.range;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantMemberCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.rolap.RolapMember;

public class RangeFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new InfixOperationAtom(":");
    static final RangeFunDef instance = new RangeFunDef();
    static final FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom,
            "Infix colon operator returns the set of members between a given pair of members.",
            DataType.SET, new FunctionParameterR[] { new FunctionParameterR(  DataType.MEMBER, "Member1" ), new FunctionParameterR( DataType.MEMBER, "Member2" ) });
    private final static String twoNullsNotSupported = "Function does not support two NULL member parameters";

    public RangeFunDef() {
        super(functionMetaData);
    }

    /**
     * Returns two membercalc objects, substituting nulls with the hierarchy null
     * member of the other expression.
     *
     * @param exp0 first expression
     * @param exp1 second expression
     *
     * @return two member calcs
     */
    private MemberCalc[] compileMembers(Expression exp0, Expression exp1, ExpressionCompiler compiler) {
        MemberCalc[] members = new MemberCalc[2];

        if (exp0.getType() instanceof NullType) {
            members[0] = null;
        } else {
            members[0] = compiler.compileMember(exp0);
        }

        if (exp1.getType() instanceof NullType) {
            members[1] = null;
        } else {
            members[1] = compiler.compileMember(exp1);
        }

        // replace any null types with hierachy null member
        // if both objects are null, throw exception

        if (members[0] == null && members[1] == null) {
            throw new OlapRuntimeException(twoNullsNotSupported);
        } else if (members[0] == null) {
            Member nullMember = ((RolapMember) members[1].evaluate(null)).getHierarchy().getNullMember();
            members[0] = ConstantMemberCalc.of(nullMember);
        } else if (members[1] == null) {
            Member nullMember = ((RolapMember) members[0].evaluate(null)).getHierarchy().getNullMember();
            members[1] = ConstantMemberCalc.of(nullMember);
        }

        return members;
    }

    @Override
    public Calc<?> compileCall(final ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc[] memberCalcs = compileMembers(call.getArg(0), call.getArg(1), compiler);
        return new RangeCalc(call.getType(), memberCalcs[0], memberCalcs[1]);
    }
}
