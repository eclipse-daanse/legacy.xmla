/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2020 Hitachi Vantara and others
// Copyright (C) 2021 Sergei Semenkov
//
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.mdx.ResolvedFunCallImpl;
import mondrian.mdx.UnresolvedFunCallImpl;
import mondrian.olap.MondrianException;
import mondrian.olap.Util;
import mondrian.olap.fun.sort.Sorter;
import mondrian.olap.type.EmptyType;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.NumericType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.TupleType;

/**
 * Definition of the <code>Descendants</code> MDX function.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class DescendantsFunDef extends AbstractFunctionDefinition {

    public static final String DESCENDANTS = "Descendants";
    static final ReflectiveMultiResolver Resolver =
    new ReflectiveMultiResolver(
        DESCENDANTS,
      "Descendants(<Member>[, <Level>[, <Desc_flag>]])",
      "Returns the set of descendants of a member at a specified level, optionally including or excluding descendants"
        + " in other levels.",
      new String[] { "fxm", "fxml", "fxmly", "fxmn", "fxmny", "fxmey" },
      DescendantsFunDef.class,
      Flag.asReservedWords() );

  static final ReflectiveMultiResolver Resolver2 =
    new ReflectiveMultiResolver(
      DESCENDANTS,
      "Descendants(<Set>[, <Level>[, <Desc_flag>]])",
      "Returns the set of descendants of a set of members at a specified level, optionally including or excluding "
        + "descendants in other levels.",
      new String[] { "fxx", "fxxl", "fxxly", "fxxn", "fxxny", "fxxey" },
      DescendantsFunDef.class,
      Flag.asReservedWords() );
    private final static String descendantsAppliedToSetOfTuples =
        "Argument to Descendants function must be a member or set of members, not a set of tuples";
    private final static String cannotDeduceTypeOfSet = "Cannot deduce type of set";

    public DescendantsFunDef( FunctionMetaData functionMetaData ) {
    super( functionMetaData );
  }

  @Override
public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
    final Type type0 = call.getArg( 0 ).getType();
    if ( type0 instanceof SetType setType ) {
      if ( setType.getElementType() instanceof TupleType ) {
        throw new MondrianException(descendantsAppliedToSetOfTuples);
      }

      MemberType memberType = (MemberType) setType.getElementType();
      final Hierarchy hierarchy = memberType.getHierarchy();
      if ( hierarchy == null ) {
        throw new MondrianException(cannotDeduceTypeOfSet);
      }
      // Convert
      //   Descendants(<set>, <args>)
      // into
      //   Generate(<set>, Descendants(<dimension>.CurrentMember, <args>))
      Expression[] descendantsArgs = call.getArgs().clone();
      descendantsArgs[ 0 ] =
        new UnresolvedFunCallImpl(new PlainPropertyOperationAtom(
          "CurrentMember"),
          new Expression[] {
            new HierarchyExpressionImpl( hierarchy )
          } );
      final ResolvedFunCallImpl generateCall =
        (ResolvedFunCallImpl) compiler.getValidator().validate(
          new UnresolvedFunCallImpl(
           new FunctionOperationAtom( "Generate"),
            new Expression[] {
              call.getArg( 0 ),
              new UnresolvedFunCallImpl(
            		  new FunctionOperationAtom(
                DESCENDANTS),
                descendantsArgs )
            } ),
          false );
      return generateCall.accept( compiler );
    }

    final MemberCalc memberCalc = compiler.compileMember( call.getArg( 0 ) );
    Flag flag = Flag.SELF;
    if ( call.getArgCount() == 1 ) {
      flag = Flag.SELF_BEFORE_AFTER;
    }
    final boolean depthSpecified =
      call.getArgCount() >= 2
        && call.getArg( 1 ).getType() instanceof NumericType;
    final boolean depthEmpty =
      call.getArgCount() >= 2
        && call.getArg( 1 ).getType() instanceof EmptyType;
    if ( call.getArgCount() >= 3 ) {
      flag = FunUtil.getLiteralArg( call, 2, Flag.SELF, Flag.class );
    }

    if ( call.getArgCount() >= 2 && depthEmpty && flag != Flag.LEAVES) {
        throw Util.newError(
          "depth must be specified unless DESC_FLAG is LEAVES" );
    }
    if ( ( depthSpecified || depthEmpty ) && flag.leaves ) {
      final IntegerCalc depthCalc =
        depthSpecified
          ? compiler.compileInteger( call.getArg( 1 ) )
          : null;
      return new AbstractListCalc(call.getType(), new Calc[] { memberCalc, depthCalc } ) {
        @Override
		public TupleList evaluateList( Evaluator evaluator ) {
          final Member member = memberCalc.evaluate( evaluator );
          List<Member> result = new ArrayList<>();
          Integer depth = -1;
          if ( depthCalc != null ) {
            depth = depthCalc.evaluate( evaluator );
            if ( depth < 0 ) {
              depth = -1; // no limit
            }
          }
          final SchemaReader schemaReader =
            evaluator.getSchemaReader();
          DescendantsFunDef.descendantsLeavesByDepth(
            member, result, schemaReader, depth );
          Sorter.hierarchizeMemberList( result, false );
          return new UnaryTupleList( result );
        }
      };
    } else if ( depthSpecified ) {
      final IntegerCalc depthCalc =
        compiler.compileInteger( call.getArg( 1 ) );
      final Flag flag1 = flag;
      return new AbstractListCalc(
    		  call.getType(), new Calc[] { memberCalc, depthCalc } ) {
        @Override
		public TupleList evaluateList( Evaluator evaluator ) {
          final Member member = memberCalc.evaluate( evaluator );
          List<Member> result = new ArrayList<>();
          final Integer depth = depthCalc.evaluate( evaluator );
          final SchemaReader schemaReader =
            evaluator.getSchemaReader();
          DescendantsFunDef.descendantsByDepth(
            member, result, schemaReader,
            depth, flag1.before, flag1.self, flag1.after,
            evaluator );
          Sorter.hierarchizeMemberList( result, false );
          return new UnaryTupleList( result );
        }
      };
    } else {
      final LevelCalc levelCalc =
        call.getArgCount() > 1
          ? compiler.compileLevel( call.getArg( 1 ) )
          : null;
      final Flag flag2 = flag;
      return new AbstractListCalc(
    		  call.getType(), new Calc[] { memberCalc, levelCalc } ) {
        @Override
		public TupleList evaluateList( Evaluator evaluator ) {
          final Evaluator context =
            evaluator.isNonEmpty() ? evaluator : null;
          final Member member = memberCalc.evaluate( evaluator );
          List<Member> result = new ArrayList<>();
          final SchemaReader schemaReader =
            evaluator.getSchemaReader();
          final Level level =
            levelCalc != null
              ? levelCalc.evaluate( evaluator )
              : member.getLevel();
          DescendantsFunDef.descendantsByLevel(
            schemaReader, member, level, result,
            flag2.before, flag2.self,
            flag2.after, flag2.leaves, context );
          Sorter.hierarchizeMemberList( result, false );
          return new UnaryTupleList( result );
        }
      };
    }
  }

  private static void descendantsByDepth(
    Member member,
    List<Member> result,
    final SchemaReader schemaReader,
    final int depthLimitFinal,
    final boolean before,
    final boolean self,
    final boolean after,
    final Evaluator context ) {
    List<Member> children = new ArrayList<>();
    children.add( member );
    for ( int depth = 0;; ++depth ) {
      if ( depth == depthLimitFinal ) {
        if ( self ) {
          result.addAll( children );
        }
        if ( !after ) {
          break; // no more results after this level
        }
      } else if ( depth < depthLimitFinal ) {
        if ( before ) {
          result.addAll( children );
        }
      } else {
        if ( after ) {
          result.addAll( children );
        } else {
          break; // no more results after this level
        }
      }

      if ( context.isNonEmpty() ) {
        children = schemaReader.getMemberChildren( children, context );
      } else {
        children = schemaReader.getMemberChildren( children );
      }
      if ( children.isEmpty() ) {
        break;
      }
    }
  }

  /**
   * Populates 'result' with the descendants at the leaf level at depth 'depthLimit' or less. If 'depthLimit' is -1,
   * does not apply a depth constraint.
   */
  private static void descendantsLeavesByDepth(
    final Member member,
    final List<Member> result,
    final SchemaReader schemaReader,
    final int depthLimit) {
    if ( !schemaReader.isDrillable( member ) ) {
      if ( depthLimit >= 0 ) {
        result.add( member );
      }
      return;
    }
    List<Member> children = new ArrayList<>();
    children.add( member );
    for ( int depth = 0; depthLimit == -1 || depth <= depthLimit; ++depth ) {
      children = schemaReader.getMemberChildren( children );
      if ( children.isEmpty() ) {
        throw Util.newInternal( "drillable member must have children" );
      }
      List<Member> nextChildren = new ArrayList<>();
      for ( Member child : children ) {
        // TODO: Implement this more efficiently. The current
        // implementation of isDrillable for a parent-child hierarchy
        // simply retrieves the children sees whether there are any,
        // so we end up fetching each member's children twice.
        if ( schemaReader.isDrillable( child ) ) {
          nextChildren.add( child );
        } else {
          result.add( child );
        }
      }
      if ( nextChildren.isEmpty() ) {
        return;
      }
      children = nextChildren;
    }
  }

  /**
   * Finds all descendants of a member which are before/at/after a level, and/or are leaves (have no descendants) and
   * adds them to a result list.
   *
   * @param schemaReader Member reader
   * @param ancestor     Member to find descendants of
   * @param level        Level relative to which to filter, must not be null
   * @param result       Result list
   * @param before       Whether to output members above <code>level</code>
   * @param self         Whether to output members at <code>level</code>
   * @param after        Whether to output members below <code>level</code>
   * @param leaves       Whether to output members which are leaves
   * @param context      Evaluation context; determines criteria by which the result set should be filtered
   */
  static void descendantsByLevel(
    SchemaReader schemaReader,
    Member ancestor,
    Level level,
    List<Member> result,
    boolean before,
    boolean self,
    boolean after,
    boolean leaves,
    Evaluator context ) {
    // We find the descendants of a member by making breadth-first passes
    // down the hierarchy. Initially the list just contains the ancestor.
    // Then we find its children. We add those children to the result if
    // they fulfill the before/self/after conditions relative to the level.
    //
    // We add a child to the "fertileMembers" list if some of its children
    // might be in the result. Again, this depends upon the
    // before/self/after conditions.
    //
    // Note that for some member readers -- notably the
    // RestrictedMemberReader, when it is reading a ragged hierarchy -- the
    // children of a member do not always belong to the same level. For
    // example, the children of USA include WA (a state) and Washington
    // (a city). This is why we repeat the before/self/after logic for
    // each member.
    final int levelDepth = level.getDepth();
    List<Member> members = Collections.singletonList( ancestor );
    // Each pass, "fertileMembers" has the same contents as "members",
    // except that we omit members whose children we are not interested
    // in. We allocate it once, and clear it each pass, to save a little
    // memory allocation.
    if ( leaves ) {
      assert !before && !self && !after;
      do {
        List<Member> nextMembers = new ArrayList<>();
        for ( Member member : members ) {
          final int currentDepth = member.getLevel().getDepth();
          if(currentDepth == levelDepth) {
            result.add( member );
          }
          else  {
            List<Member> childMembers =
                    schemaReader.getMemberChildren( member, context );
            if ( childMembers.isEmpty() ) {
              // this member is a leaf -- add it
              if ( currentDepth <= levelDepth ) {
                result.add( member );
              }
            } else {
              // this member is not a leaf -- add its children
              // to the list to be considered next iteration
              if ( currentDepth <= levelDepth ) {
                nextMembers.addAll( childMembers );
              }
            }
          }
        }
        members = nextMembers;
      } while ( !members.isEmpty() );
    } else {
      List<Member> fertileMembers = new ArrayList<>();
      do {
        fertileMembers.clear();
        for ( Member member : members ) {
          final int currentDepth = member.getLevel().getDepth();
          if ( currentDepth == levelDepth ) {
            if ( self ) {
              result.add( member );
            }
            if ( after ) {
              // we are interested in member's children
              fertileMembers.add( member );
            }
          } else if ( currentDepth < levelDepth ) {
            if ( before ) {
              result.add( member );
            }
            fertileMembers.add( member );
          } else {
            if ( after ) {
              result.add( member );
              fertileMembers.add( member );
            }
          }
        }
        members =
          schemaReader.getMemberChildren( fertileMembers, context );
      } while ( !members.isEmpty() );
    }
  }

  /**
   * Enumeration of the flags allowed to the <code>DESCENDANTS</code> function.
   */
  enum Flag {
    SELF( true, false, false, false ),
    AFTER( false, true, false, false ),
    BEFORE( false, false, true, false ),
    BEFORE_AND_AFTER( false, true, true, false ),
    SELF_AND_AFTER( true, true, false, false ),
    SELF_AND_BEFORE( true, false, true, false ),
    SELF_BEFORE_AFTER( true, true, true, false ),
    LEAVES( false, false, false, true );

    private final boolean self;
    private final boolean after;
    private final boolean before;
    private final boolean leaves;

    Flag( boolean self, boolean after, boolean before, boolean leaves ) {
      this.self = self;
      this.after = after;
      this.before = before;
      this.leaves = leaves;
    }

    private static List<String> reservedWords=Stream.of(Flag.values()).map(Flag::name).toList();

    public static List<String> asReservedWords() {
    	return reservedWords;
    }
  }
}
