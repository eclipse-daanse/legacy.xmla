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
package org.eclipse.daanse.olap.calc.base.compiler;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;

public class ElevatorSimplifyer {
	/**
	 * Returns a simplified evalator whose context is the same for every dimension
	 * which an expression depends on, and the default member for every dimension
	 * which it does not depend on.
	 *
	 * <p>
	 * The default member is often the 'all' member, so this evaluator is usually
	 * the most efficient context in which to evaluate the expression.
	 *
	 * @param calc
	 * @param evaluator
	 */
	public static Evaluator simplifyEvaluator(Calc calc, Evaluator evaluator) {
		if (evaluator.isNonEmpty()) {
			// If NON EMPTY is present, we cannot simplify the context, because
			// we have to assume that the expression depends on everything.
			// TODO: Bug 1456418: Convert 'NON EMPTY Crossjoin' to
			// 'NonEmptyCrossJoin'.
			return evaluator;
		}
		int changeCount = 0;
		Evaluator ev = evaluator;
		final List<Hierarchy> hierarchies = evaluator.getCube().getHierarchies();
		for (final Hierarchy hierarchy : hierarchies) {
			final Member member = ev.getContext(hierarchy);
			if (member.isAll() || calc.dependsOn(hierarchy)) {
				continue;
			}
			final Member unconstrainedMember = member.getHierarchy().getDefaultMember();
			if (member == unconstrainedMember) {
				// This is a hierarchy without an 'all' member, and the context
				// is already the default member.
				continue;
			}
			if (changeCount++ == 0) {
				ev = evaluator.push();
			}
			ev.setContext(unconstrainedMember);
		}
		return ev;
	}
}