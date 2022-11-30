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
package org.eclipse.daanse.db.datasource.mssqlserver;

import java.sql.SQLException;

import javax.sql.XAConnectionBuilder;
import javax.sql.XADataSource;

import org.eclipse.daanse.db.datasource.common.AbstractDelegateXADataSource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;

import com.microsoft.sqlserver.jdbc.SQLServerXADataSource;

@Designate(ocd = MsSqlConfig.class, factory = true)
@Component(service = XADataSource.class, scope = ServiceScope.SINGLETON)
public class XADataSourceService extends AbstractDelegateXADataSource<SQLServerXADataSource> {

    private MsSqlConfig config;
    private SQLServerXADataSource ds;

    @Activate
    public XADataSourceService(MsSqlConfig config) throws SQLException {
        this.ds = new SQLServerXADataSource();
        this.config = config;
    }
    // no @Modified to force consumed Services get new configured connections.

    @Deactivate
    public void deactivate() {

        config = null;
    }

    @Override
    public XAConnectionBuilder createXAConnectionBuilder() throws SQLException {
        return super.createXAConnectionBuilder().user(config._password()).password(config._password());
    }

    @Override
    protected SQLServerXADataSource delegate() {
        return ds;
    }

}
