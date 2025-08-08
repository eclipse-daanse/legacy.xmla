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
package org.eclipse.daanse.check.predicate;

import java.util.function.Predicate;

import org.eclipse.daanse.olap.api.Context;

public class ContextPredicates {

    public static Predicate<Context<?>> isExist() {
        return c -> c != null;
    }

    public static Predicate<Context<?>> hasDataSource() {
        return c -> c.getDataSource() != null;
    }

    public static Predicate<Context<?>> hasDescription() {
        return c -> c.getDescription() != null;
    }

    public static Predicate<Context<?>> hasDialect() {
        return c -> c.getDialect() != null;
    }

    public static Predicate<Context<?>> hasExpressionCompilerFactory() {
        return c -> c.getExpressionCompilerFactory() != null;
    }

    public static Predicate<Context<?>> hasName() {
        return c -> c.getName() != null;
    }

    public static Predicate<Context<?>> hasMdxParserProvider() {
        return c -> c.getMdxParserProvider() != null;
    }

    public static Predicate<Context<?>> hasSqlGuardFactory() {
        return c -> c.getSqlGuardFactory() != null && c.getSqlGuardFactory().isPresent();
    }

}
