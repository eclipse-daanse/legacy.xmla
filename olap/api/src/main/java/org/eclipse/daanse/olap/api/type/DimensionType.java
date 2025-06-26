/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2005-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara
 * All Rights Reserved.
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
 */

package org.eclipse.daanse.olap.api.type;

import java.util.Objects;

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;


/**
 * The type of an expression which represents a Dimension.
 *
 * @author jhyde
 * @since Feb 17, 2005
 */
public class DimensionType implements Type {
    private final Dimension dimension;
    private final String digest;

    public static final DimensionType Unknown = new DimensionType(null);

    /**
     * Creates a type representing a dimension.
     *
     * @param dimension Dimension that values of this type must belong to, or
     *   null if the dimension is unknown
     */
    public DimensionType(Dimension dimension) {
        this.dimension = dimension;
        StringBuilder buf = new StringBuilder("DimensionType<");
        if (dimension != null) {
            buf.append("dimension=").append(dimension.getUniqueName());
        }
        buf.append(">");
        this.digest = buf.toString();
    }

    public static DimensionType forDimension(Dimension dimension) {
        return new DimensionType(dimension);
    }

    public static DimensionType forType(Type type) {
        return new DimensionType(type.getDimension());
    }

    @Override
	public boolean usesDimension(Dimension dimension, boolean definitely) {
        // REVIEW: Should be '!definitely'?
        return this.dimension == dimension
            || (definitely && this.dimension == null);
    }

    @Override
	public boolean usesHierarchy(Hierarchy hierarchy, boolean definitely) {
        // If hierarchy belongs to this type's dimension, we might use it.
        return hierarchy.getDimension() == this.dimension
            && !definitely;
    }

    @Override
	public Hierarchy getHierarchy() {
        if (dimension == null) {
            return null;
        } else {
            return dimension.getHierarchy();
        }
    }

    @Override
	public Level getLevel() {
        return null;
    }

    @Override
	public Dimension getDimension() {
        return dimension;
    }

    @Override
	public int hashCode() {
        return digest.hashCode();
    }

    @Override
	public boolean equals(Object obj) {
        if (obj instanceof DimensionType that) {
            return Objects.equals(this.getDimension(), that.getDimension());
        }
        return false;
    }

    @Override
	public String toString() {
        return digest;
    }

    @Override
	public Type computeCommonType(Type type, int[] conversionCount) {
        if (conversionCount != null && type instanceof HierarchyType hierarchyType) {
            if (Objects.equals(hierarchyType.getDimension(), dimension)) {
                ++conversionCount[0];
                return this;
            }
            return null;
        }
        if (!(type instanceof DimensionType that)) {
            return null;
        }
        if (this.getDimension() != null
            && this.getDimension().equals(that.getDimension()))
        {
            return new DimensionType(
                this.getDimension());
        }
        return DimensionType.Unknown;
    }

    @Override
	public boolean isInstance(Object value) {
        return value instanceof Dimension
            && (dimension == null
                || value.equals(dimension));
    }

    @Override
	public int getArity() {
        return 1;
    }
}
