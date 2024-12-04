package org.eclipse.daanse.olap.function.def.dimensions.numeric;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedHierarchyCalc;

import mondrian.olap.fun.FunUtil;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapHierarchy;

public class DimensionNumericCalc extends AbstractProfilingNestedHierarchyCalc {

	public DimensionNumericCalc(Type type, IntegerCalc integerCalc) {
		super(type, integerCalc);
	}

	@Override
	public Hierarchy evaluate(Evaluator evaluator) {
		Integer n = getChildCalc(0, IntegerCalc.class).evaluate(evaluator);
		return nthHierarchy(evaluator, n);
	}

	private RolapHierarchy nthHierarchy(Evaluator evaluator, Integer n) {
		RolapCube cube = (RolapCube) evaluator.getCube();
		List<RolapHierarchy> hierarchies = cube.getHierarchies();
		if (n >= hierarchies.size() || n < 0) {
			throw FunUtil.newEvalException(DimensionsNumericFunDef.functionalMetaData,
					new StringBuilder("Index '").append(n).append("' out of bounds").toString());
		}
		// n=0 is the Measurement Hierarchy
		return hierarchies.get(n);
	}
}