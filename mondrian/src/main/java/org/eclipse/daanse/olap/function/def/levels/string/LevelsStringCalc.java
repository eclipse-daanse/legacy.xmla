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
package org.eclipse.daanse.olap.function.def.levels.string;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedLevelCalc;

import mondrian.olap.Util;
import mondrian.olap.fun.FunUtil;

public class LevelsStringCalc extends AbstractProfilingNestedLevelCalc {

    protected LevelsStringCalc(Type type, final StringCalc stringCalc) {
        super(type, stringCalc);
    }

    @Override
    public Level evaluate(Evaluator evaluator) {
        String levelName = getChildCalc(0, StringCalc.class).evaluate(evaluator);
        return findLevel(evaluator, levelName);
    }

    private Level findLevel(Evaluator evaluator, String s) {
        Cube cube = evaluator.getCube();
        OlapElement o = (s.startsWith("["))
                ? evaluator.getSchemaReader().lookupCompound(cube, Util.parseIdentifier(s), false, DataType.LEVEL)
                // lookupCompound barfs if "s" doesn't have matching
                // brackets, so don't even try
                : null;

        if (o instanceof Level level) {
            return level;
        } else if (o == null) {
            throw FunUtil.newEvalException(LevelsStringFunDef.levelsFunctionMetaData,
                    new StringBuilder("Level '").append(s).append("' not found").toString());
        } else {
            throw FunUtil.newEvalException(LevelsStringFunDef.levelsFunctionMetaData,
                    new StringBuilder("Levels('").append(s).append("') found ").append(o).toString());
        }
    }

}
