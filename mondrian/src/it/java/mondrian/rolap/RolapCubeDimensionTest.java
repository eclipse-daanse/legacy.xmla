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

import org.eclipse.daanse.olap.api.Hierarchy;
import mondrian.olap.MondrianDef;

import junit.framework.TestCase;

import org.mockito.Mockito;

import java.util.List;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RolapCubeDimensionTest extends TestCase {

  private RolapCubeDimension stubRolapCubeDimension(boolean virtualCube) {
    RolapCube cube = mock(RolapCube.class);
    doReturn(virtualCube).when(cube).isVirtual();

    RolapDimension rolapDim = mock(RolapDimension.class);
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

  void testLookupCube_null() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);

    assertEquals(null, rcd.lookupFactCube(null, null));
  }
  void testLookupCube_notVirtual() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    MondrianDef.CubeDimension cubeDim = new MondrianDef.Dimension();
    RolapCatalog schema = mock(RolapCatalog.class);

    assertEquals(null, rcd.lookupFactCube(cubeDim, schema));
    verify(schema, times(0)).lookupCube(anyString());
    verify(schema, times(0)).lookupCube(anyString(), anyBoolean());
  }

  void testLookupCube_noSuchCube() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    MondrianDef.VirtualCubeDimension cubeDim =
        new MondrianDef.VirtualCubeDimension();
    RolapCatalog schema = mock(RolapCatalog.class);
    final String cubeName = "TheCubeName";
    cubeDim.cubeName = cubeName;
    // explicit doReturn - just to make it evident
    doReturn(null).when(schema).lookupCube(anyString());

    assertEquals(null, rcd.lookupFactCube(cubeDim, schema));
    Mockito.verify(schema).lookupCube(cubeName);
  }

  void testLookupCube_found() {
    RolapCubeDimension rcd = stubRolapCubeDimension(false);
    MondrianDef.VirtualCubeDimension cubeDim =
        mock(MondrianDef.VirtualCubeDimension.class);
    RolapCatalog schema = mock(RolapCatalog.class);
    RolapCube factCube = mock(RolapCube.class);
    final String cubeName = "TheCubeName";
    cubeDim.cubeName = cubeName;
    doReturn(factCube).when(schema).lookupCube(cubeName);

    assertEquals(factCube, rcd.lookupFactCube(cubeDim, schema));
  }
}