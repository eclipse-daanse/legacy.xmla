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

import java.util.Objects;

import org.eclipse.daanse.olap.api.SqlExpression;
import org.eclipse.daanse.rolap.mapping.api.model.LevelMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SQLExpressionColumnMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableMapping;

import mondrian.rolap.RolapColumn;
import mondrian.rolap.RolapSqlExpression;

public class LevelUtil {

    private LevelUtil() {
        // constructor
    }

    public static SqlExpression getKeyExp(LevelMapping level) {
        if (level.getColumn() instanceof SQLExpressionColumnMapping sec) {
            return new RolapSqlExpression(sec);
        } else if (level.getColumn() != null) {
            return new RolapColumn(level.getColumn().getTable() != null ? level.getColumn().getTable().getName() : null, level.getColumn().getName());
        } else {
            return null;
        }
    }

    public static RolapSqlExpression getNameExp(LevelMapping level) {
        if (level.getNameColumn() instanceof SQLExpressionColumnMapping sec) {
            return new RolapSqlExpression(sec);
        } else if (level.getNameColumn() != null && !Objects.equals(level.getNameColumn(), level.getColumn())) {
            return new RolapColumn(getTableName(level.getColumn().getTable()), level.getNameColumn().getName());
        } else {
            return null;
        }
    }

    private static String getTableName(TableMapping table) {
        if (table != null) {
            return table.getName();
        }
        return null;
	}

	public static RolapSqlExpression getCaptionExp(LevelMapping level) {
	    if (level.getCaptionColumn() instanceof SQLExpressionColumnMapping sec) {
            return new RolapSqlExpression(sec);
        } else if (level.getCaptionColumn() != null) {
            return new RolapColumn(getTableName(level.getColumn().getTable()), level.getCaptionColumn().getName());
        } else {
            return null;
        }
    }

    public static RolapSqlExpression getOrdinalExp(LevelMapping level) {
        if (level.getOrdinalColumn() instanceof SQLExpressionColumnMapping sec) {
            return new RolapSqlExpression(sec);
        } else if (level.getOrdinalColumn() != null) {
            return new RolapColumn(getTableName(level.getColumn().getTable()), level.getOrdinalColumn().getName());
        } else {
            return null;
        }
    }

    public static RolapSqlExpression getParentExp(LevelMapping level) {
        if (level.getParentColumn() instanceof SQLExpressionColumnMapping sec) {
            return new RolapSqlExpression(sec);
        } else if (level.getParentColumn() != null) {
            return new RolapColumn(getTableName(level.getColumn().getTable()), level.getParentColumn().getName());
        } else {
            return null;
        }
    }

    public static RolapSqlExpression getPropertyExp(LevelMapping level, int i) {
        return new RolapColumn(getTableName(level.getColumn().getTable()), level.getMemberProperties().get(i).getColumn().getName());
    }
}
