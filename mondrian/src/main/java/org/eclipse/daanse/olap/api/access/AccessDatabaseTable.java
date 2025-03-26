/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*/
package org.eclipse.daanse.olap.api.access;

import java.util.EnumSet;
import java.util.Set;

public enum AccessDatabaseTable {

    /** No access to an object and its children. */
    NONE,

    /**
     * A grant that covers none of the children unless explicitly granted.
     */
    CUSTOM,

    /** All access to an object and its children. */
    ALL;

    @Override
    public String toString() {
        return this.name();
    }

   public static final Set<AccessDatabaseTable> ALLOWED_SET = EnumSet.of(AccessDatabaseTable.NONE, AccessDatabaseTable.ALL, AccessDatabaseTable.CUSTOM);

}
