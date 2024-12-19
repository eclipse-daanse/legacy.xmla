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
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;

public class ReassemblyCommand  {
    private final OlapElement element;
    private final String elementName;
    private final NativeElementType memberType;
    private ReassemblyGuide nextColGuide;

    public ReassemblyCommand(
        Member member,
        NativeElementType memberType)
    {
        this.element = member;
        this.memberType = memberType;
        this.elementName = member.toString();
    }

    public ReassemblyCommand(
        Level level,
        NativeElementType memberType)
    {
        this.element = level;
        this.memberType = memberType;
        this.elementName = new StringBuilder(level.toString()).append(".members").toString();
    }

    public OlapElement getElement() {
        return element;
    }

    public String getElementName() {
        return elementName;
    }

    public Member getMember() {
        return (Member) element;
    }

    public Level getLevel() {
        return (Level) element;
    }

    public boolean hasNextGuide() {
        return nextColGuide != null;
    }

    public ReassemblyGuide forNextCol() {
        return nextColGuide;
    }

    public ReassemblyGuide forNextCol(int index) {
        if (nextColGuide == null) {
            nextColGuide = new ReassemblyGuide(index);
        }
        return nextColGuide;
    }

    public NativeElementType getMemberType() {
        return memberType;
    }

    public static Set<NativeElementType> getMemberTypes(
        Collection<ReassemblyCommand> commands)
    {
        Set<NativeElementType> types =
                EnumSet.noneOf(NativeElementType.class);
        for (ReassemblyCommand command : commands) {
            types.add(command.getMemberType());
        }
        return types;
    }

    @Override
    public String toString() {
        return new StringBuilder(memberType.toString()).append(": ").append(getElementName()).toString();
    }
}
