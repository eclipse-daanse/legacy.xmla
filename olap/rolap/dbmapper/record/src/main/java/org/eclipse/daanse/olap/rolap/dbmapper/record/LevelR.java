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

import java.math.BigInteger;
import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.api.Level;

public record LevelR(String name,
                     String table,
                     String column,
                     String nameColumn,
                     String ordinalColumn,
                     String parentColumn,
                     String nullParentValue,
                     String type,
                     BigInteger approxRowCount,
                     boolean uniqueMembers,
                     String levelType,
                     String hideMemberIf,
                     String formatter,
                     String caption,
                     String description,
                     String captionColumn,
                     List<AnnotationR> annotations,
                     ExpressionViewR keyExpression,
                     ExpressionViewR nameExpression,
                     ExpressionViewR captionExpression,
                     ExpressionViewR ordinalExpression,
                     ExpressionViewR parentExpression,
                     ClosureR closure,
                     List<PropertyR> property)
        implements Level {

}
