/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package mondrian.rolap.aggmatcher;

import java.util.Collection;
import java.util.List;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.AggregationExclude;
import org.eclipse.daanse.rolap.mapping.model.AggregationTable;
import org.eclipse.daanse.rolap.mapping.model.AvgMeasure;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.Column;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.CountMeasure;
import org.eclipse.daanse.rolap.mapping.model.DatabaseSchema;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.JoinQuery;
import org.eclipse.daanse.rolap.mapping.model.JoinedQueryElement;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.Member;
import org.eclipse.daanse.rolap.mapping.model.MemberProperty;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.PhysicalTable;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.Table;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.TimeDimension;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class ExplicitRecognizerTestModifierEmf implements CatalogMappingSupplier {

    protected final Catalog catalog;
    protected final EcoreUtil.Copier copier;

    // Measures (as instance variables to reference in resolveMeasure)
    protected SumMeasure unitSales;
    protected AvgMeasure avgUnitSales;
    protected SumMeasure storeCost;
    protected CountMeasure customerCount;

    public ExplicitRecognizerTestModifierEmf(Catalog catalogMapping, EcoreUtil.Copier copier) {
        this.copier = copier;
        this.catalog = RolapMappingFactory.eINSTANCE.createCatalog();
        catalog.getDbschemas().addAll((Collection<? extends DatabaseSchema>) catalogMapping.getDbschemas());
        createCatalog();
    }

    /*
                + "<Schema name=\"FoodMart\">\n"
            + "  <Dimension name=\"Store\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
            + "      <Table name=\"store\"/>\n"
            + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store State\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\">\n"
            + "        <Property name=\"Street address\" column=\"store_street_address\" type=\"String\"/>\n"
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Product\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
            + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
            + "        <Table name=\"product\"/>\n"
            + "        <Table name=\"product_class\"/>\n"
            + "      </Join>\n"
            + "      <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
            + "          uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
            + "          uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
            + "          uniqueMembers=\"false\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "<Cube name=\"ExtraCol\" defaultMeasure='#DEFMEASURE#'>\n"
            + "  <Table name=\"sales_fact_1997\">\n"
            + "           #AGGNAME# "
            + "  </Table>"
            + "  <Dimension name=\"TimeExtra\" foreignKey=\"time_id\">\n"
            + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
            + "      <Table name=\"time_by_day\"/>\n"
            + "      <Level name=\"Year\" #YEARCOLS#  type=\"Numeric\" uniqueMembers=\"true\""
            + "          levelType=\"TimeYears\">\n"
            + "      </Level>\n"
            + "      <Level name=\"Quarter\" #QTRCOLS#  uniqueMembers=\"false\""
            + "          levelType=\"TimeQuarters\">\n"
            + "      </Level>\n"
            + "      <Level name=\"Month\" #MONTHCOLS# uniqueMembers=\"false\" type=\"Numeric\""
            + "          levelType=\"TimeMonths\">\n"
            + "           #MONTHPROP# "
            + "      </Level>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
            + "    <Table name=\"customer\"/>\n"
            + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>  "
            + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>"
            + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>"
            + "<Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "<Measure name=\"Avg Unit Sales\" column=\"unit_sales\" aggregator=\"avg\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "<Measure name=\"Customer Count\" column=\"customer_id\" aggregator=\"distinct-count\" formatString=\"#,###\"/>"
            + "</Cube>\n"
            + "</Schema>";
     */

    protected void createCatalog() {
        // Add custom tables to database schema if any
        if (catalog.getDbschemas().size() > 0) {
            DatabaseSchema dbSchema = catalog.getDbschemas().get(0);
            List<PhysicalTable> customTables = getDatabaseSchemaTables();
            dbSchema.getTables().addAll(customTables);
        }

        // Create shared Store dimension
        StandardDimension storeDimension = createStoreDimension();

        // Create shared Product dimension
        StandardDimension productDimension = createProductDimension();

        // Create ExtraCol cube
        PhysicalCube extraColCube = createExtraColCube(storeDimension, productDimension);

        // Clear existing cubes and add the new one
        catalog.setName("FoodMart");
        catalog.getCubes().clear();
        catalog.getCubes().add(extraColCube);
    }

    protected StandardDimension createStoreDimension() {
        StandardDimension storeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        storeDimension.setName("Store");

        ExplicitHierarchy storeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        storeHierarchy.setHasAll(true);
        storeHierarchy.setPrimaryKey((Column) copier.get(CatalogSupplier.COLUMN_STORE_ID_STORE));

        TableQuery storeQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        storeQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_STORE));
        storeHierarchy.setQuery(storeQuery);

        // Store Country level
        Level storeCountryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeCountryLevel.setName("Store Country");
        storeCountryLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_COUNTRY_STORE));
        storeCountryLevel.setUniqueMembers(true);

        // Store State level
        Level storeStateLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeStateLevel.setName("Store State");
        storeStateLevel.setColumn(CatalogSupplier.COLUMN_STORE_STATE_STORE);
        storeStateLevel.setUniqueMembers(true);

        // Store City level
        Level storeCityLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeCityLevel.setName("Store City");
        storeCityLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_CITY_STORE));
        storeCityLevel.setUniqueMembers(false);

        // Store Name level with property
        Level storeNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
        storeNameLevel.setName("Store Name");
        storeNameLevel.setColumn(CatalogSupplier.COLUMN_STORE_NAME_STORE);
        storeNameLevel.setUniqueMembers(true);

        // Street address property
        MemberProperty streetAddressProp = RolapMappingFactory.eINSTANCE.createMemberProperty();
        streetAddressProp.setName("Street address");
        streetAddressProp.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STREET_ADDRESS_STORE));
        streetAddressProp.setPropertyType(ColumnInternalDataType.STRING);

        storeNameLevel.getMemberProperties().add(streetAddressProp);

        // Add levels to hierarchy
        storeHierarchy.getLevels().add(storeCountryLevel);
        storeHierarchy.getLevels().add(storeStateLevel);
        storeHierarchy.getLevels().add(storeCityLevel);
        storeHierarchy.getLevels().add(storeNameLevel);

        storeDimension.getHierarchies().add(storeHierarchy);

        return storeDimension;
    }

    protected StandardDimension createProductDimension() {
        StandardDimension productDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        productDimension.setName("Product");

        ExplicitHierarchy productHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        productHierarchy.setHasAll(true);
        productHierarchy.setPrimaryKey((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_ID_PRODUCT));

        // Create join: product JOIN product_class
        TableQuery productQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_PRODUCT));

        JoinedQueryElement productElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        productElement.setKey((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_CLASS_ID_PRODUCT));
        productElement.setQuery(productQuery);

        TableQuery productClassQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        productClassQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_PRODUCT_CLASS));

        JoinedQueryElement productClassElement = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
        productClassElement.setKey((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_CLASS_ID_PRODUCT_CLASS));
        productClassElement.setQuery(productClassQuery);

        JoinQuery joinQuery = RolapMappingFactory.eINSTANCE.createJoinQuery();
        joinQuery.setLeft(productElement);
        joinQuery.setRight(productClassElement);

        productHierarchy.setQuery(joinQuery);

        // Product Family level
        Level productFamilyLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productFamilyLevel.setName("Product Family");
        productFamilyLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_FAMILY_PRODUCT_CLASS));
        productFamilyLevel.setUniqueMembers(true);

        // Product Department level
        Level productDepartmentLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productDepartmentLevel.setName("Product Department");
        productDepartmentLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_DEPARTMENT_PRODUCT_CLASS));
        productDepartmentLevel.setUniqueMembers(false);

        // Product Category level
        Level productCategoryLevel = RolapMappingFactory.eINSTANCE.createLevel();
        productCategoryLevel.setName("Product Category");
        productCategoryLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_CATEGORY_PRODUCT_CLASS));
        productCategoryLevel.setUniqueMembers(false);

        // Add levels to hierarchy
        productHierarchy.getLevels().add(productFamilyLevel);
        productHierarchy.getLevels().add(productDepartmentLevel);
        productHierarchy.getLevels().add(productCategoryLevel);

        productDimension.getHierarchies().add(productHierarchy);

        return productDimension;
    }

    protected PhysicalCube createExtraColCube(StandardDimension storeDimension, StandardDimension productDimension) {
        PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
        cube.setName("ExtraCol");

        // Set default measure if provided
        String defaultMeasureName = getDefaultMeasure();
        if (defaultMeasureName != null) {
            Member defaultMeasure = resolveMeasure(defaultMeasureName);
            if (defaultMeasure != null) {
                cube.setDefaultMeasure(defaultMeasure);
            }
        }

        // Create table query with aggregations
        TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        tableQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_SALES_FACT));
        tableQuery.getAggregationExcludes().addAll(getAggExcludes());
        tableQuery.getAggregationTables().addAll(getAggTables());
        cube.setQuery(tableQuery);

        // Create TimeExtra dimension
        DimensionConnector timeExtraConnector = createTimeExtraDimension();
        cube.getDimensionConnectors().add(timeExtraConnector);

        // Create Gender dimension
        DimensionConnector genderConnector = createGenderDimension();
        cube.getDimensionConnectors().add(genderConnector);

        // Add Store dimension usage
        DimensionConnector storeConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        storeConnector.setOverrideDimensionName("Store");
        storeConnector.setDimension(storeDimension);
        storeConnector.setForeignKey((Column) copier.get(CatalogSupplier.COLUMN_STORE_ID_SALESFACT));
        cube.getDimensionConnectors().add(storeConnector);

        // Add Product dimension usage
        DimensionConnector productConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        productConnector.setOverrideDimensionName("Product");
        productConnector.setDimension(productDimension);
        productConnector.setForeignKey((Column) copier.get(CatalogSupplier.COLUMN_PRODUCT_ID_SALESFACT));
        cube.getDimensionConnectors().add(productConnector);

        // Create measures
        createMeasures();

        MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
        measureGroup.getMeasures().add(unitSales);
        measureGroup.getMeasures().add(avgUnitSales);
        measureGroup.getMeasures().add(storeCost);
        measureGroup.getMeasures().add(customerCount);

        cube.getMeasureGroups().add(measureGroup);

        return cube;
    }

    protected void createMeasures() {
        // Unit Sales measure
        unitSales = RolapMappingFactory.eINSTANCE.createSumMeasure();
        unitSales.setName("Unit Sales");
        unitSales.setColumn((Column) copier.get(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT));
        unitSales.setFormatString("Standard");
        unitSales.setVisible(false);

        // Avg Unit Sales measure
        avgUnitSales = RolapMappingFactory.eINSTANCE.createAvgMeasure();
        avgUnitSales.setName("Avg Unit Sales");
        avgUnitSales.setColumn((Column) copier.get(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT));
        avgUnitSales.setFormatString("Standard");
        avgUnitSales.setVisible(false);

        // Store Cost measure
        storeCost = RolapMappingFactory.eINSTANCE.createSumMeasure();
        storeCost.setName("Store Cost");
        storeCost.setColumn((CatalogSupplier.COLUMN_STORE_COST_SALESFACT));
        storeCost.setFormatString("#,###.00");

        // Customer Count measure
        customerCount = RolapMappingFactory.eINSTANCE.createCountMeasure();
        customerCount.setName("Customer Count");
        customerCount.setColumn((CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT));
        customerCount.setDistinct(true);
        customerCount.setFormatString("#,###");
    }

    protected DimensionConnector createTimeExtraDimension() {
        DimensionConnector connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        connector.setOverrideDimensionName("TimeExtra");
        connector.setForeignKey((CatalogSupplier.COLUMN_TIME_ID_SALESFACT));

        TimeDimension timeDimension = RolapMappingFactory.eINSTANCE.createTimeDimension();
        timeDimension.setName("TimeExtra");

        ExplicitHierarchy timeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        timeHierarchy.setHasAll(false);
        timeHierarchy.setPrimaryKey((Column) copier.get(CatalogSupplier.COLUMN_TIME_ID_TIME_BY_DAY));

        TableQuery timeQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        timeQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_TIME_BY_DAY));
        timeHierarchy.setQuery(timeQuery);

        // Year level
        Level yearLevel = RolapMappingFactory.eINSTANCE.createLevel();
        yearLevel.setName("Year");
        PhysicalColumn yearCol = (PhysicalColumn) getYearCol();
        if (yearCol != null) {
            yearLevel.setColumn(yearCol);
        }
        yearLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        yearLevel.setUniqueMembers(true);
        yearLevel.setType(LevelDefinition.TIME_YEARS);

        // Quarter level
        Level quarterLevel = RolapMappingFactory.eINSTANCE.createLevel();
        quarterLevel.setName("Quarter");
        PhysicalColumn quarterCol = (PhysicalColumn) getQuarterCol();
        if (quarterCol != null) {
            quarterLevel.setColumn(quarterCol);
        }
        quarterLevel.setUniqueMembers(false);
        quarterLevel.setType(LevelDefinition.TIME_QUARTERS);

        // Month level
        Level monthLevel = RolapMappingFactory.eINSTANCE.createLevel();
        monthLevel.setName("Month");
        PhysicalColumn monthCol = (PhysicalColumn) getMonthCol();
        if (monthCol != null) {
            monthLevel.setColumn(monthCol);
        }
        PhysicalColumn monthCaptionCol = (PhysicalColumn) getMonthCaptionCol();
        if (monthCaptionCol != null) {
            monthLevel.setCaptionColumn(monthCaptionCol);
        }
        PhysicalColumn monthOrdinalCol = (PhysicalColumn) getMonthOrdinalCol();
        if (monthOrdinalCol != null) {
            monthLevel.setOrdinalColumn(monthOrdinalCol);
        }
        PhysicalColumn monthNameCol = (PhysicalColumn) getMonthNameCol();
        if (monthNameCol != null) {
            monthLevel.setNameColumn(monthNameCol);
        }
        monthLevel.setUniqueMembers(false);
        monthLevel.setColumnType(ColumnInternalDataType.NUMERIC);
        monthLevel.setType(LevelDefinition.TIME_MONTHS);
        monthLevel.getMemberProperties().addAll(getMonthProp());

        // Add levels to hierarchy
        timeHierarchy.getLevels().add(yearLevel);
        timeHierarchy.getLevels().add(quarterLevel);
        timeHierarchy.getLevels().add(monthLevel);

        timeDimension.getHierarchies().add(timeHierarchy);
        connector.setDimension(timeDimension);

        return connector;
    }

    protected DimensionConnector createGenderDimension() {
        DimensionConnector connector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        connector.setOverrideDimensionName("Gender");
        connector.setForeignKey((Column) copier.get(CatalogSupplier.COLUMN_CUSTOMER_ID_SALESFACT));

        StandardDimension genderDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
        genderDimension.setName("Gender");

        ExplicitHierarchy genderHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        genderHierarchy.setHasAll(true);
        genderHierarchy.setPrimaryKey((Column) copier.get(CatalogSupplier.COLUMN_CUSTOMER_ID_CUSTOMER));

        TableQuery customerQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
        customerQuery.setTable(CatalogSupplier.TABLE_CUSTOMER);
        genderHierarchy.setQuery(customerQuery);

        // Gender level
        Level genderLevel = RolapMappingFactory.eINSTANCE.createLevel();
        genderLevel.setName("Gender");
        genderLevel.setColumn((Column) copier.get(CatalogSupplier.COLUMN_GENDER_CUSTOMER));
        genderLevel.setUniqueMembers(true);

        genderHierarchy.getLevels().add(genderLevel);
        genderDimension.getHierarchies().add(genderHierarchy);
        connector.setDimension(genderDimension);

        return connector;
    }

    protected Member resolveMeasure(String defaultMeasure) {
        switch (defaultMeasure) {
            case "Unit Sales":
                return unitSales;
            case "Avg Unit Sales":
                return avgUnitSales;
            case "Store Cost":
                return storeCost;
            case "Customer Count":
                return customerCount;
            default:
                return null;
        }
    }

    // Methods to be overridden by subclasses for customization
    protected List<MemberProperty> getMonthProp() {
        return List.of();
    }

    protected Column getMonthOrdinalCol() {
        return null;
    }

    protected Column getMonthCaptionCol() {
        return null;
    }

    protected Column getQuarterCol() {
        return null;
    }

    protected Column getMonthNameCol() {
        return null;
    }

    protected Column getMonthCol() {
        return null;
    }

    protected Column getYearCol() {
        return null;
    }

    protected List<AggregationTable> getAggTables() {
        return List.of();
    }

    protected List<AggregationExclude> getAggExcludes() {
        return List.of();
    }

    protected String getDefaultMeasure() {
        return null;
    }

    protected List<PhysicalTable> getDatabaseSchemaTables() {
        return List.of();
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
