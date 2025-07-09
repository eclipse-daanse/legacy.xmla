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

import org.eclipse.daanse.olap.api.Command;
import org.eclipse.daanse.olap.api.query.component.TransactionCommand;
import org.eclipse.daanse.olap.common.AbstractQueryPart;

public class TransactionCommandImpl extends AbstractQueryPart implements TransactionCommand {

    private final Command command;

    public TransactionCommandImpl(Command command)
    {
        this.command = command;
    }

    @Override
    public void unparse(PrintWriter pw) {
        pw.print( new StringBuilder(this.command.name()).append("TRANSACTION").toString());
    }

    @Override
    public Object[] getChildren() {
        return new Object[] {this.command};
    }

    @Override
    public Command getCommand() {
        return this.command;
    }
}
