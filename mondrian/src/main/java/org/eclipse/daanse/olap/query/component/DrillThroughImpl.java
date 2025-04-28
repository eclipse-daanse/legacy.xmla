/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package org.eclipse.daanse.olap.query.component;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.query.base.Expressions;

import mondrian.olap.AbstractQueryPart;
import mondrian.olap.Util;

/**
 * Drill through statement.
 *
 * @author jhyde
 */
public class DrillThroughImpl extends AbstractQueryPart implements DrillThrough {
    private final Query query;
    private final int maxRowCount;
    private final int firstRowOrdinal;
    private final List<OlapElement> returnList;

    /**
     * Creates a DrillThrough.
     *
     * @param query Query
     * @param maxRowCount Maximum number of rows to return, or -1
     * @param firstRowOrdinal Ordinal of first row to return, or -1
     * @param returnList List of columns to return
     */
    public DrillThroughImpl(
        Query query,
        int maxRowCount,
        int firstRowOrdinal,
        List<Expression> returnList)
    {
        this.query = query;
        this.maxRowCount = maxRowCount;
        this.firstRowOrdinal = firstRowOrdinal;
        this.returnList = Collections.unmodifiableList(
            resolveReturnList(returnList));
    }



    @Override
    public void unparse(PrintWriter pw) {
        pw.print("DRILLTHROUGH");
        if (maxRowCount >= 0) {
            pw.print(" MAXROWS ");
            pw.print(maxRowCount);
        }
        if (firstRowOrdinal >= 0) {
            pw.print(" FIRSTROWSET ");
            pw.print(firstRowOrdinal);
        }
        pw.print(" ");
        query.unparse(pw);
        if (returnList != null) {
        	Expressions.unparseExpressions(
                pw, returnList.toArray(new Expression[returnList.size()]),
                " RETURN ", ", ", "");
        }
    }

    @Override
    public Object[] getChildren() {
        return new Object[] {maxRowCount, firstRowOrdinal, query, returnList};
    }

    public Query getQuery() {
        return query;
    }

    public int getMaxRowCount() {
        return maxRowCount;
    }

    public int getFirstRowOrdinal() {
        return firstRowOrdinal;
    }

    public List<OlapElement> getReturnList() {
        return returnList;
    }


    private List<OlapElement> resolveReturnList(List<Expression> returnList) {
        if (returnList == null) {
            return Collections.emptyList();
        }
        List<OlapElement> returnClauseElements = new ArrayList<>();
        CatalogReader reader = query.getCatalogReader(true);
        for (Expression exp : returnList) {
            final OlapElement olapElement =
                reader.lookupCompound(
                    query.getCube(),
                    Util.parseIdentifier(exp.toString()),
                    true,
                    DataType.UNKNOWN);
            if (olapElement instanceof OlapElement) {
                returnClauseElements.add(olapElement);
            }
        }
        return returnClauseElements;
    }


}
