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
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */
package org.eclipse.daanse.olap.function.def.as;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterable;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.query.component.QueryImpl.ScopedNamedSet;

import mondrian.calc.impl.AbstractIterCalc;

public class AsAliasCalc extends AbstractIterCalc {

	private ScopedNamedSet scopedNamedSet;

	public AsAliasCalc(Type type, ScopedNamedSet scopedNamedSet) {
		super(type, new Calc[0]);
		this.scopedNamedSet = scopedNamedSet;
	}

	@Override
	public TupleIterable evaluateIterable(Evaluator evaluator) {
		Evaluator.NamedSetEvaluator namedSetEvaluator = evaluator.getNamedSetEvaluator(scopedNamedSet, false);
		return namedSetEvaluator.evaluateTupleIterable(evaluator);
	}
}