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
package org.eclipse.daanse.olap.function.def.nonemptycrossjoin;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.def.crossjoin.CrossJoinFunDef;

public class NonEmptyCrossJoinFunDef extends CrossJoinFunDef {

        public NonEmptyCrossJoinFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData);
        }

        @Override
        public Calc compileCall(final ResolvedFunCall call, ExpressionCompiler compiler) {
            final TupleListCalc listCalc1 = compiler.compileList(call.getArg(0));
            final TupleListCalc listCalc2 = compiler.compileList(call.getArg(1));
            return new NonEmptyCrossJoinCalc(
                    call.getType(), listCalc1, listCalc2, false, call, getCtag())
            {
            };
        }

    }
