/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.function.definition.math;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.CaseOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.query.base.Expressions;

public class CaseTestFunctionResolver implements FunctionResolver {

	private static final String NAME = "_CaseTest";
	private static final OperationAtom OPERATION_ATOM = new CaseOperationAtom(NAME);

	private static final String SIGNATURE = "Case When <Logical Expression> Then <Expression> [...] [Else <Expression>] End";
	private static final String DESCRIPTION = "Evaluates various conditions, and returns the corresponding expression for the first which evaluates to true.";

	@Override
	public FunctionDefinition resolve(Expression[] argumentExpressions, Validator validator,
			List<Conversion> conversions) {
		if (argumentExpressions.length < 1) {
			return null;
		}
		int clauseCount = argumentExpressions.length / 2;

		DataType returnType = argumentExpressions[1].getCategory();

		int mismatchingArgs = 0;
		int j = 0;
		for (int i = 0; i < clauseCount; i++) {
			// when
			if (!validator.canConvert(j, argumentExpressions[j++], DataType.LOGICAL, conversions)) {
				mismatchingArgs++;
			}
			// then
			if (!validator.canConvert(j, argumentExpressions[j++], returnType, conversions)) {
				mismatchingArgs++;
			}
		}

		// default value
		if (j < argumentExpressions.length
				&& !validator.canConvert(j, argumentExpressions[j++], returnType, conversions)) {
			mismatchingArgs++;
		}

		if (mismatchingArgs != 0) {
			return null;
		}
		FunctionMetaData functionInformation = new FunctionMetaDataR(OPERATION_ATOM,
				DESCRIPTION,
				SIGNATURE, 
				returnType, Expressions.categoriesOf(argumentExpressions));
		return new CaseTestFunctionDefinition(functionInformation);
	}

	@Override
	public boolean requiresScalarExpressionOnArgument(int k) {
		return true;
	}

	@Override
	public OperationAtom getFunctionAtom() {
		return OPERATION_ATOM;
	}

}