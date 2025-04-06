/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2021 Hitachi Vantara..  All rights reserved.
*/

package org.eclipse.daanse.olap.function.def.periodstodate;

import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.Util;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

/**
 * Definition of the <code>PeriodsToDate</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class PeriodsToDateFunDef extends AbstractFunctionDefinition {

    public PeriodsToDateFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
    public Type getResultType(Validator validator, Expression[] args) {
        if (args.length == 0) {
            // With no args, the default implementation cannot
            // guess the hierarchy.
            RolapHierarchy defaultTimeHierarchy = ((RolapCube) validator.getQuery().getCube())
                    .getTimeHierarchy(getFunctionMetaData().operationAtom().name());
            return new SetType(MemberType.forHierarchy(defaultTimeHierarchy));
        }

        if (args.length >= 2) {
            Type hierarchyType = args[0].getType();
            MemberType memberType = (MemberType) args[1].getType();
            if (memberType.getHierarchy() != null && hierarchyType.getHierarchy() != null
                    && memberType.getHierarchy() != hierarchyType.getHierarchy()) {
                throw Util.newError("Type mismatch: member must belong to hierarchy "
                        + hierarchyType.getHierarchy().getUniqueName());
            }
        }

        // If we have at least one arg, it's a level which will
        // tell us the type.
        return super.getResultType(validator, args);
    }

    @Override
    public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
        final LevelCalc levelCalc = call.getArgCount() > 0 ? compiler.compileLevel(call.getArg(0)) : null;
        final MemberCalc memberCalc = call.getArgCount() > 1 ? compiler.compileMember(call.getArg(1)) : null;
        final RolapHierarchy timeHierarchy = levelCalc == null ? ((RolapCube) compiler.getEvaluator().getCube())
                .getTimeHierarchy(getFunctionMetaData().operationAtom().name()) : null;

        return new PeriodsToDateCalc(call.getType(), levelCalc, memberCalc, timeHierarchy);
    }
}
