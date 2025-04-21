package org.eclipse.daanse.olap.api;

import org.eclipse.daanse.olap.api.aggregator.Aggregator;

public interface AggregationFactory {
    Aggregator getAggregator(Object measure);
}
