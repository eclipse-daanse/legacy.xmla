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
package org.eclipse.daanse.olap.function.def.dimension.dimension;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDimensionCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

public class DimensionOfDimensionFunDef extends AbstractFunctionDefinition {

	private static final OperationAtom atom = new PlainPropertyOperationAtom("Dimension");

	private static final FunctionMetaData functionMetaData = new FunctionMetaDataR(atom,
			"Returns the dimension that contains a specified dimension.", "<DIMENSION>.Dimension", DataType.DIMENSION,
			new FunctionParameterR[] { new FunctionParameterR(  DataType.DIMENSION ) });

	public DimensionOfDimensionFunDef() {
		super(functionMetaData);
	}

	@Override
	public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		DimensionExpression dimensionExpression = (DimensionExpression) call.getArg(0);
		Dimension dimension = dimensionExpression.getDimension();
		return ConstantDimensionCalc.of(dimension);
	}

}
