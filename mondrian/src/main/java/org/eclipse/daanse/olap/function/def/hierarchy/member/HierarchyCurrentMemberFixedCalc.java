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
package org.eclipse.daanse.olap.function.def.hierarchy.member;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;

import mondrian.rolap.RolapHierarchy;

public class HierarchyCurrentMemberFixedCalc extends AbstractProfilingNestedMemberCalc {
    // getContext works faster if we give RolapHierarchy rather than
    // Hierarchy
    private final RolapHierarchy hierarchy;

    public HierarchyCurrentMemberFixedCalc(Type type, Hierarchy hierarchy) {
        super(type, new Calc[] {});
        assert hierarchy != null;
        this.hierarchy = (RolapHierarchy) hierarchy;
    }

    @Override
    public Member evaluate(Evaluator evaluator) {
        HierarchyCurrentMemberCalc.validateSlicerMembers(hierarchy, evaluator);
        return evaluator.getContext(hierarchy);
    }

    @Override
    public boolean dependsOn(Hierarchy hierarchy) {
        return this.hierarchy == hierarchy;
    }
}
