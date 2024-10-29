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
package mondrian.rolap.util;

import static mondrian.rolap.util.SQLUtil.toCodeSetSqlStatement;

import org.eclipse.daanse.rolap.mapping.api.model.SqlSelectQueryMapping;

import mondrian.rolap.sql.SqlQuery;

public class ViewUtil {
    public static SqlQuery.CodeSet getCodeSet(SqlSelectQueryMapping view) {
        return toCodeSetSqlStatement(view.getSql().getSqlStatements());
    }

}
