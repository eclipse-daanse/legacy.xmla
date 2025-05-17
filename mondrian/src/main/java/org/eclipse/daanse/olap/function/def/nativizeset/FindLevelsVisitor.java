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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.function.def.set.level.LevelMembersFunDef;
import org.eclipse.daanse.olap.query.component.LevelExpressionImpl;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;

public class FindLevelsVisitor  extends MdxVisitorImpl {
    private final SubstitutionMap substitutionMap;
    private final Set<Dimension> dimensions;

    public FindLevelsVisitor(
        SubstitutionMap substitutionMap, HashSet<Dimension> dimensions)
    {
        this.substitutionMap = substitutionMap;
        this.dimensions = dimensions;
    }

    @Override
    public Object visitResolvedFunCall(ResolvedFunCall call) {
        if (call.getFunDef() instanceof LevelMembersFunDef) {
            if (call.getArg(0) instanceof LevelExpressionImpl) {
                Level level = ((LevelExpression) call.getArg(0)).getLevel();
                substitutionMap.put(NativizeSetFunDef.createMemberId(level), level);
                dimensions.add(level.getDimension());
            }
        } else if (
            NativizeSetFunDef.functionWhitelist.contains(call.getFunDef().getClass()))
        {
            for (Expression arg : call.getArgs()) {
                arg.accept(this);
            }
        }
        turnOffVisitChildren();
        return null;
    }


    @Override
    public Object visitMemberExpression(MemberExpression member) {
        dimensions.add(member.getMember().getDimension());
        return null;
    }
}
