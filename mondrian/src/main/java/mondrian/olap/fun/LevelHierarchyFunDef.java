/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedHierarchyCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>&lt;Level&gt;.Hierarchy</code> MDX builtin function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class LevelHierarchyFunDef extends AbstractFunctionDefinition {

    static OperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("Hierarchy");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns a level's hierarchy.",
            "<LEVEL>.Hierarchy", DataType.HIERARCHY, new DataType[] { DataType.LEVEL });

    static final LevelHierarchyFunDef instance = new LevelHierarchyFunDef();

    private LevelHierarchyFunDef() {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final LevelCalc levelCalc =
                compiler.compileLevel(call.getArg(0));
        return new LevelHirarchyCalc(call.getType(), levelCalc);
    }

    public static class LevelHirarchyCalc extends AbstractProfilingNestedHierarchyCalc {
        private final LevelCalc levelCalc;

        public LevelHirarchyCalc(Type type, LevelCalc levelCalc) {
            super(type, new Calc[] {levelCalc});
            this.levelCalc = levelCalc;
        }

        @Override
		public Hierarchy evaluate(Evaluator evaluator) {
            Level level = levelCalc.evaluate(evaluator);
            return level.getHierarchy();
        }
    }
}
