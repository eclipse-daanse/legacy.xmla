/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 1998-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package org.eclipse.daanse.olap.query.component;

import java.util.List;

import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.query.component.CellProperty;

import mondrian.olap.AbstractQueryPart;
import mondrian.olap.Util;

/**
 * Represents Cell Property.
 *
 * @author Shishir
 * @since 08 May, 2007
 */

public class CellPropertyImpl extends AbstractQueryPart implements CellProperty {
    private String name;

    public CellPropertyImpl(List<Segment> segments) {
        this.name = Util.implode(segments);
    }

    /**
     * checks whether cell property is equals to passed parameter.
     * It adds '[' and ']' before and after the propertyName before comparing.
     * The comparison is case insensitive.
     */
    @Override
    public boolean isNameEquals(String propertyName) {
        return name.equalsIgnoreCase(Util.quoteMdxIdentifier(propertyName));
    }

    @Override
	public String toString() {
        return name;
    }
}
