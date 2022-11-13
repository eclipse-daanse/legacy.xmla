/*********************************************************************
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
**********************************************************************/
package org.osgi.impl.service.jdbc.clickhouse.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.eclipse.daanse.db.datasource.common.AbstractDelegateDataSource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;

import com.clickhouse.jdbc.ClickHouseDataSource;

@Designate(ocd = ClickHouseConfig.class, factory = true)
@Component(service = DataSource.class, scope = ServiceScope.SINGLETON)
public class DataSourceService extends AbstractDelegateDataSource<ClickHouseDataSource> {

    private ClickHouseConfig config;
    private ClickHouseDataSource ds;

    @Activate
    public DataSourceService(ClickHouseConfig config) throws SQLException {
        String url = "";
        this.ds = new ClickHouseDataSource(url);
        this.config = config;
    }

    // no @Modified to force consumed Services get new configured connections.
    @Deactivate
    public void deactivate() {

        config = null;
    }

    @Override
    public Connection getConnection() throws SQLException {

        return ds.getConnection(config.username(), config._password());
    }

    @Override
    protected ClickHouseDataSource delegate() {
        return ds;
    }

}
