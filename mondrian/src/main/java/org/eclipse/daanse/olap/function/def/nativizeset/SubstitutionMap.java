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
package org.eclipse.daanse.olap.function.def.nativizeset;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Id;

public class SubstitutionMap  {
    private final Map<String, Level> map = new HashMap<>();

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Member member) {
        return map.containsKey(toKey(member));
    }

    public Level get(Member member) {
        return map.get(toKey(member));
    }

    public Level put(Id id, Level level) {
        return map.put(toKey(id), level);
    }

    public Collection<Level> values() {
        return map.values();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    private String toKey(Id id) {
        return id.toString();
    }

    private String toKey(Member member) {
        return member.getUniqueName();
    }
}
