
package org.eclipse.daanse.olap.calc.base.type.tuplelist;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.calc.api.TupleIterableCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleCursor;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedTupleListCalc;

import mondrian.calc.impl.TupleCollections;

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
