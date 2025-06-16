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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.MemberProperty;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.query.component.FormulaImpl;
import org.eclipse.daanse.olap.query.component.MdxVisitorImpl;
import org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl;

public class AddFormulasVisitor extends MdxVisitorImpl {
    private final Query query;
    private final Collection<Level> levels;
    private final Set<Dimension> dimensions;

    public AddFormulasVisitor(
        Query query,
        SubstitutionMap substitutionMap,
        Set<Dimension> dimensions)
    {
        NativizeSetFunDef.LOGGER.debug("---- AddFormulasVisitor constructor");
        this.query = query;
        this.levels = substitutionMap.values();
        this.dimensions = dimensions;
    }

    @Override
    public Object visitResolvedFunCall(ResolvedFunCall call) {
        if (call.getFunDef() instanceof NativizeSetFunDef) {
            addFormulasToQuery();
        }
        turnOffVisitChildren();
        return null;
    }

    private void addFormulasToQuery() {
        NativizeSetFunDef.LOGGER.debug("FormulaResolvingVisitor addFormulas");
        List<FormulaImpl> formulas = new ArrayList<>();

        for (Level level : levels) {
            FormulaImpl memberFormula = createDefaultMemberFormula(level);
            formulas.add(memberFormula);
            formulas.add(createNamedSetFormula(level, memberFormula));
        }

        for (Dimension dim : dimensions) {
            Level level = dim.getHierarchy().getLevels().getFirst();
            formulas.add(createSentinelFormula(level));
        }

        query.addFormulas(formulas.toArray(new FormulaImpl[formulas.size()]));
    }

    private FormulaImpl createSentinelFormula(Level level) {
        Id memberId = NativizeSetFunDef.createSentinelId(level);
        Expression memberExpr = query.getConnection()
            .parseExpression("101010");

        NativizeSetFunDef.LOGGER.debug(
            "createSentinelFormula memberId={} memberExpr={}"
            , memberId, memberExpr);
        return new FormulaImpl(memberId, memberExpr, new MemberProperty[0]);
    }

    private FormulaImpl createDefaultMemberFormula(Level level) {
        Id memberId = NativizeSetFunDef.createMemberId(level);
        Expression memberExpr =
            new UnresolvedFunCallImpl( new PlainPropertyOperationAtom("DEFAULTMEMBER"),
                new Expression[] {NativizeSetFunDef.hierarchyId(level)});

        NativizeSetFunDef.LOGGER.debug(
            "createLevelMembersFormulas memberId={} memberExpr={}",
            memberId, memberExpr);
        return new FormulaImpl(memberId, memberExpr, new MemberProperty[0]);
    }

    private FormulaImpl createNamedSetFormula(
        Level level, FormulaImpl memberFormula)
    {
        Id setId = NativizeSetFunDef.createSetId(level);
        Expression setExpr = query.getConnection()
            .parseExpression(
                new StringBuilder("{")
                .append(memberFormula.getIdentifier().toString())
                .append("}").toString());

        NativizeSetFunDef.LOGGER.debug(
            "createNamedSetFormula setId={} setExpr={}",
            setId, setExpr);
        return new FormulaImpl(setId, setExpr);
    }
}
