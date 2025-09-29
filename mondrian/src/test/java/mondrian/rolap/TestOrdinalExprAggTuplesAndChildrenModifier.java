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

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.JoinQuery;
import org.eclipse.daanse.rolap.mapping.model.JoinedQueryElement;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

/**
 * EMF version of TestOrdinalExprAggTuplesAndChildrenModifier from TestAggregationManager.
 */
public class TestOrdinalExprAggTuplesAndChildrenModifier implements CatalogMappingSupplier {

    private final Catalog catalog;

    public TestOrdinalExprAggTuplesAndChildrenModifier(Catalog baseCatalog) {
        // Copy the base catalog using EcoreUtil
        this.catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) baseCatalog);

        // Create objects using RolapMappingFactory
        RolapMappingFactory factory = RolapMappingFactory.eINSTANCE;

        // Create TableQuery for sales_fact_1997
        TableQuery salesFactQuery = factory.createTableQuery();
        salesFactQuery.setTable(CatalogSupplier.TABLE_SALES_FACT);

        // Create Product dimension with join between product and product_class tables
        TableQuery productTableQuery = factory.createTableQuery();
        productTableQuery.setTable(CatalogSupplier.TABLE_PRODUCT);

        TableQuery productClassTableQuery = factory.createTableQuery();
        productClassTableQuery.setTable(CatalogSupplier.TABLE_PRODUCT_CLASS);

        JoinedQueryElement productJoinLeft = factory.createJoinedQueryElement();
        productJoinLeft.setKey(CatalogSupplier.COLUMN_PRODUCT_CLASS_ID_PRODUCT);
        productJoinLeft.setQuery(productTableQuery);

        JoinedQueryElement productClassJoinRight = factory.createJoinedQueryElement();
        productClassJoinRight.setKey(CatalogSupplier.COLUMN_PRODUCT_CLASS_ID_PRODUCT_CLASS);
        productClassJoinRight.setQuery(productClassTableQuery);

        JoinQuery productJoinQuery = factory.createJoinQuery();
        productJoinQuery.setLeft(productJoinLeft);
        productJoinQuery.setRight(productClassJoinRight);

        // Create levels for Product hierarchy
        Level productFamilyLevel = factory.createLevel();
        productFamilyLevel.setName("Product Family");
        productFamilyLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_FAMILY_PRODUCT_CLASS);
        productFamilyLevel.setUniqueMembers(true);

        Level productDepartmentLevel = factory.createLevel();
        productDepartmentLevel.setName("Product Department");
        productDepartmentLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_DEPARTMENT_PRODUCT_CLASS);
        productDepartmentLevel.setUniqueMembers(false);

        Level productCategoryLevel = factory.createLevel();
        productCategoryLevel.setName("Product Category");
        productCategoryLevel.setCaptionColumn(CatalogSupplier.COLUMN_PRODUCT_FAMILY_PRODUCT_CLASS);
        productCategoryLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_CATEGORY_PRODUCT_CLASS);
        productCategoryLevel.setUniqueMembers(false);

        Level productSubcategoryLevel = factory.createLevel();
        productSubcategoryLevel.setName("Product Subcategory");
        productSubcategoryLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_SUBCATEGORY_PRODUCT_CLASS);
        productSubcategoryLevel.setUniqueMembers(false);

        Level brandNameLevel = factory.createLevel();
        brandNameLevel.setName("Brand Name");
        brandNameLevel.setColumn(CatalogSupplier.COLUMN_BRAND_NAME_PRODUCT);
        brandNameLevel.setUniqueMembers(false);

        Level productNameLevel = factory.createLevel();
        productNameLevel.setName("Product Name");
        productNameLevel.setColumn(CatalogSupplier.COLUMN_PRODUCT_NAME_PRODUCT);
        productNameLevel.setUniqueMembers(true);

        // Create Product hierarchy
        ExplicitHierarchy productHierarchy = factory.createExplicitHierarchy();
        productHierarchy.setHasAll(true);
        productHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_PRODUCT_ID_PRODUCT);
        productHierarchy.setQuery(productJoinQuery);
        productHierarchy.getLevels().add(productFamilyLevel);
        productHierarchy.getLevels().add(productDepartmentLevel);
        productHierarchy.getLevels().add(productCategoryLevel);
        productHierarchy.getLevels().add(productSubcategoryLevel);
        productHierarchy.getLevels().add(brandNameLevel);
        productHierarchy.getLevels().add(productNameLevel);

        // Create Product dimension
        StandardDimension productDimension = factory.createStandardDimension();
        productDimension.setName("Product");
        productDimension.getHierarchies().add(productHierarchy);

        // Create Product dimension connector
        DimensionConnector productDimensionConnector = factory.createDimensionConnector();
        productDimensionConnector.setOverrideDimensionName("Product");
        productDimensionConnector.setForeignKey(CatalogSupplier.COLUMN_PRODUCT_ID_SALESFACT);
        productDimensionConnector.setDimension(productDimension);

        // Create Gender dimension
        TableQuery customerTableQuery = factory.createTableQuery();
        customerTableQuery.setTable(CatalogSupplier.TABLE_CUSTOMER);

        Level genderLevel = factory.createLevel();
        genderLevel.setName("Gender");
        genderLevel.setColumn(CatalogSupplier.COLUMN_GENDER_CUSTOMER);
        genderLevel.setUniqueMembers(true);

        ExplicitHierarchy genderHierarchy = factory.createExplicitHierarchy();
        genderHierarchy.setHasAll(false);
        genderHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER);
        genderHierarchy.setQuery(customerTableQuery);
        genderHierarchy.getLevels().add(genderLevel);

        StandardDimension genderDimension = factory.createStandardDimension();
        genderDimension.setName("Gender");
        genderDimension.getHierarchies().add(genderHierarchy);

        // Create Gender dimension connector
        DimensionConnector genderDimensionConnector = factory.createDimensionConnector();
        genderDimensionConnector.setOverrideDimensionName("Gender");
        genderDimensionConnector.setForeignKey(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT);
        genderDimensionConnector.setDimension(genderDimension);

        // Create measures
        SumMeasure unitSalesMeasure = factory.createSumMeasure();
        unitSalesMeasure.setName("Unit Sales");
        unitSalesMeasure.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
        unitSalesMeasure.setFormatString("Standard");
        unitSalesMeasure.setVisible(false);

        SumMeasure storeCostMeasure = factory.createSumMeasure();
        storeCostMeasure.setName("Store Cost");
        storeCostMeasure.setColumn(CatalogSupplier.COLUMN_STORE_COST_SALESFACT);
        storeCostMeasure.setFormatString("#,###.00");

        // Create measure group
        MeasureGroup measureGroup = factory.createMeasureGroup();
        measureGroup.getMeasures().add(unitSalesMeasure);
        measureGroup.getMeasures().add(storeCostMeasure);

        // Create Sales_Prod_Ord cube
        PhysicalCube salesProdOrdCube = factory.createPhysicalCube();
        salesProdOrdCube.setName("Sales_Prod_Ord");
        salesProdOrdCube.setQuery(salesFactQuery);
        salesProdOrdCube.getDimensionConnectors().add(productDimensionConnector);
        salesProdOrdCube.getDimensionConnectors().add(genderDimensionConnector);
        salesProdOrdCube.getMeasureGroups().add(measureGroup);

        // Add cube to catalog
        this.catalog.getCubes().add(salesProdOrdCube);
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
