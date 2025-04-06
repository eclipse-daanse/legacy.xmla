/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api;

public enum Quoting {
    /**
     * Unquoted identifier, for example "Measures".
     */
    UNQUOTED,

    /**
     * Quoted identifier, for example "[Measures]".
     */
    QUOTED,

    /**
     * Identifier quoted with an ampersand to indicate a key value, for
     * example the second segment in "[Employees].&amp;[89]".
     */
    KEY
}
