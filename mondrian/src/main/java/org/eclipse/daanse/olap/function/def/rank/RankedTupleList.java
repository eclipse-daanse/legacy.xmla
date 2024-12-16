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
package org.eclipse.daanse.olap.function.def.rank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;

public class RankedTupleList {
    final Map<List<Member>, Integer> map = new HashMap<>();

    RankedTupleList(TupleList tupleList) {
        int i = -1;
        for (final List<Member> tupleMembers : tupleList.fix()) {
            ++i;
            final Integer value = map.put(tupleMembers, i);
            if (value != null) {
                // The list already contained a value for this key -- put
                // it back.
                map.put(tupleMembers, value);
            }
        }
    }

    int indexOf(List<Member> tupleMembers) {
        Integer integer = map.get(tupleMembers);
        if (integer == null) {
            return -1;
        } else {
            return integer;
        }
    }
}
