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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReassemblyGuide {
    private final int index;
    private final List<ReassemblyCommand> commands =
        new ArrayList<>();

    public ReassemblyGuide(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public List<ReassemblyCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    void addCommandTuple(List<ReassemblyCommand> commandTuple) {
        ReassemblyCommand curr = currentCommand(commandTuple);

        if (index < commandTuple.size() - 1) {
            curr.forNextCol(index + 1).addCommandTuple(commandTuple);
        }
    }

    private ReassemblyCommand currentCommand(
        List<ReassemblyCommand> commandTuple)
    {
        ReassemblyCommand curr = commandTuple.get(index);
        ReassemblyCommand prev = commands.isEmpty()
            ? null : commands.get(commands.size() - 1);

        if (prev != null && prev.getMemberType() == NativeElementType.SENTINEL) {
            commands.set(commands.size() - 1, curr);
        } else if (prev == null
            || !prev.getElement().equals(curr.getElement()))
        {
            commands.add(curr);
        } else {
            curr = prev;
        }
        return curr;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(index).append(":")
            .append(commands.toString().replaceAll("=null", "").replaceAll("=", " "))
                .append(" ").toString();
    }
}
