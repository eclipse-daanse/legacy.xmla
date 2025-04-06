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
package org.eclipse.daanse.olap.function.def.set.members;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.olap.fun.FunUtil;

public class MembersCalc extends AbstractListCalc {

    protected MembersCalc(Type type, final HierarchyCalc hierarchyCalc) {
        super(type, hierarchyCalc);
    }

    @Override
    public TupleList evaluateList(Evaluator evaluator) {
        Hierarchy hierarchy = getChildCalc(0, HierarchyCalc.class).evaluate(evaluator);
        return FunUtil.hierarchyMembers(hierarchy, evaluator, false);
    }

}
