/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.eclipse.daanse.olap.query.component;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Explain;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;

import mondrian.olap.AbstractQueryPart;

/**
 * Explain statement.
 *
 * @author jhyde
 */
public class ExplainImpl extends AbstractQueryPart implements Explain {
    private final QueryComponent query;

    /**
     * Creates an Explain statement.
     *
     * @param query Query (SELECT or DRILLTHROUGH)
     */
    public ExplainImpl(
        QueryComponent query)
    {
        this.query = query;
        assert this.query != null;
        assert this.query instanceof Query
            || this.query instanceof DrillThrough;
    }

    @Override
    public void unparse(PrintWriter pw) {
        pw.print("EXPLAIN PLAN FOR ");
        query.unparse(pw);
    }

    @Override
    public Object[] getChildren() {
        return new Object[] {query};
    }

    public QueryComponent getQuery() {
        return query;
    }
}
