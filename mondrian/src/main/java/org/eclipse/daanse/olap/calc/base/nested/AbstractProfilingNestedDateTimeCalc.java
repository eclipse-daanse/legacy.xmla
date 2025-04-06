/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.eclipse.daanse.olap.calc.base.nested;

import java.util.Date;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;

public abstract class AbstractProfilingNestedDateTimeCalc extends AbstractProfilingNestedCalc<Date>
		implements DateTimeCalc {

	protected AbstractProfilingNestedDateTimeCalc(Type type, Calc<?>... calcs) {
		super(type, calcs);
	}

}
