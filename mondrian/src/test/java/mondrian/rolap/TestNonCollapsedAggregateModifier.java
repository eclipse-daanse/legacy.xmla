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
import org.eclipse.daanse.rolap.mapping.model.AggregationColumnName;
import org.eclipse.daanse.rolap.mapping.model.AggregationExclude;
import org.eclipse.daanse.rolap.mapping.model.AggregationLevel;
import org.eclipse.daanse.rolap.mapping.model.AggregationMeasure;
import org.eclipse.daanse.rolap.mapping.model.AggregationName;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.Column;
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
import org.eclipse.daanse.rolap.mapping.model.Table;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * EMF version of TestNonCollapsedAggregateModifier from TestAggregationManager.
 * Creates a cube "Foo" with Product dimension and aggregation table agg_l_05_sales_fact_1997
 * that has collapsed=false for the Product Id level.
 *
 * <Cube name="Foo">
 *   <Table name="sales_fact_1997">
 *     <AggExclude name="agg_g_ms_pcat_sales_fact_1997"/>
 *     <AggExclude name="agg_c_14_sales_fact_1997"/>
 *     <AggExclude name="agg_pl_01_sales_fact_1997"/>
 *     <AggExclude name="agg_ll_01_sales_fact_1997"/>
 *     <AggName name="agg_l_05_sales_fact_1997">
 *       <AggFactCount column="fact_count"/>
 *       <AggIgnoreColumn column="customer_id"/>
 *       <AggIgnoreColumn column="store_id"/>
 *       <AggIgnoreColumn column="promotion_id"/>
 *       <AggIgnoreColumn column="store_sales"/>
 *       <AggIgnoreColumn column="store_cost"/>
 *       <AggMeasure name="[Measures].[Unit Sales]" column="unit_sales"/>
 *       <AggLevel name="[Product].[Product].[Product Id]" column="product_id" collapsed="false"/>
 *     </AggName>
 *   </Table>
 *   <Dimension name="Product" foreignKey="product_id">
 *     <Hierarchy hasAll="true" primaryKey="product_id">
 *       <Join leftKey="product_class_id" rightKey="product_class_id">
 *         <Table name="product"/>
 *         <Table name="product_class"/>
 *       </Join>
 *       <Level name="Product Family" column="product_family" uniqueMembers="true"/>
 *       <Level name="Product Department" column="product_department" uniqueMembers="false"/>
 *       <Level name="Product Category" column="product_category" uniqueMembers="false"/>
 *       <Level name="Product Subcategory" column="product_subcategory" uniqueMembers="false"/>
 *       <Level name="Brand Name" column="brand_name" uniqueMembers="false"/>
 *       <Level name="Product Name" column="product_name" uniqueMembers="true"/>
 *       <Level name="Product Id" column="product_id" uniqueMembers="true"/>
 *     </Hierarchy>
 *   </Dimension>
 *   <Measure name="Unit Sales" column="unit_sales" aggregator="sum" formatString="Standard" visible="false"/>
 * </Cube>
 */
public class TestNonCollapsedAggregateModifier implements CatalogMappingSupplier {

    private final Catalog catalog;


    public TestNonCollapsedAggregateModifier(Catalog baseCatalog) {
        // Copy the base catalog using EcoreUtil
        EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) baseCatalog);
        this.catalog = (Catalog) copier.get(baseCatalog);

        // Static measure
        SumMeasure MEASURE_UNIT_SALES;

        // Static aggregation excludes
        AggregationExclude AGG_EXCLUDE_1;
        AggregationExclude AGG_EXCLUDE_2;
        AggregationExclude AGG_EXCLUDE_3;
        AggregationExclude AGG_EXCLUDE_4;

        // Static aggregation ignore columns
        AggregationColumnName AGG_IGNORE_CUSTOMER_ID;
        AggregationColumnName AGG_IGNORE_STORE_ID;
        AggregationColumnName AGG_IGNORE_PROMOTION_ID;
        AggregationColumnName AGG_IGNORE_STORE_SALES;
        AggregationColumnName AGG_IGNORE_STORE_COST;

        // Static aggregation fact count
        AggregationColumnName AGG_FACT_COUNT;

        // Static aggregation measure
        AggregationMeasure AGG_MEASURE_UNIT_SALES;

        // Static aggregation level
        AggregationLevel AGG_LEVEL_PRODUCT_ID;

        // Static aggregation table
        AggregationName AGGREGATION_TABLE;

        // Static fact table query
        TableQuery TABLE_QUERY_SALES_FACT;

        // Static Product dimension levels
        Level LEVEL_PRODUCT_FAMILY;
        Level LEVEL_PRODUCT_DEPARTMENT;
        Level LEVEL_PRODUCT_CATEGORY;
        Level LEVEL_PRODUCT_SUBCATEGORY;
        Level LEVEL_BRAND_NAME;
        Level LEVEL_PRODUCT_NAME;
        Level LEVEL_PRODUCT_ID;

        // Static join query for Product dimension
        JoinedQueryElement JOIN_LEFT_PRODUCT;
        JoinedQueryElement JOIN_RIGHT_PRODUCT_CLASS;
        JoinQuery JOIN_QUERY_PRODUCT;

        // Static Product hierarchy
        ExplicitHierarchy HIERARCHY_PRODUCT;

        // Static Product dimension
        StandardDimension DIMENSION_PRODUCT;

        // Static dimension connector
        DimensionConnector CONNECTOR_PRODUCT;

        // Static measure group
        MeasureGroup MEASURE_GROUP;

        // Static cube
        PhysicalCube CUBE_FOO;

        // Create measure (visible=false)
        MEASURE_UNIT_SALES = RolapMappingFactory.eINSTANCE.createSumMeasure();
        MEASURE_UNIT_SALES.setName("Unit Sales");
        MEASURE_UNIT_SALES.setColumn((Column) copier.get(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT));
        MEASURE_UNIT_SALES.setFormatString("Standard");
        MEASURE_UNIT_SALES.setVisible(false);

        // Create aggregation excludes
        AGG_EXCLUDE_1 = RolapMappingFactory.eINSTANCE.createAggregationExclude();
        AGG_EXCLUDE_1.setName("agg_g_ms_pcat_sales_fact_1997");

        AGG_EXCLUDE_2 = RolapMappingFactory.eINSTANCE.createAggregationExclude();
        AGG_EXCLUDE_2.setName("agg_c_14_sales_fact_1997");

        AGG_EXCLUDE_3 = RolapMappingFactory.eINSTANCE.createAggregationExclude();
        AGG_EXCLUDE_3.setName("agg_pl_01_sales_fact_1997");

        AGG_EXCLUDE_4 = RolapMappingFactory.eINSTANCE.createAggregationExclude();
        AGG_EXCLUDE_4.setName("agg_ll_01_sales_fact_1997");

        // Create aggregation ignore columns
        AGG_IGNORE_CUSTOMER_ID = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        AGG_IGNORE_CUSTOMER_ID.setColumn((Column) copier.get(CatalogSupplier.COLUMN_CUSTOMER_ID_AGG_L_05_SALES_FACT_1997));

        AGG_IGNORE_STORE_ID = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        AGG_IGNORE_STORE_ID.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_ID_AGG_L_05_SALES_FACT_1997));

        AGG_IGNORE_PROMOTION_ID = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        AGG_IGNORE_PROMOTION_ID.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PROMOTION_ID_AGG_L_05_SALES_FACT_1997));

        AGG_IGNORE_STORE_SALES = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        AGG_IGNORE_STORE_SALES.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_SALES_AGG_L_05_SALES_FACT_1997));

        AGG_IGNORE_STORE_COST = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        AGG_IGNORE_STORE_COST.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_COST_AGG_L_05_SALES_FACT_1997));

        // Create aggregation fact count
        AGG_FACT_COUNT = RolapMappingFactory.eINSTANCE.createAggregationColumnName();
        AGG_FACT_COUNT.setColumn((Column) copier.get(CatalogSupplier.COLUMN_FACT_COUNT_AGG_L_05_SALES_FACT_1997));

        // Create aggregation measure
        AGG_MEASURE_UNIT_SALES = RolapMappingFactory.eINSTANCE.createAggregationMeasure();
        AGG_MEASURE_UNIT_SALES.setName("[Measures].[Unit Sales]");
        AGG_MEASURE_UNIT_SALES.setColumn((Column) copier.get(CatalogSupplier.COLUMN_UNIT_SALES_AGG_L_05_SALES_FACT_1997));

        // Create aggregation level with collapsed=false
        AGG_LEVEL_PRODUCT_ID = RolapMappingFactory.eINSTANCE.createAggregationLevel();
        AGG_LEVEL_PRODUCT_ID.setName("[Product].[Product].[Product Id]");
        AGG_LEVEL_PRODUCT_ID.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_ID_AGG_L_05_SALES_FACT_1997));
        AGG_LEVEL_PRODUCT_ID.setCollapsed(false);

        // Create aggregation table
        AGGREGATION_TABLE = RolapMappingFactory.eINSTANCE.createAggregationName();
        AGGREGATION_TABLE.setName((Table) copier.get(CatalogSupplier.TABLE_AGG_L_05_SALES_FACT));
        AGGREGATION_TABLE.setAggregationFactCount(AGG_FACT_COUNT);
        AGGREGATION_TABLE.getAggregationIgnoreColumns().addAll(List.of(
            AGG_IGNORE_CUSTOMER_ID,
            AGG_IGNORE_STORE_ID,
            AGG_IGNORE_PROMOTION_ID,
            AGG_IGNORE_STORE_SALES,
            AGG_IGNORE_STORE_COST
        ));
        AGGREGATION_TABLE.getAggregationMeasures().add(AGG_MEASURE_UNIT_SALES);
        AGGREGATION_TABLE.getAggregationLevels().add(AGG_LEVEL_PRODUCT_ID);

        // Create fact table query with aggregation table and excludes
        TABLE_QUERY_SALES_FACT = RolapMappingFactory.eINSTANCE.createTableQuery();
        TABLE_QUERY_SALES_FACT.setTable((Table) copier.get(CatalogSupplier.TABLE_SALES_FACT));
        TABLE_QUERY_SALES_FACT.getAggregationExcludes().addAll(List.of(
            AGG_EXCLUDE_1, AGG_EXCLUDE_2, AGG_EXCLUDE_3, AGG_EXCLUDE_4
        ));
        TABLE_QUERY_SALES_FACT.getAggregationTables().add(AGGREGATION_TABLE);

        // Create Product dimension levels
        LEVEL_PRODUCT_FAMILY = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_PRODUCT_FAMILY.setName("Product Family");
        LEVEL_PRODUCT_FAMILY.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_FAMILY_PRODUCT_CLASS));
        LEVEL_PRODUCT_FAMILY.setUniqueMembers(true);

        LEVEL_PRODUCT_DEPARTMENT = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_PRODUCT_DEPARTMENT.setName("Product Department");
        LEVEL_PRODUCT_DEPARTMENT.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_DEPARTMENT_PRODUCT_CLASS));
        LEVEL_PRODUCT_DEPARTMENT.setUniqueMembers(false);

        LEVEL_PRODUCT_CATEGORY = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_PRODUCT_CATEGORY.setName("Product Category");
        LEVEL_PRODUCT_CATEGORY.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_CATEGORY_PRODUCT_CLASS));
        LEVEL_PRODUCT_CATEGORY.setUniqueMembers(false);

        LEVEL_PRODUCT_SUBCATEGORY = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_PRODUCT_SUBCATEGORY.setName("Product Subcategory");
        LEVEL_PRODUCT_SUBCATEGORY.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_SUBCATEGORY_PRODUCT_CLASS));
        LEVEL_PRODUCT_SUBCATEGORY.setUniqueMembers(false);

        LEVEL_BRAND_NAME = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_BRAND_NAME.setName("Brand Name");
        LEVEL_BRAND_NAME.setColumn((Column) copier.get(CatalogSupplier.COLUMN_BRAND_NAME_PRODUCT));
        LEVEL_BRAND_NAME.setUniqueMembers(false);

        LEVEL_PRODUCT_NAME = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_PRODUCT_NAME.setName("Product Name");
        LEVEL_PRODUCT_NAME.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_NAME_PRODUCT));
        LEVEL_PRODUCT_NAME.setUniqueMembers(true);

        LEVEL_PRODUCT_ID = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_PRODUCT_ID.setName("Product Id");
        LEVEL_PRODUCT_ID.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_ID_PRODUCT));
        LEVEL_PRODUCT_ID.setUniqueMembers(true);

        // Create join query for Product dimension (product JOIN product_class)
        TableQuery productTableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productTableQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_PRODUCT));

        JOIN_LEFT_PRODUCT = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        JOIN_LEFT_PRODUCT.setKey((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_CLASS_ID_PRODUCT));
        JOIN_LEFT_PRODUCT.setQuery(productTableQuery);

        TableQuery productClassTableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productClassTableQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_PRODUCT_CLASS));

        JOIN_RIGHT_PRODUCT_CLASS = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        JOIN_RIGHT_PRODUCT_CLASS.setKey((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_CLASS_ID_PRODUCT_CLASS));
        JOIN_RIGHT_PRODUCT_CLASS.setQuery(productClassTableQuery);

        JOIN_QUERY_PRODUCT = RolapMappingFactory.eINSTANCE.createJoinQuery();
        JOIN_QUERY_PRODUCT.setLeft(JOIN_LEFT_PRODUCT);
        JOIN_QUERY_PRODUCT.setRight(JOIN_RIGHT_PRODUCT_CLASS);

        // Create Product hierarchy
        HIERARCHY_PRODUCT = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        HIERARCHY_PRODUCT.setHasAll(true);
        HIERARCHY_PRODUCT.setPrimaryKey((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_ID_PRODUCT));
        HIERARCHY_PRODUCT.setQuery(JOIN_QUERY_PRODUCT);
        HIERARCHY_PRODUCT.getLevels().addAll(List.of(
            LEVEL_PRODUCT_FAMILY,
            LEVEL_PRODUCT_DEPARTMENT,
            LEVEL_PRODUCT_CATEGORY,
            LEVEL_PRODUCT_SUBCATEGORY,
            LEVEL_BRAND_NAME,
            LEVEL_PRODUCT_NAME,
            LEVEL_PRODUCT_ID
        ));

        // Create Product dimension
        DIMENSION_PRODUCT = RolapMappingFactory.eINSTANCE.createStandardDimension();
        DIMENSION_PRODUCT.setName("Product");
        DIMENSION_PRODUCT.getHierarchies().add(HIERARCHY_PRODUCT);

        // Create dimension connector
        CONNECTOR_PRODUCT = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        CONNECTOR_PRODUCT.setOverrideDimensionName("Product");
        CONNECTOR_PRODUCT.setDimension(DIMENSION_PRODUCT);
        CONNECTOR_PRODUCT.setForeignKey((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_ID_SALESFACT));

        // Create measure group
        MEASURE_GROUP = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        MEASURE_GROUP.getMeasures().add(MEASURE_UNIT_SALES);

        // Create Foo cube
        CUBE_FOO = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        CUBE_FOO.setName("Foo");
        CUBE_FOO.setDefaultMeasure(MEASURE_UNIT_SALES);
        CUBE_FOO.setQuery(TABLE_QUERY_SALES_FACT);
        CUBE_FOO.getDimensionConnectors().add(CONNECTOR_PRODUCT);
        CUBE_FOO.getMeasureGroups().add(MEASURE_GROUP);

        // Add the cube to the catalog
        this.catalog.getCubes().add(CUBE_FOO);
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
