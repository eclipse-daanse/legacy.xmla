/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2021 Hitachi Vantara
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.calc.impl.CacheCalc;
import mondrian.olap.ExpCacheDescriptorImpl;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.olap.type.TupleType;
import mondrian.rolap.RolapUtil;

/**
 * Definition of the <code>RANK</code> MDX function.
 *
 * @author Richard Emberson
 * @since 17 January, 2005
 */
public class RankFunDef extends AbstractFunctionDefinition {
  private static final Logger LOGGER = LoggerFactory.getLogger(RankFunDef.class);
  static final boolean DEBUG = false;
  static final ReflectiveMultiResolver Resolver =
      new ReflectiveMultiResolver( "Rank", "Rank(<Tuple>, <Set> [, <Calc Expression>])",
          "Returns the one-based rank of a tuple in a set.", new String[] { "fitx", "fitxn", "fimx", "fimxn" },
          RankFunDef.class );
  private static final String TIMING_NAME = RankFunDef.class.getSimpleName();

  public RankFunDef( FunctionMetaData functionMetaData ) {
    super( functionMetaData );
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    switch ( call.getArgCount() ) {
      case 2:
        return compileCall2( call, compiler );
      case 3:
        return compileCall3( call, compiler );
      default:
        throw Util.newInternal( "invalid arg count " + call.getArgCount() );
    }
  }

  public Calc compileCall3( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final Type type0 = call.getArg( 0 ).getType();
    final TupleListCalc tupleListCalc = compiler.compileList( call.getArg( 1 ) );
    final Calc keyCalc = compiler.compileScalar( call.getArg( 2 ), true );
    Calc sortedListCalc = new SortedListCalc( call.getType(), tupleListCalc, keyCalc );
    final ExpCacheDescriptorImpl cacheDescriptor = new ExpCacheDescriptorImpl( call, sortedListCalc, compiler.getEvaluator() );
    if ( type0 instanceof TupleType ) {
      final TupleCalc tupleCalc = compiler.compileTuple( call.getArg( 0 ) );
      return new Rank3TupleCalc( call, tupleCalc, keyCalc, cacheDescriptor );
    } else {
      final MemberCalc memberCalc = compiler.compileMember( call.getArg( 0 ) );
      return new Rank3MemberCalc( call, memberCalc, keyCalc, cacheDescriptor );
    }
  }

  public Calc compileCall2( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final boolean tuple = call.getArg( 0 ).getType() instanceof TupleType;
    final Expression listExp = call.getArg( 1 );
    final TupleListCalc listCalc0 = compiler.compileList( listExp );
    Calc listCalc1 = new RankedListCalc( listCalc0, tuple );
    final Calc listCalc;
    if ( SystemWideProperties.instance().EnableExpCache ) {
      final ExpCacheDescriptorImpl key = new ExpCacheDescriptorImpl( listExp, listCalc1, compiler.getEvaluator() );
      listCalc = new CacheCalc( listExp.getType(), key );
    } else {
      listCalc = listCalc1;
    }
    if ( tuple ) {
      final TupleCalc tupleCalc = compiler.compileTuple( call.getArg( 0 ) );
      return new Rank2TupleCalc( call, tupleCalc, listCalc );
    } else {
      final MemberCalc memberCalc = compiler.compileMember( call.getArg( 0 ) );
      return new Rank2MemberCalc( call, memberCalc, listCalc );
    }
  }

  private static class Rank2TupleCalc extends AbstractProfilingNestedIntegerCalc {
    private final TupleCalc tupleCalc;
    private final Calc listCalc;

    public Rank2TupleCalc( ResolvedFunCall call, TupleCalc tupleCalc, Calc listCalc ) {
      super( call.getType(), new Calc[] { tupleCalc, listCalc } );
      this.tupleCalc = tupleCalc;
      this.listCalc = listCalc;
    }

    @Override
	public Integer evaluate( Evaluator evaluator ) {
      evaluator.getTiming().markStart( RankFunDef.TIMING_NAME );
      try {
        // Get member or tuple.
        // If the member is null (or the tuple contains a null member)
        // the result is null (even if the list is null).
        final Member[] members = tupleCalc.evaluate( evaluator );
        if ( members == null ) {
          return null;
        }
        assert !FunUtil.tupleContainsNullMember( members );

        // Get the set of members/tuples.
        // If the list is empty, MSAS cannot figure out the type of the
        // list, so returns an error "Formula error - dimension count is
        // not valid - in the Rank function". We will naturally return 0,
        // which I think is better.
        final RankedTupleList rankedTupleList = (RankedTupleList) listCalc.evaluate( evaluator );
        if ( rankedTupleList == null ) {
          return 0;
        }

        // Find position of member in list. -1 signifies not found.
        final List<Member> memberList = Arrays.asList( members );
        final int i = rankedTupleList.indexOf( memberList );
        // Return 1-based rank. 0 signifies not found.
        return i + 1;
      } finally {
        evaluator.getTiming().markEnd( RankFunDef.TIMING_NAME );
      }
    }
  }

  private static class Rank2MemberCalc extends AbstractProfilingNestedIntegerCalc {
    private final MemberCalc memberCalc;
    private final Calc listCalc;

    public Rank2MemberCalc( ResolvedFunCall call, MemberCalc memberCalc, Calc listCalc ) {
      super( call.getType(), new Calc[] { memberCalc, listCalc } );
      this.memberCalc = memberCalc;
      this.listCalc = listCalc;
    }

    @Override
	public Integer evaluate( Evaluator evaluator ) {
      evaluator.getTiming().markStart( RankFunDef.TIMING_NAME );
      try {

        // Get member or tuple.
        // If the member is null (or the tuple contains a null member)
        // the result is null (even if the list is null).
        final Member member = memberCalc.evaluate( evaluator );
        if ( member == null || member.isNull() ) {
          return null;
        }
        // Get the set of members/tuples.
        // If the list is empty, MSAS cannot figure out the type of the
        // list, so returns an error "Formula error - dimension count is
        // not valid - in the Rank function". We will naturally return 0,
        // which I think is better.
        RankedMemberList rankedMemberList = (RankedMemberList) listCalc.evaluate( evaluator );
        if ( rankedMemberList == null ) {
          return 0;
        }

        // Find position of member in list. -1 signifies not found.
        final int i = rankedMemberList.indexOf( member );
        // Return 1-based rank. 0 signifies not found.
        return i + 1;
      } finally {
        evaluator.getTiming().markEnd( RankFunDef.TIMING_NAME );
      }
    }
  }

  private static class Rank3TupleCalc extends AbstractProfilingNestedIntegerCalc {
    private final TupleCalc tupleCalc;
    private final Calc sortCalc;
    private final ExpCacheDescriptor cacheDescriptor;

    public Rank3TupleCalc( ResolvedFunCall call, TupleCalc tupleCalc, Calc sortCalc,
        ExpCacheDescriptor cacheDescriptor ) {
      super( call.getType(), new Calc[] { tupleCalc, sortCalc } );
      this.tupleCalc = tupleCalc;
      this.sortCalc = sortCalc;
      this.cacheDescriptor = cacheDescriptor;
    }

    @Override
	public Integer evaluate( Evaluator evaluator ) {
      evaluator.getTiming().markStart( RankFunDef.TIMING_NAME );
      try {
        Member[] members = tupleCalc.evaluate( evaluator );
        if ( members == null ) {
          return null;
        }
        assert !FunUtil.tupleContainsNullMember( members );

        // Evaluate the list (or retrieve from cache).
        // If there is an exception while calculating the
        // list, propagate it up.
        final SortResult sortResult = (SortResult) evaluator.getCachedResult( cacheDescriptor );
        if ( RankFunDef.DEBUG) {
          sortResult.log();
        }

        if ( sortResult.isEmpty() ) {
          // If list is empty, the rank is null.
          return null;
        }

        // First try to find the member in the cached SortResult
        Integer rank = null;
        if (sortResult instanceof TupleSortResult tupleSortResult) {
          rank = tupleSortResult.rankOf(members);
        }
        if (sortResult instanceof MemberSortResult memberSortResult && members.length > 0) {
            rank = memberSortResult.rankOf(members[0]);
        }
        if ( rank != null ) {
          return rank;
        }

        // member is not seen before, now compute the value of the tuple.
        final int savepoint = evaluator.savepoint();
        Object value;
        try {
          evaluator.setContext( members );
          value = sortCalc.evaluate( evaluator );
        } finally {
          evaluator.restore( savepoint );
        }

        if ( RankFunDef.valueNotReady( value ) ) {
          // The value wasn't ready, so quit now... we'll be back.
          return 0;
        }

        // If value is null, it won't be in the values array.
        if ( value == Util.nullValue || value == null ) {
          return sortResult.values.length + 1;
        }

        value = RankFunDef.coerceValue( sortResult.values, value );

        // Look for the ranked value in the array.
        int j = Arrays.binarySearch( sortResult.values, value, Collections.<Object>reverseOrder() );
        if ( j < 0 ) {
          // Value not found. Flip the result to find the
          // insertion point.
          j = -( j + 1 );
        }
        return j + 1; // 1-based
      } finally {
        evaluator.getTiming().markEnd( RankFunDef.TIMING_NAME );
      }
    }
  }

  private static class Rank3MemberCalc extends AbstractProfilingNestedIntegerCalc {
    private final MemberCalc memberCalc;
    private final Calc sortCalc;
    private final ExpCacheDescriptor cacheDescriptor;

    public Rank3MemberCalc( ResolvedFunCall call, MemberCalc memberCalc, Calc sortCalc,
        ExpCacheDescriptor cacheDescriptor ) {
      super( call.getType(), new Calc[] { memberCalc, sortCalc } );
      this.memberCalc = memberCalc;
      this.sortCalc = sortCalc;
      this.cacheDescriptor = cacheDescriptor;
    }

    @Override
	public Integer evaluate( Evaluator evaluator ) {
      evaluator.getTiming().markStart( RankFunDef.TIMING_NAME );
      try {
        Member member = memberCalc.evaluate( evaluator );
        if ( member == null || member.isNull() ) {
          return null;
        }

        // Evaluate the list (or retrieve from cache).
        // If there was an exception while calculating the
        // list, propagate it up.
        final MemberSortResult sortResult = (MemberSortResult) evaluator.getCachedResult( cacheDescriptor );
        if ( RankFunDef.DEBUG) {
          sortResult.log();
        }
        if ( sortResult.isEmpty() ) {
          // If list is empty, the rank is null.
          return null;
        }

        // First try to find the member in the cached SortResult
        Integer rank = sortResult.rankOf( member );
        if ( rank != null ) {
          return rank;
        }

        // member is not seen before, now compute the value of the tuple.
        final int savepoint = evaluator.savepoint();
        evaluator.setContext( member );
        Object value;
        try {
          value = sortCalc.evaluate( evaluator );
        } finally {
          evaluator.restore( savepoint );
        }

        if ( RankFunDef.valueNotReady( value ) ) {
          // The value wasn't ready, so quit now... we'll be back.
          return 0;
        }

        // If value is null, it won't be in the values array.
        if ( value == Util.nullValue || value == null ) {
          return sortResult.values.length + 1;
        }

        value = RankFunDef.coerceValue( sortResult.values, value );

        // Look for the ranked value in the array.
        int j = Arrays.binarySearch( sortResult.values, value, Collections.<Object>reverseOrder() );
        if ( j < 0 ) {
          // Value not found. Flip the result to find the
          // insertion point.
          j = -( j + 1 );
        }
        return j + 1; // 1-based
      } finally {
        evaluator.getTiming().markEnd( RankFunDef.TIMING_NAME );
      }
    }
  }

  private static Object coerceValue( Object[] values, Object value ) {
    if ( values.length > 0 ) {
      final Object firstValue = values[0];
      if ( firstValue instanceof Integer && value instanceof Double ) {
        return ( (Double) value ).intValue();
      }
    }
    return value;
  }

  private static boolean valueNotReady( Object value ) {
    return value == RolapUtil.valueNotReadyException || value == Double.valueOf( Double.NaN );
  }

  /**
   * Calc which evaluates an expression to form a list of tuples, evaluates a scalar expression at each tuple, then
   * sorts the list of values. The result is a value of type {@link SortResult}, and can be used to implement the
   * <code>Rank</code> function efficiently.
   */
  private static class SortedListCalc extends AbstractProfilingNestedCalc {
    private final TupleListCalc tupleListCalc;
    private final Calc keyCalc;

    private static final Integer ONE = 1;

    /**
     * Creates a SortCalc.
     *
     * @param exp
     *          Source expression
     * @param tupleListCalc
     *          Compiled expression to compute the list
     * @param keyCalc
     *          Compiled expression to compute the sort key
     */
    public SortedListCalc( Type type, TupleListCalc tupleListCalc, Calc keyCalc ) {
      super( type, new Calc[] { tupleListCalc, keyCalc } );
      this.tupleListCalc = tupleListCalc;
      this.keyCalc = keyCalc;
    }

    @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
      return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
    }

    @Override
	public Object evaluate( Evaluator evaluator ) {
      // Save the state of the evaluator.
      final int savepoint = evaluator.savepoint();
      RuntimeException exception = null;
      final Map<Member, Object> memberValueMap;
      final Map<List<Member>, Object> tupleValueMap;
      final int numValues;
      // noinspection unchecked
      final Map<Object, Integer> uniqueValueCounterMap =
          new TreeMap<>( FunUtil.DescendingValueComparator.instance );
      TupleList list;
      try {
        evaluator.setNonEmpty( false );

        // Construct an array containing the value of the expression
        // for each member.

        list = tupleListCalc.evaluateList( evaluator );
        assert list != null;
        if ( list.isEmpty() ) {
          return list.getArity() == 1 ? new MemberSortResult( new Object[0], Collections.<Member, Integer>emptyMap() )
              : new TupleSortResult( new Object[0], Collections.<List<Member>, Integer>emptyMap() );
        }

        if ( list.getArity() == 1 ) {
          memberValueMap = new HashMap<>();
          tupleValueMap = null;
          for ( Member member : list.slice( 0 ) ) {
            evaluator.setContext( member );
            final Object keyValue = keyCalc.evaluate( evaluator );
            if ( keyValue instanceof RuntimeException runtimeException ) {
              if ( exception == null ) {
                exception = runtimeException;
              }
            } else if ( Util.isNull( keyValue ) ) {
              // nothing to do
            } else {
              // Assume it's the first time seeing this keyValue.
              Integer valueCounter = uniqueValueCounterMap.put( keyValue, SortedListCalc.ONE );
              if ( valueCounter != null ) {
                // Update the counter on how many times this
                // keyValue has been seen.
                uniqueValueCounterMap.put( keyValue, valueCounter + 1 );
              }
              memberValueMap.put( member, keyValue );
            }
          }
          numValues = memberValueMap.keySet().size();
        } else {
          tupleValueMap = new HashMap<>();
          memberValueMap = null;
          for ( List<Member> tuple : list ) {
            evaluator.setContext( tuple );
            final Object keyValue = keyCalc.evaluate( evaluator );
            if ( keyValue instanceof RuntimeException runtimeException) {
              if ( exception == null ) {
                exception = runtimeException;
              }
            } else if ( Util.isNull( keyValue ) ) {
              // nothing to do
            } else {
              // Assume it's the first time seeing this keyValue.
              Integer valueCounter = uniqueValueCounterMap.put( keyValue, SortedListCalc.ONE );
              if ( valueCounter != null ) {
                // Update the counter on how many times this
                // keyValue has been seen.
                uniqueValueCounterMap.put( keyValue, valueCounter + 1 );
              }
              tupleValueMap.put( tuple, keyValue );
            }
          }
          numValues = tupleValueMap.keySet().size();
        }
      } finally {
        evaluator.restore( savepoint );
      }

      // If there were exceptions, quit now... we'll be back.
      if ( exception != null ) {
        return exception;
      }

      final Object[] allValuesSorted = new Object[numValues];

      // Now build the sorted array containing all keyValues
      // And update the counter to the rank
      int currentOrdinal = 0;
      // noinspection unchecked
      final Map<Object, Integer> uniqueValueRankMap =
          new TreeMap<>( Collections.<Object>reverseOrder() );

      for ( Map.Entry<Object, Integer> entry : uniqueValueCounterMap.entrySet() ) {
        Object keyValue = entry.getKey();
        Integer valueCount = entry.getValue();
        // Because uniqueValueCounterMap is already sorted, so the
        // reconstructed allValuesSorted is guaranteed to be sorted.
        for ( int i = 0; i < valueCount; i++ ) {
          allValuesSorted[currentOrdinal + i] = keyValue;
        }
        uniqueValueRankMap.put( keyValue, currentOrdinal + 1 );
        currentOrdinal += valueCount;
      }

      // Build a member/tuple to rank map
      if ( list.getArity() == 1 ) {
        final Map<Member, Integer> rankMap = new HashMap<>();
        for ( Map.Entry<Member, Object> entry : memberValueMap.entrySet() ) {
          int oneBasedRank = uniqueValueRankMap.get( entry.getValue() );
          rankMap.put( entry.getKey(), oneBasedRank );
        }
        return new MemberSortResult( allValuesSorted, rankMap );
      } else {
        final Map<List<Member>, Integer> rankMap = new HashMap<>();
        if (tupleValueMap != null) {
            for (Map.Entry<List<Member>, Object> entry : tupleValueMap.entrySet()) {
                int oneBasedRank = uniqueValueRankMap.get(entry.getValue());
                rankMap.put(entry.getKey(), oneBasedRank);
            }
        }
        return new TupleSortResult( allValuesSorted, rankMap );
      }
    }
  }

  /**
   * Holder for the result of sorting a set of values. It provides simple interface to look up the rank for a member or
   * a tuple.
   */
  private abstract static class SortResult {
    /**
     * All values in sorted order; Duplicates are not removed. E.g. Set (15,15,5,0) 10 should be ranked 3.
     *
     * <p>
     * Null values are not present: they would be at the end, anyway.
     */
    final Object[] values;

    public SortResult( Object[] values ) {
      this.values = values;
    }

    public boolean isEmpty() {
      return values == null;
    }

    public void log() {
      if ( values == null ) {
          LOGGER.debug( "SortResult: empty" );
      } else {
        StringBuilder sb = new StringBuilder();
          sb.append( "SortResult {" );
        for ( int i = 0; i < values.length; i++ ) {
          if ( i > 0 ) {
              sb.append( "\n," );
          }
          Object value = values[i];
            sb.append( value );
        }
        sb.append( "}" );
        String msg = sb.toString();
        LOGGER.debug(msg);
      }

    }
  }

  private static class MemberSortResult extends SortResult {
    /**
     * The precomputed rank associated with all members
     */
    final Map<Member, Integer> rankMap;

    public MemberSortResult( Object[] values, Map<Member, Integer> rankMap ) {
      super( values );
      this.rankMap = rankMap;
    }

    public Integer rankOf( Member member ) {
      return rankMap.get( member );
    }
  }

  private static class TupleSortResult extends SortResult {
    /**
     * The precomputed rank associated with all tuples
     */
    final Map<List<Member>, Integer> rankMap;

    public TupleSortResult( Object[] values, Map<List<Member>, Integer> rankMap ) {
      super( values );
      this.rankMap = rankMap;
    }

    public Integer rankOf( Member[] tuple ) {
      return rankMap.get( Arrays.asList( tuple ) );
    }
  }

  /**
   * Expression which evaluates an expression to form a list of tuples.
   *
   * <p>
   * The result is a value of type {@link mondrian.olap.fun.RankFunDef.RankedMemberList} or
   * {@link mondrian.olap.fun.RankFunDef.RankedTupleList}, or null if the list is empty.
   */
  private static class RankedListCalc extends AbstractProfilingNestedCalc {
    private final TupleListCalc tupleListCalc;
    private final boolean tuple;

    /**
     * Creates a RankedListCalc.
     *
     * @param tupleListCalc
     *          Compiled expression to compute the list
     * @param tuple
     *          Whether elements of the list are tuples (as opposed to members)
     */
    public RankedListCalc( TupleListCalc tupleListCalc, boolean tuple ) {
      super(  tupleListCalc.getType() , new Calc[] { tupleListCalc } );
      this.tupleListCalc = tupleListCalc;
      this.tuple = tuple;
    }

    @Override
	public Object evaluate( Evaluator evaluator ) {
      // Construct an array containing the value of the expression
      // for each member.
      TupleList tupleList = tupleListCalc.evaluateList( evaluator );
      assert tupleList != null;
      if ( tuple ) {
        return new RankedTupleList( tupleList );
      } else {
        return new RankedMemberList( tupleList.slice( 0 ) );
      }
    }
  }

  /**
   * Data structure which contains a list and can return the position of an element in the list in O(log N).
   */
  static class RankedMemberList {
    Map<Member, Integer> map = new HashMap<>();

    RankedMemberList( List<Member> members ) {
      int i = -1;
      for ( final Member member : members ) {
        ++i;
        final Integer value = map.put( member, i );
        if ( value != null ) {
          // The list already contained a value for this key -- put
          // it back.
          map.put( member, value );
        }
      }
    }

    int indexOf( Member m ) {
      Integer integer = map.get( m );
      if ( integer == null ) {
        return -1;
      } else {
        return integer;
      }
    }
  }

  /**
   * Data structure which contains a list and can return the position of an element in the list in O(log N).
   */
  static class RankedTupleList {
    final Map<List<Member>, Integer> map = new HashMap<>();

    RankedTupleList( TupleList tupleList ) {
      int i = -1;
      for ( final List<Member> tupleMembers : tupleList.fix() ) {
        ++i;
        final Integer value = map.put( tupleMembers, i );
        if ( value != null ) {
          // The list already contained a value for this key -- put
          // it back.
          map.put( tupleMembers, value );
        }
      }
    }

    int indexOf( List<Member> tupleMembers ) {
      Integer integer = map.get( tupleMembers );
      if ( integer == null ) {
        return -1;
      } else {
        return integer;
      }
    }
  }
}
