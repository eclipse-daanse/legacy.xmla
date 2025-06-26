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
*   Stefan Bischof (bipolis.org) - initial
*/
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
