/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.xmla.model.record.xmla;

import org.eclipse.daanse.xmla.api.xmla.Annotation;
import org.eclipse.daanse.xmla.api.xmla.DataItem;
import org.eclipse.daanse.xmla.api.xmla.Measure;
import org.eclipse.daanse.xmla.api.xmla.Translation;

import java.util.List;

public record MeasureR(String name,
                       String id,
                       String description,
                       String aggregateFunction,
                       String dataType,
                       DataItem source,
                       Boolean visible,
                       String measureExpression,
                       String displayFolder,
                       String formatString,
                       String backColor,
                       String foreColor,
                       String fontName,
                       String fontSize,
                       String fontFlags,
                       Measure.Translations translations,
                       Measure.Annotations annotations) implements Measure {

    public record Annotations(List<Annotation> annotation) implements Measure.Annotations {

    }

    public record Translations(List<Translation> translation) implements Measure.Translations {

    }

}