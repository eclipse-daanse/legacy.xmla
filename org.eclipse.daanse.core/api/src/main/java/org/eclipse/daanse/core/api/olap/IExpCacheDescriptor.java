/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/

package org.eclipse.daanse.core.api.olap;

import org.eclipse.daanse.core.api.calc.*;


/**
 * Holds information necessary to add an expression to the expression result
 * cache (see {@link Evaluator#getCachedResult(IExpCacheDescriptor)}).
 *
 * @author jhyde
 * @since Aug 16, 2005
 */
public interface IExpCacheDescriptor {

    Exp getExp();

    Calc getCalc();

    Object evaluate(Evaluator evaluator);

    int[] getDependentHierarchyOrdinals();

}

// End ExpCacheDescriptor.java
