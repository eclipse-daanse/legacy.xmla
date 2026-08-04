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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.opencube.junit5.dataloader;


import org.eclipse.daanse.cwm.model.cwm.resource.relational.Schema;
import org.eclipse.daanse.cwm.testkit.api.DataSupplier;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.expressivenames.ExpressiveNamesDatabaseSupplier;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.expressivenames.ExpressiveNamesTestInstance;

/**
 * The ten expressivenames tables, built from the CWM schema of the
 * rolap.mapping instance. Its CSVs replace the copy that used to live under
 * {@code testfiles/loader/expressivenames/} — the same rows, only unquoted.
 */
public class ExpressiveNamesDataLoader extends CwmDataLoader {


    @Override
    protected Schema schema() {
        return new ExpressiveNamesDatabaseSupplier().schema();
    }

    @Override
    protected DataSupplier data() {
        return new ExpressiveNamesTestInstance().dataSupplier();
    }
}
