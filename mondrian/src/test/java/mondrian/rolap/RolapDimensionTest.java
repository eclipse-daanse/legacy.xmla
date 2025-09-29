/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2015-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.eclipse.daanse.rolap.element.RolapCatalog;
import org.eclipse.daanse.rolap.element.RolapCube;
import org.eclipse.daanse.rolap.element.RolapDimension;
import org.eclipse.daanse.rolap.mapping.model.ColumnInternalDataType;
import org.eclipse.daanse.rolap.mapping.model.Dimension;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.HideMemberIf;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.LevelDefinition;
import org.eclipse.daanse.rolap.mapping.model.Query;
import org.eclipse.daanse.rolap.mapping.model.RelationalQuery;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;



class RolapDimensionTest {

  private RolapCatalog schema;
  private RolapCube cube;
  private Dimension xmlDimension;
  private DimensionConnector xmlCubeDimension;
  private ExplicitHierarchy hierarchy;


  @BeforeEach
  public void beforeEach() {

    schema = Mockito.mock(RolapCatalog.class);
    cube = Mockito.mock(RolapCube.class);
    RelationalQuery fact = Mockito.mock(RelationalQuery.class);

    Mockito.when(cube.getCatalog()).thenReturn(schema);
    Mockito.when(cube.getFact()).thenReturn(fact);

    xmlDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
    hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
    Level level = RolapMappingFactory.eINSTANCE.createLevel();
    level.setVisible(true);
    level.setUniqueMembers(true);
    level.setColumnType(ColumnInternalDataType.STRING);
    level.setHideMemberIf(HideMemberIf.NEVER);
    level.setType(LevelDefinition.REGULAR);

    xmlCubeDimension = RolapMappingFactory.eINSTANCE.createDimensionConnector();

    xmlDimension.setName("dimensionName");
    xmlDimension.setVisible(true);
    xmlDimension.getHierarchies().add(hierarchy);


    hierarchy.setVisible(true);
    hierarchy.setHasAll(false);
    hierarchy.getLevels().add(level);

  }

  @AfterEach
  public void afterEach() {
    SystemWideProperties.instance().populateInitial();
  }

  @Disabled("disabled for CI build") //disabled for CI build
  @Test
  void testHierarchyRelation() {
	  Query hierarchyTable = (Query) Mockito
            .mock(RelationalQuery.class);
    hierarchy.setQuery(hierarchyTable);

    new RolapDimension(schema, cube, xmlDimension, xmlCubeDimension);
    assertNotNull(hierarchy);
    assertEquals(hierarchyTable, hierarchy.getQuery());
  }

  /**
   * Check that hierarchy.relation is not set to cube.fact
   */
  @Disabled("disabled for CI build") //disabled for CI build
  @Test
  void testHierarchyRelationNotSet() {
    new RolapDimension(schema, cube, xmlDimension, xmlCubeDimension);

    assertNotNull(hierarchy);
    assertNull(hierarchy.getQuery());
  }

}
