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

import java.util.List;

import org.eclipse.daanse.rolap.mapping.api.model.RelationalQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.HideMemberIfType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.DataType;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.QueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import mondrian.olap.SystemWideProperties;



class RolapDimensionTest {

  private RolapCatalog schema;
  private RolapCube cube;
  private DimensionMappingImpl xmlDimension;
  private DimensionConnectorMappingImpl xmlCubeDimension;
  private HierarchyMappingImpl hierarchy;


  @BeforeEach
  public void beforeEach() {

    schema = Mockito.mock(RolapCatalog.class);
    cube = Mockito.mock(RolapCube.class);
    RelationalQueryMapping fact = Mockito.mock(RelationalQueryMapping.class);

    Mockito.when(cube.getCatalog()).thenReturn(schema);
    Mockito.when(cube.getFact()).thenReturn(fact);

    xmlDimension = StandardDimensionMappingImpl.builder().build();
    hierarchy = HierarchyMappingImpl.builder().build();
    LevelMappingImpl level = LevelMappingImpl.builder()
    	    .withVisible(true)
            .withMemberProperties(List.of())
            .withUniqueMembers(true)
            .withType(DataType.STRING)
            .withHideMemberIfType(HideMemberIfType.NEVER)
            .withLevelType(LevelType.REGULAR)
    		.build();
    xmlCubeDimension = DimensionConnectorMappingImpl.builder().build();

    xmlDimension.setName("dimensionName");
    xmlDimension.setVisible(true);
    xmlDimension.setHierarchies(List.of(hierarchy));


    hierarchy.setVisible(true);
    hierarchy.setHasAll(false);
    hierarchy.setLevels(List.of(level));

  }

  @AfterEach
  public void afterEach() {
    SystemWideProperties.instance().populateInitial();
  }

  @Disabled("disabled for CI build") //disabled for CI build
  @Test
  void testHierarchyRelation() {
	  QueryMappingImpl hierarchyTable = (QueryMappingImpl) Mockito
            .mock(RelationalQueryMapping.class);
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
