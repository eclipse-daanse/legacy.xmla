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

package org.eclipse.daanse.olap.api.calc.compiler;

import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.calc.Calc;

public interface ParameterSlot {
    /**
     * Returns the unique index of the slot.
     */
    int getIndex();

    /**
     * Returns a compiled expression to compute the default value of the
     * parameter.
     */
    Calc<?> getDefaultValueCalc();

    /**
     * Returns the parameter.
     */
    Parameter getParameter();

    /**
     * Sets the value of this parameter.
     *
     * <p>NOTE: This method will be removed when we store parameter values
     * in the {@link org.eclipse.daanse.olap.api.result.Result} rather than in the
     * {@link mondrian.olap.QueryImpl}.
     *
     * @param value New value
     * @param assigned Whether {@link #isParameterSet()} should return true;
     *   supply value {@code false} if this is an internal assignment, to
     *   remember the default value
     */
    void setParameterValue(Object value, boolean assigned);

    /**
     * Returns the value of this parameter.
     *
     * <p>NOTE: This method will be removed when we store parameter values
     * in the {@link org.eclipse.daanse.olap.api.result.Result} rather than in the
     * {@link mondrian.olap.QueryImpl}.
     */
    Object getParameterValue();

    /**
     * Returns whether the parameter has been assigned a value. (That value
     * may be null.)
     *
     * @return Whether parmaeter has been assigned a value.
     */
    boolean isParameterSet();

    void setCachedDefaultValue(Object value);

    Object getCachedDefaultValue();

    /**
     * Unsets the parameter value.
     */
    void unsetParameterValue();
}
