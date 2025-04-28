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

package mondrian.olap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.query.component.CellPropertyImpl;
import org.eclipse.daanse.olap.query.component.IdImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for <code>Cell Property<code>.
 *
 * @author Shishir
 * @since 08 May, 2007
 */
class CellPropertyTest{
    private CellPropertyImpl cellProperty;

    @BeforeEach
    protected void setUp() throws Exception {

        cellProperty = new CellPropertyImpl(IdImpl.toList("Format_String"));
    }

    @Test
    void testIsNameEquals() {
        assertTrue(cellProperty.isNameEquals("Format_String"));
    }

    @Test
    void testIsNameEqualsDoesCaseInsensitiveMatch() {
        assertTrue(cellProperty.isNameEquals("format_string"));
    }

    @Test
    void testIsNameEqualsParameterShouldNotBeQuoted() {
        assertFalse(cellProperty.isNameEquals("[Format_String]"));
    }

}
