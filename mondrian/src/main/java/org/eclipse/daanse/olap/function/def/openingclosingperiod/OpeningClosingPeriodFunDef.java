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
package org.eclipse.daanse.olap.function.def.openingclosingperiod;

import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.def.hierarchy.member.HierarchyCurrentMemberFixedCalc;

import mondrian.olap.exceptions.FunctionMbrAndLevelHierarchyMismatchException;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

public class OpeningClosingPeriodFunDef extends AbstractFunctionDefinition {
    private final boolean opening;



    public OpeningClosingPeriodFunDef(
        FunctionMetaData functionMetaData ,
        boolean opening)
    {
        super(functionMetaData);
        this.opening = opening;
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        if (args.length == 0) {
            // With no args, the default implementation cannot
            // guess the hierarchy, so we supply the Time
            // dimension.
            RolapHierarchy defaultTimeHierarchy =
                ((RolapCube) validator.getQuery().getCube()).getTimeHierarchy(
                        getFunctionMetaData().operationAtom().name());
            return MemberType.forHierarchy(defaultTimeHierarchy);
        }
        return super.getResultType(validator, args);
    }

    @Override
    public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final Expression[] args = call.getArgs();
        final LevelCalc levelCalc;
        final MemberCalc memberCalc;
        RolapHierarchy defaultTimeHierarchy = null;
        switch (args.length) {
        case 0:
            defaultTimeHierarchy =
                ((RolapCube) compiler.getEvaluator().getCube())
                    .getTimeHierarchy(getFunctionMetaData().operationAtom().name());
            memberCalc =
                new HierarchyCurrentMemberFixedCalc(
                        MemberType.forHierarchy(defaultTimeHierarchy),
                    defaultTimeHierarchy);
            levelCalc = null;
            break;
        case 1:
            defaultTimeHierarchy =
                ((RolapCube) compiler.getEvaluator().getCube())
                    .getTimeHierarchy(getFunctionMetaData().operationAtom().name());
            levelCalc = compiler.compileLevel(call.getArg(0));
            memberCalc =
                new HierarchyCurrentMemberFixedCalc(

                        MemberType.forHierarchy(defaultTimeHierarchy),
                    defaultTimeHierarchy);
            break;
        default:
            levelCalc = compiler.compileLevel(call.getArg(0));
            memberCalc = compiler.compileMember(call.getArg(1));
            break;
        }

        // Make sure the member and the level come from the same dimension.
        if (levelCalc != null) {
            final Dimension memberDimension =
                memberCalc.getType().getDimension();
            final Dimension levelDimension = levelCalc.getType().getDimension();
            if (!memberDimension.equals(levelDimension)) {
                throw new FunctionMbrAndLevelHierarchyMismatchException(
                        opening ? "OpeningPeriod" : "ClosingPeriod",
                        levelDimension.getUniqueName(),
                        memberDimension.getUniqueName());
            }
        }
        return new OpeningClosingPeriodCalc(
                call.getType(), levelCalc, memberCalc, opening);
    }
}