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

package mondrian.olap;

import java.util.List;

public class DmvQuery extends QueryPart {
    private final String tableName;
    private final List<String> columns;
    private final Exp whereExpression;

    public DmvQuery(
            String tableName,
            List<String> columns,
            Exp whereExpression)
    {
        this.tableName = tableName;
        this.columns = columns;
        this.whereExpression = whereExpression;
    }

//    @Override
//    public void unparse(PrintWriter pw) {
//    }

//    @Override
//    public Object[] getChildren() {
//        return new Object[] {};
//    }

    public String getTableName() {
        return this.tableName;
    }

    public Exp getWhereExpression() {
        return this.whereExpression;
    }

}

