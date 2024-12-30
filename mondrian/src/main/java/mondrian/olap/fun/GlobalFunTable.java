/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.olap.api.Syntax;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionTable;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.olap.Util;
import mondrian.spi.UserDefinedFunction;
import mondrian.util.ServiceDiscovery;

/**
 * Global function table contains builtin functions and global user-defined
 * functions.
 *
 * @author Gang Chen
 */
public class GlobalFunTable extends FunTableImpl {

    private static GlobalFunTable instance = new GlobalFunTable();
    static {
        GlobalFunTable.instance.init();
    }

    public static GlobalFunTable instance() {
        return GlobalFunTable.instance;
    }

    private GlobalFunTable() {
    }

    @Override
	public void defineFunctions(FunctionTableCollector builder) {
        final FunctionTable builtinFunTable = BuiltinFunTable.instance();
        final List<String> reservedWords = builtinFunTable.getReservedWords();
        for (String reservedWord : reservedWords) {
            builder.defineReserved(reservedWord);
        }
        final List<FunctionResolver> resolvers = builtinFunTable.getResolvers();
        for (FunctionResolver resolver : resolvers) {
            builder.define(resolver);
        }
    }
}
