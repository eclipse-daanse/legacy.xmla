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
 * Renders null members as {@code null} rather than the default {@code #null} for
 * the test's own context.
 *
 * <p>
 * The ragged-hierarchy tests spell out member names such as
 * {@code [Store].[Vatican].[Vatican].[null].[Store 17]} and so need the shorter
 * literal. It used to be set in a {@code @BeforeEach} on the JVM-wide singleton,
 * which renamed null members for every test running at the same time.
 * </p>
 */
public class NullMemberRepresentationNull implements TestContextUpdater {

    @Override
    public void updateContext(Context<?> context) {
        ((TestContextImpl) context).setNullMemberRepresentation("null");
    }

}
