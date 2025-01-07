/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.calc.base.value;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedUnknownCalc;

/**
 * Expression which yields the value of the current member in the current
 * dimensional context.
 *
 * @see mondrian.calc.impl.MemberValueCalc
 *
 * @author jhyde
 * @since Sep 27, 2005
 */
public class CurrentValueUnknownCalc extends AbstractProfilingNestedUnknownCalc {

	public CurrentValueUnknownCalc(Type type) {
		super(type, new Calc[0]);
	}

	@Override
	public Object evaluate(Evaluator evaluator) {
		return evaluator.evaluateCurrent();
	}

	@Override
	public boolean dependsOn(Hierarchy hierarchy) {
		return true;
	}
}
