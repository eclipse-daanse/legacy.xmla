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
package org.eclipse.daanse.db.dialect.db.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.junit.jupiter.api.Test;
import org.osgi.test.common.annotation.InjectService;

public class OSGiServiceTest {
    @Test
    void serviceExists(@InjectService List<Dialect> dialects) throws Exception {

        assertThat(dialects).isNotNull().isNotEmpty().anyMatch(MySqlDialect.class::isInstance);
    }
}
