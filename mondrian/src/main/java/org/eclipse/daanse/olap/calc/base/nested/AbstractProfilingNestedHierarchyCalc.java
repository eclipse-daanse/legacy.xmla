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

package org.eclipse.daanse.olap.calc.base.nested;

import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.HierarchyType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;

public abstract class AbstractProfilingNestedHierarchyCalc extends AbstractProfilingNestedCalc<Hierarchy>
		implements HierarchyCalc {

	protected AbstractProfilingNestedHierarchyCalc(Type type, Calc<?>... calcs) {
		super(type, calcs);
		requiresType(HierarchyType.class);
	}

}
