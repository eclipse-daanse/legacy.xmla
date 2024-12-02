/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.function.definition.math;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantCalcs;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.BooleanType;

/**
 * Definition of <code>CASE</code> MDX operator.
 *
 * Syntax is: <blockquote>
 * 
 * <pre>
 * <code>Case
 * When &lt;Logical Expression&gt; Then &lt;Expression&gt;
 * [...]
 * [Else &lt;Expression&gt;]
 * End</code></blockquote>.
 */
class CaseTestFunctionDefinition extends AbstractFunctionDefinition {


	public CaseTestFunctionDefinition(FunctionMetaData functionInformation) {
		super(functionInformation);
	}

	@Override
	public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {

		Type type = call.getType();
		final Expression[] argumentExpressions = call.getArgs();

		int argumentsCount = argumentExpressions.length;
		int calcPairsCount = argumentsCount / 2;
		boolean hasDefaultCalc = argumentsCount % 2 == 1;

		HashMap<BooleanCalc, Calc<?>> calcPairs = new LinkedHashMap<>(calcPairsCount);

		final List<Calc<?>> allCalcs = new ArrayList<>();

		for (int i = 0, j = 0; i < calcPairsCount; i++) {
			BooleanCalc whenCondition = compiler.compileBoolean(argumentExpressions[j++]);
			allCalcs.add(whenCondition);

			Calc<?> thenExpressioCalc = compiler.compile(argumentExpressions[j++]);
			allCalcs.add(thenExpressioCalc);

			calcPairs.put(whenCondition, thenExpressioCalc);
		}

		Calc<?> defaultCalc = null;
		if (hasDefaultCalc) {
			defaultCalc = compiler.compileScalar(argumentExpressions[calcPairsCount - 1], true);
		} else {
			defaultCalc = ConstantCalcs.nullCalcOf(type);
		}
		allCalcs.add(defaultCalc);

		final Calc<?>[] allCalcsArray = allCalcs.stream().toArray(Calc[]::new);

		if (type instanceof BooleanType) {
			return new CaseTestBooleanCalc(calcPairs, defaultCalc, allCalcsArray);
		}

		return new CaseTestGenericCalc(type, calcPairs, defaultCalc, allCalcsArray);
	}


}
