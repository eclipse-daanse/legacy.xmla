/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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
 *   Stefan Bischof (bipolis.org) - initial
 */


package org.eclipse.daanse.olap.api.calc.todo;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;

/**
 * Expression that evaluates a set of tuples to a {@link TupleIterable}.
 *
 * @author Richard Emberson
 * @since Jan 11, 2007
 */
public interface TupleIteratorCalc extends Calc<Object> {
    /**
     * Evaluates an expression to yield an Iterable of members or tuples.
     *
     * <p>The Iterable is immutable.
     *
     * @param evaluator Evaluation context
     * @return An Iterable of members or tuples, never null.
     */
    TupleIterable evaluateIterable(Evaluator evaluator);
}
