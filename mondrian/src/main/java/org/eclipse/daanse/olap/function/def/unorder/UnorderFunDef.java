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
package org.eclipse.daanse.olap.function.def.unorder;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class UnorderFunDef extends AbstractFunctionDefinition {


    public UnorderFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        // Currently Unorder has no effect. In future, we may use the function
        // as a marker to weaken the ordering required from an expression and
        // therefore allow the compiler to use a more efficient implementation
        // that does not return a strict order.
        return compiler.compile(call.getArg(0));
    }
}
