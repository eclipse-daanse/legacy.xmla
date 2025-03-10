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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.element.OlapMetaData;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RolapCubeDimensionTest {

  private RolapCubeDimension stubRolapCubeDimension(boolean virtualCube) {
    RolapCube cube = mock(RolapCube.class);
    doReturn(virtualCube).when(cube).isVirtual();

    RolapDimension rolapDim = mock(TestPublicRolapDimension.class);    
    Hierarchy[] rolapDim_hierarchies = new Hierarchy[]{};
    doReturn(rolapDim_hierarchies).when(rolapDim).getHierarchies();
    doReturn(OlapMetaData.empty()).when(rolapDim).getMetaData();
    
    StandardDimensionMappingImpl cubeDim = StandardDimensionMappingImpl.builder()
    		.withName("StubCubeDimCaption")
    		.withDescription("StubCubeDimDescription")
    		.withVisible(true)
    		.build();
    String name = "StubCubeName";
    int cubeOrdinal = 0;
    List<RolapHierarchy> hierarchyList = null;
    DimensionConnectorMappingImpl dimensionConnector = DimensionConnectorMappingImpl.builder()
    		.withDimension(cubeDim)
    		.build();
    
    return new RolapCubeDimension(
        cube,
        rolapDim,
        dimensionConnector,
        name,
        cubeOrdinal,
        hierarchyList);
  }

  @Test
  void testLookupCube_null() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    DimensionConnectorMappingImpl dimConnector = DimensionConnectorMappingImpl.builder().build();
    assertEquals(null, rcd.lookupFactCube(dimConnector, null));
  }

  @Test
  void testLookupCube_notVirtual() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    DimensionConnectorMappingImpl cubeDim = DimensionConnectorMappingImpl.builder().build();
    RolapCatalog schema = mock(RolapCatalog.class);

    assertEquals(null, rcd.lookupFactCube(cubeDim, schema));
    verify(schema, times(0)).lookupCube(anyString());
  }

  @Test
  void testLookupCube_noSuchCube() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    RolapCatalog schema = mock(RolapCatalog.class);
    final String cubeName = "TheCubeName";
    PhysicalCubeMappingImpl cube = PhysicalCubeMappingImpl.builder().withName(cubeName).build();   
    DimensionConnectorMappingImpl dimCon = DimensionConnectorMappingImpl.builder()
    		.withPhysicalCube(cube)
    		.build();
    // explicit doReturn - just to make it evident
    doReturn(null).when(schema).lookupCube(any(CubeMapping.class));

    assertEquals(null, rcd.lookupFactCube(dimCon, schema));
    Mockito.verify(schema).lookupCube(cube);
  }

  @Test
  void testLookupCube_found() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    final String cubeName = "TheCubeName";
    PhysicalCubeMappingImpl cube = PhysicalCubeMappingImpl.builder().withName(cubeName).build();
    DimensionConnectorMappingImpl cubeCon = DimensionConnectorMappingImpl.builder()
    		.withPhysicalCube(cube)
    		.build();
    RolapCatalog schema = mock(RolapCatalog.class);
    RolapCube factCube = mock(RolapCube.class);        
    doReturn(factCube).when(schema).lookupCube(cube);

    assertEquals(factCube, rcd.lookupFactCube(cubeCon, schema));
  }
}
