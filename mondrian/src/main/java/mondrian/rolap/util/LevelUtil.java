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

import org.eclipse.daanse.rdb.structure.api.model.Table;
import org.eclipse.daanse.rolap.mapping.api.model.LevelMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SQLExpressionMapping;

import mondrian.rolap.RolapColumn;

public class LevelUtil {

    private LevelUtil() {
        // constructor
    }

    public static SQLExpressionMapping getKeyExp(LevelMapping level) {
        if (level.getKeyExpression() != null) {
            return level.getKeyExpression();
        } else if (level.getColumn() != null) {
            return new RolapColumn(level.getTable() != null ? level.getTable().getName() : null, level.getColumn().getName());
        } else {
            return null;
        }
    }

    public static SQLExpressionMapping getNameExp(LevelMapping level) {
        if (level.getNameExpression() != null) {
            return level.getNameExpression();
        } else if (level.getNameColumn() != null && !Objects.equals(level.getNameColumn(), level.getColumn())) {
            return new RolapColumn(getTableName(level.getTable()), level.getNameColumn().getName());
        } else {
            return null;
        }
    }

    private static String getTableName(Table table) {
        if (table != null) {
            return table.getName();
        }
        return null;
	}

	public static SQLExpressionMapping getCaptionExp(LevelMapping level) {
        if (level.getCaptionExpression() != null) {
            return level.getCaptionExpression();
        } else if (level.getCaptionColumn() != null) {
            return new RolapColumn(getTableName(level.getTable()), level.getCaptionColumn().getName());
        } else {
            return null;
        }
    }

    public static SQLExpressionMapping getOrdinalExp(LevelMapping level) {
        if (level.getOrdinalExpression() != null) {
            return level.getOrdinalExpression();
        } else if (level.getOrdinalColumn() != null) {
            return new RolapColumn(getTableName(level.getTable()), level.getOrdinalColumn().getName());
        } else {
            return null;
        }
    }

    public static SQLExpressionMapping getParentExp(LevelMapping level) {
        if (level.getParentExpression() != null) {
            return level.getParentExpression();
        } else if (level.getParentColumn() != null) {
            return new RolapColumn(getTableName(level.getTable()), level.getParentColumn().getName());
        } else {
            return null;
        }
    }

    public static SQLExpressionMapping getPropertyExp(LevelMapping level, int i) {
        return new RolapColumn(getTableName(level.getTable()), level.getMemberProperties().get(i).getColumn().getName());
    }
}
