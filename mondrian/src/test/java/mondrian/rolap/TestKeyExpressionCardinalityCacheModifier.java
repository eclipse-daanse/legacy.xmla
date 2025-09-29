/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package mondrian.rolap;

import java.util.List;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.SQLExpressionColumn;
import org.eclipse.daanse.rolap.mapping.model.SqlStatement;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

/**
 * EMF version of TestKeyExpressionCardinalityCacheModifier from TestAggregationManager.
 * Creates two cubes "Sales1" and "Sales2" with dimensions that use SQL expressions for key columns.
 *
 * Cube 1: "Sales1"
 * - Dimension: Store1 with Store Country level using SQL expression column
 * - Measures: Unit Sales, Store Sales
 *
 * Cube 2: "Sales2"
 * - Dimension: Store2 with Store Country level using SQL expression column
 * - Measures: Unit Sales
 *
 * Both dimensions use SQL expressions with multiple dialect-specific SQL statements
 * for the store_country column to test key expression cardinality caching.
 */
public class TestKeyExpressionCardinalityCacheModifier implements CatalogMappingSupplier {

    private final Catalog catalog;

    // Static SQL statements for Store1 dimension
    private static final SqlStatement SQL_STMT_STORE1_ORACLE;
    private static final SqlStatement SQL_STMT_STORE1_HSQLDB;
    private static final SqlStatement SQL_STMT_STORE1_DERBY;
    private static final SqlStatement SQL_STMT_STORE1_LUCIDDB;
    private static final SqlStatement SQL_STMT_STORE1_MYSQL;
    private static final SqlStatement SQL_STMT_STORE1_NETEZZA;
    private static final SqlStatement SQL_STMT_STORE1_NEOVIEW;
    private static final SqlStatement SQL_STMT_STORE1_GENERIC;

    // Static SQL expression column for Store1
    private static final SQLExpressionColumn SQL_EXPR_COLUMN_STORE1;

    // Static SQL statements for Store2 dimension
    private static final SqlStatement SQL_STMT_STORE2_ORACLE;
    private static final SqlStatement SQL_STMT_STORE2_DERBY;
    private static final SqlStatement SQL_STMT_STORE2_LUCIDDB;
    private static final SqlStatement SQL_STMT_STORE2_MYSQL;
    private static final SqlStatement SQL_STMT_STORE2_GENERIC;

    // Static SQL expression column for Store2
    private static final SQLExpressionColumn SQL_EXPR_COLUMN_STORE2;

    // Static Store1 dimension components
    private static final Level LEVEL_STORE1_COUNTRY;
    private static final ExplicitHierarchy HIERARCHY_STORE1;
    private static final TableQuery TABLE_QUERY_STORE1;
    private static final StandardDimension DIMENSION_STORE1;

    // Static Store2 dimension components
    private static final Level LEVEL_STORE2_COUNTRY;
    private static final ExplicitHierarchy HIERARCHY_STORE2;
    private static final TableQuery TABLE_QUERY_STORE2;
    private static final StandardDimension DIMENSION_STORE2;

    // Static Sales1 cube components
    private static final SumMeasure MEASURE_SALES1_UNIT_SALES;
    private static final SumMeasure MEASURE_SALES1_STORE_SALES;
    private static final DimensionConnector CONNECTOR_SALES1_STORE1;
    private static final TableQuery TABLE_QUERY_SALES1_FACT;
    private static final MeasureGroup MEASURE_GROUP_SALES1;
    private static final PhysicalCube CUBE_SALES1;

    // Static Sales2 cube components
    private static final SumMeasure MEASURE_SALES2_UNIT_SALES;
    private static final DimensionConnector CONNECTOR_SALES2_STORE2;
    private static final TableQuery TABLE_QUERY_SALES2_FACT;
    private static final MeasureGroup MEASURE_GROUP_SALES2;
    private static final PhysicalCube CUBE_SALES2;

    static {
        // Create SQL statements for Store1 dimension
        SQL_STMT_STORE1_ORACLE = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE1_ORACLE.getDialects().add("oracle");
        SQL_STMT_STORE1_ORACLE.setSql("\"store_country\"");

        SQL_STMT_STORE1_HSQLDB = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE1_HSQLDB.getDialects().add("hsqldb");
        SQL_STMT_STORE1_HSQLDB.setSql("\"store_country\"");

        SQL_STMT_STORE1_DERBY = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE1_DERBY.getDialects().add("derby");
        SQL_STMT_STORE1_DERBY.setSql("\"store_country\"");

        SQL_STMT_STORE1_LUCIDDB = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE1_LUCIDDB.getDialects().add("luciddb");
        SQL_STMT_STORE1_LUCIDDB.setSql("\"store_country\"");

        SQL_STMT_STORE1_MYSQL = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE1_MYSQL.getDialects().add("mysql");
        SQL_STMT_STORE1_MYSQL.setSql("`store_country`");

        SQL_STMT_STORE1_NETEZZA = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE1_NETEZZA.getDialects().add("netezza");
        SQL_STMT_STORE1_NETEZZA.setSql("\"store_country\"");

        SQL_STMT_STORE1_NEOVIEW = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE1_NEOVIEW.getDialects().add("neoview");
        SQL_STMT_STORE1_NEOVIEW.setSql("\"store_country\"");

        SQL_STMT_STORE1_GENERIC = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE1_GENERIC.getDialects().add("generic");
        SQL_STMT_STORE1_GENERIC.setSql("store_country");

        // Create SQL expression column for Store1
        SQL_EXPR_COLUMN_STORE1 = RolapMappingFactory.eINSTANCE.createSQLExpressionColumn();
        SQL_EXPR_COLUMN_STORE1.getSqls().addAll(List.of(
            SQL_STMT_STORE1_ORACLE,
            SQL_STMT_STORE1_HSQLDB,
            SQL_STMT_STORE1_DERBY,
            SQL_STMT_STORE1_LUCIDDB,
            SQL_STMT_STORE1_MYSQL,
            SQL_STMT_STORE1_NETEZZA,
            SQL_STMT_STORE1_NEOVIEW,
            SQL_STMT_STORE1_GENERIC
        ));
        SQL_EXPR_COLUMN_STORE1.setType(ColumnType.VARCHAR);

        // Create SQL statements for Store2 dimension
        SQL_STMT_STORE2_ORACLE = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE2_ORACLE.getDialects().add("oracle");
        SQL_STMT_STORE2_ORACLE.setSql("\"store_country\"");

        SQL_STMT_STORE2_DERBY = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE2_DERBY.getDialects().add("derby");
        SQL_STMT_STORE2_DERBY.setSql("\"store_country\"");

        SQL_STMT_STORE2_LUCIDDB = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE2_LUCIDDB.getDialects().add("luciddb");
        SQL_STMT_STORE2_LUCIDDB.setSql("\"store_country\"");

        SQL_STMT_STORE2_MYSQL = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE2_MYSQL.getDialects().add("mysql");
        SQL_STMT_STORE2_MYSQL.setSql("`store_country`");

        SQL_STMT_STORE2_GENERIC = RolapMappingFactory.eINSTANCE.createSqlStatement();
        SQL_STMT_STORE2_GENERIC.getDialects().add("generic");
        SQL_STMT_STORE2_GENERIC.setSql("store_country");

        // Create SQL expression column for Store2
        SQL_EXPR_COLUMN_STORE2 = RolapMappingFactory.eINSTANCE.createSQLExpressionColumn();
        SQL_EXPR_COLUMN_STORE2.getSqls().addAll(List.of(
            SQL_STMT_STORE2_ORACLE,
            SQL_STMT_STORE2_DERBY,
            SQL_STMT_STORE2_LUCIDDB,
            SQL_STMT_STORE2_MYSQL,
            SQL_STMT_STORE2_GENERIC
        ));
        SQL_EXPR_COLUMN_STORE2.setType(ColumnType.VARCHAR);

        // Create Store1 dimension
        LEVEL_STORE1_COUNTRY = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_STORE1_COUNTRY.setName("Store Country");
        LEVEL_STORE1_COUNTRY.setUniqueMembers(true);
        LEVEL_STORE1_COUNTRY.setColumn(SQL_EXPR_COLUMN_STORE1);

        TABLE_QUERY_STORE1 = RolapMappingFactory.eINSTANCE.createTableQuery();
        TABLE_QUERY_STORE1.setTable(CatalogSupplier.TABLE_STORE);

        HIERARCHY_STORE1 = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        HIERARCHY_STORE1.setHasAll(true);
        HIERARCHY_STORE1.setPrimaryKey(CatalogSupplier.COLUMN_STORE_ID_STORE);
        HIERARCHY_STORE1.setQuery(TABLE_QUERY_STORE1);
        HIERARCHY_STORE1.getLevels().add(LEVEL_STORE1_COUNTRY);

        DIMENSION_STORE1 = RolapMappingFactory.eINSTANCE.createStandardDimension();
        DIMENSION_STORE1.setName("Store1");
        DIMENSION_STORE1.getHierarchies().add(HIERARCHY_STORE1);

        // Create Store2 dimension
        LEVEL_STORE2_COUNTRY = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_STORE2_COUNTRY.setName("Store Country");
        LEVEL_STORE2_COUNTRY.setUniqueMembers(true);
        LEVEL_STORE2_COUNTRY.setColumn(SQL_EXPR_COLUMN_STORE2);

        TABLE_QUERY_STORE2 = RolapMappingFactory.eINSTANCE.createTableQuery();
        TABLE_QUERY_STORE2.setTable(CatalogSupplier.TABLE_STORE_RAGGED);

        HIERARCHY_STORE2 = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        HIERARCHY_STORE2.setHasAll(true);
        HIERARCHY_STORE2.setPrimaryKey(CatalogSupplier.COLUMN_STORE_ID_STORE_RAGGED);
        HIERARCHY_STORE2.setQuery(TABLE_QUERY_STORE2);
        HIERARCHY_STORE2.getLevels().add(LEVEL_STORE2_COUNTRY);

        DIMENSION_STORE2 = RolapMappingFactory.eINSTANCE.createStandardDimension();
        DIMENSION_STORE2.setName("Store2");
        DIMENSION_STORE2.getHierarchies().add(HIERARCHY_STORE2);

        // Create Sales1 cube
        MEASURE_SALES1_UNIT_SALES = RolapMappingFactory.eINSTANCE.createSumMeasure();
        MEASURE_SALES1_UNIT_SALES.setName("Unit Sales");
        MEASURE_SALES1_UNIT_SALES.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
        MEASURE_SALES1_UNIT_SALES.setFormatString("Standard");

        MEASURE_SALES1_STORE_SALES = RolapMappingFactory.eINSTANCE.createSumMeasure();
        MEASURE_SALES1_STORE_SALES.setName("Store Sales");
        MEASURE_SALES1_STORE_SALES.setColumn(CatalogSupplier.COLUMN_STORE_SALES_SALESFACT);
        MEASURE_SALES1_STORE_SALES.setFormatString("Standard");

        CONNECTOR_SALES1_STORE1 = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        CONNECTOR_SALES1_STORE1.setOverrideDimensionName("Store1");
        CONNECTOR_SALES1_STORE1.setDimension(DIMENSION_STORE1);
        CONNECTOR_SALES1_STORE1.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);

        TABLE_QUERY_SALES1_FACT = RolapMappingFactory.eINSTANCE.createTableQuery();
        TABLE_QUERY_SALES1_FACT.setTable(CatalogSupplier.TABLE_SALES_FACT);

        MEASURE_GROUP_SALES1 = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        MEASURE_GROUP_SALES1.getMeasures().addAll(List.of(
            MEASURE_SALES1_UNIT_SALES,
            MEASURE_SALES1_STORE_SALES
        ));

        CUBE_SALES1 = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        CUBE_SALES1.setName("Sales1");
        CUBE_SALES1.setDefaultMeasure(MEASURE_SALES1_UNIT_SALES);
        CUBE_SALES1.setQuery(TABLE_QUERY_SALES1_FACT);
        CUBE_SALES1.getDimensionConnectors().add(CONNECTOR_SALES1_STORE1);
        CUBE_SALES1.getMeasureGroups().add(MEASURE_GROUP_SALES1);

        // Create Sales2 cube
        MEASURE_SALES2_UNIT_SALES = RolapMappingFactory.eINSTANCE.createSumMeasure();
        MEASURE_SALES2_UNIT_SALES.setName("Unit Sales");
        MEASURE_SALES2_UNIT_SALES.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
        MEASURE_SALES2_UNIT_SALES.setFormatString("Standard");

        CONNECTOR_SALES2_STORE2 = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        CONNECTOR_SALES2_STORE2.setOverrideDimensionName("Store2");
        CONNECTOR_SALES2_STORE2.setDimension(DIMENSION_STORE2);
        CONNECTOR_SALES2_STORE2.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);

        TABLE_QUERY_SALES2_FACT = RolapMappingFactory.eINSTANCE.createTableQuery();
        TABLE_QUERY_SALES2_FACT.setTable(CatalogSupplier.TABLE_SALES_FACT);

        MEASURE_GROUP_SALES2 = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        MEASURE_GROUP_SALES2.getMeasures().add(MEASURE_SALES2_UNIT_SALES);

        CUBE_SALES2 = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        CUBE_SALES2.setName("Sales2");
        CUBE_SALES2.setDefaultMeasure(MEASURE_SALES2_UNIT_SALES);
        CUBE_SALES2.setQuery(TABLE_QUERY_SALES2_FACT);
        CUBE_SALES2.getDimensionConnectors().add(CONNECTOR_SALES2_STORE2);
        CUBE_SALES2.getMeasureGroups().add(MEASURE_GROUP_SALES2);
    }

    public TestKeyExpressionCardinalityCacheModifier(Catalog baseCatalog) {
        // Copy the base catalog using EcoreUtil
        this.catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) baseCatalog);

        // Add the two cubes to the catalog
        this.catalog.getCubes().add(CUBE_SALES1);
        this.catalog.getCubes().add(CUBE_SALES2);
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
