/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opencube.junit5.TestUtil.assertQueryReturns;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.rolap.element.RolapCatalog;
import org.eclipse.daanse.rolap.common.RolapConnection;
import org.eclipse.daanse.rolap.element.RolapCube;
import org.eclipse.daanse.rolap.element.RolapCubeDimension;
import org.eclipse.daanse.rolap.element.RolapCubeHierarchy;
import org.eclipse.daanse.rolap.element.RolapHierarchy;
import org.eclipse.daanse.rolap.element.RolapVirtualCube;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionConnectorMapping;
import org.eclipse.daanse.rolap.mapping.api.model.QueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableQueryMapping;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalTableMappingImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class RolapCubeHierarchyTest {

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMONDRIAN2535(Context<?> context) {
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "Select\n"
        + "  [Customers].children on rows,\n"
        + "  [Gender].children on columns\n "
        + "From [Warehouse and Sales]",
        "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Gender].[Gender].[F]}\n"
        + "{[Gender].[Gender].[M]}\n"
        + "Axis #2:\n"
        + "{[Customers].[Customers].[Canada]}\n"
        + "{[Customers].[Customers].[Mexico]}\n"
        + "{[Customers].[Customers].[USA]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #1: \n"
        + "Row #1: \n"
        + "Row #2: 280,226.21\n"
        + "Row #2: 285,011.92\n");
  }

  @Test
  void testInit_NoFactCube() {
    RolapCubeDimension cubeDimension = mock(RolapCubeDimension.class);
    RolapCube cubeDimension_cube = mock(RolapVirtualCube.class);
    String cubeDimension_uniqueName = "TheDimUniqueName";
    String dimName = "DimName";
    RolapCatalog cubeDimension_schema = mock(RolapCatalog.class);
    RolapConnection cubeDimension_schema_connection =
        mock(RolapConnection.class);
    DataSource cubeDimension_schema_connection_DS = mock(DataSource.class);
    CatalogReader schemaReader = mock(CatalogReader.class);
    Context<?> context = mock(Context.class);
    doReturn(false).when(context).getConfigValue(ConfigConstants.MEMORY_MONITOR, ConfigConstants.MEMORY_MONITOR_DEFAULT_VALUE, Boolean.class);
    doReturn(context).when(schemaReader).getContext();


    DimensionConnectorMapping cubeDim = null;

    RolapHierarchy rolapHierarchy = mock(RolapHierarchy.class);
    Hierarchy rolapHierarchy_hierarchy = null;
    String rolapHierarchy_uniqueName = "TheDimUniqueName";
    List<? extends Level> rolapHierarchy_levels = new ArrayList<>();

    String subName = null;

    int ordinal = 0;

    RolapCube factCube = null;

    doReturn(cubeDimension_cube).when(cubeDimension).getCube();
    doReturn(dimName).when(cubeDimension).getName();
    doReturn(cubeDimension_schema).when(cubeDimension).getCatalog();
    doReturn(schemaReader).when(cubeDimension_schema)
    .getCatalogReaderWithDefaultRole();
    doReturn(cubeDimension_schema_connection).when(cubeDimension_schema)
      .getInternalConnection();
    doReturn(cubeDimension_schema_connection_DS)
      .when(cubeDimension_schema_connection).getDataSource();
    doReturn(cubeDimension_uniqueName).when(cubeDimension).getUniqueName();
    doReturn(rolapHierarchy_hierarchy).when(rolapHierarchy).getHierarchy();
    doReturn(rolapHierarchy_levels).when(rolapHierarchy).getLevels();
    doReturn(rolapHierarchy_uniqueName).when(rolapHierarchy).getUniqueName();

    RolapCubeHierarchy rch = new RolapCubeHierarchy(
        cubeDimension, cubeDim, rolapHierarchy, subName, ordinal, factCube);
    assertEquals(true, rch.isUsingCubeFact(), "If factCube is null");

    rch = new RolapCubeHierarchy(
        cubeDimension, cubeDim, rolapHierarchy, subName, ordinal);
    assertEquals(true, rch.isUsingCubeFact(), "If factCube is not specified");
  }

  @Test
  void testInit_FactCube_NoFactTable() {
    RolapCubeDimension cubeDimension = mock(RolapCubeDimension.class);
    RolapCube cubeDimension_cube = mock(RolapVirtualCube.class);
    String cubeDimension_uniqueName = "TheDimUniqueName";
    String dimName = "DimName";
    RolapCatalog cubeDimension_schema = mock(RolapCatalog.class);
    RolapConnection cubeDimension_schema_connection =
        mock(RolapConnection.class);
    DataSource cubeDimension_schema_connection_DS = mock(DataSource.class);
    CatalogReader schemaReader = mock(CatalogReader.class);
    Context<?> context = mock(Context.class);
    doReturn(false).when(context).getConfigValue(ConfigConstants.MEMORY_MONITOR, ConfigConstants.MEMORY_MONITOR_DEFAULT_VALUE, Boolean.class);
    doReturn(context).when(schemaReader).getContext();


    DimensionConnectorMapping cubeDim = null;

    RolapHierarchy rolapHierarchy = mock(RolapHierarchy.class);
    Hierarchy rolapHierarchy_hierarchy = null;
    String rolapHierarchy_uniqueName = "TheDimUniqueName";
    List<? extends Level> rolapHierarchy_levels = new ArrayList<>();

    String subName = null;

    int ordinal = 0;

    RolapCube factCube = mock(RolapCube.class);
    RolapCube factCube_Fact = null;

    doReturn(cubeDimension_cube).when(cubeDimension).getCube();
    doReturn(dimName).when(cubeDimension).getName();
    doReturn(cubeDimension_schema).when(cubeDimension).getCatalog();
    doReturn(cubeDimension_schema_connection).when(cubeDimension_schema)
      .getInternalConnection();
    doReturn(schemaReader).when(cubeDimension_schema)
    .getCatalogReaderWithDefaultRole();
    doReturn(cubeDimension_schema_connection_DS)
      .when(cubeDimension_schema_connection).getDataSource();
    doReturn(cubeDimension_uniqueName).when(cubeDimension).getUniqueName();
    doReturn(rolapHierarchy_hierarchy).when(rolapHierarchy).getHierarchy();
    doReturn(rolapHierarchy_levels).when(rolapHierarchy).getLevels();
    doReturn(rolapHierarchy_uniqueName).when(rolapHierarchy).getUniqueName();
    doReturn(factCube_Fact).when(factCube).getFact();

    RolapCubeHierarchy rch = new RolapCubeHierarchy(
        cubeDimension, cubeDim, rolapHierarchy, subName, ordinal, factCube);
    assertEquals(true, rch.isUsingCubeFact());
  }

  @Test
  void testInit_FactCube_FactTableDiffers() {
    RolapCubeDimension cubeDimension = mock(RolapCubeDimension.class);
    RolapCube cubeDimension_cube = mock(RolapVirtualCube.class);
    String cubeDimension_uniqueName = "TheDimUniqueName";
    RolapCatalog cubeDimension_schema = mock(RolapCatalog.class);
    RolapConnection cubeDimension_schema_connection =
        mock(RolapConnection.class);
    DataSource cubeDimension_schema_connection_DS = mock(DataSource.class);

    DimensionConnectorMapping cubeDim = null;

    RolapHierarchy rolapHierarchy = mock(RolapHierarchy.class);
    Hierarchy rolapHierarchy_hierarchy = null;
    String rolapHierarchy_uniqueName = "TheDimUniqueName";
    String dimName = "DimName";
    List<? extends Level> rolapHierarchy_levels = new ArrayList<>();
    QueryMapping rolapHierarchy_relation = mock(TableQueryMapping.class);
    CatalogReader schemaReader = mock(CatalogReader.class);
    Context<?> context = mock(Context.class);
    doReturn(false).when(context).getConfigValue(ConfigConstants.MEMORY_MONITOR, ConfigConstants.MEMORY_MONITOR_DEFAULT_VALUE, Boolean.class);
    doReturn(context).when(schemaReader).getContext();

    String subName = null;

    int ordinal = 0;

    RolapCube factCube = mock(RolapCube.class);
    QueryMapping factCube_Fact = mock(TableQueryMapping.class);
    boolean factCube_Fact_equals = false;

    // check
    assertEquals(
        factCube_Fact_equals, factCube_Fact.equals(rolapHierarchy_relation));
    assertEquals(
        factCube_Fact_equals, rolapHierarchy_relation.equals(factCube_Fact));

    doReturn(cubeDimension_cube).when(cubeDimension).getCube();
    doReturn(dimName).when(cubeDimension).getName();
    doReturn(cubeDimension_schema).when(cubeDimension).getCatalog();
    doReturn(cubeDimension_schema_connection).when(cubeDimension_schema)
      .getInternalConnection();
    doReturn(schemaReader).when(cubeDimension_schema)
    .getCatalogReaderWithDefaultRole();
    doReturn(cubeDimension_schema_connection_DS)
      .when(cubeDimension_schema_connection).getDataSource();
    doReturn(cubeDimension_uniqueName).when(cubeDimension).getUniqueName();
    doReturn(rolapHierarchy_hierarchy).when(rolapHierarchy).getHierarchy();
    doReturn(rolapHierarchy_levels).when(rolapHierarchy).getLevels();
    doReturn(rolapHierarchy_uniqueName).when(rolapHierarchy).getUniqueName();
    doReturn(rolapHierarchy_relation).when(rolapHierarchy).getRelation();
    doReturn(factCube_Fact).when(factCube).getFact();

    RolapCubeHierarchy rch = new RolapCubeHierarchy(
        cubeDimension, cubeDim, rolapHierarchy, subName, ordinal, factCube);
    assertEquals(false, rch.isUsingCubeFact());
  }

  @Test
  void testInit_FactCube_FactTableEquals() {
    RolapCubeDimension cubeDimension = mock(RolapCubeDimension.class);
    RolapCube cubeDimension_cube = mock(RolapVirtualCube.class);
    String cubeDimension_uniqueName = "TheDimUniqueName";
    String dimName = "DimName";
    RolapCatalog cubeDimension_schema = mock(RolapCatalog.class);
    RolapConnection cubeDimension_schema_connection =
        mock(RolapConnection.class);
    DataSource cubeDimension_schema_connection_DS = mock(DataSource.class);
    CatalogReader schemaReader = mock(CatalogReader.class);
    Context<?> context = mock(Context.class);
    doReturn(false).when(context).getConfigValue(ConfigConstants.MEMORY_MONITOR, ConfigConstants.MEMORY_MONITOR_DEFAULT_VALUE, Boolean.class);
    doReturn(context).when(schemaReader).getContext();

    DimensionConnectorMapping cubeDim = null;

    RolapHierarchy rolapHierarchy = mock(RolapHierarchy.class);
    Hierarchy rolapHierarchy_hierarchy = null;
    String rolapHierarchy_uniqueName = "TheDimUniqueName";
    List<? extends Level> rolapHierarchy_levels = new ArrayList<>();
    TableQueryMapping rolapHierarchy_relation = mock(TableQueryMapping.class);
    PhysicalTableMappingImpl table = mock(PhysicalTableMappingImpl.class);
    doReturn("TableName").when(table).getName();
    doReturn(table).when(rolapHierarchy_relation).getTable();
    String subName = null;

    int ordinal = 0;

    RolapCube factCube = mock(RolapCube.class);
    QueryMapping factCube_Fact = rolapHierarchy_relation;
    boolean factCube_Fact_equals = true;

    // check
    assertEquals(
        factCube_Fact_equals, factCube_Fact.equals(rolapHierarchy_relation));
    assertEquals(
        factCube_Fact_equals, rolapHierarchy_relation.equals(factCube_Fact));

    doReturn(cubeDimension_cube).when(cubeDimension).getCube();
    doReturn(dimName).when(cubeDimension).getName();
    doReturn(cubeDimension_schema).when(cubeDimension).getCatalog();
    doReturn(cubeDimension_schema_connection).when(cubeDimension_schema)
      .getInternalConnection();
    doReturn(schemaReader).when(cubeDimension_schema)
    .getCatalogReaderWithDefaultRole();
    doReturn(cubeDimension_schema_connection_DS)
      .when(cubeDimension_schema_connection).getDataSource();
    doReturn(cubeDimension_uniqueName).when(cubeDimension).getUniqueName();
    doReturn(rolapHierarchy_hierarchy).when(rolapHierarchy).getHierarchy();
    doReturn(rolapHierarchy_levels).when(rolapHierarchy).getLevels();
    doReturn(rolapHierarchy_uniqueName).when(rolapHierarchy).getUniqueName();
    doReturn(rolapHierarchy_relation).when(rolapHierarchy).getRelation();
    doReturn(factCube_Fact).when(factCube).getFact();

    RolapCubeHierarchy rch = new RolapCubeHierarchy(
        cubeDimension, cubeDim, rolapHierarchy, subName, ordinal, factCube);
    assertEquals(true, rch.isUsingCubeFact());
  }

}
