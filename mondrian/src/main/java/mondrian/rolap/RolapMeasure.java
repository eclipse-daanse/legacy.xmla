/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.rolap;

import org.eclipse.daanse.olap.api.element.Measure;

/**
 * Interface implemented by all measures (both stored and calculated).
 *
 * @author jhyde
 * @since 10 August, 2001
 */
public interface RolapMeasure extends Measure {
    /**
     * Returns the object that formats cells of this measure, or null to use
     * default formatting.
     *
     * @return formatter
     */
    RolapResult.ValueFormatter getFormatter();
}
