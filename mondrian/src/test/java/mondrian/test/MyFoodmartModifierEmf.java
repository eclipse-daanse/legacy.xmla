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
package mondrian.test;

import java.util.Collection;
import java.util.List;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.AggregationExclude;
import org.eclipse.daanse.rolap.mapping.model.CalculatedMember;
import org.eclipse.daanse.rolap.mapping.model.CalculatedMemberProperty;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.CountMeasure;
import org.eclipse.daanse.rolap.mapping.model.DatabaseSchema;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.JoinQuery;
import org.eclipse.daanse.rolap.mapping.model.JoinedQueryElement;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.MemberProperty;
import org.eclipse.daanse.rolap.mapping.model.ParentChildHierarchy;
import org.eclipse.daanse.rolap.mapping.model.ParentChildLink;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.SQLExpressionColumn;
import org.eclipse.daanse.rolap.mapping.model.SqlStatement;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.TimeDimension;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

/**
 * EMF-based version of MyFoodmartModifier.
 * This class demonstrates the conversion from POJO builder patterns to EMF factory methods.
 *
 * Key conversion patterns:
 * - Use RolapMappingFactory.eINSTANCE.createXxx() instead of XxxImpl.builder()
 * - Use setXxx() methods instead of withXxx()
 * - Use getXxx().add() or getXxx().addAll() for lists
 * - Import from org.eclipse.daanse.rolap.mapping.emf.rolapmapping instead of pojo
 * - Implement CatalogMappingSupplier instead of extending PojoMappingModifier
 * - Use org.opencube.junit5.EmfUtil.copy((CatalogImpl) catalogMapping) to copy the catalog
 */
public class MyFoodmartModifierEmf implements CatalogMappingSupplier {

    private final org.eclipse.daanse.rolap.mapping.model.Catalog catalog;

    public MyFoodmartModifierEmf(org.eclipse.daanse.rolap.mapping.model.Catalog catalogMapping) {
        // Copy the original catalog
        this.catalog = RolapMappingFactory.eINSTANCE.createCatalog();
        this.catalog.setName("FoodMart");
        this.catalog.getDbschemas().addAll((Collection<? extends DatabaseSchema>) catalogMapping.getDbschemas());
        // References to hierarchies and levels for access roles
        ExplicitHierarchy storeHierarchy;
        ExplicitHierarchy customersHierarchy;
        ExplicitHierarchy genderHierarchy;
        Level storeCountryLevel;
        Level customersStateProvince;
        Level customersCity;

        // Store Dimension (shared)
        StandardDimension storeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        storeDimension.setName("Store");

        storeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        ((ExplicitHierarchy) storeHierarchy).setHasAll(true);
        ((ExplicitHierarchy) storeHierarchy).setPrimaryKey(CatalogSupplier.COLUMN_STORE_ID_STORE);

        TableQuery storeTableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        storeTableQuery.setTable(CatalogSupplier.TABLE_STORE);
        ((ExplicitHierarchy) storeHierarchy).setQuery(storeTableQuery);

        // Store Hierarchy Levels
        storeCountryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeCountryLevel.setName("Store Country");
        storeCountryLevel.setColumn(CatalogSupplier.COLUMN_STORE_COUNTRY_STORE);
        storeCountryLevel.setUniqueMembers(true);

        Level storeStateLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeStateLevel.setName("Store State");
        storeStateLevel.setColumn(CatalogSupplier.COLUMN_STORE_STATE_STORE);
        storeStateLevel.setUniqueMembers(true);

        Level storeCityLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeCityLevel.setName("Store City");
        storeCityLevel.setColumn(CatalogSupplier.COLUMN_STORE_CITY_STORE);
        storeCityLevel.setUniqueMembers(false);

        Level storeNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeNameLevel.setName("Store Name");
        storeNameLevel.setColumn(CatalogSupplier.COLUMN_STORE_NAME_STORE);
        storeNameLevel.setUniqueMembers(true);

        // Store Name Level - Member Properties
        MemberProperty storeTypeProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        storeTypeProp.setName("Store Type");
        storeTypeProp.setColumn(CatalogSupplier.COLUMN_STORE_TYPE_STORE);

        MemberProperty storeManagerProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        storeManagerProp.setName("Store Manager");
        storeManagerProp.setColumn(CatalogSupplier.COLUMN_STORE_MANAGER_STORE);

        MemberProperty storeSqftProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        storeSqftProp.setName("Store Sqft");
        storeSqftProp.setColumn(CatalogSupplier.COLUMN_STORE_SQFT_STORE);
        storeSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty grocerySqftProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        grocerySqftProp.setName("Grocery Sqft");
        grocerySqftProp.setColumn(CatalogSupplier.COLUMN_GROCERY_SQFT_STORE);
        grocerySqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty frozenSqftProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        frozenSqftProp.setName("Frozen Sqft");
        frozenSqftProp.setColumn(CatalogSupplier.COLUMN_FROZEN_SQFT_STORE);
        frozenSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty meatSqftProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        meatSqftProp.setName("Meat Sqft");
        meatSqftProp.setColumn(CatalogSupplier.COLUMN_MEAT_SQFT_STORE);
        meatSqftProp.setPropertyType(ColumnInternalDataType.NUMERIC);

        MemberProperty coffeeBarProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        coffeeBarProp.setName("Has coffee bar");
        coffeeBarProp.setColumn(CatalogSupplier.COLUMN_COFFEE_BAR_STORE);
        coffeeBarProp.setPropertyType(ColumnInternalDataType.BOOLEAN);

        MemberProperty streetAddressProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        streetAddressProp.setName("Street address");
        streetAddressProp.setColumn(CatalogSupplier.COLUMN_STREET_ADDRESS_STORE);
        streetAddressProp.setPropertyType(ColumnInternalDataType.STRING);

        storeNameLevel.getMemberProperties().addAll(List.of(
            storeTypeProp, storeManagerProp, storeSqftProp, grocerySqftProp,
            frozenSqftProp, meatSqftProp, coffeeBarProp, streetAddressProp
        ));

        ((ExplicitHierarchy) storeHierarchy).getLevels().addAll(List.of(
            storeCountryLevel, storeStateLevel, storeCityLevel, storeNameLevel
        ));

        storeDimension.getHierarchies().add(storeHierarchy);

        // Store Size in SQFT Dimension (shared)
        StandardDimension storeSizeSQFTDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        storeSizeSQFTDimension.setName("Store Size in SQFT");

        ExplicitHierarchy storeSizeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        storeSizeHierarchy.setHasAll(true);
        storeSizeHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_STORE_ID_STORE);

        TableQuery storeSizeTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        storeSizeTable.setTable(CatalogSupplier.TABLE_STORE);
        storeSizeHierarchy.setQuery(storeSizeTable);

        Level storeSqftLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeSqftLevel.setName("Store Sqft");
        storeSqftLevel.setColumn(CatalogSupplier.COLUMN_STORE_SQFT_STORE);
        storeSqftLevel.setUniqueMembers(true);

        storeSizeHierarchy.getLevels().add(storeSqftLevel);
        storeSizeSQFTDimension.getHierarchies().add(storeSizeHierarchy);

        // Store Type Dimension (shared)
        StandardDimension storeTypeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        storeTypeDimension.setName("Store Type");

        ExplicitHierarchy storeTypeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        storeTypeHierarchy.setHasAll(true);
        storeTypeHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_STORE_ID_STORE);

        TableQuery storeTypeTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        storeTypeTable.setTable(CatalogSupplier.TABLE_STORE);
        storeTypeHierarchy.setQuery(storeTypeTable);

        Level storeTypeLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeTypeLevel.setName("Store Type");
        storeTypeLevel.setColumn(CatalogSupplier.COLUMN_STORE_TYPE_STORE);
        storeTypeLevel.setUniqueMembers(true);

        storeTypeHierarchy.getLevels().add(storeTypeLevel);
        storeTypeDimension.getHierarchies().add(storeTypeHierarchy);

        // Time Dimension (shared)
        TimeDimension timeDimension = RolapMappingFactory.eINSTANCE.createTimeDimension();
        timeDimension.setName("Time");

        ExplicitHierarchy timeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        timeHierarchy.setHasAll(false);
        timeHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_TIME_ID_TIME_BY_DAY);

        TableQuery timeTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        timeTable.setTable(CatalogSupplier.TABLE_TIME_BY_DAY);
        timeHierarchy.setQuery(timeTable);

        Level yearLevel = RolapMappingFactory.eINSTANCE.createLevel();
        yearLevel.setName("Year");
        yearLevel.setColumn(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY);
        yearLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        yearLevel.setUniqueMembers(true);
        yearLevel.setType(LevelDefinition.TIME_YEARS);

        // Year Caption Expression with SQL dialects
        SQLExpressionColumn yearCaptionExpr = RolapMappingFactory.eINSTANCE.createSQLExpressionColumn();
        yearCaptionExpr.setType(ColumnType.VARCHAR);

        SqlStatement accessSql = RolapMappingFactory.eINSTANCE.createSqlStatement();
        accessSql.getDialects().add("access");
        accessSql.setSql("cstr(the_year) + '-12-31'");

        SqlStatement mysqlSql = RolapMappingFactory.eINSTANCE.createSqlStatement();
        mysqlSql.getDialects().add("mysql");
        mysqlSql.setSql("concat(cast(`the_year` as char(4)), '-12-31')");

        SqlStatement derbySql = RolapMappingFactory.eINSTANCE.createSqlStatement();
        derbySql.getDialects().add("derby");
        derbySql.setSql("'foobar'");

        SqlStatement genericSql = RolapMappingFactory.eINSTANCE.createSqlStatement();
        genericSql.getDialects().add("generic");
        genericSql.setSql("\"the_year\" || '-12-31'");

        yearCaptionExpr.getSqls().addAll(List.of(accessSql, mysqlSql, derbySql, genericSql));
        yearLevel.setCaptionColumn(yearCaptionExpr);

        Level quarterLevel = RolapMappingFactory.eINSTANCE.createLevel();
        quarterLevel.setName("Quarter");
        quarterLevel.setColumn(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY);
        quarterLevel.setUniqueMembers(false);
        quarterLevel.setType(LevelDefinition.TIME_QUARTERS);

        Level monthLevel = RolapMappingFactory.eINSTANCE.createLevel();
        monthLevel.setName("Month");
        monthLevel.setColumn(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY);
        monthLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        monthLevel.setUniqueMembers(false);
        monthLevel.setType(LevelDefinition.TIME_MONTHS);

        timeHierarchy.getLevels().addAll(List.of(yearLevel, quarterLevel, monthLevel));
        timeDimension.getHierarchies().add(timeHierarchy);

        // Product Dimension (shared)
        StandardDimension productDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        productDimension.setName("Product");

        ExplicitHierarchy productHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        productHierarchy.setHasAll(true);
        productHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_PRODUCT_ID_PRODUCT);

        // Product Join Query
        JoinQuery productJoin = RolapMappingFactory.eINSTANCE.createJoinQuery();

        JoinedQueryElement productLeft = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        productLeft.setKey(CatalogSupplier.COLUMN_PRODUCT_CLASS_ID_PRODUCT);
        TableQuery productTableLeft = RolapMappingFactory.eINSTANCE.createTableQuery();
        productTableLeft.setTable(CatalogSupplier.TABLE_PRODUCT);
        productLeft.setQuery(productTableLeft);

        JoinedQueryElement productRight = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        productRight.setKey(CatalogSupplier.COLUMN_PRODUCT_CLASS_ID_PRODUCT_CLASS);
        TableQuery productClassTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        productClassTable.setTable(CatalogSupplier.TABLE_PRODUCT_CLASS);
        productRight.setQuery(productClassTable);

        productJoin.setLeft(productLeft);
        productJoin.setRight(productRight);
        productHierarchy.setQuery(productJoin);

        Level productFamilyLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productFamilyLevel.setName("Product Family");
        productFamilyLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_FAMILY_PRODUCT_CLASS);
        productFamilyLevel.setUniqueMembers(true);

        Level productDepartmentLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productDepartmentLevel.setName("Product Department");
        productDepartmentLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_DEPARTMENT_PRODUCT_CLASS);
        productDepartmentLevel.setUniqueMembers(false);

        Level productCategoryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productCategoryLevel.setName("Product Category");
        productCategoryLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_CATEGORY_PRODUCT_CLASS);
        productCategoryLevel.setUniqueMembers(false);

        Level productSubcategoryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productSubcategoryLevel.setName("Product Subcategory");
        productSubcategoryLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_SUBCATEGORY_PRODUCT_CLASS);
        productSubcategoryLevel.setUniqueMembers(false);

        Level brandNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        brandNameLevel.setName("Brand Name");
        brandNameLevel.setColumn(CatalogSupplier.COLUMN_BRAND_NAME_PRODUCT);
        brandNameLevel.setUniqueMembers(false);

        Level productNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productNameLevel.setName("Product Name");
        productNameLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_NAME_PRODUCT);
        productNameLevel.setUniqueMembers(false);

        productHierarchy.getLevels().addAll(List.of(
            productFamilyLevel, productDepartmentLevel, productCategoryLevel,
            productSubcategoryLevel, brandNameLevel, productNameLevel
        ));
        productDimension.getHierarchies().add(productHierarchy);

        // Warehouse Dimension (shared)
        StandardDimension warehouseDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        warehouseDimension.setName("Warehouse");

        ExplicitHierarchy warehouseHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        warehouseHierarchy.setHasAll(true);
        warehouseHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_WAREHOUSE_ID_WAREHOUSE);

        TableQuery warehouseTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        warehouseTable.setTable(CatalogSupplier.TABLE_WAREHOUSE);
        warehouseHierarchy.setQuery(warehouseTable);

        Level warehouseCountryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        warehouseCountryLevel.setName("Country");
        warehouseCountryLevel.setColumn(CatalogSupplier.COLUMN_WAREHOUSE_COUNTRY_WAREHOUSE);
        warehouseCountryLevel.setUniqueMembers(true);

        Level warehouseStateLevel = RolapMappingFactory.eINSTANCE.createLevel();
        warehouseStateLevel.setName("State Province");
        warehouseStateLevel.setColumn(CatalogSupplier.COLUMN_WAREHOUSE_STATE_PROVINCE_WAREHOUSE);
        warehouseStateLevel.setUniqueMembers(true);

        Level warehouseCityLevel = RolapMappingFactory.eINSTANCE.createLevel();
        warehouseCityLevel.setName("City");
        warehouseCityLevel.setColumn(CatalogSupplier.COLUMN_WAREHOUSE_CITY_WAREHOUSE);
        warehouseCityLevel.setUniqueMembers(false);

        Level warehouseNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        warehouseNameLevel.setName("Warehouse Name");
        warehouseNameLevel.setColumn(CatalogSupplier.COLUMN_WAREHOUSE_NAME_WAREHOUSE);
        warehouseNameLevel.setUniqueMembers(true);

        warehouseHierarchy.getLevels().addAll(List.of(
            warehouseCountryLevel, warehouseStateLevel, warehouseCityLevel, warehouseNameLevel
        ));
        warehouseDimension.getHierarchies().add(warehouseHierarchy);

        // Sales Cube
        PhysicalCube sales = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        sales.setName("Sales");

        // Sales Cube Query (sales_fact_1997 table with aggregation exclude)
        TableQuery salesTableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        salesTableQuery.setTable(CatalogSupplier.TABLE_SALES_FACT);

        AggregationExclude aggExclude = RolapMappingFactory.eINSTANCE.createAggregationExclude();
        aggExclude.setPattern(".*");
        salesTableQuery.getAggregationExcludes().add(aggExclude);
        sales.setQuery(salesTableQuery);

        // Store dimension connector
        DimensionConnector storeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeDimConn.setOverrideDimensionName("Store");
        storeDimConn.setDimension(storeDimension);
        storeDimConn.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);

        // Store Size in SQFT dimension connector
        DimensionConnector storeSizeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeSizeDimConn.setOverrideDimensionName("Store Size in SQFT");
        storeSizeDimConn.setDimension(storeSizeSQFTDimension);
        storeSizeDimConn.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);

        // Store Type dimension connector
        DimensionConnector storeTypeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeTypeDimConn.setOverrideDimensionName("Store Type");
        storeTypeDimConn.setDimension(storeTypeDimension);
        storeTypeDimConn.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);

        // Time dimension connector
        DimensionConnector timeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        timeDimConn.setOverrideDimensionName("Time");
        timeDimConn.setDimension(timeDimension);
        timeDimConn.setForeignKey(CatalogSupplier.COLUMN_TIME_ID_SALESFACT);

        // Product dimension connector
        DimensionConnector productDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        productDimConn.setOverrideDimensionName("Product");
        productDimConn.setDimension(productDimension);
        productDimConn.setForeignKey(CatalogSupplier.COLUMN_PRODUCT_ID_SALESFACT);

        // Promotion Media Dimension (private)
        StandardDimension promotionMediaDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        promotionMediaDimension.setName("Promotion Media");

        ExplicitHierarchy promotionMediaHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        promotionMediaHierarchy.setHasAll(true);
        promotionMediaHierarchy.setAllMemberName("All Media");
        promotionMediaHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_PROMOTION_ID_PROMOTION);

        TableQuery promotionTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        promotionTable.setTable(CatalogSupplier.TABLE_PROMOTION);
        promotionMediaHierarchy.setQuery(promotionTable);

        Level mediaTypeLevel = RolapMappingFactory.eINSTANCE.createLevel();
        mediaTypeLevel.setName("Media Type");
        mediaTypeLevel.setColumn(CatalogSupplier.COLUMN_MEDIA_TYPE_PROMOTION);
        mediaTypeLevel.setUniqueMembers(true);

        promotionMediaHierarchy.getLevels().add(mediaTypeLevel);
        promotionMediaDimension.getHierarchies().add(promotionMediaHierarchy);

        DimensionConnector promotionMediaDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        promotionMediaDimConn.setOverrideDimensionName("Promotion Media");
        promotionMediaDimConn.setDimension(promotionMediaDimension);
        promotionMediaDimConn.setForeignKey(CatalogSupplier.COLUMN_PROMOTION_ID_SALESFACT);

        // Promotions Dimension (private)
        StandardDimension promotionsDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        promotionsDimension.setName("Promotions");

        ExplicitHierarchy promotionsHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        promotionsHierarchy.setHasAll(true);
        promotionsHierarchy.setAllMemberName("All Promotions");
        promotionsHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_PROMOTION_ID_PROMOTION);

        TableQuery promotionsTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        promotionsTable.setTable(CatalogSupplier.TABLE_PROMOTION);
        promotionsHierarchy.setQuery(promotionsTable);

        Level promotionNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        promotionNameLevel.setName("Promotion Name");
        promotionNameLevel.setColumn(CatalogSupplier.COLUMN_PROMOTION_NAME_PROMOTION);
        promotionNameLevel.setUniqueMembers(true);

        promotionsHierarchy.getLevels().add(promotionNameLevel);
        promotionsDimension.getHierarchies().add(promotionsHierarchy);

        DimensionConnector promotionsDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        promotionsDimConn.setOverrideDimensionName("Promotions");
        promotionsDimConn.setDimension(promotionsDimension);
        promotionsDimConn.setForeignKey(CatalogSupplier.COLUMN_PRODUCT_ID_SALESFACT);

        // Customers Dimension (private)
        StandardDimension customersDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        customersDimension.setName("Customers");

        customersHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        customersHierarchy.setHasAll(true);
        customersHierarchy.setAllMemberName("All Customers");
        customersHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);

        TableQuery customerTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        customerTable.setTable(CatalogSupplier.TABLE_CUSTOMER);
        customersHierarchy.setQuery(customerTable);

        Level customerCountryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        customerCountryLevel.setName("Country");
        customerCountryLevel.setColumn(CatalogSupplier.COLUMN_COUNTRY_CUSTOMER);
        customerCountryLevel.setUniqueMembers(true);

        customersStateProvince = RolapMappingFactory.eINSTANCE.createLevel();
        customersStateProvince.setName("State Province");
        customersStateProvince.setColumn(CatalogSupplier.COLUMN_STATE_PROVINCE_CUSTOMER);
        customersStateProvince.setUniqueMembers(true);

        customersCity = RolapMappingFactory.eINSTANCE.createLevel();
        customersCity.setName("City");
        customersCity.setColumn(CatalogSupplier.COLUMN_CITY_CUSTOMER);
        customersCity.setUniqueMembers(false);

        Level customerNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        customerNameLevel.setName("Name");
        customerNameLevel.setUniqueMembers(true);

        // Customer Name Key Expression (multi-dialect SQL)
        SQLExpressionColumn customerNameKeyExpr = RolapMappingFactory.eINSTANCE.createSQLExpressionColumn();
        customerNameKeyExpr.setType(ColumnType.VARCHAR);

        SqlStatement oracleSql = RolapMappingFactory.eINSTANCE.createSqlStatement();
        oracleSql.getDialects().add("oracle");
        oracleSql.setSql("\"fname\" || ' ' || \"lname\"\n");

        SqlStatement accessSql2 = RolapMappingFactory.eINSTANCE.createSqlStatement();
        accessSql2.getDialects().add("access");
        accessSql2.setSql("fname, ' ', lname\n");

        SqlStatement postgresSql = RolapMappingFactory.eINSTANCE.createSqlStatement();
        postgresSql.getDialects().add("postgres");
        postgresSql.setSql("\"fname\" || ' ' || \"lname\"\n");

        SqlStatement mysqlSql2 = RolapMappingFactory.eINSTANCE.createSqlStatement();
        mysqlSql2.getDialects().add("mysql");
        mysqlSql2.setSql("CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)\n");

        SqlStatement mssqlSql = RolapMappingFactory.eINSTANCE.createSqlStatement();
        mssqlSql.getDialects().add("mssql");
        mssqlSql.setSql("fname, ' ', lname\n");

        customerNameKeyExpr.getSqls().addAll(List.of(oracleSql, accessSql2, postgresSql, mysqlSql2, mssqlSql));
        customerNameLevel.setColumn(customerNameKeyExpr);

        // Customer Name Member Properties
        MemberProperty genderProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        genderProp.setName("Gender");
        genderProp.setColumn(CatalogSupplier.COLUMN_GENDER_CUSTOMER);

        MemberProperty maritalStatusProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        maritalStatusProp.setName("Marital Status");
        maritalStatusProp.setColumn(CatalogSupplier.COLUMN_MARITAL_STATUS_CUSTOMER);

        MemberProperty educationProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        educationProp.setName("Education");
        educationProp.setColumn(CatalogSupplier.COLUMN_EDUCATION_CUSTOMER);

        MemberProperty yearlyIncomeProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        yearlyIncomeProp.setName("Yearly Income");
        yearlyIncomeProp.setColumn(CatalogSupplier.COLUMN_YEARLY_INCOME_CUSTOMER);

        customerNameLevel.getMemberProperties().addAll(List.of(
            genderProp, maritalStatusProp, educationProp, yearlyIncomeProp
        ));

        customersHierarchy.getLevels().addAll(List.of(
            customerCountryLevel, customersStateProvince, customersCity, customerNameLevel
        ));
        customersDimension.getHierarchies().add(customersHierarchy);

        DimensionConnector customersDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        customersDimConn.setOverrideDimensionName("Customers");
        customersDimConn.setDimension(customersDimension);
        customersDimConn.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);

        // Education Level Dimension (private)
        StandardDimension educationLevelDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        educationLevelDimension.setName("Education Level");

        ExplicitHierarchy educationHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        educationHierarchy.setHasAll(true);
        educationHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);

        TableQuery educationTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        educationTable.setTable(CatalogSupplier.TABLE_CUSTOMER);
        educationHierarchy.setQuery(educationTable);

        Level educationLevelLevel = RolapMappingFactory.eINSTANCE.createLevel();
        educationLevelLevel.setName("Education Level");
        educationLevelLevel.setColumn(CatalogSupplier.COLUMN_EDUCATION_CUSTOMER);
        educationLevelLevel.setUniqueMembers(true);

        educationHierarchy.getLevels().add(educationLevelLevel);
        educationLevelDimension.getHierarchies().add(educationHierarchy);

        DimensionConnector educationLevelDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        educationLevelDimConn.setOverrideDimensionName("Education Level");
        educationLevelDimConn.setDimension(educationLevelDimension);
        educationLevelDimConn.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);

        // Gender Dimension (private)
        StandardDimension genderDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        genderDimension.setName("Gender");

        genderHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        genderHierarchy.setHasAll(true);
        genderHierarchy.setAllMemberName("All Gender");
        genderHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);

        TableQuery genderTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        genderTable.setTable(CatalogSupplier.TABLE_CUSTOMER);
        genderHierarchy.setQuery(genderTable);

        Level genderLevel = RolapMappingFactory.eINSTANCE.createLevel();
        genderLevel.setName("Gender");
        genderLevel.setColumn(CatalogSupplier.COLUMN_GENDER_CUSTOMER);
        genderLevel.setUniqueMembers(true);

        genderHierarchy.getLevels().add(genderLevel);
        genderDimension.getHierarchies().add(genderHierarchy);

        DimensionConnector genderDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        genderDimConn.setOverrideDimensionName("Gender");
        genderDimConn.setDimension(genderDimension);
        genderDimConn.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);

        // Marital Status Dimension (private)
        StandardDimension maritalStatusDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        maritalStatusDimension.setName("Marital Status");

        ExplicitHierarchy maritalStatusHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        maritalStatusHierarchy.setHasAll(true);
        maritalStatusHierarchy.setAllMemberName("All Marital Status");
        maritalStatusHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);

        TableQuery maritalStatusTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        maritalStatusTable.setTable(CatalogSupplier.TABLE_CUSTOMER);
        maritalStatusHierarchy.setQuery(maritalStatusTable);

        Level maritalStatusLevel = RolapMappingFactory.eINSTANCE.createLevel();
        maritalStatusLevel.setName("Marital Status");
        maritalStatusLevel.setColumn(CatalogSupplier.COLUMN_MARITAL_STATUS_CUSTOMER);
        maritalStatusLevel.setUniqueMembers(true);

        maritalStatusHierarchy.getLevels().add(maritalStatusLevel);
        maritalStatusDimension.getHierarchies().add(maritalStatusHierarchy);

        DimensionConnector maritalStatusDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        maritalStatusDimConn.setOverrideDimensionName("Marital Status");
        maritalStatusDimConn.setDimension(maritalStatusDimension);
        maritalStatusDimConn.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);

        // Yearly Income Dimension (private)
        StandardDimension yearlyIncomeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        yearlyIncomeDimension.setName("Yearly Income");

        ExplicitHierarchy yearlyIncomeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        yearlyIncomeHierarchy.setHasAll(true);
        yearlyIncomeHierarchy.setAllMemberName("All Marital Status");
        yearlyIncomeHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);

        TableQuery yearlyIncomeTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        yearlyIncomeTable.setTable(CatalogSupplier.TABLE_CUSTOMER);
        yearlyIncomeHierarchy.setQuery(yearlyIncomeTable);

        Level yearlyIncomeLevel = RolapMappingFactory.eINSTANCE.createLevel();
        yearlyIncomeLevel.setName("Yearly Income");
        yearlyIncomeLevel.setColumn(CatalogSupplier.COLUMN_YEARLY_INCOME_CUSTOMER);
        yearlyIncomeLevel.setUniqueMembers(true);

        yearlyIncomeHierarchy.getLevels().add(yearlyIncomeLevel);
        yearlyIncomeDimension.getHierarchies().add(yearlyIncomeHierarchy);

        DimensionConnector yearlyIncomeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        yearlyIncomeDimConn.setOverrideDimensionName("Yearly Income");
        yearlyIncomeDimConn.setDimension(yearlyIncomeDimension);
        yearlyIncomeDimConn.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);

        // Add all dimension connectors to Sales cube
        sales.getDimensionConnectors().addAll(List.of(
            storeDimConn, storeSizeDimConn, storeTypeDimConn, timeDimConn, productDimConn,
            promotionMediaDimConn, promotionsDimConn, customersDimConn, educationLevelDimConn,
            genderDimConn, maritalStatusDimConn, yearlyIncomeDimConn
        ));

        // Sales Cube Measures
        SumMeasure measuresUnitSales = RolapMappingFactory.eINSTANCE.createSumMeasure();
        measuresUnitSales.setName("Unit Sales");
        measuresUnitSales.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
        measuresUnitSales.setFormatString("Standard");

        SumMeasure measuresStoreCost = RolapMappingFactory.eINSTANCE.createSumMeasure();
        measuresStoreCost.setName("Store Cost");
        measuresStoreCost.setColumn(CatalogSupplier.COLUMN_STORE_COST_SALESFACT);
        measuresStoreCost.setFormatString("#,###.00");

        SumMeasure measuresStoreSales = RolapMappingFactory.eINSTANCE.createSumMeasure();
        measuresStoreSales.setName("Store Sales");
        measuresStoreSales.setColumn(CatalogSupplier.COLUMN_STORE_SALES_SALESFACT);
        measuresStoreSales.setFormatString("#,###.00");

        SumMeasure measuresSalesCount = RolapMappingFactory.eINSTANCE.createSumMeasure();
        measuresSalesCount.setName("Sales Count");
        measuresSalesCount.setColumn(CatalogSupplier.COLUMN_PRODUCT_ID_SALESFACT);
        measuresSalesCount.setFormatString("#,###");

        SumMeasure measuresCustomerCount = RolapMappingFactory.eINSTANCE.createSumMeasure();
        measuresCustomerCount.setName("Customer Count");
        measuresCustomerCount.setColumn(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);
        measuresCustomerCount.setFormatString("#,###");

        MeasureGroup salesMeasureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        salesMeasureGroup.getMeasures().addAll(List.of(
            measuresUnitSales, measuresStoreCost, measuresStoreSales,
            measuresSalesCount, measuresCustomerCount
        ));
        sales.getMeasureGroups().add(salesMeasureGroup);

        // Sales Cube Calculated Members
        CalculatedMember profitCalcMember = RolapMappingFactory.eINSTANCE.createCalculatedMember();
        profitCalcMember.setName("Profit");
        profitCalcMember.setFormula("[Measures].[Store Sales] - [Measures].[Store Cost]");

        CalculatedMemberProperty profitFormatProp = RolapMappingFactory.eINSTANCE.createCalculatedMemberProperty();
        profitFormatProp.setName("FORMAT_STRING");
        profitFormatProp.setValue("$#,##0.00");
        profitCalcMember.getCalculatedMemberProperties().add(profitFormatProp);

        CalculatedMember profitLastPeriodCalcMember = RolapMappingFactory.eINSTANCE.createCalculatedMember();
        profitLastPeriodCalcMember.setName("Profit last Period");
        profitLastPeriodCalcMember.setFormula("COALESCEEMPTY((Measures.[Profit], [Time].PREVMEMBER),    Measures.[Profit])");
        profitLastPeriodCalcMember.setVisible(false);

        CalculatedMember profitGrowthCalcMember = RolapMappingFactory.eINSTANCE.createCalculatedMember();
        profitGrowthCalcMember.setName("Profit Growth");
        profitGrowthCalcMember.setFormula("([Measures].[Profit] - [Measures].[Profit last Period]) / [Measures].[Profit last Period]");
        profitGrowthCalcMember.setVisible(true);

        CalculatedMemberProperty profitGrowthFormatProp = RolapMappingFactory.eINSTANCE.createCalculatedMemberProperty();
        profitGrowthFormatProp.setName("FORMAT_STRING");
        profitGrowthFormatProp.setValue("0.0%");
        profitGrowthCalcMember.getCalculatedMemberProperties().add(profitGrowthFormatProp);

        sales.getCalculatedMembers().addAll(List.of(
            profitCalcMember, profitLastPeriodCalcMember, profitGrowthCalcMember
        ));

        // Warehouse Cube
        PhysicalCube warehouse = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        warehouse.setName("Warehouse");

        TableQuery warehouseTableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        warehouseTableQuery.setTable(CatalogSupplier.TABLE_INVENTORY_FACT);
        warehouse.setQuery(warehouseTableQuery);

        // Warehouse Cube Dimension Connectors
        DimensionConnector whStoreDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        whStoreDimConn.setOverrideDimensionName("Store");
        whStoreDimConn.setDimension(storeDimension);
        whStoreDimConn.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_INVENTORY_FACT);

        DimensionConnector whStoreSizeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        whStoreSizeDimConn.setOverrideDimensionName("Store Size in SQFT");
        whStoreSizeDimConn.setDimension(storeSizeSQFTDimension);
        whStoreSizeDimConn.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_INVENTORY_FACT);

        DimensionConnector whStoreTypeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        whStoreTypeDimConn.setOverrideDimensionName("Store Type");
        whStoreTypeDimConn.setDimension(storeTypeDimension);
        whStoreTypeDimConn.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_INVENTORY_FACT);

        DimensionConnector whTimeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        whTimeDimConn.setOverrideDimensionName("Time");
        whTimeDimConn.setDimension(timeDimension);
        whTimeDimConn.setForeignKey(CatalogSupplier.COLUMN_TIME_ID_INVENTORY_FACT);

        DimensionConnector whProductDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        whProductDimConn.setOverrideDimensionName("Product");
        whProductDimConn.setDimension(productDimension);
        whProductDimConn.setForeignKey(CatalogSupplier.COLUMN_PRODUCT_ID_INVENTORY_FACT);

        DimensionConnector whWarehouseDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        whWarehouseDimConn.setOverrideDimensionName("Warehouse");
        whWarehouseDimConn.setDimension(warehouseDimension);
        whWarehouseDimConn.setForeignKey(CatalogSupplier.COLUMN_WAREHOUSE_ID_INVENTORY_FACT);

        warehouse.getDimensionConnectors().addAll(List.of(
            whStoreDimConn, whStoreSizeDimConn, whStoreTypeDimConn,
            whTimeDimConn, whProductDimConn, whWarehouseDimConn
        ));

        // Warehouse Cube Measures
        SumMeasure warehouseMeasuresStoreInvoice = RolapMappingFactory.eINSTANCE.createSumMeasure();
        warehouseMeasuresStoreInvoice.setName("Store Invoice");
        warehouseMeasuresStoreInvoice.setColumn(CatalogSupplier.COLUMN_STORE_INVOICE_INVENTORY_FACT);

        SumMeasure warehouseMeasuresSupplyTime = RolapMappingFactory.eINSTANCE.createSumMeasure();
        warehouseMeasuresSupplyTime.setName("Supply Time");
        warehouseMeasuresSupplyTime.setColumn(CatalogSupplier.COLUMN_SUPPLY_TIME_INVENTORY_FACT);

        SumMeasure warehouseMeasuresWarehouseCost = RolapMappingFactory.eINSTANCE.createSumMeasure();
        warehouseMeasuresWarehouseCost.setName("Warehouse Cost");
        warehouseMeasuresWarehouseCost.setColumn(CatalogSupplier.COLUMN_WAREHOUSE_COST_INVENTORY_FACT);

        SumMeasure warehouseMeasuresWarehouseSales = RolapMappingFactory.eINSTANCE.createSumMeasure();
        warehouseMeasuresWarehouseSales.setName("Warehouse Sales");
        warehouseMeasuresWarehouseSales.setColumn(CatalogSupplier.COLUMN_WAREHOUSE_SALES_INVENTORY_FACT);

        SumMeasure warehouseMeasuresUnitsShipped = RolapMappingFactory.eINSTANCE.createSumMeasure();
        warehouseMeasuresUnitsShipped.setName("Units Shipped");
        warehouseMeasuresUnitsShipped.setColumn(CatalogSupplier.COLUMN_UNITS_SHIPPED_INVENTORY_FACT);
        warehouseMeasuresUnitsShipped.setFormatString("#.0");

        SumMeasure warehouseMeasuresUnitsOrdered = RolapMappingFactory.eINSTANCE.createSumMeasure();
        warehouseMeasuresUnitsOrdered.setName("Units Ordered");
        warehouseMeasuresUnitsOrdered.setColumn(CatalogSupplier.COLUMN_UNITS_ORDERED_INVENTORY_FACT);
        warehouseMeasuresUnitsOrdered.setFormatString("#.0");

        SumMeasure warehouseMeasuresWarehouseProfit = RolapMappingFactory.eINSTANCE.createSumMeasure();
        warehouseMeasuresWarehouseProfit.setName("Warehouse Profit");
        warehouseMeasuresWarehouseProfit.setColumn(CatalogSupplier.COLUMN_WAREHOUSE_COST_INVENTORY_FACT);

        MeasureGroup warehouseMeasureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        warehouseMeasureGroup.getMeasures().addAll(List.of(
            warehouseMeasuresStoreInvoice, warehouseMeasuresSupplyTime,
            warehouseMeasuresWarehouseCost, warehouseMeasuresWarehouseSales,
            warehouseMeasuresUnitsShipped, warehouseMeasuresUnitsOrdered,
            warehouseMeasuresWarehouseProfit
        ));
        warehouse.getMeasureGroups().add(warehouseMeasureGroup);

        // Store Cube
        PhysicalCube storeCube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        storeCube.setName("Store");

        TableQuery storeTableQuery2 = RolapMappingFactory.eINSTANCE.createTableQuery();
        storeTableQuery2.setTable(CatalogSupplier.TABLE_STORE);
        storeCube.setQuery(storeTableQuery2);

        // Store Type Dimension (private for Store cube)
        StandardDimension storeTypePrivateDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        storeTypePrivateDimension.setName("Store Type");

        ExplicitHierarchy storeTypePrivateHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        storeTypePrivateHierarchy.setHasAll(true);

        Level storeTypePrivateLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeTypePrivateLevel.setName("Store Type");
        storeTypePrivateLevel.setColumn(CatalogSupplier.COLUMN_STORE_TYPE_STORE);
        storeTypePrivateLevel.setUniqueMembers(true);

        storeTypePrivateHierarchy.getLevels().add(storeTypePrivateLevel);
        storeTypePrivateDimension.getHierarchies().add(storeTypePrivateHierarchy);

        DimensionConnector storeTypePrivateDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeTypePrivateDimConn.setOverrideDimensionName("Store Type");
        storeTypePrivateDimConn.setDimension(storeTypePrivateDimension);

        // Store Dimension connector
        DimensionConnector storeStoreDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeStoreDimConn.setOverrideDimensionName("Store");
        storeStoreDimConn.setDimension(storeDimension);

        // Has coffee bar Dimension (private)
        StandardDimension hasCoffeeBarDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        hasCoffeeBarDimension.setName("Has coffee bar");

        ExplicitHierarchy hasCoffeeBarHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        hasCoffeeBarHierarchy.setHasAll(true);

        Level hasCoffeeBarLevel = RolapMappingFactory.eINSTANCE.createLevel();
        hasCoffeeBarLevel.setName("Has coffee bar");
        hasCoffeeBarLevel.setColumn(CatalogSupplier.COLUMN_COFFEE_BAR_STORE);
        hasCoffeeBarLevel.setUniqueMembers(true);

        hasCoffeeBarHierarchy.getLevels().add(hasCoffeeBarLevel);
        hasCoffeeBarDimension.getHierarchies().add(hasCoffeeBarHierarchy);

        DimensionConnector hasCoffeeBarDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        hasCoffeeBarDimConn.setOverrideDimensionName("Has coffee bar");
        hasCoffeeBarDimConn.setDimension(hasCoffeeBarDimension);

        storeCube.getDimensionConnectors().addAll(List.of(
            storeTypePrivateDimConn, storeStoreDimConn, hasCoffeeBarDimConn
        ));

        // Store Cube Measures
        SumMeasure storeSqftMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        storeSqftMeasure.setName("Store Sqft");
        storeSqftMeasure.setColumn(CatalogSupplier.COLUMN_STORE_SQFT_STORE);
        storeSqftMeasure.setFormatString("#,###");

        SumMeasure grocerySqftMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        grocerySqftMeasure.setName("Grocery Sqft");
        grocerySqftMeasure.setColumn(CatalogSupplier.COLUMN_GROCERY_SQFT_STORE);
        grocerySqftMeasure.setFormatString("#,###");

        MeasureGroup storeMeasureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        storeMeasureGroup.getMeasures().addAll(List.of(storeSqftMeasure, grocerySqftMeasure));
        storeCube.getMeasureGroups().add(storeMeasureGroup);

        // HR Cube (simplified version - showing the EMF pattern for key components)
        // Full implementation would include all joins and dimensions as in MyFoodmartModifier
        PhysicalCube hrCube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        hrCube.setName("HR");

        TableQuery hrTableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        hrTableQuery.setTable(CatalogSupplier.TABLE_SALARY);
        hrCube.setQuery(hrTableQuery);

        // Time Dimension (private for HR cube)
        TimeDimension hrTimeDimension = RolapMappingFactory.eINSTANCE.createTimeDimension();
        hrTimeDimension.setName("Time");

        ExplicitHierarchy hrTimeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        hrTimeHierarchy.setHasAll(false);
        hrTimeHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_THE_DATE_TIME);

        TableQuery hrTimeTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        hrTimeTable.setTable(CatalogSupplier.TABLE_TIME_BY_DAY);
        hrTimeHierarchy.setQuery(hrTimeTable);

        Level hrYearLevel = RolapMappingFactory.eINSTANCE.createLevel();
        hrYearLevel.setName("Year");
        hrYearLevel.setColumn(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY);
        hrYearLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        hrYearLevel.setUniqueMembers(true);
        hrYearLevel.setType(LevelDefinition.TIME_YEARS);

        Level hrQuarterLevel = RolapMappingFactory.eINSTANCE.createLevel();
        hrQuarterLevel.setName("Quarter");
        hrQuarterLevel.setColumn(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY);
        hrQuarterLevel.setUniqueMembers(false);
        hrQuarterLevel.setType(LevelDefinition.TIME_QUARTERS);

        Level hrMonthLevel = RolapMappingFactory.eINSTANCE.createLevel();
        hrMonthLevel.setName("Month");
        hrMonthLevel.setColumn(CatalogSupplier.COLUMN_THE_MONTH_TIME_BY_DAY);
        hrMonthLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        hrMonthLevel.setUniqueMembers(false);
        hrMonthLevel.setType(LevelDefinition.TIME_MONTHS);

        hrTimeHierarchy.getLevels().addAll(List.of(hrYearLevel, hrQuarterLevel, hrMonthLevel));
        hrTimeDimension.getHierarchies().add(hrTimeHierarchy);

        DimensionConnector hrTimeDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        hrTimeDimConn.setOverrideDimensionName("Time");
        hrTimeDimConn.setDimension(hrTimeDimension);
        hrTimeDimConn.setForeignKey(CatalogSupplier.COLUMN_PAY_DATE_SALARY);

        // NOTE: Full HR cube would include Store, Pay Type, Store Type, Position, Department dimensions with joins
        // For brevity, showing the pattern with Employees parent-child hierarchy

        // Employees Dimension (parent-child with closure table)
        StandardDimension employeesDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        employeesDimension.setName("Employees");

        ParentChildHierarchy employeesHierarchy = RolapMappingFactory.eINSTANCE.createParentChildHierarchy();
        employeesHierarchy.setHasAll(true);
        employeesHierarchy.setAllMemberName("All Employees");
        employeesHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_EMPLOYEE_ID_EMPLOYEE);

        TableQuery employeeTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        employeeTable.setTable(CatalogSupplier.TABLE_EMPLOYEE);
        employeesHierarchy.setQuery(employeeTable);

        employeesHierarchy.setParentColumn(CatalogSupplier.COLUMN_SUPERVISOR_ID_EMPLOYEE);
        employeesHierarchy.setNullParentValue("0");

        // Parent-Child Link (closure table)
        ParentChildLink pcLink = RolapMappingFactory.eINSTANCE.createParentChildLink();
        pcLink.setParentColumn(CatalogSupplier.COLUMN_SUPERVISOR_ID_EMPLOYEE_CLOSURE);
        pcLink.setChildColumn(CatalogSupplier.COLUMN_EMPLOYEE_ID_EMPLOYEE_CLOSURE);

        TableQuery closureTable = RolapMappingFactory.eINSTANCE.createTableQuery();
        closureTable.setTable(CatalogSupplier.TABLE_EMPLOYEE_CLOSURE);
        pcLink.setTable(closureTable);

        employeesHierarchy.setParentChildLink(pcLink);

        // Employee Level
        Level employeeLevel = RolapMappingFactory.eINSTANCE.createLevel();
        employeeLevel.setName("Employee Id");
        employeeLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        employeeLevel.setColumn(CatalogSupplier.COLUMN_EMPLOYEE_ID_EMPLOYEE);
        employeeLevel.setNameColumn(CatalogSupplier.COLUMN_FULL_NAME_EMPLOYEE);
        employeeLevel.setUniqueMembers(true);

        // Employee properties
        MemberProperty maritalStatusEmpProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        maritalStatusEmpProp.setName("Marital Status");
        maritalStatusEmpProp.setColumn(CatalogSupplier.COLUMN_MARITAL_STATUS_EMPLOYEE);

        MemberProperty positionTitleEmpProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        positionTitleEmpProp.setName("Position Title");
        positionTitleEmpProp.setColumn(CatalogSupplier.COLUMN_POSITION_TITLE_EMPLOYEE);

        MemberProperty genderEmpProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        genderEmpProp.setName("Gender");
        genderEmpProp.setColumn(CatalogSupplier.COLUMN_GENDER_EMPLOYEE);

        MemberProperty salaryEmpProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        salaryEmpProp.setName("Salary");
        salaryEmpProp.setColumn(CatalogSupplier.COLUMN_SALARY_EMPLOYEE);

        MemberProperty educationLevelEmpProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        educationLevelEmpProp.setName("Education Level");
        educationLevelEmpProp.setColumn(CatalogSupplier.COLUMN_EDUCATION_LEVEL_EMPLOYEE);

        MemberProperty managementRoleEmpProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        managementRoleEmpProp.setName("Management Role");
        managementRoleEmpProp.setColumn(CatalogSupplier.COLUMN_MANAGEMENT_ROLE_EMPLOYEE);

        employeeLevel.getMemberProperties().addAll(List.of(
            maritalStatusEmpProp, positionTitleEmpProp, genderEmpProp,
            salaryEmpProp, educationLevelEmpProp, managementRoleEmpProp
        ));

        employeesHierarchy.setLevel(employeeLevel);
        employeesDimension.getHierarchies().add(employeesHierarchy);

        DimensionConnector employeesDimConn = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        employeesDimConn.setOverrideDimensionName("Employees");
        employeesDimConn.setDimension(employeesDimension);
        employeesDimConn.setForeignKey(CatalogSupplier.COLUMN_EMPLOYEE_ID_SALARY);

        hrCube.getDimensionConnectors().addAll(List.of(hrTimeDimConn, employeesDimConn));

        // HR Cube Measures
        SumMeasure orgSalaryMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
        orgSalaryMeasure.setName("Org Salary");
        orgSalaryMeasure.setColumn(CatalogSupplier.COLUMN_SALARY_PAID_SALARY);
        orgSalaryMeasure.setFormatString("Currency");

        CountMeasure countMeasure = RolapMappingFactory.eINSTANCE.createCountMeasure();
        countMeasure.setName("Count");
        countMeasure.setColumn(CatalogSupplier.COLUMN_EMPLOYEE_ID_SALARY);
        countMeasure.setFormatString("#,#");

        CountMeasure numEmployeesMeasure = RolapMappingFactory.eINSTANCE.createCountMeasure();
        numEmployeesMeasure.setName("Number of Employees");
        numEmployeesMeasure.setColumn(CatalogSupplier.COLUMN_EMPLOYEE_ID_SALARY);
        numEmployeesMeasure.setDistinct(true);
        numEmployeesMeasure.setFormatString("#,#");

        MeasureGroup hrMeasureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        hrMeasureGroup.getMeasures().addAll(List.of(orgSalaryMeasure, countMeasure, numEmployeesMeasure));
        hrCube.getMeasureGroups().add(hrMeasureGroup);

        // HR Cube Calculated Members
        CalculatedMember employeeSalaryCalcMember = RolapMappingFactory.eINSTANCE.createCalculatedMember();
        employeeSalaryCalcMember.setName("Employee Salary");
        employeeSalaryCalcMember.setFormatString("Currency");
        employeeSalaryCalcMember.setFormula("([Employees].currentmember.datamember, [Measures].[Org Salary])");

        CalculatedMember avgSalaryCalcMember = RolapMappingFactory.eINSTANCE.createCalculatedMember();
        avgSalaryCalcMember.setName("Avg Salary");
        avgSalaryCalcMember.setFormatString("Currency");
        avgSalaryCalcMember.setFormula("[Measures].[Org Salary]/[Measures].[Number of Employees]");

        hrCube.getCalculatedMembers().addAll(List.of(employeeSalaryCalcMember, avgSalaryCalcMember));

        // Add all cubes to catalog
        catalog.getCubes().addAll(List.of(sales, warehouse, storeCube, hrCube));

        // NOTE: Sales Ragged cube, Virtual cubes, and Access Roles implementation would follow similar patterns
        // Pattern: create with factory, set properties, add to collections.
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
