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
package org.eclipse.daanse.olap.function.def.parallelperiod;

import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantIntegerCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.def.hierarchy.member.HierarchyCurrentMemberFixedCalc;

import mondrian.olap.type.DecimalType;
import mondrian.olap.type.MemberType;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

public class ParallelPeriodFunDef extends AbstractFunctionDefinition {

        public ParallelPeriodFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData);
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
        public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            // Member defaults to [Time].currentmember
            Expression[] args = call.getArgs();

            // Numeric Expression defaults to 1.
            final IntegerCalc lagValueCalc =
                (args.length >= 2)
                ? compiler.compileInteger(args[1])
                : new ConstantIntegerCalc(new DecimalType(Integer.MAX_VALUE, 0), 1);

            // If level is not specified, we compute it from
            // member at runtime.
            final LevelCalc ancestorLevelCalc =
                args.length >= 1
                ? compiler.compileLevel(args[0])
                : null;

            final MemberCalc memberCalc;
            switch (args.length) {
            case 3:
                memberCalc = compiler.compileMember(args[2]);
                break;
            case 1:
                final Hierarchy hierarchy = args[0].getType().getHierarchy();
                if (hierarchy != null) {
                    // For some functions, such as Levels(<string expression>),
                    // the dimension cannot be determined at compile time.
                    memberCalc =
                        new HierarchyCurrentMemberFixedCalc(
                            call.getType(), hierarchy);
                } else {
                    memberCalc = null;
                }
                break;
            default:
                final RolapHierarchy timeHierarchy =
                    ((RolapCube) compiler.getEvaluator().getCube())
                        .getTimeHierarchy(getFunctionMetaData().operationAtom().name());
                memberCalc =
                    new HierarchyCurrentMemberFixedCalc(
                            call.getType(), timeHierarchy);
                break;
            }

            return new ParallelPeriodCalc(
                    call.getType(),
                memberCalc, lagValueCalc, ancestorLevelCalc);
        }
    }
