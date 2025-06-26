/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2000-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * Contributors:
 *  SmartCity Jena - refactor, clean API
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */


package org.eclipse.daanse.olap.api.element;

import java.util.List;

import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.Type;

/**
 * A named set of members or tuples.
 *
 * <p>
 * A set can be defined in a query, using a <code>WITH SET</code> clause, or in
 * a schema. Named sets in a schema can be defined against a particular cube or
 * virtual cube, or shared between all cubes.
 * </p>
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public interface NamedSet extends OlapElement, MetaElement {
    /**
     * Sets the name of this named set.
     */
    void setName(String newName);

    /**
     * Returns the type of this named set.
     */
    Type getType();

    /**
     * Returns the expression used to derive this named set.
     */
    Expression getExp();

    NamedSet validate(Validator validator);

    /**
     * Returns a name for this set that is unique within the query.
     *
     * <p>
     * This is necessary when there are several 'AS' expressions, or an 'AS'
     * expression overrides a named set defined using 'WITH MEMBER' clause or
     * against a cube.
     */
    String getNameUniqueWithinQuery();

    /**
     * Returns whether this named set is dynamic.
     *
     * <p>
     * Evaluation rules:
     * <ul>
     * <li>A dynamic set is evaluated each time it is used, and inherits the context
     * in which it is evaluated.
     * <li>A static set is evaluated only on first use, in the base context of the
     * cube.
     * </ul>
     *
     * @return Whether this named set is dynamic
     */
    boolean isDynamic();

    List<Hierarchy> getHierarchies();

    String getDisplayFolder();
}
