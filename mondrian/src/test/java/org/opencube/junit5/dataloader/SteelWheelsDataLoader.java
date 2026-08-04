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
package org.opencube.junit5.dataloader;

import org.eclipse.daanse.cwm.model.cwm.resource.relational.Schema;
import org.eclipse.daanse.cwm.testkit.api.DataSupplier;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.steelwheels.SteelWheelsDatabaseSupplier;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.steelwheels.SteelWheelsTestInstance;

/**
 * SteelWheels — five tables, 3827 rows — built from the CWM schema of the
 * rolap.mapping instance.
 *
 * <p>
 * It used to be replayed from {@code SteelWheels.mysql.sql}, a MySQL
 * Administrator dump that only MySQL and MariaDB could read; everywhere else
 * the loader threw and all 24 tests of {@code SteelWheelsSchemaTest} reported
 * "data loader already failed". Then it came from CSV against a hand-written
 * table list kept here. Now the tables come from the same description the
 * mapping itself uses, and that list is gone.
 *
 * <p>
 * Completing the description is what the move needed: the model knew four of
 * the five tables and 32 of the 54 columns — {@code orders}, which
 * {@code SteelWheelsSchemaTestModifier5} builds a cube on, was missing
 * entirely.
 */
public class SteelWheelsDataLoader extends CwmDataLoader {

    @Override
    protected Schema schema() {
        return new SteelWheelsDatabaseSupplier().schema();
    }

    @Override
    protected DataSupplier data() {
        return new SteelWheelsTestInstance().dataSupplier();
    }
}
