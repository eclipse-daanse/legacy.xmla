/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2018 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.rolap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import mondrian.olap.Property;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import mondrian.olap.MondrianDef;
import mondrian.recorder.MessageRecorder;
import mondrian.rolap.aggmatcher.AggStar;
import mondrian.rolap.aggmatcher.JdbcSchema;
import mondrian.rolap.sql.SqlQuery;
import mondrian.rolap.sql.TupleConstraint;

/**
 * Created by Dmitriy Stepanov on 20.01.18.
 */
public class SqlTupleReaderTest {


  @Test
  public void testAddLevelMemberSql() throws Exception {
    TupleConstraint constraint = mock( TupleConstraint.class );
    SqlQuery sqlQuery = mock( SqlQuery.class, Answers.RETURNS_MOCKS );
    RolapCube baseCube = mock( RolapCube.class );
    RolapLevel targetLevel = mock( RolapLevel.class );
    MondrianDef.Expression expression =  mock(MondrianDef.Expression.class);
    RolapCubeLevel levelIter = mock( RolapCubeLevel.class, Answers.RETURNS_MOCKS );
    RolapProperty rolapProperty = mock( TestPublicRolapProperty.class, Answers.RETURNS_MOCKS );
    String propertyName = "property_1";
    when( rolapProperty.getName() ).thenReturn( propertyName );
    when( rolapProperty.getType() ).thenReturn(Property.Datatype.TYPE_STRING);
    when(rolapProperty.getExp()).thenReturn(expression);
    RolapProperty[] properties = { rolapProperty };
    when( levelIter.getProperties() ).thenReturn( properties );
    when( levelIter.getKeyExp() ).thenReturn( mock( MondrianDef.Expression.class ) );
    when( levelIter.getOrdinalExp() ).thenReturn( mock( MondrianDef.Expression.class ) );
    when( levelIter.getParentExp() ).thenReturn( null );
    RolapHierarchy hierarchy = mock( RolapHierarchy.class, Answers.RETURNS_MOCKS );
    when( targetLevel.getHierarchy() ).thenReturn( hierarchy );
    when( hierarchy.getLevels() ).thenReturn( new RolapLevel[] { levelIter } );
    SqlTupleReader.WhichSelect whichSelect = SqlTupleReader.WhichSelect.LAST;
    JdbcSchema.Table dbTable = mock( JdbcSchema.Table.class, Answers.RETURNS_MOCKS );
    when( dbTable.getColumnUsages( any() ) ).thenReturn( mock( Iterator.class ) );
    RolapStar star = mock( RolapStar.class );
    when( star.getColumnCount() ).thenReturn( 1 );
    AggStar aggStar = spy( AggStar.makeAggStar( star, dbTable, mock( MessageRecorder.class ), 10 ) );
    AggStar.Table.Column column = mock( AggStar.Table.Column.class, Answers.RETURNS_MOCKS );
    doReturn( column ).when( aggStar ).lookupColumn( 0 );
    RolapStar.Column starColumn = mock( RolapStar.Column.class, Answers.RETURNS_MOCKS );
    when( starColumn.getBitPosition() ).thenReturn( 0 );
    doReturn( starColumn ).when( levelIter ).getStarKeyColumn();
    AggStar.FactTable factTable =
      (AggStar.FactTable) createInstance( "mondrian.rolap.aggmatcher.AggStar$FactTable",
        new Class[] { mondrian.rolap.aggmatcher.AggStar.class, JdbcSchema.Table.class },
        new Object[] { aggStar, dbTable }, AggStar.FactTable.class.getClassLoader() );
    factTable = spy( factTable );
    Map<String, MondrianDef.Expression> propertiesAgg = new HashMap<>();
    propertiesAgg.put( propertyName, mock( MondrianDef.Expression.class ) );
    Class[] constructorArgsClasses =
      { mondrian.rolap.aggmatcher.AggStar.Table.class, String.class, MondrianDef.Expression.class, int.class,
        RolapStar.Column.class, boolean.class,
        MondrianDef.Expression.class, MondrianDef.Expression.class, Map.class };
    Object[] constructorArgs =
      { factTable, "name", mock( MondrianDef.Expression.class ), 0, starColumn, true,
        mock( MondrianDef.Expression.class ), null,
        propertiesAgg };
    AggStar.Table.Level aggStarLevel =
      (AggStar.Table.Level) createInstance( "mondrian.rolap.aggmatcher.AggStar$Table$Level", constructorArgsClasses,
        constructorArgs, AggStar.Table.Level.class.getClassLoader() );
    when( aggStar.lookupLevel( 0 ) ).thenReturn( aggStarLevel );
    doReturn( factTable ).when( column ).getTable();
    SqlTupleReader reader = new SqlTupleReader( constraint );
    reader.addLevelMemberSql( sqlQuery, targetLevel, baseCube, whichSelect, aggStar );
    verify( factTable ).addToFrom( any(), eq( false ), eq( true ) );
  }

  private Object createInstance( String className, Class[] constructorArgsClasses, Object[] constructorArgs,
                                 ClassLoader classLoader )

    throws Exception {
    Class cl = classLoader.loadClass( className );
    Constructor constructor = cl.getDeclaredConstructor( constructorArgsClasses );
    constructor.setAccessible( true );
    return constructor.newInstance( constructorArgs );
  }


}
