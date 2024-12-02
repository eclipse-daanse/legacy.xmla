/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDimensionCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>&lt;Level&gt;.Dimension</code>
 * MDX builtin function.
 *
 * @author jhyde
 * @since Jul 20, 2009
 */
class LevelDimensionFunDef extends AbstractFunctionDefinition {
    public static final AbstractFunctionDefinition INSTANCE = new LevelDimensionFunDef();

    public LevelDimensionFunDef() {
        super(
            "Dimension",
            "Returns the dimension that contains a specified level.", "pdl");
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler)
    {
        final LevelCalc levelCalc =
            compiler.compileLevel(call.getArg(0));
        return new AbstractProfilingNestedDimensionCalc(call.getType(), new Calc[] {levelCalc}) {
            @Override
			public Dimension evaluate(Evaluator evaluator) {
                Level level =  levelCalc.evaluate(evaluator);
                return level.getDimension();
            }
        };
    }
}
