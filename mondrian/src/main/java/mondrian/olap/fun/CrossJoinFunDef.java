/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2020 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.olap.fun;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Execution;
import org.eclipse.daanse.olap.api.NativeEvaluator;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleCursor;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.calc.impl.AbstractIterCalc;
import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.AbstractTupleCursor;
import mondrian.calc.impl.AbstractTupleIterable;
import mondrian.calc.impl.DelegatingTupleList;
import mondrian.calc.impl.ListTupleList;
import mondrian.calc.impl.TupleCollections;
import mondrian.mdx.MdxVisitorImpl;
import mondrian.olap.ResultStyleException;
import mondrian.olap.Util;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.TupleType;
import mondrian.rolap.RolapEvaluator;
import mondrian.rolap.SqlConstraintUtils;
import mondrian.server.LocusImpl;
import mondrian.util.CancellationChecker;
import mondrian.util.CartesianProductList;

/**
 * Definition of the <code>CrossJoin</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
public class CrossJoinFunDef extends AbstractFunctionDefinition {
  private static final Logger LOGGER = LoggerFactory.getLogger( CrossJoinFunDef.class );
	static OperationAtom functionAtom = new FunctionOperationAtom("Crossjoin");


  static final ResolverImpl Resolver = new ResolverImpl();

  static final StarCrossJoinResolver StarResolver = new StarCrossJoinResolver();

  private static int counterTag = 0;

  // used to tell the difference between crossjoin expressions.
  private final int ctag = CrossJoinFunDef.counterTag++;

  public CrossJoinFunDef( FunctionMetaData functionMetaData ) {
    super( functionMetaData );
  }

  @Override
public Type getResultType( Validator validator, Expression[] args ) {
    // CROSSJOIN(<Set1>,<Set2>) has type [Hie1] x [Hie2].
    List<MemberType> list = new ArrayList<>();
    for ( Expression arg : args ) {
      final Type type = arg.getType();
      if ( type instanceof SetType ) {
        CrossJoinFunDef.addTypes( type, list );
      } else if ( getFunctionMetaData().operationAtom().name().equals( "*" ) ) {
        // The "*" form of CrossJoin is lenient: args can be either
        // members/tuples or sets.
        CrossJoinFunDef.addTypes( type, list );
      } else if ( getFunctionMetaData().operationAtom().name().equals( "()" ) ) {
        // The "()" form of CrossJoin is lenient: args can be either
        // members/tuples or sets.
        CrossJoinFunDef.addTypes( type, list );
      } else {
        throw Util.newInternal( "arg to crossjoin must be a set" );
      }
    }
    final MemberType[] types = list.toArray( new MemberType[list.size()] );
    TupleType.checkHierarchies( types );
    final TupleType tupleType = new TupleType( types );
    return new SetType( tupleType );
  }

  /**
   * Adds a type to a list of types. If type is a {@link TupleType}, does so recursively.
   *
   * @param type
   *          Type to add to list
   * @param list
   *          List of types to add to
   */
  private static void addTypes( final Type type, List<MemberType> list ) {
    if ( type instanceof SetType setType ) {
      CrossJoinFunDef.addTypes( setType.getElementType(), list );
    } else if ( type instanceof TupleType tupleType ) {
      for ( Type elementType : tupleType.elementTypes ) {
        CrossJoinFunDef.addTypes( elementType, list );
      }
    } else if ( type instanceof MemberType memberType) {
      list.add( memberType );
    } else {
      throw Util.newInternal( "Unexpected type: " + type );
    }
  }

  @Override
public Calc compileCall( final ResolvedFunCall call, ExpressionCompiler compiler ) {
    // What is the desired return type?
    for ( ResultStyle r : compiler.getAcceptableResultStyles() ) {
      switch ( r ) {
        case ITERABLE, ANY:
          // Consumer wants ITERABLE or ANY
          return compileCallIterable( call, compiler );
        case LIST:
          // Consumer wants (immutable) LIST
          return compileCallImmutableList( call, compiler );
        case MUTABLE_LIST:
          // Consumer MUTABLE_LIST
          return compileCallMutableList( call, compiler );
      }
    }
    throw ResultStyleException.generate( ResultStyle.ITERABLE_LIST_MUTABLELIST_ANY, compiler
        .getAcceptableResultStyles() );
  }

  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////
  // Iterable
  ///////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////////

  protected TupleIteratorCalc compileCallIterable( final ResolvedFunCall call, ExpressionCompiler compiler ) {
    final Expression[] args = call.getArgs();
    Calc[] calcs =  new Calc[args.length];
    for (int i = 0; i < args.length; i++) {
      calcs[i] = toIter( compiler, args[i] );
    }
    return compileCallIterableArray(call, compiler, calcs);
  }

  protected TupleIteratorCalc compileCallIterableArray( final ResolvedFunCall call, ExpressionCompiler compiler, Calc[] calcs) {
    TupleIteratorCalc tupleIteratorCalc = compileCallIterableLeaf(call, calcs[0], calcs[1]);

    if(calcs.length == 2){
      return tupleIteratorCalc;
    }
    else {
      Calc[] nextClasls = new Calc[calcs.length - 1];
      nextClasls[0] = tupleIteratorCalc;
      for (int i = 1; i < calcs.length - 1; i++) {
        nextClasls[i] = calcs[i + 1];
      }
      return compileCallIterableArray(call, compiler, nextClasls);
    }
  }

  protected TupleIteratorCalc compileCallIterableLeaf( final ResolvedFunCall call,
                                                final Calc  calc1, final Calc calc2) {
    Calc[] calcs = new Calc[] { calc1, calc2 };
    // The Calcs, 1 and 2, can be of type: Member or Member[] and
    // of ResultStyle: ITERABLE, LIST or MUTABLE_LIST, but
    // LIST and MUTABLE_LIST are treated the same; so
    // there are 16 possible combinations - sweet.

    // Check returned calc ResultStyles
    FunUtil.checkIterListResultStyles( calc1 );
    FunUtil.checkIterListResultStyles( calc2 );

    return new CrossJoinIterCalc( call, calcs );
  }

  private Calc toIter( ExpressionCompiler compiler, final Expression exp ) {
    // Want iterable, immutable list or mutable list in that order
    // It is assumed that an immutable list is easier to get than
    // a mutable list.
    final Type type = exp.getType();
    if ( type instanceof SetType ) {
      // this can return an TupleIteratorCalc or TupleListCalc
      return compiler.compileAs( exp, null, ResultStyle.ITERABLE_LIST_MUTABLELIST );
    } else {
      // this always returns an TupleIteratorCalc
      return new SetFunDef.ExprIterCalc(  new SetType( type ) , new Expression[] { exp }, compiler,
          ResultStyle.ITERABLE_LIST_MUTABLELIST );
    }
  }

  class CrossJoinIterCalc extends AbstractIterCalc {
	private  ResolvedFunCall call;
    CrossJoinIterCalc( ResolvedFunCall call, Calc[] calcs ) {
      super( call.getType(), calcs );
      this.call=call;
    }

    @Override
	public TupleIterable evaluateIterable( Evaluator evaluator ) {

      // Use a native evaluator, if more efficient.
      // TODO: Figure this out at compile time.
      SchemaReader schemaReader = evaluator.getSchemaReader();
      NativeEvaluator nativeEvaluator =
          schemaReader.getNativeSetEvaluator( call.getFunDef(), call.getArgs(), evaluator, this );
      if ( nativeEvaluator != null ) {
        return (TupleIterable) nativeEvaluator.execute( ResultStyle.ITERABLE );
      }

      Calc[] calcs = getChildCalcs();
      TupleIteratorCalc calc1 = (TupleIteratorCalc) calcs[0];
      TupleIteratorCalc calc2 = (TupleIteratorCalc) calcs[1];

      TupleIterable o1 = calc1.evaluateIterable( evaluator );
      if ( o1 instanceof TupleList l1 ) {
        l1 = nonEmptyOptimizeList( evaluator, l1, call );
        if ( l1.isEmpty() ) {
          return TupleCollections.emptyList( getType().getArity() );
        }
        o1 = l1;
      }

      TupleIterable o2 = calc2.evaluateIterable( evaluator );
      if ( o2 instanceof TupleList l2 ) {
        l2 = nonEmptyOptimizeList( evaluator, l2, call );
        if ( l2.isEmpty() ) {
          return TupleCollections.emptyList( getType().getArity() );
        }
        o2 = l2;
      }

      return makeIterable( o1, o2 );
    }

    protected TupleIterable makeIterable( final TupleIterable it1, final TupleIterable it2 ) {
      // There is no knowledge about how large either it1 ore it2
      // are or how many null members they might have, so all
      // one can do is iterate across them:
      // iterate across it1 and for each member iterate across it2

      return new AbstractTupleIterable( it1.getArity() + it2.getArity() ) {
        @Override
		public TupleCursor tupleCursor() {
          return new AbstractTupleCursor( getArity() ) {
            final TupleCursor i1 = it1.tupleCursor();
            final int arity1 = i1.getArity();
            TupleCursor i2 = TupleCollections.emptyList( 1 ).tupleCursor();
            final Member[] members = new Member[arity];

            long currentIteration = 0;
            Execution execution = LocusImpl.peek().getExecution();

            @Override
			public boolean forward() {
              if ( i2.forward() ) {
                return true;
              }
              while ( i1.forward() ) {
                CancellationChecker.checkCancelOrTimeout( currentIteration++, execution );
                i2 = it2.tupleCursor();
                if ( i2.forward() ) {
                  return true;
                }
              }
              return false;
            }

            @Override
			public List<Member> current() {
              i1.currentToArray( members, 0 );
              i2.currentToArray( members, arity1 );
              return Util.flatList( members );
            }

            @Override
            public Member member( int column ) {
              if ( column < arity1 ) {
                return i1.member( column );
              } else {
                return i2.member( column - arity1 );
              }
            }

            @Override
            public void setContext( Evaluator evaluator ) {
              i1.setContext( evaluator );
              i2.setContext( evaluator );
            }

            @Override
            public void currentToArray( Member[] members, int offset ) {
              i1.currentToArray( members, offset );
              i2.currentToArray( members, offset + arity1 );
            }
          };
        }
      };
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Immutable List
  ///////////////////////////////////////////////////////////////////////////

  protected TupleListCalc compileCallImmutableList( final ResolvedFunCall call, ExpressionCompiler compiler ) {
    final Expression[] args = call.getArgs();
    Calc[] calcs =  new Calc[args.length];
    for (int i = 0; i < args.length; i++) {
      calcs[i] = toList( compiler, args[i] );
    }
    return compileCallImmutableListArray(call, compiler, calcs);
  }

  protected TupleListCalc compileCallImmutableListArray( final ResolvedFunCall call, ExpressionCompiler compiler, Calc[] calcs ) {
    TupleListCalc tupleListCalc = compileCallImmutableListLeaf(call, calcs[0], calcs[1]);

    if(calcs.length == 2){
      return tupleListCalc;
    }
    else {
      Calc[] nextClasls = new Calc[calcs.length - 1];
      nextClasls[0] = tupleListCalc;
      for (int i = 1; i < calcs.length - 1; i++) {
        nextClasls[i] = calcs[i + 1];
      }
      return compileCallImmutableListArray(call, compiler, nextClasls);
    }
  }

  protected TupleListCalc compileCallImmutableListLeaf( final ResolvedFunCall call,
                                                   final Calc  calc1, final Calc calc2 ) {
    Calc[] calcs = new Calc[] { calc1, calc2 };
    // The Calcs, 1 and 2, can be of type: Member or Member[] and
    // of ResultStyle: LIST or MUTABLE_LIST.
    // Since we want an immutable list as the result, it does not
    // matter whether the Calc list are of type
    // LIST and MUTABLE_LIST - they are treated the same; so
    // there are 4 possible combinations - even sweeter.

    // Check returned calc ResultStyles
    FunUtil.checkListResultStyles( calc1 );
    FunUtil.checkListResultStyles( calc2 );

    return new ImmutableListCalc( call, calcs );
  }

  /**
   * Compiles an expression to list (or mutable list) format. Never returns null.
   *
   * @param compiler
   *          Compiler
   * @param exp
   *          Expression
   * @return Compiled expression that yields a list or mutable list
   */
  private TupleListCalc toList( ExpressionCompiler compiler, final Expression exp ) {
    // Want immutable list or mutable list in that order
    // It is assumed that an immutable list is easier to get than
    // a mutable list.
    final Type type = exp.getType();
    if ( type instanceof SetType ) {
      final Calc calc = compiler.compileAs( exp, null, ResultStyle.LIST_MUTABLELIST );
      if ( calc == null ) {
        return compiler.compileList( exp, false );
      }
      return (TupleListCalc) calc;
    } else {
      return new SetFunDef.SetListCalc(  new SetType( type ), new Expression[] { exp }, compiler,
          ResultStyle.LIST_MUTABLELIST );
    }
  }

  abstract class BaseListCalc extends AbstractListCalc {
	  ResolvedFunCall call;
    protected BaseListCalc( ResolvedFunCall call, Calc[] calcs, boolean mutable ) {
      super( call.getType(), calcs, mutable );
      this.call=call;
    }

    @Override
	public TupleList evaluateList( Evaluator evaluator ) {
      // Use a native evaluator, if more efficient.
      // TODO: Figure this out at compile time.
      SchemaReader schemaReader = evaluator.getSchemaReader();
      NativeEvaluator nativeEvaluator =
          schemaReader.getNativeSetEvaluator( call.getFunDef(), call.getArgs(), evaluator, this );
      if ( nativeEvaluator != null ) {
        return (TupleList) nativeEvaluator.execute( ResultStyle.LIST );
      }

      Calc[] calcs = getChildCalcs();
      TupleListCalc listCalc1 = (TupleListCalc) calcs[0];
      TupleListCalc listCalc2 = (TupleListCalc) calcs[1];

      TupleList l1 = listCalc1.evaluateList( evaluator );
      // check if size of first list already exceeds limit
      Util.checkCJResultLimit( l1.size() );
      TupleList l2 = listCalc2.evaluateList( evaluator );
      // check if size of second list already exceeds limit
      Util.checkCJResultLimit( l2.size() );
      // check crossjoin
      Util.checkCJResultLimit( (long) l1.size() * l2.size() );

      l1 = nonEmptyOptimizeList( evaluator, l1, call );
      if ( l1.isEmpty() ) {
        return TupleCollections.emptyList( l1.getArity() + l2.getArity() );
      }
      l2 = nonEmptyOptimizeList( evaluator, l2, call );
      if ( l2.isEmpty() ) {
        return TupleCollections.emptyList( l1.getArity() + l2.getArity() );
      }

      return makeList( l1, l2 );
    }

    protected abstract TupleList makeList( TupleList l1, TupleList l2 );
  }

  class ImmutableListCalc extends BaseListCalc {
    ImmutableListCalc( ResolvedFunCall call, Calc[] calcs ) {
      super( call, calcs, false );
    }

    @Override
	protected TupleList makeList( final TupleList l1, final TupleList l2 ) {
      final int arity = l1.getArity() + l2.getArity();
      return new DelegatingTupleList( arity, new AbstractList<List<Member>>() {
        final List<List<List<Member>>> lists = Arrays.<List<List<Member>>> asList( l1, l2 );
        final Member[] members = new Member[arity];

        final CartesianProductList cartesianProductList = new CartesianProductList<>( lists );

        @Override
        public List<Member> get( int index ) {
          cartesianProductList.getIntoArray( index, members );
          return Util.flatListCopy( members );
        }

        @Override
        public int size() {
          return cartesianProductList.size();
        }
      } );
    }
  }

  protected TupleListCalc compileCallMutableList( final ResolvedFunCall call, ExpressionCompiler compiler ) {
    final Expression[] args = call.getArgs();
    Calc[] calcs =  new Calc[args.length];
    for (int i = 0; i < args.length; i++) {
      calcs[i] = toList( compiler, args[i] );
    }
    return compileCallMutableListArray(call, compiler, calcs);
  }

  protected TupleListCalc compileCallMutableListArray( final ResolvedFunCall call, ExpressionCompiler compiler, Calc[] calcs ) {
    TupleListCalc tupleListCalc = compileCallMutableListLeaf(call, calcs[0], calcs[1]);

    if(calcs.length == 2){
      return tupleListCalc;
    }
    else {
      Calc[] nextClasls = new Calc[calcs.length - 1];
      nextClasls[0] = tupleListCalc;
      for (int i = 1; i < calcs.length - 1; i++) {
        nextClasls[i] = calcs[i + 1];
      }
      return compileCallMutableListArray(call, compiler, nextClasls);
    }
  }

  protected TupleListCalc compileCallMutableListLeaf( final ResolvedFunCall call,
                                                 final Calc  calc1, final Calc calc2 ) {
    Calc[] calcs = new Calc[] { calc1, calc2 };
    // The Calcs, 1 and 2, can be of type: Member or Member[] and
    // of ResultStyle: LIST or MUTABLE_LIST.
    // Since we want an mutable list as the result, it does not
    // matter whether the Calc list are of type
    // LIST and MUTABLE_LIST - they are treated the same,
    // regardless of type, one must materialize the result list; so
    // there are 4 possible combinations - even sweeter.

    // Check returned calc ResultStyles
    FunUtil.checkListResultStyles( calc1 );
    FunUtil.checkListResultStyles( calc2 );

    return new MutableListCalc( call, calcs );
  }

  class MutableListCalc extends BaseListCalc {
    MutableListCalc( ResolvedFunCall call, Calc[] calcs ) {
      super( call, calcs, true );
    }

    @Override
	@SuppressWarnings( { "unchecked" } )
    protected TupleList makeList( final TupleList l1, final TupleList l2 ) {
      final int arity = l1.getArity() + l2.getArity();
      final List<Member> members = new ArrayList<>( arity * l1.size() * l2.size() );
      for ( List<Member> ma1 : l1 ) {
        for ( List<Member> ma2 : l2 ) {
          members.addAll( ma1 );
          members.addAll( ma2 );
        }
      }
      return new ListTupleList( arity, members );
    }
  }

  protected TupleList nonEmptyOptimizeList( Evaluator evaluator, TupleList list, ResolvedFunCall call ) {
    int opSize = evaluator.getSchemaReader().getContext().getConfig().crossJoinOptimizerSize();
    if ( list.isEmpty() ) {
      return list;
    }

    int size = list.size();

    if ( size > opSize && evaluator.isNonEmpty() ) {
      // instead of overflow exception try to further
      // optimize nonempty(crossjoin(a,b)) ==
      // nonempty(crossjoin(nonempty(a),nonempty(b))
      final int missCount = evaluator.getMissCount();

      list = nonEmptyList( evaluator, list, call );
      size = list.size();
      // list may be empty after nonEmpty optimization
      if ( size == 0 ) {
        return TupleCollections.emptyList( list.getArity() );
      }
      final int missCount2 = evaluator.getMissCount();
      final int puntMissCountListSize = 1000;
      if ( missCount2 > missCount && size > puntMissCountListSize ) {
        // We've hit some cells which are not in the cache. They
        // registered as non-empty, but we won't really know until
        // we've populated the cache. The cartesian product is still
        // huge, so let's quit now, and try again after the cache
        // has been loaded.
        // Return an empty list short circuits higher level
        // evaluation poping one all the way to the top.
        return TupleCollections.emptyList( list.getArity() );
      }
    }
    return list;
  }

  public static TupleList mutableCrossJoin( TupleList list1, TupleList list2 ) {
    return CrossJoinFunDef.mutableCrossJoin( Arrays.asList( list1, list2 ) );
  }

  public static TupleList mutableCrossJoin( List<TupleList> lists ) {
    long size = 1;
    int arity = 0;
    for ( TupleList list : lists ) {
      size *= list.size();
      arity += list.getArity();
    }
    if ( size == 0L ) {
      return TupleCollections.emptyList( arity );
    }

    // Optimize nonempty(crossjoin(a,b)) ==
    // nonempty(crossjoin(nonempty(a),nonempty(b))

    // FIXME: If we're going to apply a NON EMPTY constraint later, it's
    // possible that the ultimate result will be much smaller.

    Util.checkCJResultLimit( size );

    // Now we can safely cast size to an integer. It still might be very
    // large - which means we're allocating a huge array which we might
    // pare down later by applying NON EMPTY constraints - which is a
    // concern.
    List<Member> result = new ArrayList<>( (int) size * arity );

    final Member[] partialArray = new Member[arity];
    final List<Member> partial = Arrays.asList( partialArray );
    CrossJoinFunDef.cartesianProductRecurse( 0, lists, partial, partialArray, 0, result );
    return new ListTupleList( arity, result );
  }

  private static void cartesianProductRecurse( int i, List<TupleList> lists, List<Member> partial,
      Member[] partialArray, int partialSize, List<Member> result ) {
    final TupleList tupleList = lists.get( i );
    final int partialSizeNext = partialSize + tupleList.getArity();
    final int iNext = i + 1;
    final TupleCursor cursor = tupleList.tupleCursor();
    int currentIteration = 0;
    Execution execution = LocusImpl.peek().getExecution();
    while ( cursor.forward() ) {
      CancellationChecker.checkCancelOrTimeout( currentIteration++, execution );
      cursor.currentToArray( partialArray, partialSize );
      if ( i == lists.size() - 1 ) {
        result.addAll( partial );
      } else {
        CrossJoinFunDef.cartesianProductRecurse( iNext, lists, partial, partialArray, partialSizeNext, result );
      }
    }
  }

  /**
   * Traverses the function call tree of the non empty crossjoin function and populates the queryMeasureSet with base
   * measures
   */
  private static class MeasureVisitor extends MdxVisitorImpl {

    private final Set<Member> queryMeasureSet;
    private final ResolvedFunCallFinder finder;
    private final Set<Member> activeMeasures = new HashSet<>();

    /**
     * Creates a MeasureVisitor.
     *
     * @param queryMeasureSet
     *          Set of measures in query
     * @param crossJoinCall
     *          Measures referencing this call should be excluded from the list of measures found
     */
    MeasureVisitor( Set<Member> queryMeasureSet, ResolvedFunCall crossJoinCall ) {
      this.queryMeasureSet = queryMeasureSet;
      this.finder = new ResolvedFunCallFinder( crossJoinCall );
    }

    @Override
	public Object visitParameterExpression( ParameterExpression parameterExpr ) {
      final Parameter parameter = parameterExpr.getParameter();
      final Type type = parameter.getType();
      if ( type instanceof mondrian.olap.type.MemberType ) {
        final Object value = parameter.getValue();
        if ( value instanceof Member member ) {
          process( member );
        }
      }

      return null;
    }

    @Override
	public Object visitMemberExpression( MemberExpression memberExpr ) {
      Member member = memberExpr.getMember();
      process( member );
      return null;
    }

    private void process( final Member member ) {
      if ( member.isMeasure() ) {
        if ( member.isCalculated() ) {
          if ( activeMeasures.add( member ) ) {
            Expression exp = member.getExpression();
            finder.found = false;
            exp.accept( finder );
            if ( !finder.found ) {
              exp.accept( this );
            }
            activeMeasures.remove( member );
          }
        } else {
          queryMeasureSet.add( member );
        }
      }
    }
  }

  /**
   * This is the entry point to the crossjoin non-empty optimizer code.
   *
   * <p>
   * What one wants to determine is for each individual Member of the input parameter list, a 'List-Member', whether
   * across a slice there is any data.
   *
   * <p>
   * But what data?
   *
   * <p>
   * For Members other than those in the list, the 'non-List-Members', one wants to consider all data across the scope
   * of these other Members. For instance, if Time is not a List-Member, then one wants to consider data across All
   * Time. Or, if Customer is not a List-Member, then look at data across All Customers. The theory here, is if there is
   * no data for a particular Member of the list where all other Members not part of the list are span their complete
   * hierarchy, then there is certainly no data for Members of that Hierarchy at a more specific Level (more on this
   * below).
   *
   * <p>
   * When a Member that is a non-List-Member is part of a Hierarchy that has an All Member (hasAll="true"), then its
   * very easy to make sure that the All Member is used during the optimization. If a non-List-Member is part of a
   * Hierarchy that does not have an All Member, then one must, in fact, iterate over all top-level Members of the
   * Hierarchy!!! - otherwise a List-Member might be excluded because the optimization code was not looking everywhere.
   *
   * <p>
   * Concerning default Members for those Hierarchies for the non-List-Members, ignore them. What is wanted is either
   * the All Member or one must iterate across all top-level Members, what happens to be the default Member of the
   * Hierarchy is of no relevant.
   *
   * <p>
   * The Measures Hierarchy has special considerations. First, there is no All Measure. But, certainly one need only
   * involve Measures that are actually in the query... yes and no. For Calculated Measures one must also get all of the
   * non-Calculated Measures that make up each Calculated Measure. Thus, one ends up iterating across all Calculated and
   * non-Calculated Measures that are explicitly mentioned in the query as well as all Calculated and non-Calculated
   * Measures that are used to define the Calculated Measures in the query. Why all of these? because this represents
   * the total scope of possible Measures that might yield a non-null value for the List-Members and that is what we
   * what to find. It might be a super set, but thats ok; we just do not want to miss anything.
   *
   * <p>
   * For other Members, the default Member is used, but for Measures one should look for that data for all Measures
   * associated with the query, not just one Measure. For a dense dataset this may not be a problem or even apparent,
   * but for a sparse dataset, the first Measure may, in fact, have not data but other Measures associated with the
   * query might. Hence, the solution here is to identify all Measures associated with the query and then for each
   * Member of the list, determine if there is any data iterating across all Measures until non-null data is found or
   * the end of the Measures is reached.
   *
   * <p>
   * This is a non-optimistic implementation. This means that an element of the input parameter List is only not
   * included in the returned result List if for no combination of Measures, non-All Members (for Hierarchies that have
   * no All Members) and evaluator default Members did the element evaluate to non-null.
   *
   * @param evaluator
   *          Evaluator
   * @param list
   *          List of members or tuples
   * @param call
   *          Calling ResolvedFunCall used to determine what Measures to use
   * @return List of elements from the input parameter list that have evaluated to non-null.
   */
  protected TupleList nonEmptyList( Evaluator evaluator, TupleList list, ResolvedFunCall call ) {
    if ( list.isEmpty() ) {
      return list;
    }

    TupleList result = TupleCollections.createList( list.getArity(), ( list.size() + 2 ) >> 1 );

    // Get all of the Measures
    final Query query = evaluator.getQuery();

    final String measureSetKey = "MEASURE_SET-" + ctag;
    Set<Member> measureSet = Util.cast( (Set) query.getEvalCache( measureSetKey ) );

    final String memberSetKey = "MEMBER_SET-" + ctag;
    Set<Member> memberSet = Util.cast( (Set) query.getEvalCache( memberSetKey ) );
    // If not in query cache, then create and place into cache.
    // This information is used for each iteration so it makes
    // sense to create and cache it.
    if ( measureSet == null || memberSet == null ) {
      measureSet = new HashSet<>();
      memberSet = new HashSet<>();
      Set<Member> queryMeasureSet = query.getMeasuresMembers();
      MeasureVisitor measureVisitor = new MeasureVisitor( measureSet, call );

      // MemberExtractingVisitor will collect the dimension members
      // referenced within the measures in the query.
      // One or more measures may conflict with the members in the tuple,
      // overriding the context of the tuple member when determining
      // non-emptiness.
      MemberExtractingVisitor memVisitor = new MemberExtractingVisitor( memberSet, call, false );

      for ( Member m : queryMeasureSet ) {
        if ( m.isCalculated() ) {
          Expression exp = m.getExpression();
          exp.accept( measureVisitor );
          exp.accept( memVisitor );
        } else {
          measureSet.add( m );
        }
      }
      Formula[] formula = query.getFormulas();
      if ( formula != null ) {
        for ( Formula f : formula ) {
          if ( SqlConstraintUtils.containsValidMeasure( f.getExpression() ) ) {
            // short circuit if VM is present.
            return list;
          }
          f.accept( measureVisitor );
        }
      }
      query.putEvalCache( measureSetKey, measureSet );
      query.putEvalCache( memberSetKey, memberSet );
    }

    final String allMemberListKey = "ALL_MEMBER_LIST-" + ctag;
    List<Member> allMemberList = Util.cast( (List) query.getEvalCache( allMemberListKey ) );

    final String nonAllMembersKey = "NON_ALL_MEMBERS-" + ctag;
    Member[][] nonAllMembers = (Member[][]) query.getEvalCache( nonAllMembersKey );
    if ( nonAllMembers == null ) {
      //
      // Get all of the All Members and those Hierarchies that
      // do not have All Members.
      //
      Member[] evalMembers = evaluator.getMembers().clone();

      List<Member> listMembers = list.get( 0 );

      // Remove listMembers from evalMembers and independentSlicerMembers
      for ( Member lm : listMembers ) {
        Hierarchy h = lm.getHierarchy();
        for ( int i = 0; i < evalMembers.length; i++ ) {
          Member em = evalMembers[i];
          if ( ( em != null ) && h.equals( em.getHierarchy() ) ) {
            evalMembers[i] = null;
          }
        }
      }

      Map<Hierarchy, Set<Member>> mapOfSlicerMembers = new HashMap<>();
      if ( evaluator instanceof RolapEvaluator rev ) {
        mapOfSlicerMembers = rev.getSlicerMembersByHierarchy();
      }

      // Now we have the non-List-Members, but some of them may not be
      // All Members (default Member need not be the All Member) and
      // for some Hierarchies there may not be an All Member.
      // So we create an array of Objects some elements of which are
      // All Members and others elements will be an array of all top-level
      // Members when there is not an All Member.
      SchemaReader schemaReader = evaluator.getSchemaReader();
      allMemberList = new ArrayList<>();
      List<Member[]> nonAllMemberList = new ArrayList<>();

      Member em;
      boolean isSlicerMember;
      for ( Member evalMember : evalMembers ) {
        em = evalMember;
        if ( em == null ) {
          // Above we might have removed some by setting them
          // to null. These are the CrossJoin axes.
          continue;
        }
        if ( em.isMeasure() ) {
          continue;
        }

        isSlicerMember = false;
        if ( mapOfSlicerMembers != null ) {
          Set<Member> members = mapOfSlicerMembers.get( em.getHierarchy() );
          if ( members != null ) {
            isSlicerMember = members.contains( em );
          }
        }

        //
        // The unconstrained members need to be replaced by the "All"
        // member based on its usage and property. This is currently
        // also the behavior of native cross join evaluation. See
        // SqlConstraintUtils.addContextConstraint()
        //
        // on slicer? | calculated? | replace with All?
        // -----------------------------------------------
        // Y | Y | Y always
        // Y | N | N
        // N | Y | N
        // N | N | Y if not "All"
        // -----------------------------------------------
        //
        if ( ( isSlicerMember && !em.isCalculated() ) || ( !isSlicerMember && em.isCalculated() ) ) {
          // If the slicer contains multiple members from this one's
          // hierarchy, add them to nonAllMemberList
          if ( isSlicerMember ) {
            Set<Member> hierarchySlicerMembers = mapOfSlicerMembers.get( em.getHierarchy() );
            if ( hierarchySlicerMembers.size() > 1 ) {
              nonAllMemberList.add( hierarchySlicerMembers.toArray( new Member[hierarchySlicerMembers.size()] ) );
            }
          }
          continue;
        }

        // If the member is not the All member;
        // or if it is a slicer member,
        // replace with the "all" member.
        if ( isSlicerMember || !em.isAll() ) {
          Hierarchy h = em.getHierarchy();
          final List<Member> rootMemberList = schemaReader.getHierarchyRootMembers( h );
          if ( h.hasAll() ) {
            // The Hierarchy has an All member
            boolean found = false;
            for ( Member m : rootMemberList ) {
              if ( m.isAll() ) {
                allMemberList.add( m );
                found = true;
                break;
              }
            }
            if ( !found ) {
              CrossJoinFunDef.LOGGER.warn( "CrossJoinFunDef.nonEmptyListNEW: ERROR" );
            }
          } else {
            // The Hierarchy does NOT have an All member
            Member[] rootMembers = rootMemberList.toArray( new Member[rootMemberList.size()] );
            nonAllMemberList.add( rootMembers );
          }
        }
      }
      nonAllMembers = nonAllMemberList.toArray( new Member[nonAllMemberList.size()][] );

      query.putEvalCache( allMemberListKey, allMemberList );
      query.putEvalCache( nonAllMembersKey, nonAllMembers );
    }

    //
    // Determine if there is any data.
    //
    // Put all of the All Members into Evaluator
    final int savepoint = evaluator.savepoint();
    try {
      evaluator.setContext( allMemberList );
      // Iterate over elements of the input list. If for any
      // combination of
      // Measure and non-All Members evaluation is non-null, then
      // add it to the result List.
      final TupleCursor cursor = list.tupleCursor();
      int currentIteration = 0;
      Execution execution = query.getStatement().getCurrentExecution();
      while ( cursor.forward() ) {
        cursor.setContext( evaluator );
        for ( Member member : memberSet ) {
          // memberSet contains members referenced within measures.
          // Make sure that we don't incorrectly assume a context
          // that will be changed by the measure, so conservatively
          // push context to [All] for each of the associated
          // hierarchies.
          evaluator.setContext( member.getHierarchy().getAllMember() );
        }
        // Check if the MDX query was canceled.
        // Throws an exception in case of timeout is exceeded
        // see MONDRIAN-2425
        CancellationChecker.checkCancelOrTimeout( currentIteration++, execution );
        if ( tupleContainsCalcs( cursor.current() ) || CrossJoinFunDef.checkData( nonAllMembers, nonAllMembers.length - 1, measureSet,
            evaluator ) ) {
          result.addCurrent( cursor );
        }
      }
      return result;
    } finally {
      evaluator.restore( savepoint );
    }
  }

  private boolean tupleContainsCalcs( List<Member> current ) {
    return current.stream().anyMatch( Member::isCalculated );
  }

  /**
   * Return <code>true</code> if for some combination of Members from the nonAllMembers array of Member arrays and
   * Measures from the Set of Measures evaluate to a non-null value. Even if a particular combination is non-null, all
   * combinations are tested just to make sure that the data is loaded.
   *
   * @param nonAllMembers
   *          array of Member arrays of top-level Members for Hierarchies that have no All Member.
   * @param cnt
   *          which Member array is to be processed.
   * @param measureSet
   *          Set of all that should be tested against.
   * @param evaluator
   *          the Evaluator.
   * @return True if at least one combination evaluated to non-null.
   */
  private static boolean checkData( Member[][] nonAllMembers, int cnt, Set<Member> measureSet, Evaluator evaluator ) {
    if ( cnt < 0 ) {
      // no measures found, use standard algorithm
      if ( measureSet.isEmpty() ) {
        Object value = evaluator.evaluateCurrent();
        if ( value != null && !( value instanceof Throwable ) ) {
          return true;
        }
      } else {
        // Here we evaluate across all measures just to
        // make sure that the data is all loaded
        boolean found = false;
        for ( Member measure : measureSet ) {
          evaluator.setContext( measure );
          Object value = evaluator.evaluateCurrent();
          if ( value != null && !( value instanceof Throwable ) ) {
            found = true;
          }
        }
        return found;
      }
    } else {
      boolean found = false;
      for ( Member m : nonAllMembers[cnt] ) {
        evaluator.setContext( m );
        if ( CrossJoinFunDef.checkData( nonAllMembers, cnt - 1, measureSet, evaluator ) ) {
          found = true;
        }
      }
      return found;
    }
    return false;
  }

  private static class ResolverImpl extends NoExpressionRequiredFunctionResolver {


    @Override
	public FunctionDefinition resolve(
            Expression[] args,
            Validator validator,
            List<Conversion> conversions)
    {
      if (args.length < 2) {
        return null;
      } else {
        for (int i = 0; i < args.length; i++) {
          if (!validator.canConvert(
                  i, args[i], org.eclipse.daanse.olap.api.DataType.SET, conversions)) {
            return null;
          }
        }


		FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Returns the cross product of two sets.", "Crossjoin(<Set1>, <Set2>[, <Set3>...])",
				org.eclipse.daanse.olap.api.DataType.SET, Expressions.categoriesOf(args));
        return new CrossJoinFunDef(functionMetaData);
      }
    }

    protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData) {
      return new CrossJoinFunDef(functionMetaData);
    }

	@Override
	public OperationAtom getFunctionAtom() {
		return functionAtom;
	}
  }

	private static class StarCrossJoinResolver extends AbstractFunctionDefinitionMultiResolver {

		static final OperationAtom atom = new InfixOperationAtom("*");

		static final FunctionMetaData starCrossJoinSetSet = new FunctionMetaDataR(atom,
				"Returns the cross product of two sets.", "<Set1> * <Set2>", DataType.SET,
				new DataType[] { DataType.SET, DataType.SET });

		static final FunctionMetaData starCrossJoinSetMember = new FunctionMetaDataR(atom,
				"Returns the cross product of Set and Member.", "<Set> * <Member>", DataType.SET,
				new DataType[] { DataType.SET, DataType.MEMBER });

		static final FunctionMetaData starCrossJoinMemberSet = new FunctionMetaDataR(atom,
				"Returns the cross product of Member and Set.", "<Member> * <Set>", DataType.SET,
				new DataType[] { DataType.MEMBER, DataType.SET });

		static final FunctionMetaData starCrossJoinMemberMember = new FunctionMetaDataR(atom,
				"Returns the cross product of two Members.", "<Member> * <Set>", DataType.SET,
				new DataType[] { DataType.MEMBER, DataType.MEMBER });

		public StarCrossJoinResolver() {
			super(List.of(new CrossJoinFunDef(starCrossJoinSetSet), new CrossJoinFunDef(starCrossJoinSetMember),
					new CrossJoinFunDef(starCrossJoinMemberSet), new CrossJoinFunDef(starCrossJoinMemberMember)));
		}

		@Override
		public FunctionDefinition resolve(Expression[] args, Validator validator, List<Conversion> conversions) {
			// This function only applies in contexts which require a set.
			// Elsewhere, "*" is the multiplication operator.
			// This means that [Measures].[Unit Sales] * [Gender].[M] is
			// well-defined.
			if (validator.requiresExpression()) {
				return null;
			}
			return super.resolve(args, validator, conversions);
		}

	}
}
