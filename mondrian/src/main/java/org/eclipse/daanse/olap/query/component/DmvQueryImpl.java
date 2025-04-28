/*
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* History:
*  This files came from the mondrian project. Some of the Flies
*  (mostly the Tests) did not have License Header.
*  But the Project is EPL Header. 2002-2022 Hitachi Vantara.
*
* Contributors:
*   Hitachi Vantara.
*   SmartCity Jena - initial  Java 8, Junit5
*/
// Copyright (c) 2021 Sergei Semenkov.  All rights reserved.

package org.eclipse.daanse.olap.query.component;

import java.util.List;

import org.eclipse.daanse.olap.api.query.component.DmvQuery;
import org.eclipse.daanse.olap.api.query.component.Expression;

import mondrian.olap.AbstractQueryPart;

public class DmvQueryImpl extends AbstractQueryPart implements DmvQuery {
    private final String tableName;
    private final List<String> columns;
    private final Expression whereExpression;

    public DmvQueryImpl(
            String tableName,
            List<String> columns,
            Expression whereExpression)
    {
        this.tableName = tableName;
        this.columns = columns;
        this.whereExpression = whereExpression;
    }

    @Override
    public String getTableName() {
        return this.tableName;
    }

    @Override
    public Expression getWhereExpression() {
        return this.whereExpression;
    }

    @Override
    public List<String> getColumns() {
        return columns;
    }
}

