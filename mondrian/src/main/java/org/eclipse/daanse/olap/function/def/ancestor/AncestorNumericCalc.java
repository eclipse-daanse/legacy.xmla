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

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;

import mondrian.olap.fun.FunUtil;

public class AncestorNumericCalc extends AbstractProfilingNestedMemberCalc<Calc<?>> {
	private final IntegerCalc distanceCalc;
	private final MemberCalc memberCalc;

	public AncestorNumericCalc(Type type, MemberCalc memberCalc, IntegerCalc distanceCalc) {
		super(type, new Calc<?>[] { memberCalc, distanceCalc });
		this.memberCalc = memberCalc;
		this.distanceCalc = distanceCalc;
	}

	@Override
	public Member evaluate(Evaluator evaluator) {
		Integer distance = distanceCalc.evaluate(evaluator);
		Member member = memberCalc.evaluate(evaluator);
		return FunUtil.ancestor(evaluator, member, distance, null);
	}
}