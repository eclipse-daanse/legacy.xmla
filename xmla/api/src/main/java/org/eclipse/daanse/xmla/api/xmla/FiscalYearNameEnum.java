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
package org.eclipse.daanse.xmla.api.xmla;

/**
 * This enumeration value specifies how
 * the name of the fiscal year is
 * generated.
 */
public enum FiscalYearNameEnum {

    CalendarYearName,

    NextCalendarYearName;

    public static FiscalYearNameEnum fromValue(String v) {
        if (v == null) {
            return NextCalendarYearName;
        }
        for (FiscalYearNameEnum e : FiscalYearNameEnum.values()) {
            if (e.name().equals(v)) {
                return e;
            }
        }
        throw new IllegalArgumentException(new StringBuilder("FiscalYearNameEnum Illegal argument ")
            .append(v).toString());
    }

}