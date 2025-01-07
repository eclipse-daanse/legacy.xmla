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
package org.eclipse.daanse.olap.function.def.tupletostr;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.util.type.TypeUtil;

import mondrian.olap.fun.FunUtil;

public class TupleToStrFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new FunctionOperationAtom("TupleToStr");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Constructs a string from a tuple.",
            "TupleToStr()", DataType.STRING, new FunctionParameterR[] { new FunctionParameterR(DataType.TUPLE) });

    static final TupleToStrFunDef instance = new TupleToStrFunDef();

    public TupleToStrFunDef() {
        super(functionMetaData);
    }

    @Override
    public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        if (TypeUtil.couldBeMember(call.getArg(0).getType())) {
            final MemberCalc memberCalc =
                    compiler.compileMember(call.getArg(0));
            return new TupleToStrMemberCalc(call.getType(), memberCalc);
        } else {
            final TupleCalc tupleCalc =
                    compiler.compileTuple(call.getArg(0));
            return new TupleToStrCalc(call.getType(), tupleCalc);
        }
    }

}
