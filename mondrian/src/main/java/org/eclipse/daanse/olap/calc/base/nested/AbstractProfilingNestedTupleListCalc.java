package org.eclipse.daanse.olap.calc.base.nested;

import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.TupleListCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;

import mondrian.olap.type.SetType;

public abstract class AbstractProfilingNestedTupleListCalc extends AbstractProfilingNestedCalc<TupleList>
		implements TupleListCalc {
	private final boolean mutable;

	protected AbstractProfilingNestedTupleListCalc(Type type, Calc<?>... calcs) {
		this(type, true, calcs);
	}

	protected AbstractProfilingNestedTupleListCalc(Type type, boolean mutable, Calc<?>... calcs) {
		super(type, calcs);
		this.mutable = mutable;
		requiresType(SetType.class);
	}

	@Override
	public SetType getType() {
		return (SetType) super.getType();
	}

	@Override
	public ResultStyle getResultStyle() {
		return mutable ? ResultStyle.MUTABLE_LIST : ResultStyle.LIST;
	}

}
