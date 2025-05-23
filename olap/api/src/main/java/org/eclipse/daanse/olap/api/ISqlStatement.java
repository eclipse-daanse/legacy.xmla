/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api;

import java.sql.ResultSet;

public interface ISqlStatement {

    /**
       * Executes the current statement, and handles any SQLException.
       */
    void execute();

    /**
       * Closes all resources (statement, result set) held by this SqlStatement.
       *
       * <p>If any of them fails, wraps them in a
       * {@link RuntimeException} describing the high-level operation which this statement was performing. No further
       * error-handling is required to produce a descriptive stack trace, unless you want to absorb the error.</p>
       *
       * <p>This method is idempotent.</p>
       */
    void close();

    ResultSet getResultSet();

    /**
       * Returns the result set in a proxy which automatically closes this SqlStatement (and hence also the statement and
       * result set) when the result set is closed.
       *
       * <p>This helps to prevent connection leaks. The caller still has to
       * remember to call ResultSet.close(), of course.
       *
       * @return Wrapped result set
       */
    ResultSet getWrappedResultSet();

}
