
package org.eclipse.daanse.olap.calc.base.nested;

import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.TupleIterableCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;

import mondrian.olap.type.SetType;

public abstract class AbstractProfilingNestedTupleIterableCalc extends AbstractProfilingNestedCalc<TupleIterable>
		implements TupleIterableCalc {

	protected AbstractProfilingNestedTupleIterableCalc(Type type, Calc<?>... calcs) {
		super(type, calcs);
	}

	@Override
	public SetType getType() {
		return (SetType) super.getType();
	}

	@Override
	public ResultStyle getResultStyle() {
		return ResultStyle.ITERABLE;
	}

}
