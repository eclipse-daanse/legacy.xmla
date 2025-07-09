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

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.select.Allocation;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Update;
import org.eclipse.daanse.olap.api.query.component.UpdateClause;
import org.eclipse.daanse.olap.common.AbstractQueryPart;

public class UpdateImpl extends AbstractQueryPart implements Update {
    private final String cubeName;
    private List<UpdateClause> updateClauses;

    public UpdateImpl(String cubeName, List<UpdateClause> updateClauses)
    {
        this.cubeName = cubeName;
        this.updateClauses = updateClauses;
    }

    @Override
    public void unparse(PrintWriter pw) {
        pw.print(new StringBuilder("UPDATE CUBE [").append(cubeName).append("]").toString());
    }

    @Override
    public Object[] getChildren() {
        return new Object[] {cubeName};
    }

    @Override
    public String getCubeName() {
        return cubeName;
    }

    @Override
    public List<UpdateClause> getUpdateClauses() {
        return this.updateClauses;
    }

    public static class UpdateClauseImpl extends AbstractQueryPart implements UpdateClause {
        private final Expression tuple;
        private Expression value;
        private Allocation allocation;
        private Expression weight;

        public UpdateClauseImpl(Expression tuple, Expression value, Allocation allocation, Expression weight) {
            this.tuple = tuple;
            this.value = value;
            this.allocation = allocation;
            this.weight = weight;
        }

        @Override
        public Expression getTupleExp() {
            return this.tuple;
        }

        @Override
        public Expression getValueExp() {
            return this.value;
        }

        public Allocation getAllocation() {
            return allocation == null ? Allocation.USE_EQUAL_ALLOCATION : allocation;
        }
    }
}

