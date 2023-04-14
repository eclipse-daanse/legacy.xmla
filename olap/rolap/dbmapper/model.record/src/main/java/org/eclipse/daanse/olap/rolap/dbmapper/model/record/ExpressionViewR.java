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
package org.eclipse.daanse.olap.rolap.dbmapper.model.record;

import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.ExpressionView;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.SQL;

public record ExpressionViewR(List<SQL> sqls,
                              String gnericExpression,
                              String table,
                              String name) implements ExpressionView {

    @Override
	public boolean equals(Object obj) {
        if (!(obj instanceof ExpressionView that)) {
            return false;
        }
        if (sqls().size() != that.sqls().size()) {
            return false;
        }
        for (int i = 0; i < sqls().size(); i++) {
            if (! sqls().get(i).equals(that.sqls().get(i))) {
                return false;
            }
        }
        return true;
    }
}
