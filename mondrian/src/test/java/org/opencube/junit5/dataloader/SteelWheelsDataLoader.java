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

import java.sql.Connection;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.opencube.junit5.dataloader.DataLoaderUtil.Column;
import org.opencube.junit5.dataloader.DataLoaderUtil.Table;
import org.opencube.junit5.dataloader.DataLoaderUtil.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SteelWheels used to be replayed from {@code SteelWheels.mysql.sql}, a MySQL Administrator
 * dump. Only MySQL and MariaDB could read it -- everywhere else the loader threw and all 24
 * tests of SteelWheelsSchemaTest reported "data loader already failed". It is now loaded from
 * CSV through the same typed path FoodMart uses, which every dialect already speaks.
 *
 * <p>
 * The dump declared thirteen tables; the SteelWheels mapping references four. The other nine
 * were never queried.
 */
public class SteelWheelsDataLoader implements DataLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(SteelWheelsDataLoader.class);

	/**
	 * Column types mirror the original dump's DDL. Two of them matter:
	 * <ul>
	 * <li>{@code TOTALPRICE} was {@code double}; {@link Type#Real} is single precision and
	 * could not hold the column's sum (10,645,949.18) on postgres.</li>
	 * <li>{@code TIME_ID} was {@code varchar(10)} in the dump, on both sides of the star's time
	 * join. SteelWheelsSchemaTest's own schema also joins {@code orderfact.REQUIREDDATE}, a
	 * {@code timestamp}, against {@code time.TIME_ID} -- that mismatch is the point of
	 * testMondrianBug476_770_957. MySQL, h2, duckdb and mssql coerce the string; postgres, derby,
	 * sqlite and clickhouse refuse with "operator does not exist: timestamp = character varying".
	 * Every one of the 265 + 2996 values is a plain {@code YYYY-MM-DD} date (checked against the
	 * original dump) and the column is nothing but a join key -- no level and no literal predicate
	 * reads it -- so both sides are typed {@link Type#Date} and every dialect can compare them,
	 * with a timestamp as well as with each other.</li>
	 * </ul>
	 */
	public static final List<Table> steelWheelsTables = List.of( //
			new Table(null, "customer_w_ter", List.of(), //
					new Column("CUSTOMERNUMBER", Type.Integer, true), //
					new Column("CUSTOMERNAME", Type.Varchar60, true), //
					new Column("CONTACTLASTNAME", Type.Varchar60, true), //
					new Column("CONTACTFIRSTNAME", Type.Varchar60, true), //
					new Column("PHONE", Type.Varchar60, true), //
					new Column("ADDRESSLINE1", Type.Varchar60, true), //
					new Column("ADDRESSLINE2", Type.Varchar60, true), //
					new Column("CITY", Type.Varchar60, true), //
					new Column("STATE", Type.Varchar60, true), //
					new Column("POSTALCODE", Type.Varchar30, true), //
					new Column("COUNTRY", Type.Varchar60, true), //
					new Column("EMPLOYEENUMBER", Type.Integer, true), //
					new Column("CREDITLIMIT", Type.Currency, true), //
					new Column("TERRITORY", Type.Varchar30, true)), //
			new Table(null, "products", List.of(), //
					new Column("PRODUCTCODE", Type.Varchar60, false), //
					new Column("PRODUCTNAME", Type.Varchar255, false), //
					new Column("PRODUCTLINE", Type.Varchar60, false), //
					new Column("PRODUCTSCALE", Type.Varchar30, false), //
					new Column("PRODUCTVENDOR", Type.Varchar60, false), //
					new Column("PRODUCTDESCRIPTION", Type.Varchar1024, false), //
					new Column("QUANTITYINSTOCK", Type.Smallint, false), //
					new Column("BUYPRICE", Type.Currency, false), //
					new Column("MSRP", Type.Currency, false)), //
			new Table(null, "time", List.of(), //
					new Column("TIME_ID", Type.Date, true), //
					new Column("MONTH_ID", Type.Integer, true), //
					new Column("QTR_ID", Type.Integer, true), //
					new Column("YEAR_ID", Type.Integer, true), //
					new Column("MONTH_NAME", Type.Varchar30, true), //
					new Column("MONTH_DESC", Type.Varchar30, true), //
					new Column("QTR_NAME", Type.Varchar30, true), //
					new Column("QTR_DESC", Type.Varchar30, true)), //
			new Table(null, "orderfact", List.of(), //
					new Column("ORDERNUMBER", Type.Integer, true), //
					new Column("PRODUCTCODE", Type.Varchar60, true), //
					new Column("QUANTITYORDERED", Type.Integer, true), //
					new Column("PRICEEACH", Type.Currency, true), //
					new Column("ORDERLINENUMBER", Type.Integer, true), //
					new Column("TOTALPRICE", Type.Double, true), //
					new Column("ORDERDATE", Type.Timestamp, true), //
					new Column("REQUIREDDATE", Type.Timestamp, true), //
					new Column("SHIPPEDDATE", Type.Timestamp, true), //
					new Column("STATUS", Type.Varchar30, true), //
					new Column("COMMENTS", Type.Varchar1024, true), //
					new Column("CUSTOMERNUMBER", Type.Integer, true), //
					new Column("TIME_ID", Type.Date, true), //
					new Column("QTR_ID", Type.Bigint, true), //
					new Column("MONTH_ID", Type.Bigint, true), //
					new Column("YEAR_ID", Type.Bigint, true)), //
			// not part of the mapping's star, but SteelWheelsSchemaTestModifier5 builds a cube
			// [Foo] on it to exercise a timestamp-typed member property
			new Table(null, "orders", List.of(), //
					new Column("ORDERNUMBER", Type.Integer, false), //
					new Column("ORDERDATE", Type.Timestamp, true), //
					new Column("REQUIREDDATE", Type.Timestamp, true), //
					new Column("SHIPPEDDATE", Type.Timestamp, true), //
					new Column("STATUS", Type.Varchar30, false), //
					new Column("COMMENTS", Type.Varchar1024, true), //
					new Column("CUSTOMERNUMBER", Type.Integer, false)));

	@Override
	public boolean loadData(Entry<DataSource, Dialect> dataBaseInfo) throws Exception {
		DataSource dataSource = dataBaseInfo.getKey();
		Dialect dialect = dataBaseInfo.getValue();

		long tDdl = System.nanoTime();
		try (Connection connection = dataSource.getConnection()) {
			DataLoaderUtil.executeSql(connection, dropTableSQLs(dialect), true);
			DataLoaderUtil.executeSql(connection, createTablesSQLs(dialect), true);
		}
		LOGGER.warn("DBTIMING db={} phase=ddl ms={}", DataLoaderUtil.dbId(dialect),
				(System.nanoTime() - tDdl) / 1_000_000);

		// canonical CSVs, from the rolap.mapping instance bundle
		DataLoaderUtil.importCSV(dataSource, dialect, steelWheelsTables, DataLoaderUtil.fromClasspath(
				org.eclipse.daanse.rolap.mapping.instance.emf.complex.steelwheels.CatalogSupplier.class));
		DataLoaderUtil.analyze(dataSource, dialect, steelWheelsTables);
		return true;
	}

	private List<String> dropTableSQLs(Dialect dialect) {
		return steelWheelsTables.stream().map(t -> DataLoaderUtil.dropTableSQL(t, dialect)).toList();
	}

	private List<String> createTablesSQLs(Dialect dialect) {
		return steelWheelsTables.stream().map(t -> DataLoaderUtil.createTableSQL(t, dialect)).toList();
	}
}
