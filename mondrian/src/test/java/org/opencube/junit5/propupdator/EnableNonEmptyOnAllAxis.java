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
 *   SmartCity Jena - initial
 */
package org.opencube.junit5.propupdator;

import org.eclipse.daanse.olap.api.Context;
import org.opencube.junit5.context.TestContextImpl;

/**
 * Turns on ENABLE_NON_EMPTY_ON_ALL_AXIS for the test's own context.
 *
 * <p>
 * Classes that need this used to set it in a {@code @BeforeEach} on the JVM-wide
 * singleton, which made them collide with every other test running at the same
 * time. A context updater is the equivalent hook that stays inside the test:
 * {@code @BeforeEach} cannot serve, because the Context only arrives as a
 * parameter of the test method itself.
 * </p>
 */
public class EnableNonEmptyOnAllAxis implements TestContextUpdater {

    @Override
    public void updateContext(Context<?> context) {
        ((TestContextImpl) context).setEnableNonEmptyOnAllAxis(true);
    }

}
