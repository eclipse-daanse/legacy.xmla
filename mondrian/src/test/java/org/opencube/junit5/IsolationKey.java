/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.opencube.junit5;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The isolation key a test class wants its database under.
 * <p>
 * Without it a class shares the database of its dataset, which is created and
 * filled once per run and reused by everybody. A class that changes its database
 * - typically by creating aggregate tables - names a key here and gets one of its
 * own; classes naming the same key deliberately share.
 * <p>
 * The name matches the testkit's own vocabulary
 * ({@code DatabaseProvider.activate(String isolationKey)}), which is what
 * actually performs the isolation: separate URLs for the embedded providers,
 * separate schemas for the container-backed ones.
 * <p>
 * This replaces a global {@code dockerWasChanged} flag that discarded every
 * cached database instead, so one such class forced the large FoodMart dataset
 * to be created and filled again - eight times over a single run.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IsolationKey {

    /** Appended to the dataset's key. Must be usable inside an identifier. */
    String value();
}
