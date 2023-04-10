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
package org.eclipse.daanse.olap.rolap.dbmapper.dbcreator.basic;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.eclipse.daanse.db.jdbc.util.api.DatabaseCreatorService;
import org.eclipse.daanse.olap.rolap.dbmapper.dbcreator.api.DbCreatorService;
import org.eclipse.daanse.olap.rolap.dbmapper.dbcreator.api.DbCreatorServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

@Component(service = DbCreatorServiceFactory.class, scope = ServiceScope.SINGLETON)
public class DbCreatorServiceFactoryImpl implements DbCreatorServiceFactory {

    @Reference
    private DatabaseCreatorService databaseCreatorService;

    @Override
    public DbCreatorService create(DataSource dataSource) throws SQLException {
        return new DbCreatorServiceImpl(dataSource, databaseCreatorService);
    }

}
