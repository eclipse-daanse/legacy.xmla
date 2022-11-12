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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import mondrian.olap.Hierarchy;
import mondrian.olap.MondrianDef;

public class RolapCubeDimensionTest {

  private RolapCubeDimension stubRolapCubeDimension(boolean virtualCube) {
    RolapCube cube = mock(RolapCube.class);
    doReturn(virtualCube).when(cube).isVirtual();

    RolapDimension rolapDim = mock(TestPublicRolapDimension.class);
    Hierarchy[] rolapDim_hierarchies = new Hierarchy[]{};
    doReturn(rolapDim_hierarchies).when(rolapDim).getHierarchies();

    MondrianDef.CubeDimension cubeDim = new MondrianDef.Dimension();
    cubeDim.caption = "StubCubeDimCaption";
    cubeDim.description = "StubCubeDimDescription";
    cubeDim.visible = true;
    String name = "StubCubeName";
    int cubeOrdinal = 0;
    List<RolapHierarchy> hierarchyList = null;
    final boolean highCardinality = false;

    return new RolapCubeDimension(
        cube,
        rolapDim,
        cubeDim,
        name,
        cubeOrdinal,
        hierarchyList,
        highCardinality);
  }

  @Test
  public void testLookupCube_null() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);

    assertEquals(null, rcd.lookupFactCube(null, null));
  }
  
  @Test
  public void testLookupCube_notVirtual() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    MondrianDef.CubeDimension cubeDim = new MondrianDef.Dimension();
    RolapSchema schema = mock(RolapSchema.class);

    assertEquals(null, rcd.lookupFactCube(cubeDim, schema));
    verify(schema, times(0)).lookupCube(anyString());
    verify(schema, times(0)).lookupCube(anyString(), anyBoolean());
  }

  @Test
  public void testLookupCube_noSuchCube() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    MondrianDef.VirtualCubeDimension cubeDim =
        new MondrianDef.VirtualCubeDimension();
    RolapSchema schema = mock(RolapSchema.class);
    final String cubeName = "TheCubeName";
    cubeDim.cubeName = cubeName;
    // explicit doReturn - just to make it evident
    doReturn(null).when(schema).lookupCube(anyString());

    assertEquals(null, rcd.lookupFactCube(cubeDim, schema));
    Mockito.verify(schema).lookupCube(cubeName);
  }

  @Test
  public void testLookupCube_found() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    MondrianDef.VirtualCubeDimension cubeDim =
        mock(MondrianDef.VirtualCubeDimension.class);
    RolapSchema schema = mock(RolapSchema.class);
    RolapCube factCube = mock(RolapCube.class);
    final String cubeName = "TheCubeName";
    cubeDim.cubeName = cubeName;
    doReturn(factCube).when(schema).lookupCube(cubeName);

    assertEquals(factCube, rcd.lookupFactCube(cubeDim, schema));
  }
}
// End RolapCubeDimensionTest.java