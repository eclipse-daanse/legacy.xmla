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
package org.eclipse.daanse.olap.rolap.dbmapper.record;

import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.api.CalculatedMemberProperty;
import org.eclipse.daanse.olap.rolap.dbmapper.api.ElementFormatter;
import org.eclipse.daanse.olap.rolap.dbmapper.api.Measure;
import org.eclipse.daanse.olap.rolap.dbmapper.api.enums.MeasureDataTypeEnum;

public record MeasureR(String name,
                       String column,
                       MeasureDataTypeEnum datatype,
                       String formatString,
                       String aggregator,
                       String formatter,
                       String caption,
                       String description,
                       boolean visible,
                       String displayFolder,
                       List<AnnotationR> annotations,
                       ExpressionViewR measureExpression,
                       List<CalculatedMemberPropertyR> calculatedMemberProperty,
                       ElementFormatter cellFormatter,
                       String backColor,
                       List<? extends CalculatedMemberProperty> memberProperties
                       )
        implements Measure {

}
