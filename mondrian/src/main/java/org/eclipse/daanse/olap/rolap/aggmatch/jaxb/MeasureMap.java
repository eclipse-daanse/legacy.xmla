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
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.rolap.aggmatch.jaxb;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import mondrian.rolap.aggmatcher.Recognizer;

import java.util.List;

/**
 * This is the template that maps from a combination of measure
 * measure_name,
 * measure_column_name, and
 * aggregate_name ("count", "sum", "avg", "min", "max",
 * "distinct-count").
 */
@XmlType(name = "MeasureMap")
@XmlAccessorType(XmlAccessType.FIELD)
public class MeasureMap extends RegexMapper {

    private static final List<String> TEMPLATE_NAMES = List.of(
        "measure_name",
        "measure_column_name",
        "aggregate_name"
    );

    protected List<String> getTemplateNames() {
        return TEMPLATE_NAMES;
    }

    public Recognizer.Matcher getMatcher(
        final String measureName,
        final String measuerColumnName,
        final String aggregateName
    ) {
        return getMatcher(new String[]{
            measureName,
            measuerColumnName,
            aggregateName
        });
    }

    @Override
    protected String getName() {
        return "MeasureMap";
    }
}