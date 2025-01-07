/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara.
 * Copyright (C) 2021 Sergei Semenkov
 * All rights reserved.
 */

package mondrian.calc.impl;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.member.UnknownToMemberCalc;
import org.eclipse.daanse.olap.calc.base.type.tuple.MemberCalcToTupleCalc;
import org.eclipse.daanse.olap.calc.base.type.tuple.UnknownToTupleCalc;

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
			final mondrian.mdx.UnresolvedFunCallImpl unresolvedFunCall = new mondrian.mdx.UnresolvedFunCallImpl(
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
			
			//TODO:	use org.eclipse.daanse.olap.calc.base.type.tuplelist.CopyOfTupleListCalc
			return new CopyListCalc(tupleListCalc);
		}
		return tupleListCalc;
	}

//TODO:	use org.eclipse.daanse.olap.calc.base.type.tuplelist.CopyOfTupleListCalc
@Deprecated
private static class CopyListCalc extends AbstractListCalc {
		private final TupleListCalc tupleListCalc;

		public CopyListCalc(TupleListCalc tupleListCalc) {
			super( tupleListCalc.getType(), new Calc[] { tupleListCalc });
			this.tupleListCalc = tupleListCalc;
		}

		@Override
		public TupleList evaluateList(Evaluator evaluator) {
			final TupleList list = tupleListCalc.evaluateList(evaluator);
			return list.copyList(-1);
		}
	}
}
