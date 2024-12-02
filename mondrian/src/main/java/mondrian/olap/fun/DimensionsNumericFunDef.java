/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedHierarchyCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.type.HierarchyType;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

/**
 * Definition of the <code>Dimensions(&lt;Numeric Expression&gt;)</code>
 * MDX builtin function.
 *
 * <p>NOTE: Actually returns a hierarchy. This is consistent with Analysis
 * Services.
 *
 * @author jhyde
 * @since Jul 20, 2009
 */
class DimensionsNumericFunDef extends AbstractFunctionDefinition {
    public static final AbstractFunctionDefinition INSTANCE = new DimensionsNumericFunDef();
    public static final String DIMENSIONS_NUMERIC_FUN_DESCRIPTION = "Returns the hierarchy whose zero-based position within the cube is specified by a numeric expression.";

    private DimensionsNumericFunDef() {
        super(
            "Dimensions",
            DIMENSIONS_NUMERIC_FUN_DESCRIPTION,
            "fhn");
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        return HierarchyType.Unknown;
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler)
    {
        final IntegerCalc integerCalc =
            compiler.compileInteger(call.getArg(0));
        return new AbstractProfilingNestedHierarchyCalc(call.getType(), new Calc[] {integerCalc})
        {
            @Override
			public Hierarchy evaluate(Evaluator evaluator) {
                Integer n = integerCalc.evaluate(evaluator);
                return nthHierarchy(evaluator, n);
            }
        };
    }

    RolapHierarchy nthHierarchy(Evaluator evaluator, Integer n) {
        RolapCube cube = (RolapCube) evaluator.getCube();
        List<RolapHierarchy> hierarchies = cube.getHierarchies();
        if (n >= hierarchies.size() || n < 0) {
            throw FunUtil.newEvalException(
                this.getFunctionMetaData(), new StringBuilder("Index '").append(n).append("' out of bounds").toString());
        }
        return hierarchies.get(n);
    }
}
