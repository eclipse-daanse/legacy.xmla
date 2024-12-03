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
package org.eclipse.daanse.olap.function.def.dimension.hierarchy;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class DimensionOfHierarchyFunDef extends AbstractFunctionDefinition {

	private static final OperationAtom atom = new PlainPropertyOperationAtom("Dimension");

	private static final FunctionMetaData functionMetaData = new FunctionMetaDataR(atom,
			"Returns the dimension that contains a specified hierarchy.", "<Hierarchy>.Dimension", DataType.DIMENSION,
			new DataType[] { DataType.HIERARCHY });

	public DimensionOfHierarchyFunDef() {
		super(functionMetaData);
	}

	@Override
	public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		final HierarchyCalc hierarchyCalc = compiler.compileHierarchy(call.getArg(0));
		return new DimensionOfHierarchyCalc(call.getType(), hierarchyCalc);
	}

}
