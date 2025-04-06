package org.eclipse.daanse.olap.calc.base.value;

import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingValueCalc;

import mondrian.olap.fun.FunUtil;

public class CurrentValueDoubleCalc extends AbstractProfilingValueCalc<Double> implements DoubleCalc{


	public CurrentValueDoubleCalc(Type type) {
		super(type);
	}

	@Override
	protected Double convertCurrentValue(Object evaluatedCurrentValue) {
		if (evaluatedCurrentValue == null) {
			return FunUtil.DOUBLE_NULL;
		} else if (evaluatedCurrentValue instanceof Double d) {
			return d;
		} else if (evaluatedCurrentValue instanceof Number n) {
			return n.doubleValue();
		}
		throw new RuntimeException("wring value");
	}

}
