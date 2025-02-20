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
package org.eclipse.daanse.olap.api.query;

import org.eclipse.daanse.mdx.model.api.DMVStatement;
import org.eclipse.daanse.mdx.model.api.DrillthroughStatement;
import org.eclipse.daanse.mdx.model.api.ExplainStatement;
import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.model.api.RefreshStatement;
import org.eclipse.daanse.mdx.model.api.SelectStatement;
import org.eclipse.daanse.mdx.model.api.UpdateStatement;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.query.component.CellProperty;
import org.eclipse.daanse.olap.api.query.component.DmvQuery;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Explain;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Refresh;
import org.eclipse.daanse.olap.api.query.component.Subcube;
import org.eclipse.daanse.olap.api.query.component.Update;

public interface QueryProvider {

    QueryComponent createQuery(Statement statement, MdxStatement selectStatement, boolean strictValidation);

    Query createQuery(Statement statement, SelectStatement selectStatement, boolean strictValidation);

    DrillThrough createDrillThrough(Statement statement, DrillthroughStatement drillThroughStatement, boolean strictValidation);

    Explain createExplain(Statement statement, ExplainStatement explainStatement, boolean strictValidation);

    DmvQuery createDMV(DMVStatement dmvStatement);

    Refresh createRefresh(RefreshStatement refreshStatement);

    Update createUpdate(UpdateStatement updateStatement);

    Query createQuery(Statement statement,
                                Formula[] formula,
                                QueryAxis[] axes,
                                Subcube subcube,
                                QueryAxis slicerAxis,
                                CellProperty[] cellProps,
                                boolean strictValidation);
}
