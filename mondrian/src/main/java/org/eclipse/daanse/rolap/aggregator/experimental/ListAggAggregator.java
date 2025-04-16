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
*   SmartCity Jena - initial
*/
package org.eclipse.daanse.rolap.aggregator.experimental;

import java.util.List;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.aggregator.Aggregator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;

import mondrian.rolap.RolapColumn;
import mondrian.rolap.RolapOrderedColumn;

public class ListAggAggregator implements Aggregator {

    private boolean distinct;
    private String separator;
    private List<RolapOrderedColumn> columns;
    private String coalesce;
    private String onOverflowTruncate;
    
    //
    public ListAggAggregator(boolean distinct, String separator, List<RolapOrderedColumn> columns, String coalesce, String onOverflowTruncate) {
        this.distinct = distinct;
        this.separator = separator;
        this.columns = columns;
        this.coalesce = coalesce;
        this.onOverflowTruncate = onOverflowTruncate;
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        // We iterate in the natural order in which the members appear in the TupleList.
        for (int i = 0; i < members.size(); i++) {
            evaluator.setContext(members.get(i));
            Object value = exp.evaluate(evaluator);
            // Return the first encountered non-null value
            if (value != null) {
                return value;
            }
        }
        // If everything is null or the collection is empty, return null
        return null;
    }

    @Override
    public StringBuilder getExpression(CharSequence operand) {
        StringBuilder buf = new StringBuilder(64);
        buf.append("LISTAGG");
        buf.append("( ");
        if (this.distinct) {
            buf.append("DISTINCT ");
        }
        if (this.coalesce != null) {
            buf.append("COALESCE(").append(operand).append(", '").append(this.coalesce).append("')");
        } else {
            buf.append(operand);
        }
        buf.append(", '");
        if (this.separator != null) {
            buf.append(this.separator);
        } else {
            buf.append(", ");
        }
        buf.append("'");
        if (this.onOverflowTruncate != null) {
            buf.append(" ON OVERFLOW TRUNCATE '").append(this.onOverflowTruncate).append("' WITHOUT COUNT)");
        } else {
            buf.append(")");
        }
        if (columns != null && !columns.isEmpty()) {
            buf.append(" WITHIN GROUP (ORDER BY ");
            boolean first = true;
            for(RolapOrderedColumn column : columns) {
                if (!first) {
                    buf.append(", ");
                }
                RolapColumn c = column.getColumn();
                buf.append("\"").append(c.getTable()).append("\".\"").append(c.getName()).append("\"");
                if (!column.isAscend()) {
                    buf.append(" DESC");
                }
                first = false;
            }
            buf.append(")");
        }
        //LISTAGG(NAME, ', ') WITHIN GROUP (ORDER BY ID)
        //LISTAGG(COALESCE(NAME, 'null'), ', ') WITHIN GROUP (ORDER BY ID)
        //LISTAGG(ID, ', ') WITHIN GROUP (ORDER BY ID) OVER (ORDER BY ID)
        //LISTAGG(ID, ';' ON OVERFLOW TRUNCATE 'etc' WITHOUT COUNT) WITHIN GROUP (ORDER BY ID)
        return buf;
    }

    @Override
    public boolean supportsFastAggregates(DataTypeJdbc dataType) {
        // Usually no, because we need the actual "first" item, which
        // cannot be computed by typical pre-aggregation statistics.
        return false;
    }

    @Override
    public String getName() {
        return "LISTAGG";
    }

    @Override
    public Aggregator getRollup() {
        return this;
    }

    @Override
    public Object aggregate(List<Object> rawData, DataTypeJdbc datatype) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDistinct() {
        return this.distinct;
    }

    @Override
    public Aggregator getNonDistinctAggregator() {
        return null;
    }
}