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
 * Turns ENABLE_ROLAP_CUBE_MEMBER_CACHE off for the test's own context.
 *
 * <p>
 * Member cache control operations are rejected while the cube member cache is
 * on, so the tests for them need it off. This used to happen in a
 * {@code @BeforeEach} on the JVM-wide singleton, which switched the cache off for
 * every test running at the same time.
 * </p>
 */
public class DisableRolapCubeMemberCache implements TestContextUpdater {

    @Override
    public void updateContext(Context<?> context) {
        ((TestContextImpl) context).setEnableRolapCubeMemberCache(false);
    }

}
