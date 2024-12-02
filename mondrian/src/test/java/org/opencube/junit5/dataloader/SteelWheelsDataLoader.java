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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.opencube.junit5.Constants;

public class SteelWheelsDataLoader implements DataLoader {
	public static List<String> tables = List.of(
	"customer_w_ter", "customers", "department_managers", "employees", "offices", "orderdetails", "orderfact", "orders",
	"payments", "products", "quadrant_actuals", "time", "trial_balance");

	@Override
	public boolean loadData(Entry<DataSource, Dialect> dataBaseInfo) throws Exception {
		DataSource dataSource=dataBaseInfo.getKey();
		Dialect dialect = dataBaseInfo.getValue();
	try (Connection connection = dataSource.getConnection()) {

	    List<String> dropTableSQLs = dropTableSQLs();
	    DataLoaderUtil.executeSql(connection, dropTableSQLs,true);

	    InputStream sqlFile= new FileInputStream(new File(Constants.TESTFILES_DIR + "loader/steelwheel/SteelWheels.mysql.sql"));
	    DataLoaderUtil.loadFromSqlInserts(connection, dialect, sqlFile);

	}
	return true;
    }

	private List<String> dropTableSQLs() {

		return tables.stream().map(this::dropTableSQL).toList();
	}

	private String dropTableSQL(String table) {
		return "DROP TABLE IF EXISTS " + table;
	}
}
