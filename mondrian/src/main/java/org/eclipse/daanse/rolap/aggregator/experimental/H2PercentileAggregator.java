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
import org.eclipse.daanse.rolap.mapping.api.model.enums.PercentileType;

import mondrian.rolap.RolapOrderedColumn;

public class H2PercentileAggregator implements Aggregator {

    private Double percentile;
    private PercentileType percentileType;
    private RolapOrderedColumn rolapOrderedColumn;


    public H2PercentileAggregator(PercentileType percentileType, Double percentile,
            RolapOrderedColumn rolapOrderedColumn) {
        this.percentile = percentile;
        this.percentileType = percentileType;
        this.rolapOrderedColumn = rolapOrderedColumn;
    }

    @Override
    public Object aggregate(Evaluator evaluator, TupleList members, Calc<?> exp) {
        return null;
    }

    @Override
    public StringBuilder getExpression(CharSequence operand) {
        StringBuilder buf = new StringBuilder(64);
        switch (percentileType) {
            case PercentileType.DISC:
                return disc();
            case PercentileType.CONT:
                return cont();
        }
        return buf;

    }

    private StringBuilder disc() {
        StringBuilder buf = new StringBuilder(64);
        buf.append("percentile_disc(").append(this.percentile).append(")").append(" WITHIN GROUP (ORDER BY ");
        buf.append(getColumn(this.rolapOrderedColumn)).append(")");
        //percentile_disc(0.3) WITHIN GROUP (ORDER BY price)
        return buf;
    }

    private StringBuilder cont() {
        StringBuilder buf = new StringBuilder(64);
        buf.append("percentile_cont(").append(this.percentile).append(")").append(" WITHIN GROUP (ORDER BY ");
        buf.append(getColumn(this.rolapOrderedColumn)).append(")");
        //percentile_cont(0.3) WITHIN GROUP (ORDER BY price)
        return buf;
    }

    private CharSequence getColumn(RolapOrderedColumn orderedColumn) {
        StringBuilder buf = new StringBuilder(64);
        if (orderedColumn.getColumn() != null  && orderedColumn.getColumn().getTable() != null && orderedColumn.getColumn().getName() != null) {
            buf.append("\"").append(orderedColumn.getColumn().getTable()).append("\".\"").append(orderedColumn.getColumn().getName()).append("\"");
            if (!orderedColumn.isAscend()) {
                buf.append(" DESC");
            }
        }
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
        return "PERCENTILE";
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
        return false;
    }

    @Override
    public Aggregator getNonDistinctAggregator() {
        return null;
    }
}
