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
package org.eclipse.daanse.olap.rolap.dbmapper.mondrian;

import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.api.InlineTable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "columnDefs", "rows" })
public class InlineTableImpl implements InlineTable {

    @XmlElementWrapper(name = "ColumnDefs")
    @XmlElement(name = "ColumnDef", required = true)
    protected List<ColumnDefImpl> columnDefs;
    @XmlElementWrapper(name = "Rows", required = true)
    @XmlElement(name = "Row", required = true)
    protected List<RowImpl> rows;

    @Override
    public List<ColumnDefImpl> columnDefs() {
        return columnDefs;
    }

    public void setColumnDefs(List<ColumnDefImpl> value) {
        this.columnDefs = value;
    }

    @Override
    public List<RowImpl> rows() {
        return rows;
    }

    public void setRows(List<RowImpl> value) {
        this.rows = value;
    }

}
