/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */
package org.eclipse.daanse.olap.query.component;

import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.HierarchyExpression;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.query.component.UnresolvedFunCall;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;

/**
 * Default implementation of the visitor interface,
 * {@link QueryComponentVisitor}.
 *
 * <p>
 * The method implementations just ask the child nodes to
 * {@link Expression#accept(QueryComponentVisitor)} this visitor.
 *
 * @author jhyde
 * @since Jul 21, 2006
 */
public class MdxVisitorImpl implements QueryComponentVisitor {

    private boolean shouldVisitChildren = true;

    @Override
    public boolean visitChildren() {
        boolean returnValue = shouldVisitChildren;
        turnOnVisitChildren();
        return returnValue;
    }

    public void turnOnVisitChildren() {
        shouldVisitChildren = true;
    }

    public void turnOffVisitChildren() {
        shouldVisitChildren = false;
    }

    @Override
    public Object visitQuery(Query query) {
        return null;
    }

    @Override
    public Object visitQueryAxis(QueryAxis queryAxis) {
        return null;
    }

    @Override
    public Object visitFormula(Formula formula) {
        return null;
    }

    @Override
    public Object visitUnresolvedFunCall(UnresolvedFunCall call) {
        return null;
    }

    @Override
    public Object visitResolvedFunCall(ResolvedFunCall call) {
        return null;
    }

    @Override
    public Object visitId(Id id) {
        return null;
    }

    @Override
    public Object visitParameterExpression(ParameterExpression parameterExpr) {
        return null;
    }

    @Override
    public Object visitDimensionExpression(DimensionExpression dimensionExpr) {
        return null;
    }

    @Override
    public Object visitHierarchyExpression(HierarchyExpression hierarchyExpr) {
        return null;
    }

    @Override
    public Object visitLevelExpression(LevelExpression levelExpr) {
        return null;
    }

    @Override
    public Object visitMemberExpression(MemberExpression memberExpr) {
        return null;
    }

    @Override
    public Object visitNamedSetExpression(NamedSetExpression namedSetExpr) {
        return null;
    }

    @Override
    public Object visitLiteral(Literal<?> literal) {
        return null;
    }

}
