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
package org.eclipse.daanse.olap.function.core.resolver;

import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.query.base.Expressions;

public abstract class AbstractMetaDataMultiResolver implements FunctionResolver {

	private final List<String> reservedWords;
	private final OperationAtom operationAtom;
	private List<FunctionMetaData> fmds;

	public AbstractMetaDataMultiResolver(List<FunctionMetaData> fmds) {
		this(fmds, List.of());
	}

	public AbstractMetaDataMultiResolver(List<FunctionMetaData> fmds, List<String> reservedWords) {
		this.fmds = fmds;
		this.reservedWords = reservedWords == null ? List.of() : reservedWords;

		this.operationAtom = fmds.getFirst().operationAtom();
		for (FunctionMetaData fmd : fmds) {
			OperationAtom operationAtomTemp = fmd.operationAtom();
			if (!Objects.equals(operationAtom, operationAtomTemp)) {
				throw new OlapRuntimeException("all FunctionMetaData inside a Resolver must have same OperationAtom");
			}
		}
	}

	@Override
	public FunctionDefinition resolve(Expression[] expressions, Validator validator, List<Conversion> conversions) {
		outer: for (FunctionMetaData functionMetaData : fmds) {
			DataType[] parameterTypes = functionMetaData.parameterDataTypes();
			if (parameterTypes.length != expressions.length) {
				continue;
			}
			conversions.clear();
			for (int i = 0; i < expressions.length; i++) {
				if (!validator.canConvert(i, expressions[i], parameterTypes[i], conversions)) {
					continue outer;
				}
			}

			FunctionParameterR[] paramDataTypesOfExpr = Expressions.functionParameterOf(expressions);
			FunctionMetaData fmdTarget = new FunctionMetaDataR(operationAtom, functionMetaData.description(),
					functionMetaData.returnCategory(), paramDataTypesOfExpr);
			return createFunDef(expressions, functionMetaData, fmdTarget);
		}
		return null;
	}

	@Override
	public boolean requiresScalarExpressionOnArgument(int k) {
		for (FunctionMetaData fmd : fmds) {
			DataType[] parameterTypes = fmd.parameterDataTypes();
			if ((k < parameterTypes.length) && parameterTypes[k] == DataType.SET) {
				return false;
			}
		}
		return true;
	}

	@Override
	public OperationAtom getFunctionAtom() {
		return operationAtom;
	}

	@Override
	public List<String> getReservedWords() {
		return reservedWords;
	}

	protected abstract FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData,
			FunctionMetaData fmdTarget);
}
