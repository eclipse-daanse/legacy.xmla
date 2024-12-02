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
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDimensionCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>&lt;Hierarchy&gt;.Dimension</code> MDX
 * builtin function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class HierarchyDimensionFunDef extends AbstractFunctionDefinition {
    static final HierarchyDimensionFunDef instance =
        new HierarchyDimensionFunDef();

    private HierarchyDimensionFunDef() {
        super(
            "Dimension",
            "Returns the dimension that contains a specified hierarchy.",
            "pdh");
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final HierarchyCalc hierarchyCalc =
                compiler.compileHierarchy(call.getArg(0));
        return new DimensionCalcImpl(call.getType(), hierarchyCalc);
    }

    public static class DimensionCalcImpl extends AbstractProfilingNestedDimensionCalc {
        private final HierarchyCalc hierarchyCalc;

        public DimensionCalcImpl(Type type, HierarchyCalc hierarchyCalc) {
            super(type, new Calc[] {hierarchyCalc});
            this.hierarchyCalc = hierarchyCalc;
        }

        @Override
		public Dimension evaluate(Evaluator evaluator) {
            Hierarchy hierarchy =
                    hierarchyCalc.evaluate(evaluator);
            return hierarchy.getDimension();
        }
    }
}
