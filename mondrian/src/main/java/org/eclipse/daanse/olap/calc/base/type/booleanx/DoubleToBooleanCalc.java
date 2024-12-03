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
package org.eclipse.daanse.olap.calc.base.type.booleanx;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;

import mondrian.olap.fun.FunUtil;

public class DoubleToBooleanCalc extends AbstractProfilingNestedBooleanCalc {

	public DoubleToBooleanCalc(Type type, DoubleCalc doubleCalc) {
		super(type, doubleCalc);
	}

	@Override
	public Boolean evaluate(Evaluator evaluator) {
		Double v0 = getChildCalc(0, DoubleCalc.class).evaluate(evaluator);

		if (Double.isNaN(v0) || v0 == FunUtil.DOUBLE_NULL) {
			return FunUtil.BOOLEAN_NULL;
		}

		return v0 != 0;
	}
}