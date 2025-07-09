/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * History:
 *  This files came from the mondrian project. Some of the Flies
 *  (mostly the Tests) did not have License Header.
 *  But the Project is EPL Header. 2002-2022 Hitachi Vantara.
 *
 * Contributors:
 *   Hitachi Vantara.
 *   SmartCity Jena - initial  Java 8, Junit5
 */
// Copyright (c) 2021 Sergei Semenkov.  All rights reserved.

package org.eclipse.daanse.olap.query.component;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.query.component.CalculatedFormula;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.common.AbstractQueryPart;

public class CalculatedFormulaImpl extends AbstractQueryPart implements CalculatedFormula {
    private final String cubeName;
    private final Formula e;

    CalculatedFormulaImpl(String cubeName, Formula e)
    {
        this.cubeName = cubeName;
        this.e = e;
    }

    @Override
    public void unparse(PrintWriter pw) {
        pw.print("CREATE SESSION MEMBER ");
        pw.print("[");
        pw.print(cubeName);
        pw.print("]");
        pw.print(".");
        if(e != null) {
            pw.print(e.getIdentifier());
            pw.print(" AS ");
        }
        pw.println();
    }

    @Override
    public Object[] getChildren() {
        return new Object[] {e};
    }

    @Override
    public String getCubeName() {
        return cubeName;
    }

    @Override
    public Formula getFormula() {
        return e;
    }

}

