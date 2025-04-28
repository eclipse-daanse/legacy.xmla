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
// Copyright (c) 2021 Sergei Semenkov
// All rights reserved.

package org.eclipse.daanse.olap.query.component;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.Subcube;

import mondrian.olap.AbstractQueryPart;

public class SubcubeImpl extends AbstractQueryPart implements Subcube {
    private final String cubeName;
    private final Subcube subcube;
    private final QueryAxis[] axes;
    private final QueryAxis slicerAxis;

    public SubcubeImpl(
            String cubeName,
            Subcube subcube,
            QueryAxis[] axes,
            QueryAxis slicerAxis)
    {
        this.cubeName = cubeName;
        this.subcube = subcube;
        this.axes = axes;
        this.slicerAxis = slicerAxis;
    }

    @Override
    public void unparse(PrintWriter pw) {
        if(this.subcube != null) {
            pw.println("(");
            pw.println("select ");
            for (int i = 0; i < axes.length; i++) {
                pw.print("  ");
                axes[i].unparse(pw);
                if (i < axes.length - 1) {
                    pw.println(",");
                    pw.print("  ");
                } else {
                    pw.println();
                }
            }
            pw.println("from ");
            if (subcube != null) {
                subcube.unparse(pw);
            }
            if (slicerAxis != null) {
                pw.print("where ");
                slicerAxis.unparse(pw);
                pw.println();
            }
            pw.println(")");
        }
        else {
            pw.println(new StringBuilder("[").append(this.cubeName).append("]").toString());
        }
//        pw.print("CREATE SESSION MEMBER ");
//        pw.print("[" + cubeName + "]");
//        pw.print(".");
//        if(e != null) {
//            pw.print(e.getIdentifier());
//            pw.print(" AS ");
//            //pw.print(e,getExpression());
//        }
//        pw.println();
    }

    @Override
    public Object[] getChildren() {
        return new Object[] {cubeName};
    }

    @Override
    public String getCubeName() {
        if(this.subcube != null) {
            return this.subcube.getCubeName();
        }
        else {
            return this.cubeName;
        }
    }

    @Override
    public List<Expression> getAxisExps() {
        ArrayList<Expression> exps = new ArrayList<>();
        if(this.subcube != null) {
            exps.addAll(this.subcube.getAxisExps());
        }
        if (this.axes != null) {
        	for(int i =0; i < this.axes.length; i++) {
        		exps.add(this.axes[i].getSet());
        	}
        }
        if(this.slicerAxis != null) {
            exps.add(this.slicerAxis.getSet());
        }
        return exps;
    }
}

