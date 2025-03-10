/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */
package mondrian.rolap.format;

import org.eclipse.daanse.olap.api.formatter.CellFormatter;

public class CellFormatterImpl implements CellFormatter {

    public CellFormatterImpl() {
    }

    @Override
    public String format(Object value) {
        return "test " + value.toString();
    }
}
