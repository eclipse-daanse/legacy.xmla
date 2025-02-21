/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2021 Sergei Semenkov
// All Rights Reserved.
*/

package mondrian.rolap;

import org.eclipse.daanse.olap.api.element.DatabaseColumn;
import org.eclipse.daanse.rolap.mapping.api.model.ColumnMapping;

public abstract class RolapWritebackColumn{

    protected final RolapDatabaseColumn column;

    protected RolapWritebackColumn(ColumnMapping column) {
        this.column = new RolapDatabaseColumn();
        this.column.setName(column.getName());
    }

    public DatabaseColumn getColumn() {
        return column;
    }
}
