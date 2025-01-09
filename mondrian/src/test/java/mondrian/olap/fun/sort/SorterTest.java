/*
 *
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2020 Hitachi Vantara and others
 * All Rights Reserved.
 *
 *
 */

package mondrian.olap.fun.sort;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Comparator;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.function.def.member.memberorderkey.MemberOrderKeyCalc;
import org.eclipse.daanse.olap.rolap.core.BasicContextConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import mondrian.calc.impl.TupleCollections;
import mondrian.olap.QueryImpl;
import mondrian.rolap.RolapConnection;
import mondrian.server.ExecutionImpl;

class SorterTest{

  @Mock Evaluator evaluator;
  @Mock QueryImpl query;
  @Mock Statement statement;
  @Mock ExecutionImpl execution;
  @Mock SortKeySpec sortKeySpec1;
  @Mock SortKeySpec sortKeySpec2;
  @Mock TupleIterable tupleIterable;
  @Mock Member member1;
  @Mock Member member2;
  @Mock Hierarchy hierarchy1;
  @Mock Hierarchy hierarchy2;
  @Mock Calc calc1;
  @Mock Calc calc2;
  @Mock Comparator comparatorChain;
  @Mock Connection connection;
  @Mock Context context;
  @Mock BasicContextConfig config;
  @Mock RolapConnection rolapConnection;
  @Captor ArgumentCaptor<Comparator<?>> comparatorCaptor;






  @BeforeEach
  public void setUp() throws Exception {

    MockitoAnnotations.initMocks( this );
    when( sortKeySpec1.getKey() ).thenReturn( calc1 );
    when( sortKeySpec2.getKey() ).thenReturn( calc2 );
    when( evaluator.getQuery() ).thenReturn( query );
    when( evaluator.getQuery() ).thenReturn( query );
    when( query.getStatement() ).thenReturn( statement );
    when( statement.getCurrentExecution() ).thenReturn( execution );
    when(execution.getMondrianStatement()).thenReturn( statement );
    when( statement.getMondrianConnection() ).thenReturn( rolapConnection );
    when( rolapConnection.getContext()).thenReturn( context );
    when( context.getConfig()).thenReturn( config );
    when( config.checkCancelOrTimeoutInterval()).thenReturn( 1000 );

  }

  // tuple sort paths:
  // +--------------+---------------------+------------------------------+
  // |              |Breaking             |Non-breaking                  |
  // +--------------+---------------------+------------------------------+
  // |OrderByKey    | BreakTupleComparator|HierarchicalTupleKeyComparator|
  // +--------------+---------------------+------------------------------+
  // |Not-OrderByKey| BreakTupleComparator|HierarchicalTupleComparator   |
  // +--------------+---------------------+------------------------------+
  @Test
  void testComparatorSelectionBrkOrderByKey() {
    setupSortKeyMocks( true, Sorter.SorterFlag.BASC, Sorter.SorterFlag.BDESC );
    Sorter.applySortSpecToComparator( evaluator, 2, comparatorChain, sortKeySpec1 );
    Sorter.applySortSpecToComparator( evaluator, 2, comparatorChain, sortKeySpec2 );
    verify( comparatorChain , times( 2 )).thenComparing( comparatorCaptor.capture());
    Comparator tc0= comparatorCaptor.getAllValues().get(0);
    assertThat(tc0).isInstanceOf(TupleExpMemoComparator.BreakTupleComparator.class );
    Comparator tc1= comparatorCaptor.getAllValues().get(1);
    assertThat(tc1.reversed()).isInstanceOf(TupleExpMemoComparator.BreakTupleComparator.class );


//    verify( comparatorChain ).addComparator( any( TupleExpMemoComparator.BreakTupleComparator.class ), eq( false ) );
//    verify( comparatorChain ).addComparator( any( TupleExpMemoComparator.BreakTupleComparator.class ), eq( true ) );
  }

  @Test
  void testComparatorSelectionBrkNotOrderByKey() {
    setupSortKeyMocks( false, Sorter.SorterFlag.BASC, Sorter.SorterFlag.BDESC );
    Sorter.applySortSpecToComparator( evaluator, 2, comparatorChain, sortKeySpec1 );
    Sorter.applySortSpecToComparator( evaluator, 2, comparatorChain, sortKeySpec2 );
    verify( comparatorChain , times( 2 )).thenComparing( comparatorCaptor.capture());


    Comparator tc0= comparatorCaptor.getAllValues().get(0);
    assertThat(tc0).isInstanceOf(TupleExpMemoComparator.BreakTupleComparator.class );
    Comparator tc1= comparatorCaptor.getAllValues().get(1);
    assertThat(tc1.reversed()).isInstanceOf(TupleExpMemoComparator.BreakTupleComparator.class );


//    verify( comparatorChain ).addComparator( any( TupleExpMemoComparator.BreakTupleComparator.class ), eq( false ) );
//    verify( comparatorChain ).addComparator( any( TupleExpMemoComparator.BreakTupleComparator.class ), eq( true ) );
  }

  @Test
  void testComparatorSelectionNotBreakingOrderByKey() {
    calc1 = mock(MemberOrderKeyCalc.class);
    calc2 = mock(MemberOrderKeyCalc.class);
    when( sortKeySpec1.getKey() ).thenReturn( calc1 );
    when( sortKeySpec2.getKey() ).thenReturn( calc2 );
    setupSortKeyMocks(Sorter.SorterFlag.ASC, Sorter.SorterFlag.DESC );
    Sorter.applySortSpecToComparator( evaluator, 2, comparatorChain, sortKeySpec1 );
    Sorter.applySortSpecToComparator( evaluator, 2, comparatorChain, sortKeySpec2 );
    verify( comparatorChain , times( 2 )).thenComparing( comparatorCaptor.capture());



    Comparator tc0= comparatorCaptor.getAllValues().get(0);
    assertThat(tc0).isInstanceOf(HierarchicalTupleKeyComparator.class );
    Comparator tc1= comparatorCaptor.getAllValues().get(1);
    assertThat(tc1.reversed()).isInstanceOf(HierarchicalTupleKeyComparator.class );

//    verify( comparatorChain ).addComparator( any( HierarchicalTupleKeyComparator.class ), eq( false ) );
//    verify( comparatorChain ).addComparator( any( HierarchicalTupleKeyComparator.class ), eq( true ) );
  }

  @Test
  void testComparatorSelectionNotBreaking() {
    setupSortKeyMocks( false, Sorter.SorterFlag.ASC, Sorter.SorterFlag.DESC );
    Sorter.applySortSpecToComparator( evaluator, 2, comparatorChain, sortKeySpec1 );
    Sorter.applySortSpecToComparator( evaluator, 2, comparatorChain, sortKeySpec2 );
    verify( comparatorChain, times( 2 ) ).thenComparing( comparatorCaptor.capture() );


//    verify( comparatorChain, times( 2 ) ).addComparator( comparatorCaptor.capture(), eq( false ) );
    assertTrue( comparatorCaptor.getAllValues().get( 0 ) instanceof HierarchicalTupleComparator );
    assertTrue( comparatorCaptor.getAllValues().get( 1 ) instanceof HierarchicalTupleComparator );
  }

  @Test
  void testSortTuplesBreakingByKey() {
    TupleList tupleList = genList();
    setupSortKeyMocks( true, Sorter.SorterFlag.BASC, Sorter.SorterFlag.BDESC );

    TupleList result =
      Sorter.sortTuples( evaluator, tupleIterable, tupleList, asList( sortKeySpec1, sortKeySpec2 ), 2 );
    verifyNoInteractions( tupleIterable ); // list passed in, used instead of iterable
    verify( calc1, atLeastOnce() ).dependsOn( hierarchy1 );
    verify( calc1, atLeastOnce() ).dependsOn( hierarchy2 );
    verify( calc2, atLeastOnce() ).dependsOn( hierarchy2 );
    verify( calc2, atLeastOnce() ).dependsOn( hierarchy1 );
    assertTrue( result.size() == 1000 );
  }

  @Test
  void testCancel() {
    setupSortKeyMocks( true, Sorter.SorterFlag.ASC, Sorter.SorterFlag.DESC );
    // pass in a null tupleList, and an iterable.  cancel should be checked while generating the list
    // from the iterable
    Sorter.sortTuples( evaluator, genList(), null, asList( sortKeySpec1, sortKeySpec2 ), 2 );
    verify( execution, atLeastOnce() ).checkCancelOrTimeout();
  }


  private void setupSortKeyMocks( boolean isOrderKeyCalc, Sorter.SorterFlag flag1, Sorter.SorterFlag flag2 ) {
    when( sortKeySpec1.getDirection() ).thenReturn( flag1 );
    when( sortKeySpec2.getDirection() ).thenReturn( flag2 );
//    when( calc1.isWrapperFor( MemberOrderKeyFunDef.CalcImpl.class ) ).thenReturn( isOrderKeyCalc );
//    when( calc2.isWrapperFor( MemberOrderKeyFunDef.CalcImpl.class ) ).thenReturn( isOrderKeyCalc );
    when( calc1.evaluate( evaluator ) ).thenReturn( 1 );
    when( calc2.evaluate( evaluator ) ).thenReturn( 2 );
    when( calc1.dependsOn( hierarchy1 ) ).thenReturn( true );
    when( calc2.dependsOn( hierarchy2 ) ).thenReturn( true );
    when( member1.getHierarchy() ).thenReturn( hierarchy1 );
    when( member2.getHierarchy() ).thenReturn( hierarchy2 );
  }

  private void setupSortKeyMocks( Sorter.SorterFlag flag1, Sorter.SorterFlag flag2 ) {
      when( sortKeySpec1.getDirection() ).thenReturn( flag1 );
      when( sortKeySpec2.getDirection() ).thenReturn( flag2 );
      when( calc1.dependsOn( hierarchy1 ) ).thenReturn( true );
      when( calc2.dependsOn( hierarchy2 ) ).thenReturn( true );
      when( member1.getHierarchy() ).thenReturn( hierarchy1 );
      when( member2.getHierarchy() ).thenReturn( hierarchy2 );
    }

  private TupleList genList() {
    TupleList tupleList = TupleCollections.createList( 2 );
    range( 0, 1000 )
      .forEach( i -> tupleList.add( asList( member1, member2 ) ) );
    return tupleList;
  }


}
