/*
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.olap.rolap.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.daanse.rolap.core.BasicContext;
import org.junit.jupiter.api.Test;

class PidTest {

    @Test
    void testPidNotChanged() throws Exception {
        assertThat(BasicContext.class.getName()).isSameAs(BasicContext.PID);
    }
}
