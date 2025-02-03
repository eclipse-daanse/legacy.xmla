/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara.
* Copyright (c) 2021 Sergei Semenkov
* All rights reserved.
*/

package mondrian.parser;

import java.util.List;

import org.eclipse.daanse.olap.api.Command;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.CalculatedFormula;
import org.eclipse.daanse.olap.api.query.component.CellProperty;
import org.eclipse.daanse.olap.api.query.component.DmvQuery;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Explain;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Refresh;
import org.eclipse.daanse.olap.api.query.component.Subcube;
import org.eclipse.daanse.olap.api.query.component.TransactionCommand;
import org.eclipse.daanse.olap.api.query.component.Update;
import org.eclipse.daanse.olap.api.query.component.UpdateClause;

import mondrian.olap.ExplainImpl;

/**
 * Parses and validates an MDX statement.
 *
 * <p>NOTE: API is subject to change. Current implementation is backwards
 * compatible with the old parser based on JavaCUP.
 *
 * @author jhyde
 */
public interface MdxParserValidator {

    /**
      * Parses a string to create a {@link mondrian.olap.QueryImpl}.
      * Called only by {@link mondrian.olap.ConnectionBase#parseQuery}.
      */
    QueryComponent parseInternal(
        Statement statement,
        String queryString,
        boolean debug,
        FunctionService functionService,
        boolean strictValidation);

    Expression parseExpression(
        Statement statement,
        String queryString,
        boolean debug,
        FunctionService functionService);

    interface QueryComponentFactory {

        /**
         * Creates a {@link mondrian.olap.QueryImpl} object.
         * Override this function to make your kind of query.
         */
        Query makeQuery(
            Statement statement,
            Formula[] formulae,
            QueryAxis[] axes,
            Subcube subcube,
            Expression slicer,
            CellProperty[] cellProps,
            boolean strictValidation);

        /**
         * Creates a {@link DrillThrough} object.
         */
        DrillThrough makeDrillThrough(
            Query query,
            int maxRowCount,
            int firstRowOrdinal,
            List<Expression> returnList);

        CalculatedFormula makeCalculatedFormula(
                String cubeName,
                Formula e);

        /**
         * Creates an {@link ExplainImpl} object.
         */
        Explain makeExplain(
            QueryComponent query);

        Refresh makeRefresh(
                String cubeName);

        Update makeUpdate(
                String cubeName,
                List<UpdateClause> list);

        DmvQuery makeDmvQuery(
                String tableName,
                List<String> columns,
                Expression whereExpression);

        TransactionCommand makeTransactionCommand(
                Command c);
    }
}
