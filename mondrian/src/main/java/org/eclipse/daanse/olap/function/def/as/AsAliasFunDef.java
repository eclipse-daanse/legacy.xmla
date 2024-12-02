/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractIterCalc;
import mondrian.olap.QueryImpl;

/**
 * Definition of the <code>AS</code> MDX operator.
 *
 * <p>
 * Using <code>AS</code>, you can define an alias for an MDX expression anywhere
 * it appears in a query, and use that alias as you would a calculated yet.
 *
 * @author jhyde
 * @since Oct 7, 2009
 */
public class AsAliasFunDef extends AbstractFunctionDefinition {

	static final String SIGNATURE = "<Expression> AS <Name>";
	static final String DESCRIPTION = "Assigns an alias to an expression";

	static final OperationAtom functionAtom = new InfixOperationAtom("AS");
	static final FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, DESCRIPTION, SIGNATURE,
			DataType.SET, new DataType[] { DataType.SET, DataType.NUMERIC });

	private final QueryImpl.ScopedNamedSet scopedNamedSet;

	public AsAliasFunDef(QueryImpl.ScopedNamedSet scopedNamedSet) {
		super(functionMetaData);
		this.scopedNamedSet = scopedNamedSet;
	}

	@Override
	public Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		// Argument 0, the definition of the set, has been resolved since the
		// scoped named set was created. Implicit conversions, like converting
		// a member to a set, have been performed. Use the new expression.
		scopedNamedSet.setExp(call.getArg(0));

		return new AbstractIterCalc(call.getType(), new Calc[0]) {
			@Override
			public TupleIterable evaluateIterable(Evaluator evaluator) {
				Evaluator.NamedSetEvaluator namedSetEvaluator = evaluator.getNamedSetEvaluator(scopedNamedSet, false);
				return namedSetEvaluator.evaluateTupleIterable(evaluator);
			}
		};
	}

}
