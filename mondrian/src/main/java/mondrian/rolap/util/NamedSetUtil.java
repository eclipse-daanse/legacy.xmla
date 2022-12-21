/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package mondrian.rolap.util;

import org.eclipse.daanse.olap.rolap.dbmapper.api.NamedSet;

public class NamedSetUtil {
    /**
     * Returns the formula, looking for a sub-element called
     * "Formula" first, then looking for an attribute called
     * "formula".
     */
    public static String getFormula(NamedSet namedSet) {
        if (namedSet.formulaElement() != null) {
            return namedSet.formulaElement().cdata();
        } else {
            return namedSet.formula();
        }
    }

}
