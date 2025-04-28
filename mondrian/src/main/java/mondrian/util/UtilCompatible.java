/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.util;

import java.sql.Statement;

import mondrian.olap.Util;


public interface UtilCompatible {



    /**
     * Cancels and closes a SQL Statement object. If errors are encountered,
     * they should be logged under {@link Util}.
     * @param stmt The statement to close.
     */
    void cancelStatement(Statement stmt);


    /**
     * As {@link java.util.Arrays#binarySearch(Object[], int, int, Object)}, but
     * available pre-JDK 1.6.
     */
    <T extends Comparable<T>> int binarySearch(T[] ts, int start, int end, T t);



}
