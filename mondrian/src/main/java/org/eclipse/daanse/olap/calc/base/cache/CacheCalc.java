/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.eclipse.daanse.olap.calc.base.cache;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedUnknownCalc;

/**
 * Calculation which retrieves the value of an underlying calculation from
 * cache.
 *
 * @author jhyde
 * @since Oct 10, 2005
 */
public class CacheCalc extends AbstractProfilingNestedUnknownCalc {
	private final ExpCacheDescriptor key;

	public CacheCalc(Type type, ExpCacheDescriptor key) {
		super(type);
		this.key = key;
	}

	@Override
	public Object evaluate(Evaluator evaluator) {
		return evaluator.getCachedResult(key);
	}

	@Override
	public Calc<?>[] getChildCalcs() {
		return new Calc[] { key.getCalc() };
	}
}
