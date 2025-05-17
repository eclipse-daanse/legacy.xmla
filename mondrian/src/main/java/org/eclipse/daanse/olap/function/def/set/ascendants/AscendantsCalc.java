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
package org.eclipse.daanse.olap.function.def.set.ascendants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.AbstractProfilingNestedTupleListCalc;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;

public class AscendantsCalc extends AbstractProfilingNestedTupleListCalc {

    protected AscendantsCalc(Type type, final MemberCalc memberCalc) {
        super(type, memberCalc);
    }

    @Override
    public TupleList evaluate(Evaluator evaluator) {
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        return new UnaryTupleList(ascendants(evaluator.getCatalogReader(), member));
    }

    private List<Member> ascendants(CatalogReader schemaReader, Member member) {
        if (member.isNull()) {
            return Collections.emptyList();
        }
        final List<Member> result = new ArrayList<>();
        result.add(member);
        schemaReader.getMemberAncestors(member, result);
        return result;
    }

}
