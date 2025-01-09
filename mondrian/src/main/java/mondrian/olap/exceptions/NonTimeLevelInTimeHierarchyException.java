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
package mondrian.olap.exceptions;

import java.text.MessageFormat;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

@SuppressWarnings("serial")
public class NonTimeLevelInTimeHierarchyException extends OlapRuntimeException {

    private final static String nonTimeLevelInTimeHierarchy = """
        Level ''{0}'' belongs to a time hierarchy, so its level-type must be  ''Regular'', ''TimeYears'', ''TimeHalfYears'', ''TimeHalfYear'', ''TimeQuarters'', ''TimeMonths'', ''TimeWeeks'', ''TimeDays'', ''TimeHours'', ''TimeMinutes'', ''TimeSeconds'', ''TimeUndefined''.
        """;

    public NonTimeLevelInTimeHierarchyException(String name) {
        super(MessageFormat.format(nonTimeLevelInTimeHierarchy, name));
    }
}
