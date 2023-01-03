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

import jakarta.xml.bind.annotation.*;
import org.eclipse.daanse.olap.rolap.dbmapper.api.AggTable;
import org.eclipse.daanse.olap.rolap.dbmapper.api.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Table", propOrder = { "sql", "aggExclude", "aggTable", "hint" })
@XmlRootElement(name = "Table")
public class TableImpl implements Table {

    @XmlElement(name = "SQL")
    protected SQLImpl sql;
    @XmlElement(name = "AggExclude")
    protected List<AggExcludeImpl> aggExclude;
    @XmlElements({ @XmlElement(name = "AggName", type = AggNameImpl.class),
            @XmlElement(name = "AggPattern", type = AggPatternImpl.class) })
    protected List<AggTable> aggTable;
    @XmlElement(name = "Hint")
    protected List<HintImpl> hint;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "schema")
    protected String schema;
    @XmlAttribute(name = "alias")
    protected String alias;

    @Override
    public SQLImpl sql() {
        return sql;
    }

    public void setSql(SQLImpl value) {
        this.sql = value;
    }

    @Override
    public List<AggExcludeImpl> aggExclude() {
        if (aggExclude == null) {
            aggExclude = new ArrayList<AggExcludeImpl>();
        }
        return this.aggExclude;
    }

    @Override
    public List<AggTable> aggTable() {
        if (aggTable == null) {
            aggTable = new ArrayList<AggTable>();
        }
        return this.aggTable;
    }

    @Override
    public List<HintImpl> hint() {
        if (hint == null) {
            hint = new ArrayList<HintImpl>();
        }
        return this.hint;
    }

    @Override
    public String name() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Override
    public String schema() {
        return schema;
    }

    public void setSchema(String value) {
        this.schema = value;
    }

    @Override
    public String alias() {
        return alias;
    }

    public void setAlias(String value) {
        this.alias = value;
    }

    public boolean equals(Object o) {
        if (o instanceof Table) {
            Table that = (Table) o;
            return this.name.equals(that.name()) &&
                Objects.equals(this.alias, that.alias()) &&
                Objects.equals(this.schema, that.schema());
        } else {
            return false;
        }
    }

    public String toString() {
        return (schema == null) ?
            name :
            schema + "." + name;
    }
    public int hashCode() {
        return toString().hashCode();
    }
}
