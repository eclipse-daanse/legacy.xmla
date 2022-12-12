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
package org.eclipse.daanse.olap.rolap.dbmapper.verifyer.jdbc;
 

import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.CAUSE;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.VerificationResult;

public class NoDatabaseVerificationResult implements VerificationResult {

    public static final VerificationResult INSTANCE = new NoDatabaseVerificationResult();

    private NoDatabaseVerificationResult() {

    }

    @Override
    public String title() {
        return "Not tested. Missing DataSource";
    }

    @Override
    public Level level() {
        return Level.ERROR;
    }

    @Override
    public String description() {
        return "Not tested. Missing DataSource.";
    }

    @Override
    public CAUSE cause() {
        return CAUSE.DATABASE;
    }
};