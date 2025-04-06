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

package org.eclipse.daanse.olap.calc.base.type.tuple;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedTupleCalc;

public class UnknownToTupleCalc extends AbstractProfilingNestedTupleCalc {

	public UnknownToTupleCalc(Type type, Calc<?> childCalc) {
		super(type, childCalc);
	}

	@Override
	public Member[] evaluate(Evaluator evaluator) {
		Object o = getFirstChildCalc().evaluate(evaluator);
		if (o instanceof Member[] tuple) {
			return tuple;
		}
		throw evaluator.newEvalException(null, "expected Member[], was: " + o);
	}
}