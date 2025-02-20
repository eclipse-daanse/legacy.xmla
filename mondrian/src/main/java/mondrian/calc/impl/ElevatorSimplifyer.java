package mondrian.calc.impl;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.calc.api.Calc;

import mondrian.rolap.RolapEvaluator;

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
		final List<Hierarchy> hierarchies = ((RolapEvaluator) evaluator).getCube().getHierarchies();
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