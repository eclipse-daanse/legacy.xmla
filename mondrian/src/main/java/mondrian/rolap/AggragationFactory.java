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
package mondrian.rolap;

import java.util.List;

import org.eclipse.daanse.olap.api.aggregator.Aggregator;
import org.eclipse.daanse.rolap.aggregator.AvgAggregator;
import org.eclipse.daanse.rolap.aggregator.CountAggregator;
import org.eclipse.daanse.rolap.aggregator.DistinctCountAggregator;
import org.eclipse.daanse.rolap.aggregator.MaxAggregator;
import org.eclipse.daanse.rolap.aggregator.MinAggregator;
import org.eclipse.daanse.rolap.aggregator.SumAggregator;
import org.eclipse.daanse.rolap.aggregator.experimental.ListAggAggregator;
import org.eclipse.daanse.rolap.aggregator.experimental.NoneAggregator;
import org.eclipse.daanse.rolap.mapping.api.model.AvgMeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CountMeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CustomMeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.MaxMeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.MeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.MinMeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.NoneMeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.OrderedColumnMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SumMeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TextAggMeasureMapping;

public class AggragationFactory {

    
    public static Aggregator getAggregator(MeasureMapping measure) {
        return switch (measure) {
        case SumMeasureMapping i -> SumAggregator.INSTANCE;
        case MaxMeasureMapping i -> MaxAggregator.INSTANCE;
        case MinMeasureMapping i -> MinAggregator.INSTANCE;
        case AvgMeasureMapping i  -> AvgAggregator.INSTANCE;
        case CountMeasureMapping i -> (i.isDistinct() ? DistinctCountAggregator.INSTANCE : CountAggregator.INSTANCE);
        case TextAggMeasureMapping i -> new ListAggAggregator(i.isDistinct(), i.getSeparator(), getOrderedColumns(i.getOrderByColumns()), i.getCoalesce(), i.getOnOverflowTruncate());
        case NoneMeasureMapping i -> NoneAggregator.INSTANCE;
        case CustomMeasureMapping i -> findCustomAggregator(i);
        default -> throw new RuntimeException("Incorect aggregation type");
    };
    }

    private static List<RolapOrderedColumn> getOrderedColumns(List<? extends OrderedColumnMapping> orderByColumns) {
        if (orderByColumns != null) {
            return orderByColumns.stream().map(oc -> new RolapOrderedColumn(new RolapColumn(oc.getColumn().getTable().getName(), oc.getColumn().getName()), oc.isAscend())).toList();
        }
        return List.of();
    }

    private static Aggregator findCustomAggregator(CustomMeasureMapping i) {
        //TODO
        return null;
    }
}
