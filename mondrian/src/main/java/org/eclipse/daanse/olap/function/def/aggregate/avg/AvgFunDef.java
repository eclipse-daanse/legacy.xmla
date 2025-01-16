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
package org.eclipse.daanse.olap.function.def.aggregate.avg;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

class AvgFunDef extends AbstractAggregateFunDef {


	static final FunctionMetaData fmd = new FunctionMetaDataR(AvgResolver.operationAtom,
			"Returns the average value of a numeric expression evaluated over a set.", DataType.NUMERIC,
			new FunctionParameterR[] { new FunctionParameterR(  DataType.SET ) });

	public AvgFunDef() {
		super(fmd);
	}

	@Override
	public Calc<Double> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));

		final Calc<?> calc =  new CurrentValueUnknownCalc(call.getType());

		return new AvgCalc(call.getType(), tupleListCalc, calc, AvgFunDef.class.getSimpleName());
	}
}
