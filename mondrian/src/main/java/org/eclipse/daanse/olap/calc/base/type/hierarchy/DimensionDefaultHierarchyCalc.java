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

package org.eclipse.daanse.olap.calc.base.type.hierarchy;

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.calc.api.DimensionCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedHierarchyCalc;
import org.eclipse.daanse.olap.calc.base.util.DimensionUtil;

import mondrian.olap.Evaluator;
import mondrian.olap.type.Type;

public class DimensionDefaultHierarchyCalc extends AbstractProfilingNestedHierarchyCalc<DimensionCalc> {

	public DimensionDefaultHierarchyCalc(Type type, DimensionCalc dimensionCalc) {
		super(type, new DimensionCalc[] { dimensionCalc });
	}

	@Override
	public Hierarchy evaluate(Evaluator evaluator) {
		final DimensionCalc dimensionCalc = getFirstChildCalc();
		final Dimension dimension = dimensionCalc.evaluate(evaluator);
		final Hierarchy hierarchy = DimensionUtil.getDimensionDefaultHierarchyOrThrow(dimension);
		return hierarchy;

	}
}