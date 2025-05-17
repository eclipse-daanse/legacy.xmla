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
 *   Sergei Semenkov (2001)
 *   SmartCity Jena - initial
 */
/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara.
 * All rights reserved.
 */

package org.eclipse.daanse.olap.calc.base.compiler;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.member.UnknownToMemberCalc;
import org.eclipse.daanse.olap.calc.base.type.tuple.MemberCalcToTupleCalc;
import org.eclipse.daanse.olap.calc.base.type.tuple.UnknownToTupleCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplelist.CopyOfTupleListCalc;

import mondrian.olap.Util;

/**
 * Enhanced expression compiler. It can generate code to convert between scalar
 * types.
 *
 * @author jhyde
 * @since Sep 29, 2005
 */
public class BetterExpCompiler extends AbstractExpCompiler {
	public BetterExpCompiler(Evaluator evaluator, Validator validator) {
		super(evaluator, validator);
	}

	public BetterExpCompiler(Evaluator evaluator, Validator validator, List<ResultStyle> resultStyles) {
		super(evaluator, validator, resultStyles);
	}

	@Override
	public TupleCalc compileTuple(Expression exp) {
		final Calc<?> calc = compile(exp);
		final Type type = exp.getType();
		if (type instanceof org.eclipse.daanse.olap.api.type.DimensionType || type instanceof org.eclipse.daanse.olap.api.type.HierarchyType) {
			final org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl unresolvedFunCall = new org.eclipse.daanse.olap.query.component.UnresolvedFunCallImpl(
					new PlainPropertyOperationAtom("DefaultMember"), new Expression[] { exp });
			final Expression defaultMember = unresolvedFunCall.accept(getValidator());
			return compileTuple(defaultMember);
		}
		if (type instanceof TupleType) {

			if (calc instanceof TupleCalc tc) {
				return tc;
			}

			TupleCalc tc = new UnknownToTupleCalc( type, calc);
			return tc;
		} else if (type instanceof MemberType) {
			MemberCalc tmpCalc = null;
			if (calc instanceof MemberCalc mc) {
				tmpCalc = mc;
			} else {
				tmpCalc = new UnknownToMemberCalc( type, calc);
			}
			final MemberCalc memberCalc = tmpCalc;
			return new MemberCalcToTupleCalc( type,  memberCalc);

		} else {
			throw Util.newInternal("cannot cast " + exp);
		}
	}

	@Override
	public TupleListCalc compileList(Expression exp, boolean mutable) {
		final TupleListCalc tupleListCalc = super.compileList(exp, mutable);
		if (mutable && tupleListCalc.getResultStyle() == ResultStyle.LIST) {
			// Wrap the expression in an expression which creates a mutable
			// copy.
			
			return new CopyOfTupleListCalc(tupleListCalc);
		}
		return tupleListCalc;
	}

}
