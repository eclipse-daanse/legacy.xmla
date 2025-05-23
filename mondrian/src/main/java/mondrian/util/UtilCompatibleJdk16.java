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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ServiceLoader;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.Util;
import mondrian.rolap.RolapUtil;

// Only in Java6 and above

/**
 * Implementation of {@link mondrian.util.UtilCompatible} that runs in
 * JDK 1.6.
 *
 * <p>Prior to JDK 1.6, this class should never be loaded. Applications should
 * instantiate this class via {@link Class#forName(String)} or better, use
 * methods in {@link mondrian.olap.Util}, and not instantiate it at all.
 *
 * @author jhyde
 */
public class UtilCompatibleJdk16 implements UtilCompatible  {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(Util.class);
    private final static String executionStatementCleanupException =
        "An exception was encountered while trying to cleanup an execution context. A statement failed to cancel gracefully. Locus was : \"{0}\".";

    @Override
    public void cancelStatement(Statement stmt) {
        try {
            // A call to statement.isClosed() would be great here, but in
            // reality, some drivers will block on this check and the
            // cancellation will never happen.  This is due to the
            // non-thread-safe nature of JDBC and driver implementations. If a
            // thread is currently using the statement, calls to isClosed() are
            // synchronized internally and won't return until the query
            // completes.
            stmt.cancel();
        } catch (Throwable t) {
            // We crush this one. A lot of drivers will complain if cancel() is
            // called on a closed statement, but a call to isClosed() isn't
            // thread safe and might block. See above.

            // Also, we MUST catch all throwables. Some drivers (ie. Hive)
            // will choke on canceled queries and throw a OutOfMemoryError.
            // We can't protect ourselves against this. That's a bug on their
            // side.
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("",
                    new OlapRuntimeException(MessageFormat.format(
                        executionStatementCleanupException,
                            t.getMessage()), t),
                    t);
            }
        }
    }


    @Override
	public <T extends Comparable<T>> int binarySearch(
        T[] ts, int start, int end, T t)
    {
        return Arrays.binarySearch(
            ts, start, end, t,
            RolapUtil.ROLAP_COMPARATOR);
    }
}
