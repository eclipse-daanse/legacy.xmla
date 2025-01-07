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
package org.eclipse.daanse.olap.function.def.tuple;

import java.io.PrintWriter;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.eclipse.daanse.olap.util.type.TypeUtil;

public class TupleFunDef extends AbstractFunctionDefinition {
    

    static OperationAtom functionAtom = new ParenthesesOperationAtom();


    public TupleFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }



    @Override
    public void unparse(Expression[] args, PrintWriter pw) {
        Expressions.unparseExpressions(pw, args, "(", ", ", ")");
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        // _Tuple(<Member1>[,<MemberI>]...), which is written
        // (<Member1>[,<MemberI>]...), has type [Hie1] x ... x [HieN].
        //
        // If there is only one member, it merely represents a parenthesized
        // expression, whose Hierarchy is that of the member.
        if (args.length == 1  && !(args[0].getType() instanceof MemberType)) {
            return args[0].getType();
        } else {
            MemberType[] types = new MemberType[args.length];
            for (int i = 0; i < args.length; i++) {
                Expression arg = args[i];
                types[i] = TypeUtil.toMemberType(arg.getType());
            }
            TupleType.checkHierarchies(types);
            return new TupleType(types);
        }
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        final MemberCalc[] memberCalcs = new MemberCalc[args.length];
        for (int i = 0; i < args.length; i++) {
            memberCalcs[i] = compiler.compileMember(args[i]);
        }
        return new TupleCalc(call, memberCalcs);
    }
}
