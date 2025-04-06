/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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

package org.eclipse.daanse.olap.function.def.periodstodate.xtd;

import java.text.MessageFormat;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.LevelType;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
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

public class XtdFunDef extends AbstractFunctionDefinition {
	
	private final static String timeArgNeeded = "Argument to function ''{0}'' must belong to Time hierarchy.";

	private final LevelType levelType;


	public XtdFunDef(FunctionMetaData functionMetaData, LevelType levelType) {
		super(functionMetaData);
		this.levelType = levelType;
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
		final Type type = args[0].getType();
		if (type.getDimension().getDimensionType() != DimensionType.TIME_DIMENSION) {
			throw new OlapRuntimeException(
					MessageFormat.format(timeArgNeeded, getFunctionMetaData().operationAtom().name()));
		}
		return super.getResultType(validator, args);
	}


	@Override
	public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		Evaluator evaluator = compiler.getEvaluator();
		Cube cube= evaluator.getCube();
		final Level level = getFirstTimeLevel(cube, levelType);
		switch (call.getArgCount()) {
		case 0:
			return new XtdWithoutMemberCalc(call.getType(), level);
		default:
			final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
			return new XtdWithMemberCalc(call.getType(), memberCalc, level);
		}
	}
	
    // ------------------------------------------------------------------------

    /**
     * Returns the first level of a given type in this cube.
     * 
     * @param cube Cube
     * @param levelType Level type
     * @return First level of given type, or null
     */
    private static Level getFirstTimeLevel(Cube cube, LevelType levelType) {
        for (Dimension dimension : cube.getDimensions()) {
            if (dimension.getDimensionType() == DimensionType.TIME_DIMENSION) {
                Hierarchy[] hierarchies = dimension.getHierarchies();
                for (Hierarchy hierarchy : hierarchies) {
                    Level[] levels = hierarchy.getLevels();
                    for (Level level : levels) {
                        if (level.getLevelType() == levelType) {
                            return level;
                        }
                    }
                }
            }
        }
        throw Util.badValue(levelType);
    }

   

}
