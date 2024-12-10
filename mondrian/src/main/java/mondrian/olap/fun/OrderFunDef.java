/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2021 Hitachi Vantara
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ConstantCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.GenericIterCalc;
import mondrian.calc.impl.MemberArrayValueCalc;
import mondrian.calc.impl.MemberValueCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.calc.impl.ValueCalc;
import mondrian.olap.fun.sort.SortKeySpec;
import mondrian.olap.fun.sort.Sorter;
import mondrian.olap.fun.sort.Sorter.SorterFlag;

/**
 * Definition of the <code>Order</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class OrderFunDef extends AbstractFunctionDefinition {
	static OperationAtom functionAtom = new FunctionOperationAtom("Order");

    static final ResolverImpl Resolver = new ResolverImpl();
  private static final String TIMING_NAME = OrderFunDef.class.getSimpleName();
    public static final String CALC_IMPL = "CurrentMemberCalc";

    public OrderFunDef(  FunctionMetaData functionMetaData ) {
    super( functionMetaData);
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final TupleIteratorCalc listCalc = compiler.compileIter( call.getArg( 0 ) );
    List<SortKeySpec> keySpecList = new ArrayList<>();
    buildKeySpecList( keySpecList, call, compiler );
    final int keySpecCount = keySpecList.size();
    Calc[] calcList = new Calc[keySpecCount + 1]; // +1 for the listCalc
    calcList[0] = listCalc;

    assert keySpecCount >= 1;
    final Calc expCalc = keySpecList.get( 0 ).getKey();
    calcList[1] = expCalc;

    if (keySpecCount == 1 && ( expCalc instanceof MemberValueCalc ) ||( expCalc instanceof MemberArrayValueCalc ) ) {
        List<MemberCalc> constantList = new ArrayList<>();
        List<MemberCalc> variableList = new ArrayList<>();
        final MemberCalc[] calcs = (MemberCalc[]) ( (AbstractProfilingNestedCalc) expCalc ).getChildCalcs();
        for ( MemberCalc memberCalc : calcs ) {
			if ((memberCalc instanceof ConstantCalc) && !listCalc.dependsOn(memberCalc.getType().getHierarchy())) {
            constantList.add( memberCalc );
          } else {
            variableList.add( memberCalc );
          }
        }
        if ( constantList.isEmpty() ) {
          // All members are non-constant -- cannot optimize
        } else if ( variableList.isEmpty() ) {
          // All members are constant. Optimize by setting entire
          // context first.
          calcList[1] = new ValueCalc( expCalc.getType()  );
          return new ContextCalc( calcs, new CurrentMemberCalc(call.getType(), calcList, keySpecList ) );
        } else {
          // Some members are constant. Evaluate these before
          // evaluating the list expression.
          calcList[1] =
              MemberValueCalc.create(  expCalc.getType() , variableList.toArray(
                  new MemberCalc[variableList.size()] ), compiler.getEvaluator()
                      .mightReturnNullForUnrelatedDimension() );
          return new ContextCalc( constantList.toArray( new MemberCalc[constantList.size()] ), new CurrentMemberCalc( call.getType(),
              calcList, keySpecList ) );
        }
    }
    for ( int i = 1; i < keySpecCount; i++ ) {
      final Calc expCalcs = keySpecList.get( i ).getKey();
      calcList[i + 1] = expCalcs;
    }
    return new CurrentMemberCalc( call.getType(), calcList, keySpecList );
  }

  private void buildKeySpecList( List<SortKeySpec> keySpecList, ResolvedFunCall call, ExpressionCompiler compiler ) {
    final int argCount = call.getArgs().length;
    int j = 1; // args[0] is the input set
    Calc key;
    SorterFlag dir;
    Expression arg;
    while ( j < argCount ) {
      arg = call.getArg( j );
      key = compiler.compileScalar( arg, true );
      j++;
      if ( ( j >= argCount ) || ( call.getArg( j ).getCategory() != DataType.SYMBOL) ) {
        dir = SorterFlag.ASC;
      } else {
        dir = FunUtil.getLiteralArg( call, j, SorterFlag.ASC, SorterFlag.class );
        j++;
      }
      keySpecList.add( new SortKeySpec( key, dir ) );
    }
  }

  private interface CalcWithDual extends Calc<Object> {
    public TupleList evaluateDual( Evaluator rootEvaluator, Evaluator subEvaluator );
  }

  private static class CurrentMemberCalc extends AbstractListCalc implements CalcWithDual {
    private final TupleIteratorCalc tupleIteratorCalc;
    private final Calc sortKeyCalc;
    private final List<SortKeySpec> keySpecList;
    private final int originalKeySpecCount;
    private final int arity;

    public CurrentMemberCalc( Type type, Calc[] calcList, List<SortKeySpec> keySpecList ) {
      super( type, calcList );
      this.tupleIteratorCalc = (TupleIteratorCalc) calcList[0];
      this.sortKeyCalc = calcList[1];
      this.keySpecList = keySpecList;
      this.originalKeySpecCount = keySpecList.size();
      this.arity = getType().getArity();
    }

    @Override
	public TupleList evaluateDual( Evaluator rootEvaluator, Evaluator subEvaluator ) {
      assert originalKeySpecCount == 1;
      final TupleIterable iterable = tupleIteratorCalc.evaluateIterable( rootEvaluator );
      // REVIEW: If iterable happens to be a list, we'd like to pass it,
      // but we cannot yet guarantee that it is mutable.
      // final TupleList list = iterable instanceof ArrayTupleList && false ? (TupleList) iterable : null; old code
      final TupleList list = null;
      tupleIteratorCalc.getResultStyle();
//      discard( tupleIteratorCalc.getResultStyle() );
      return handleSortWithOneKeySpec( subEvaluator, iterable, list );
    }

    @Override
	public TupleList evaluateList( Evaluator evaluator ) {
      evaluator.getTiming().markStart( OrderFunDef.TIMING_NAME );
      try {
        final TupleIterable iterable = tupleIteratorCalc.evaluateIterable( evaluator );
        // REVIEW: If iterable happens to be a list, we'd like to pass it,
        // but we cannot yet guarantee that it is mutable.
        // final TupleList list = iterable instanceof ArrayTupleList && false ? (TupleList) iterable : null; old code list all time null
        final TupleList list = null;
        // go by size of keySpecList before purging
        if ( originalKeySpecCount == 1 ) {
          return handleSortWithOneKeySpec( evaluator, iterable, list );
        } else {
          purgeKeySpecList( keySpecList, list );
          if ( keySpecList.isEmpty() ) {
            return list;
          }
          final TupleList tupleList;
          final int savepoint = evaluator.savepoint();
          try {
            evaluator.setNonEmpty( false );
            if ( arity == 1 ) {
              //tupleList =
              //    new UnaryTupleList( Sorter.sortMembers( evaluator, iterable.slice( 0 ), list == null ? null : list
              //        .slice( 0 ), keySpecList ) ); -- old code list all time null
              tupleList =
                     new UnaryTupleList( Sorter.sortMembers( evaluator, iterable.slice( 0 ), null, keySpecList ) );
            } else {
              tupleList = Sorter.sortTuples( evaluator, iterable, list, keySpecList, arity );
            }
            return tupleList;
          } finally {
            evaluator.restore( savepoint );
          }
        }
      } finally {
        evaluator.getTiming().markEnd( OrderFunDef.TIMING_NAME );
      }
    }

    private TupleList handleSortWithOneKeySpec( Evaluator evaluator, TupleIterable iterable, TupleList list ) {
      SorterFlag sortKeyDir = keySpecList.get( 0 ).getDirection();
      final TupleList tupleList;
      final int savepoint = evaluator.savepoint();
      try {
        evaluator.setNonEmpty( false );
        if ( arity == 1 ) {
          tupleList =
              new UnaryTupleList( Sorter.sortMembers( evaluator, iterable.slice( 0 ), list == null ? null : list.slice(
                  0 ), sortKeyCalc, sortKeyDir.descending, sortKeyDir.brk ) );
        } else {
          tupleList =
              Sorter.sortTuples( evaluator, iterable, list, sortKeyCalc, sortKeyDir.descending, sortKeyDir.brk,
                  arity );
        }
        return tupleList;
      } finally {
        evaluator.restore( savepoint );
      }
    }

	@Override
	protected Map<String, Object> profilingProperties(Map<String, Object> properties) {


		StringBuilder result = new StringBuilder();
		for (SortKeySpec spec : keySpecList) {
			if (result.length() > 0) {
				result.append(",");
			}

			SorterFlag sortKeyDir = spec.getDirection();
			result.append(sortKeyDir.descending ? getDesc(sortKeyDir.brk) : getAsc(sortKeyDir.brk));
		}
		properties.put("direction", result.toString());

		return super.profilingProperties(properties);
	}


    @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
      return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
    }

    private SorterFlag getDesc(boolean brk) {
        return brk ? SorterFlag.BDESC : SorterFlag.DESC;
    }

    private SorterFlag getAsc(boolean brk) {
        return brk ? SorterFlag.BASC : SorterFlag.ASC;
    }

      private void purgeKeySpecList( List<SortKeySpec> keySpecList, TupleList list ) {
      if ( list == null || list.isEmpty() ) {
        return;
      }
      if ( keySpecList.size() == 1 ) {
        return;
      }
      List<Hierarchy> listHierarchies = new ArrayList<>( list.getArity() );
      for ( Member member : list.get( 0 ) ) {
        listHierarchies.add( member.getHierarchy() );
      }
      // do not sort (remove sort key spec from the list) if
      // 1. <member_value_expression> evaluates to a member from a
      // level/dimension which is not used in the first argument
      // 2. <member_value_expression> evaluates to the same member for
      // all cells; for example, a report showing all quarters of
      // year 1998 will not be sorted if the sort key is on the constant
      // member [1998].[Q1]
      ListIterator<SortKeySpec> iter = keySpecList.listIterator();
      while ( iter.hasNext() ) {
        SortKeySpec key = iter.next();
        Calc expCalc = key.getKey();
        if ( expCalc instanceof MemberOrderKeyFunDef.CalcImpl calc) {
          Calc[] calcs = calc.getChildCalcs();
          MemberCalc memberCalc = (MemberCalc) calcs[0];
          if ( memberCalc instanceof org.eclipse.daanse.olap.calc.api.ConstantCalc || !listHierarchies.contains( memberCalc.getType()
              .getHierarchy() ) ) {
            iter.remove();
          }
        }
      }
    }
  }

  private static class ContextCalc extends GenericIterCalc {
    private final MemberCalc[] memberCalcs;
    private final CalcWithDual calc;
    private final Member[] members; // workspace

    protected ContextCalc( MemberCalc[] memberCalcs, CalcWithDual calc ) {
      super( calc.getType() , ContextCalc.xx( memberCalcs, calc ) );
      this.memberCalcs = memberCalcs;
      this.calc = calc;
      this.members = new Member[memberCalcs.length];
    }

    private static Calc[] xx( MemberCalc[] memberCalcs, CalcWithDual calc ) {
      Calc[] calcs = new Calc[memberCalcs.length + 1];
      System.arraycopy( memberCalcs, 0, calcs, 0, memberCalcs.length );
      calcs[calcs.length - 1] = calc;
      return calcs;
    }

    @Override
	public Object evaluate( Evaluator evaluator ) {
      // Evaluate each of the members, and set as context in the
      // sub-evaluator.
      for ( int i = 0; i < memberCalcs.length; i++ ) {
        members[i] = memberCalcs[i].evaluate( evaluator );
      }
      Evaluator subEval=evaluator.push();
      subEval.setContext(members);
      // Evaluate the expression in the new context.
      return calc.evaluateDual( evaluator, subEval );
    }

    @Override
	public boolean dependsOn( Hierarchy hierarchy ) {
      if ( HirarchyDependsChecker.checkAnyDependsOnChilds(  memberCalcs,hierarchy ) ) {
        return true;
      }
      // Member calculations generate members, which mask the actual
      // expression from the inherited context.
      for ( MemberCalc memberCalc : memberCalcs ) {
        if ( memberCalc.getType().usesHierarchy( hierarchy, true ) ) {
          return false;
        }
      }
      return calc.dependsOn( hierarchy );
    }

    @Override
	public ResultStyle getResultStyle() {
      return calc.getResultStyle();
    }
  }

  private static class ResolverImpl extends NoExpressionRequiredFunctionResolver {
    private final List<String> reservedWords;
    static DataType[] argTypes;

    private ResolverImpl() {

      this.reservedWords = SorterFlag.asReservedWords();
    }

    @Override
	public FunctionDefinition resolve( Expression[] args, Validator validator, List<Conversion> conversions ) {
      ResolverImpl.argTypes = new DataType[args.length];

      if ( args.length < 2 ) {
        return null;
      }
      // first arg must be a set
      if ( !validator.canConvert( 0, args[0], DataType.SET, conversions ) ) {
        return null;
      }
      ResolverImpl.argTypes[0] = DataType.SET;
      // after fist args, should be: value [, symbol]
      int i = 1;
      while ( i < args.length ) {
        if ( !validator.canConvert( i, args[i], DataType.VALUE, conversions ) ) {
          return null;
        } else {
          ResolverImpl.argTypes[i] = DataType.VALUE;
          i++;
        }
        // if symbol is not specified, skip to the next
        if ( ( i == args.length ) ) {
          // done, will default last arg to ASC
        } else {
          if ( !validator.canConvert( i, args[i], DataType.SYMBOL, conversions ) ) {
            // continue, will default sort flag for prev arg to ASC
          } else {
            ResolverImpl.argTypes[i] = DataType.SYMBOL;
            i++;
          }
        }
      }


		FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom,
				"Arranges members of a set, optionally preserving or breaking the hierarchy.",
				"Order(<Set> {, <Key Specification>}...)", DataType.SET, ResolverImpl.argTypes);

      return new OrderFunDef( functionMetaData );
    }

    @Override
	public List<String> getReservedWords() {
      if ( reservedWords != null ) {
        return reservedWords;
      }
      return super.getReservedWords();
    }

	@Override
	public OperationAtom getFunctionAtom() {
		return functionAtom;
	}
  }
}
