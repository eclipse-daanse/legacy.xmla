package org.eclipse.daanse.olap.calc.base.constant;

import org.eclipse.daanse.olap.api.calc.ConstantCalc;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;

public class ConstantCalcs {

	public static ConstantCalc<?> nullCalcOf(Type type) {

		if (type instanceof StringType st) {
			return new ConstantStringCalc(st, null);

		} else if (type instanceof DecimalType dt) {
			return new ConstantDoubleCalc(dt, null);

		} else if (type instanceof NumericType nt) {
			return new ConstantIntegerCalc(nt, null);

		} else if (type instanceof BooleanType bt) {
			return new ConstantBooleanCalc(BooleanType.INSTANCE, null);

		} else {
			throw new RuntimeException(type.toString() + " --- " + type.getClass());
		}

	}
}
