/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedHierarchyCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.Util;
import mondrian.olap.type.HierarchyType;

/**
 * Definition of the <code>Dimensions(&lt;String Expression&gt;)</code>
 * MDX builtin function.
 *
 * <p>NOTE: Actually returns a hierarchy. This is consistent with Analysis
 * Services.
 *
 * @author jhyde
 * @since Jul 20, 2009
 */
class DimensionsStringFunDef extends AbstractFunctionDefinition {

    static OperationAtom functionAtom = new FunctionOperationAtom("Dimensions");

    static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Returns the hierarchy whose name is specified by a string.",
  	        "function(<STRING>)", DataType.HIERARCHY, new DataType[] { DataType.STRING });
    
    public static final AbstractFunctionDefinition INSTANCE = new DimensionsStringFunDef();
    
    private DimensionsStringFunDef() {
        super(functionMetaData);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        return HierarchyType.Unknown;
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler)
    {
        final StringCalc stringCalc =
            compiler.compileString(call.getArg(0));
        return new AbstractProfilingNestedHierarchyCalc(call.getType(), new Calc[] {stringCalc})
        {
            @Override
			public Hierarchy evaluate(Evaluator evaluator) {
                String dimensionName =
                    stringCalc.evaluate(evaluator);
                return findHierarchy(dimensionName, evaluator);
            }
        };
    }

    /**
     * Looks up a hierarchy in the current cube with a given name.
     *
     * @param name Hierarchy name
     * @param evaluator Evaluator
     * @return Hierarchy
     */
    Hierarchy findHierarchy(String name, Evaluator evaluator) {
        if (name.indexOf("[") == -1) {
            name = Util.quoteMdxIdentifier(name);
        }
        OlapElement o = evaluator.getSchemaReader().lookupCompound(
            evaluator.getCube(),
            Util.parseIdentifier(name),
            false,
            DataType.HIERARCHY);
        if (o instanceof Hierarchy hierarchy) {
            return hierarchy;
        } else if (o == null) {
            throw FunUtil.newEvalException(
                this.getFunctionMetaData(), new StringBuilder("Hierarchy '").append(name).append("' not found").toString());
        } else {
            throw FunUtil.newEvalException(
                this.getFunctionMetaData(), new StringBuilder("Hierarchy(").append(name).append(") found ").append(o).toString());
        }
    }
}
