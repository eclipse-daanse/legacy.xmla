/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * History:
 *  This files came from the mondrian project. Some of the Flies
 *  (mostly the Tests) did not have License Header.
 *  But the Project is EPL Header. 2002-2022 Hitachi Vantara.
 *
 * Contributors:
 *   Hitachi Vantara.
 *   SmartCity Jena - initial  Java 8, Junit5
 */
package org.eclipse.daanse.db.dialect.db.db2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.junit.jupiter.api.Test;
import org.osgi.test.common.annotation.InjectService;


public class ServiceTest {
    @Test
    void serviceExists(@InjectService List<Dialect> dialects) throws Exception {

        assertThat(dialects).isNotNull().isNotEmpty().anyMatch(Db2Dialect.class::isInstance).anyMatch(Db2OldAs400Dialect.class::isInstance);
    }
}
