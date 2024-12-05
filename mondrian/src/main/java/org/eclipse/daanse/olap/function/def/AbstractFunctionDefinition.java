/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2002-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.function.def;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionPrinter;

import mondrian.mdx.ResolvedFunCallImpl;
import mondrian.olap.fun.FunUtil;
import mondrian.olap.type.TypeUtil;

public abstract class AbstractFunctionDefinition implements FunctionDefinition {

	private FunctionMetaData functionMetaData;

	public AbstractFunctionDefinition(FunctionMetaData functionMetaData) {
		this.functionMetaData = functionMetaData;
	}

	@Override
	public FunctionMetaData getFunctionMetaData() {
		return functionMetaData;
	}

	@Override
	public Expression createCall(Validator validator, Expression[] args) {
		DataType[] categories = functionMetaData.parameterDataTypes();

		if (categories.length != args.length) {
			throw new IllegalArgumentException("Categories does not match arguments count");
		}

		for (int i = 0; i < args.length; i++) {
			args[i] = validateArgument(validator, args, i, categories[i]);
		}

		final Type type = getResultType(validator, args);

		return new ResolvedFunCallImpl(this, args, type);
	}

	/**
	 * Validates an argument to a call to this function.
	 *
	 * <p>
	 * The default implementation of this method adds an implicit conversion to the
	 * correct type. Derived classes may override.
	 *
	 * @param validator      Validator
	 * @param arguments      Arguments to this function
	 * @param argumentNumber Ordinal of argument
	 * @param category       Expected {@link DataType category} of argument
	 * @return Validated argument
	 */
	protected Expression validateArgument(Validator validator, Expression[] arguments, int argumentNumber,
			DataType category) {
		return arguments[argumentNumber];
	}

	/**
	 * Returns the type of a call to this function with a given set of arguments.
	 * <p/>
	 *
	 * The default implementation makes the coarse assumption that the return type
	 * is in some way related to the type of the first argument. Operators whose
	 * arguments don't follow the requirements of this implementation should
	 * override this method.
	 * <p/>
	 *
	 * If the function definition says it returns a literal type (numeric, string,
	 * symbol) then it's a fair guess that the function call returns the same kind
	 * of value.
	 * <p/>
	 *
	 * If the function definition says it returns an object type (cube, dimension,
	 * hierarchy, level, member) then we check the first argument of the function.
	 * Suppose that the function definition says that it returns a hierarchy, and
	 * the first argument of the function happens to be a member. Then it's
	 * reasonable to assume that this function returns a member.
	 *
	 * @param validator Validator
	 * @param args      Arguments to the call to this operator
	 * @return result type of a call this function
	 */
	public Type getResultType(Validator validator, Expression[] args) {
		Type firstArgType = args.length > 0 ? args[0].getType() : null;
		Type type = TypeUtil.castType(firstArgType, functionMetaData.returnCategory());
		if (type != null) {
			return type;
		}
		throw new IllegalArgumentException(new StringBuilder("Cannot deduce type of call to function '")
				.append(this.functionMetaData.operationAtom().name()).append("'").toString());
	}

	@Override
	public String getSignature() {
		FunctionMetaData fi = getFunctionMetaData();
		return FunctionPrinter.getSignature(fi);
	}

	@Override
	public void unparse(Expression[] args, PrintWriter pw) {

		FunctionPrinter.unparse(getFunctionMetaData().operationAtom(), args, pw);

	}

}
