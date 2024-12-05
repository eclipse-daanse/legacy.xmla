package org.eclipse.daanse.olap.function.def.periodstodate.xtd;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.fun.FunUtil;

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
public class XtdWithoutMemberCalc extends AbstractListCalc {
	private static final String TIMING_NAME = XtdWithoutMemberCalc.class.getSimpleName();

	private final Level level;

	public XtdWithoutMemberCalc(Type type, Level level) {
		super(type, new Calc[0]);
		this.level = level;
	}

	@Override
	public TupleList evaluateList(Evaluator evaluator) {
		evaluator.getTiming().markStart(XtdWithoutMemberCalc.TIMING_NAME);
		try {
			return new UnaryTupleList(FunUtil.periodsToDate(evaluator, level, null));
		} finally {
			evaluator.getTiming().markEnd(XtdWithoutMemberCalc.TIMING_NAME);
		}
	}

	@Override
	public boolean dependsOn(Hierarchy hierarchy) {
		return hierarchy.getDimension().getDimensionType() == mondrian.olap.DimensionType.TIME_DIMENSION;
	}
}
