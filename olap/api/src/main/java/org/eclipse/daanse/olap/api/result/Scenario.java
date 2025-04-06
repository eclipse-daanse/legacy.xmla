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
package org.eclipse.daanse.olap.api.result;

import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.element.Member;

/**
 * Context for a set of writeback operations.
 *
 * <p>An analyst performing a what-if analysis would first create a scenario,
 * or open an existing scenario, then modify a sequence of cell values.
 *
 * <p>Some OLAP engines allow scenarios to be saved (to a file, or perhaps to
 * the database) and restored in a future session.
 *
 * <p>Multiple scenarios may be open at the same time, by different users of
 * the OLAP engine.
 *
 * @see AllocationPolicy
 *
 * @author jhyde
 * @since 24 April, 2009
 */
public interface Scenario {
    /**
     * Returns the unique identifier of this Scenario.
     *
     * <p>The format of the string returned is implementation defined. Client
     * applications must not make any assumptions about the structure or
     * contents of such strings.
     *
     * @return Unique identifier of this Scenario.
     */
    String getId();

    List<WritebackCell> getWritebackCells();

    void setCellValue(
        Connection connection,
        List<Member> members,
        double newValue,
        double currentValue,
        AllocationPolicy allocationPolicy,
        Object[] allocationArgs);

    List<Map<String, Map.Entry<DataTypeJdbc, Object>>> getSessionValues();

    void clear();
}
