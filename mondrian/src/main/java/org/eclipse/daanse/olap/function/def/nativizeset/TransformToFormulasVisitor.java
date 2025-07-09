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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.function.def.set.SetFunDef;
import org.eclipse.daanse.olap.function.def.set.level.LevelMembersFunDef;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;

public class TransformToFormulasVisitor extends MdxVisitorImpl {
    private final Query query;

    public TransformToFormulasVisitor(Query query) {
        NativizeSetFunDef.LOGGER.debug("---- TransformToFormulasVisitor constructor");
        this.query = query;
    }

    @Override
    public Object visitResolvedFunCall(ResolvedFunCall call) {
        NativizeSetFunDef.LOGGER.debug("visit " + call);
        Object result = null;
        if (call.getFunDef() instanceof LevelMembersFunDef) {
            result = replaceLevelMembersReferences(call);
        } else if (
            NativizeSetFunDef.functionWhitelist.contains(call.getFunDef().getClass()))
        {
            result = visitCallArguments(call);
        }
        turnOffVisitChildren();
        return result;
    }

    private Object replaceLevelMembersReferences(ResolvedFunCall call) {
        NativizeSetFunDef.LOGGER.debug("replaceLevelMembersReferences " + call);
        Level level = ((LevelExpression) call.getArg(0)).getLevel();
        Id setId = NativizeSetFunDef.createSetId(level);
        Formula formula = query.findFormula(setId.toString());
        Expression exp = Util.createExpr(formula.getNamedSet());
        return query.createValidator().validate(exp, false);
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

        if (exps.length > 1
            && call.getFunDef() instanceof SetFunDef)
        {
            return flattenSetFunDef(call);
        }
        return null;
    }

    private Object flattenSetFunDef(ResolvedFunCall call) {
        List<Expression> newArgs = new ArrayList<>();
        flattenSetMembers(newArgs, call.getArgs());
        addSentinelMembers(newArgs);
        if (newArgs.size() != call.getArgCount()) {
            return new ResolvedFunCallImpl(
                call.getFunDef(),
                newArgs.toArray(new Expression[newArgs.size()]),
                call.getType());
        }
        return null;
    }

    private void flattenSetMembers(List<Expression> result, Expression[] args) {
        for (Expression arg : args) {
            if (arg instanceof ResolvedFunCallImpl
                && ((ResolvedFunCallImpl)arg).getFunDef() instanceof SetFunDef)
            {
                flattenSetMembers(result, ((ResolvedFunCallImpl)arg).getArgs());
            } else {
                result.add(arg);
            }
        }
    }

    private void addSentinelMembers(List<Expression> args) {
        Expression prev = args.get(0);
        for (int i = 1; i < args.size(); i++) {
            Expression curr = args.get(i);
            if (prev.toString().equals(curr.toString())) {
                OlapElement element = null;
                if (curr instanceof NamedSetExpression) {
                    element = ((NamedSetExpression) curr).getNamedSet();
                } else if (curr instanceof MemberExpression) {
                    element = ((MemberExpression) curr).getMember();
                }
                if (element != null) {
                    Level level = element.getHierarchy().getLevels().getFirst();
                    Id memberId = NativizeSetFunDef.createSentinelId(level);
                    Formula formula =
                        query.findFormula(memberId.toString());
                    args.add(i++, Util.createExpr(formula.getMdxMember()));
                }
            }
            prev = curr;
        }
    }
}
