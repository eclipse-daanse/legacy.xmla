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
package org.eclipse.daanse.olap.function.def.member.parentcalc;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class ParentFunDef extends AbstractFunctionDefinition {

    // <Member>.Parent
    static PlainPropertyOperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("Parent");
    static FunctionMetaData functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom,
            "Returns the parent of a member.", DataType.MEMBER, new FunctionParameterR[] { new FunctionParameterR( DataType.MEMBER, "Member" ) });

    public ParentFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        return new ParentCalc(call.getType(), memberCalc) {
            @Override
            public Member evaluate(Evaluator evaluator) {
                Member member = memberCalc.evaluate(evaluator);
                return memberParent(evaluator, member);
            }
        };
    }

    Member memberParent(Evaluator evaluator, Member member) {
        Member parent = evaluator.getSchemaReader().getMemberParent(member);
        if (parent == null) {
            parent = member.getHierarchy().getNullMember();
        }
        return parent;
    }

}
