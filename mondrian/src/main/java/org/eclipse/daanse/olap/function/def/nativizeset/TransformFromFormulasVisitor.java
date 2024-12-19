/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.nativizeset;

import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;

import mondrian.mdx.MdxVisitorImpl;

public class TransformFromFormulasVisitor  extends MdxVisitorImpl {
    private final Query query;
    private final ExpressionCompiler compiler;

    public TransformFromFormulasVisitor(Query query, ExpressionCompiler compiler) {
        NativizeSetFunDef.LOGGER.debug("---- TransformFromFormulasVisitor constructor");
        this.query = query;
        this.compiler = compiler;
    }

    @Override
    public Object visitResolvedFunCall(ResolvedFunCall call) {
        NativizeSetFunDef.LOGGER.debug("visit " + call);
        Object result;
        result = visitCallArguments(call);
        turnOffVisitChildren();
        return result;
    }

    @Override
    public Object visitNamedSetExpression(NamedSetExpression namedSetExpr) {
        String exprName = namedSetExpr.getNamedSet().getName();
        Expression membersExpr;

        if (exprName.contains(NativizeSetFunDef.SET_NAME_PREFIX)) {
            String levelMembers = new StringBuilder(exprName.replaceAll(
                NativizeSetFunDef.SET_NAME_PREFIX, "\\[")
                .replaceAll("_$", "\\]")
                .replaceAll("_", "\\]\\.\\["))
                .append(".members").toString();
            membersExpr =
                query.getConnection().parseExpression(levelMembers);
            membersExpr =
                compiler.getValidator().validate(membersExpr, false);
        } else {
            membersExpr = namedSetExpr.getNamedSet().getExp();
        }
        return membersExpr;
    }


    private Object visitCallArguments(ResolvedFunCall call) {
        Expression[] exps = call.getArgs();
        NativizeSetFunDef.LOGGER.debug("visitCallArguments " + call);

        for (int i = 0; i < exps.length; i++) {
            Expression transformedExp = (Expression) exps[i].accept(this);
            if (transformedExp != null) {
                exps[i] = transformedExp;
            }
        }
        return null;
    }
}
