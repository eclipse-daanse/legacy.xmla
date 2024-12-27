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
package org.eclipse.daanse.olap.function.def.ancestor;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.MondrianException;
import mondrian.olap.type.LevelType;

class AncestorLevelFunDef extends AbstractFunctionDefinition {

	static final FunctionMetaData fmdLevel = new FunctionMetaDataR(AncestorResolver.operationAtom,
			"Ancestor(<Member>, <Level>)", "Returns the ancestor of a member at a specified level.", DataType.MEMBER,
			new FunctionParameterR[] { new FunctionParameterR(  DataType.MEMBER ), new FunctionParameterR( DataType.LEVEL ) });

	public AncestorLevelFunDef() {
		super(fmdLevel);
	}

	@Override
	public Calc<Member> compileCall(ResolvedFunCall call, ExpressionCompiler compiler) {
		final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
		Expression expressionOfArg1 = call.getArg(1);
		final Type type1 = expressionOfArg1.getType();
		if (!(type1 instanceof LevelType)) {
			new MondrianException("unexpected type: " + type1 + " sould be " + LevelType.class);
		}
		final LevelCalc levelCalc = compiler.compileLevel(expressionOfArg1);

		return new AncestorLevelCalc(call.getType(), memberCalc, levelCalc);
	}

}
