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

import org.eclipse.daanse.rdb.structure.api.model.Column;

public abstract class RolapWritebackColumn{

    protected final Column column;

    protected RolapWritebackColumn(Column column) {
        this.column = column;
    }

    public Column getColumn() {
        return column;
    }
}
