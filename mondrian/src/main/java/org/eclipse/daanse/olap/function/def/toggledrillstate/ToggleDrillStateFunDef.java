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
package org.eclipse.daanse.olap.function.def.toggledrillstate;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class ToggleDrillStateFunDef extends AbstractFunctionDefinition {
    private final static String toggleDrillStateRecursiveNotSupported =
        "'RECURSIVE' is not supported in ToggleDrillState.";

    public ToggleDrillStateFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        if (call.getArgCount() > 2) {
            throw new OlapRuntimeException(toggleDrillStateRecursiveNotSupported);
        }
        final TupleListCalc listCalc0 =
            compiler.compileList(call.getArg(0));
        final TupleListCalc listCalc1 =
            compiler.compileList(call.getArg(1));
        return new ToggleDrillStateCalc(call.getType(), listCalc0, listCalc1) {
        };
    }

}
