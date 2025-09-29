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
package org.eclipse.daanse.olap;

import java.util.List;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.Cube;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.TimeDimension;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

/**
 * EMF version of VerifyMemberLevelNamesIdentityOlap4jDateDimModifier from HierarchyBugTest.
 * Creates a Date dimension with default hierarchy (hasAll=false) using RolapMappingFactory.
 * Uses objects from CatalogSupplier.
 *
 * <Dimension name="Date" type="TimeDimension" foreignKey="time_id">
 *     <Hierarchy hasAll="false" primaryKey="time_id">
 *       <Table name="time_by_day"/>
 *       <Level name="Year" column="the_year" type="Numeric" uniqueMembers="true"
 *           levelType="TimeYears"/>
 *       <Level name="Quarter" column="quarter" uniqueMembers="false"
 *           levelType="TimeQuarters"/>
 *       <Level name="Month" column="month_of_year" uniqueMembers="false" type="Numeric"
 *           levelType="TimeMonths"/>
 *     </Hierarchy>
 * </Dimension>
 */
public class VerifyMemberLevelNamesIdentityOlap4jDateDimModifier implements CatalogMappingSupplier {

    private final CatalogImpl catalog;

    // Static levels
    private static final Level LEVEL_YEAR;
    private static final Level LEVEL_QUARTER;
    private static final Level LEVEL_MONTH;

    // Static hierarchy
    private static final ExplicitHierarchy HIERARCHY_DATE;

    // Static dimension
    private static final TimeDimension DIMENSION_DATE;

    // Static table query
    private static final TableQuery TABLE_QUERY;

    // Static dimension connector
    private static final DimensionConnector DIMENSION_CONNECTOR_DATE;

    static {
        // Create Level definitions using columns from CatalogSupplier
        LEVEL_YEAR = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_YEAR.setName("Year");
        LEVEL_YEAR.setColumn(CatalogSupplier.COLUMN_THE_YEAR_TIME_BY_DAY);
        LEVEL_YEAR.setColumnType(ColumnInternalDataType.NUMERIC);
        LEVEL_YEAR.setUniqueMembers(true);
        LEVEL_YEAR.setType(LevelDefinition.TIME_YEARS);

        LEVEL_QUARTER = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_QUARTER.setName("Quarter");
        LEVEL_QUARTER.setColumn(CatalogSupplier.COLUMN_QUARTER_TIME_BY_DAY);
        LEVEL_QUARTER.setUniqueMembers(false);
        LEVEL_QUARTER.setType(LevelDefinition.TIME_QUARTERS);

        LEVEL_MONTH = RolapMappingFactory.eINSTANCE.createLevel();
        LEVEL_MONTH.setName("Month");
        LEVEL_MONTH.setColumn(CatalogSupplier.COLUMN_MONTH_OF_YEAR_TIME_BY_DAY);
        LEVEL_MONTH.setUniqueMembers(false);
        LEVEL_MONTH.setColumnType(ColumnInternalDataType.NUMERIC);
        LEVEL_MONTH.setType(LevelDefinition.TIME_MONTHS);

        // Create table query using table from CatalogSupplier
        TABLE_QUERY = RolapMappingFactory.eINSTANCE.createTableQuery();
        TABLE_QUERY.setTable(CatalogSupplier.TABLE_TIME_BY_DAY);

        // Create Date hierarchy with hasAll=false
        HIERARCHY_DATE = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
        HIERARCHY_DATE.setHasAll(false);
        HIERARCHY_DATE.setPrimaryKey(CatalogSupplier.COLUMN_TIME_ID_TIME_BY_DAY);
        HIERARCHY_DATE.setQuery(TABLE_QUERY);
        HIERARCHY_DATE.getLevels().addAll(List.of(LEVEL_YEAR, LEVEL_QUARTER, LEVEL_MONTH));

        // Create Time Dimension
        DIMENSION_DATE = RolapMappingFactory.eINSTANCE.createTimeDimension();
        DIMENSION_DATE.setName("Date");
        DIMENSION_DATE.getHierarchies().add(HIERARCHY_DATE);

        // Create Dimension Connector using foreign key from CatalogSupplier
        DIMENSION_CONNECTOR_DATE = RolapMappingFactory.eINSTANCE.createDimensionConnector();
        DIMENSION_CONNECTOR_DATE.setOverrideDimensionName("Date");
        DIMENSION_CONNECTOR_DATE.setForeignKey(CatalogSupplier.COLUMN_TIME_ID_SALESFACT);
        DIMENSION_CONNECTOR_DATE.setDimension(DIMENSION_DATE);
    }

    public VerifyMemberLevelNamesIdentityOlap4jDateDimModifier(Catalog baseCatalog) {
        // Copy the base catalog using EcoreUtil
        this.catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) baseCatalog);

        // Find and modify the Sales cube
        for (Cube cube : this.catalog.getCubes()) {
            if ("Sales".equals(cube.getName()) && cube instanceof PhysicalCube) {
                PhysicalCube physicalCube = (PhysicalCube) cube;
                // Add the Date dimension connector to the Sales cube
                physicalCube.getDimensionConnectors().add(DIMENSION_CONNECTOR_DATE);
            }
        }
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
