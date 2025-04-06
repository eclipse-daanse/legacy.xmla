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
package org.eclipse.daanse.olap.function.def.subset;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class SubsetFunDef extends AbstractFunctionDefinition {

        public SubsetFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData);
        }

        @Override
        public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            final TupleListCalc tupleListCalc =
                compiler.compileList(call.getArg(0));
            final IntegerCalc startCalc =
                compiler.compileInteger(call.getArg(1));
            final IntegerCalc countCalc =
                call.getArgCount() > 2
                ? compiler.compileInteger(call.getArg(2))
                : null;
            return new SubsetCalc(
                    call.getType(), tupleListCalc, startCalc, countCalc);
        }
    }
