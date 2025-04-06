/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.query.component.Expression;

public interface ExpCacheDescriptor {

	Calc getCalc();

	Object evaluate(Evaluator evaluator);

	Expression getExp();

	/**
	 * Returns the ordinals of the hierarchies which this expression is dependent
	 * upon. When the cache descriptor is used to generate a cache key, the key will
	 * consist of a member from each of these hierarchies.
	 */
	int[] getDependentHierarchyOrdinals();

}