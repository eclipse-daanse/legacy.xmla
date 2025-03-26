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

public enum AccessDatabaseColumn {

    /** No access to an object and its children. */
    NONE,

    /** All access to an object and its children. */
    ALL;

    @Override
    public String toString() {
        return this.name();
    }

   public static final Set<AccessDatabaseColumn> ALLOWED_SET = EnumSet.of(AccessDatabaseColumn.NONE, AccessDatabaseColumn.ALL);

}
