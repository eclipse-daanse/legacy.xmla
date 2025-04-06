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

package org.eclipse.daanse.olap.calc.base.type.tuplelist;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.TupleListCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedTupleListCalc;

public class CopyOfTupleListCalc extends AbstractProfilingNestedTupleListCalc {

	public CopyOfTupleListCalc(TupleListCalc tupleListCalc) {
		super(tupleListCalc.getType(), tupleListCalc);
	}

	@Override
	public TupleList evaluate(Evaluator evaluator) {
		final TupleList list = getChildCalc(0, TupleListCalc.class).evaluate(evaluator);
		return list.copyList(-1);
	}
}