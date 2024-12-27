/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2019 Hitachi Vantara and others
// Copyright (C) 2021 Sergei Semenkov
// All Rights Reserved.
*/
package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.function.FunctionDefinition;

import mondrian.olap.fun.vba.Excel;
import mondrian.olap.fun.vba.Vba;

/**
 * <code>BuiltinFunTable</code> contains a list of all built-in MDX functions.
 *
 * <p>Note: Boolean expressions return {@link Boolean#TRUE},
 * {@link Boolean#FALSE} or null. null is returned if the expression can not be
 * evaluated because some values have not been loaded from database yet.</p>
 *
 * @author jhyde
 * @since 26 February, 2002
 */
public class BuiltinFunTable extends FunTableImpl {

    public static final String LEVELS = "Levels";
    public static final String MEMBERS = "Members";
    /** the singleton */
    private static BuiltinFunTable instance;

    /**
     * Creates a function table containing all of the builtin MDX functions.
     * This method should only be called from {@link BuiltinFunTable#instance}.
     */
    protected BuiltinFunTable() {
        super();
    }

    @Override
	public void defineFunctions(FunctionTableCollector builder) {
        builder.defineReserved("NULL");

        // Define VBA functions.
        for (FunctionDefinition funDef : JavaFunDef.scan(Vba.class)) {
            builder.define(funDef);
        }

        // Define Excel functions.
        for (FunctionDefinition funDef : JavaFunDef.scan(Excel.class)) {
            builder.define(funDef);
        }
    }

    /**
     * Returns the singleton, creating if necessary.
     *
     * @return the singleton
     */
    public static BuiltinFunTable instance() {
        if (BuiltinFunTable.instance == null) {
            BuiltinFunTable.instance = new BuiltinFunTable();
            BuiltinFunTable.instance.init();
        }
        return BuiltinFunTable.instance;
    }



}
