/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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
 */
package org.eclipse.daanse.olap.function.def.as;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.NamedSetExpression;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.eclipse.daanse.olap.query.component.QueryImpl;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class AsAliasResolver extends NoExpressionRequiredFunctionResolver {

	@Override
	public FunctionDefinition resolve(Expression[] args, Validator validator, List<Conversion> conversions) {
		if (!validator.canConvert(0, args[0], DataType.SET, conversions)) {
			return null;
		}

		// By the time resolve is called, the id argument has already been
		// resolved... to a named set, namely itself. That's not pretty.
		// We'd rather it stayed as an id, and we'd rather that a named set
		// was not visible in the scope that defines it. But we can work
		// with this.

		NamedSetExpression nse = (NamedSetExpression) args[1];

		QueryImpl.ScopedNamedSet scopedNamedSet = (QueryImpl.ScopedNamedSet) nse.getNamedSet();

		return new AsAliasFunDef(scopedNamedSet);
	}

	@Override
	public OperationAtom getFunctionAtom() {
		return AsAliasFunDef.functionAtom;
	}
}