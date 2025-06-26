/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.check.predicate;

import java.util.function.Predicate;

import org.eclipse.daanse.olap.api.element.Member;

public class MemberPredicates {

    public static Predicate<Member> isExist() {
        return m -> m != null;
    }

    public static Predicate<Member> hasCaption() {
        return m -> m.getCaption() != null;
    }

    public static Predicate<Member> hasDataMember() {
        return m -> m.getDataMember() != null;
    }

    public static Predicate<Member> hasDescription() {
        return m -> m.getDescription() != null;
    }

    public static Predicate<Member> hasDimension() {
        return m -> m.getDimension() != null;
    }

    public static Predicate<Member> hasExpression() {
        return m -> m.getExpression() != null;
    }

    public static Predicate<Member> hasHierarchy() {
        return m -> m.getHierarchy() != null;
    }

    public static Predicate<Member> hasLevel() {
        return m -> m.getLevel() != null;
    }

    public static Predicate<Member> hasMemberType() {
        return m -> m.getMemberType() != null;
    }

    public static Predicate<Member> hasMetaData() {
        return m -> m.getMetaData() != null;
    }

    public static Predicate<Member> hasName() {
        return m -> m.getName() != null;
    }

    public static Predicate<Member> hasParentMember() {
        return m -> m.getParentMember() != null;
    }

    public static Predicate<Member> hasParentUniqueName() {
        return m -> m.getParentUniqueName() != null;
    }

    public static Predicate<Member> hasProperties() {
        return m -> m.getProperties() != null && m.getProperties().length > 0;
    }

    public static Predicate<Member> hasQualifiedName() {
        return m -> m.getQualifiedName() != null;
    }

    public static Predicate<Member> hasUniqueName() {
        return m -> m.getUniqueName() != null;
    }

}
