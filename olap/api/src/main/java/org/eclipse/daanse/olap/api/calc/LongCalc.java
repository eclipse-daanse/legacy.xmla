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
package org.eclipse.daanse.olap.api.calc;

import org.eclipse.daanse.olap.api.Evaluator;

/**
 * @author: Stefan Bischof 
 * 
 * Marker interface to check the returning type of an {@link org.eclipse.daanse.olap.api.calc.Calc}
 *  Returns {@link mondrian.olap.fun.FunUtil#INTEGER_NULL} If result {@link #evaluate(Evaluator)} would be null
 */
public interface LongCalc extends Calc<Long> {
}
