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
*/
package org.eclipse.daanse.olap.calc.base.type.tuplelist;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.TupleIterableCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleCursor;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.TupleCollections;

public class TupleIteratableToTupleListCalc extends AbstractProfilingNestedTupleListCalc {

	public TupleIteratableToTupleListCalc(TupleIterableCalc tupleIterableCalc) {
		super(tupleIterableCalc.getType(), tupleIterableCalc);
	}

	@Override
	public TupleList evaluate(Evaluator evaluator) {
		final TupleIterable iterable = getChildCalc(0, TupleIterableCalc.class).evaluate(evaluator);

		// If TupleIteratable is TupleList do not copy.
		if (iterable instanceof TupleList tupleList) {
			return tupleList;
		}

		final TupleList list = TupleCollections.createList(iterable.getArity());
		final TupleCursor tupleCursor = iterable.tupleCursor();
		while (tupleCursor.forward()) {
			list.addCurrent(tupleCursor);
		}
		return list;
	}
}
