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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.db.jdbc.util.impl;

public record Column(int index,String name, SqlType type) {


	public Column(String name, SqlType type) {
		this(-1, name, type);

	}

    public String getName() {
        return name;
    }

    public SqlType getSqlType() {
        return type;
    }
}
