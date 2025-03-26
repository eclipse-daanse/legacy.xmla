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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package mondrian.olap.guard;

import org.eclipse.daanse.sql.guard.api.elements.DatabaseColumn;

public class DatabaseColumnImpl implements DatabaseColumn{

    private String name;

    public DatabaseColumnImpl(org.eclipse.daanse.olap.api.element.DatabaseColumn column) {
        this.name = column.getName();
    }

    @Override
    public String getName() {
        return this.name;
    }
}
