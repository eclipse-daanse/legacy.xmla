/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara.  All rights reserved.
*/
package mondrian.olap.fun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.opencube.junit5.TestUtil.assertAxisReturns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.ArrayTupleList;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.UnaryTupleList;
import org.eclipse.daanse.olap.function.def.crossjoin.CrossJoinFunDef;
import org.eclipse.daanse.olap.function.def.crossjoin.ImmutableListCalc;
import org.eclipse.daanse.olap.function.def.union.UnionCalc;
import org.eclipse.daanse.olap.query.component.ResolvedFunCallImpl;
import org.eclipse.daanse.rolap.element.RolapMemberBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mockito;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Tests for UnionFunDef
 *
 * @author Yury Bakhmutski
 */
class UnionFunDefTest {

  /**
   * Test for MONDRIAN-2250 issue.
   * Tests that the result is independent on the hashCode.
   * For this purpose MemberForTest with rewritten hashCode is used.
   *
   * Tuples are gotten from customer attachments.
   */
  @Test
  void testMondrian2250() {
    Member[] dates = new Member[4];
    for (int i = 25; i < 29; i++) {
      dates[i - 25] =
          new MemberForTest("[Consumption Date.Calendar].[2014-07-" + i + "]");
    }
    List<Member> list = Arrays.asList(dates);
    UnaryTupleList unaryTupleList = new UnaryTupleList(list);

    Member consumptionMethod =
        new MemberForTest("[Consumption Method].[PVR]");
    Member measuresAverageTimeshift =
        new MemberForTest("[Measures].[Average Timeshift]");
    String[] hours = { "00", "14", "15", "16", "23" };
    Member[] times = new Member[5];
    for (int i = 0; i < hours.length; i++) {
      times[i] =
          new MemberForTest("[Consumption Time.Time].[" + hours[i] + ":00]");
    }

    int arity = 3;
    ArrayTupleList arrayTupleList = new ArrayTupleList(arity);
    for (Member time : times) {
      List<Member> currentList = new ArrayList(3);
      currentList.add(consumptionMethod);
      currentList.add(measuresAverageTimeshift);
      currentList.add(time);
      arrayTupleList.add(currentList);
    }

    CrossJoinFunDef crossJoinFunDef =
        new CrossJoinFunDef(new CrossJoinTest.NullFunDef().getFunctionMetaData());
    Expression[] expMock = new Expression[1];
    expMock[0] = mock(Expression.class);
    ResolvedFunCallImpl resolvedFunCall =
        new ResolvedFunCallImpl(mock(FunctionDefinition.class), expMock, mock(SetType.class));
    Calc[] calcs = new Calc[1];
    calcs[0] = Mockito.mock(Calc.class);
    ImmutableListCalc immutableListCalc =
        new ImmutableListCalc(
            resolvedFunCall, calcs, crossJoinFunDef.getCtag());

    TupleList listForUnion1 =
        immutableListCalc.makeList(unaryTupleList, arrayTupleList);

    List<Member> list2 = Arrays.asList(dates);
    UnaryTupleList unaryTupleList2 = new UnaryTupleList(list2);

    Member measuresTotalViewingTime =
        new MemberForTest("[Measures].[Total Viewing Time]");
    ArrayTupleList arrayTupleList2 = new ArrayTupleList(arity);
    for (Member time : times) {
      List<Member> currentList = new ArrayList(3);
      currentList.add(consumptionMethod);
      currentList.add(measuresTotalViewingTime);
      currentList.add(time);
      arrayTupleList2.add(currentList);
    }

    TupleList listForUnion2 =
        immutableListCalc.makeList(unaryTupleList2, arrayTupleList2);

    UnionCalc unionFunDefMock = mock(UnionCalc.class);
    doCallRealMethod().when(unionFunDefMock).union(
    		any(), any(), anyBoolean());

    TupleList tupleList =
        unionFunDefMock.union(listForUnion1, listForUnion2, false);
    System.out.println(tupleList);
    assertEquals(40, tupleList.size());
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testArity4TupleUnion(Context<?> context) {
    String tupleSet =
        "CrossJoin( [Customers].[USA].Children,"
        + " CrossJoin( Time.[1997].children, { (Gender.F, [Marital Status].M ) }) ) ";
    String expected =
        "{[Customers].[Customers].[USA].[CA], [Time].[Time].[1997].[Q1], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[CA], [Time].[Time].[1997].[Q2], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[CA], [Time].[Time].[1997].[Q3], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[CA], [Time].[Time].[1997].[Q4], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[OR], [Time].[Time].[1997].[Q1], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[OR], [Time].[Time].[1997].[Q2], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[OR], [Time].[Time].[1997].[Q3], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[OR], [Time].[Time].[1997].[Q4], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[WA], [Time].[Time].[1997].[Q1], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[WA], [Time].[Time].[1997].[Q2], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[WA], [Time].[Time].[1997].[Q3], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[USA].[WA], [Time].[Time].[1997].[Q4], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}";

    assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales", "Union( " + tupleSet + ", " + tupleSet + ")", expected);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testArity5TupleUnion(Context<?> context) {
    String tupleSet = "CrossJoin( [Customers].[Canada].Children, "
        + "CrossJoin( [Time].[1997].lastChild, "
        + "CrossJoin ([Education Level].children,{ (Gender.F, [Marital Status].M ) })) )";
    String expected =
        "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q4], [Education Level].[Education Level].[Bachelors Degree], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q4], [Education Level].[Education Level].[Graduate Degree], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q4], [Education Level].[Education Level].[High School Degree], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q4], [Education Level].[Education Level].[Partial College], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q4], [Education Level].[Education Level].[Partial High School], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}";
    Connection connection = context.getConnectionWithDefaultRole();
    assertAxisReturns(connection, "Sales", tupleSet, expected);

    assertAxisReturns(connection, "Sales", "Union( " + tupleSet + ", " + tupleSet + ")", expected);
  }

  void testArity5TupleUnionAll(Context<?> context) {
    String tupleSet = "CrossJoin( [Customers].[Canada].Children, "
        + "CrossJoin( [Time].[1998].firstChild, "
        + "CrossJoin ([Education Level].members,{ (Gender.F, [Marital Status].M ) })) )";
    String expected =
        "{[Customers].[Canada].[BC], [Time].[1998].[Q1], [Education Level].[All Education Levels], [Gender].[F], [Marital Status].[M]}\n"
        + "{[Customers].[Canada].[BC], [Time].[1998].[Q1], [Education Level].[Bachelors Degree], [Gender].[F], [Marital Status].[M]}\n"
        + "{[Customers].[Canada].[BC], [Time].[1998].[Q1], [Education Level].[Graduate Degree], [Gender].[F], [Marital Status].[M]}\n"
        + "{[Customers].[Canada].[BC], [Time].[1998].[Q1], [Education Level].[High School Degree], [Gender].[F], [Marital Status].[M]}\n"
        + "{[Customers].[Canada].[BC], [Time].[1998].[Q1], [Education Level].[Partial College], [Gender].[F], [Marital Status].[M]}\n"
        + "{[Customers].[Canada].[BC], [Time].[1998].[Q1], [Education Level].[Partial High School], [Gender].[F], [Marital Status].[M]}";

    Connection connection = context.getConnectionWithDefaultRole();
    assertAxisReturns(connection, "Sales", tupleSet, expected);

    assertAxisReturns(connection, "Sales",
        "Union( " + tupleSet + ", " + tupleSet + ", " + "ALL" + ")",
        expected + "\n" + expected);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testArity6TupleUnion(Context<?> context) {
    String tupleSet1 = "CrossJoin( [Customers].[Canada].Children, "
        + "CrossJoin( [Time].[1997].firstChild, "
        + "CrossJoin ([Education Level].lastChild,"
        + "CrossJoin ([Yearly Income].lastChild,"
        + "{ (Gender.F, [Marital Status].M ) })) ) )";
    String tupleSet2 = "CrossJoin( [Customers].[Canada].Children, "
        + "CrossJoin( [Time].[1997].firstChild, "
        + "CrossJoin ([Education Level].lastChild,"
        + "CrossJoin ([Yearly Income].children,"
        + "{ (Gender.F, [Marital Status].M ) })) ) )";

    String tupleSet1Expected =
        "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$90K - $110K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}";
    Connection connection = context.getConnectionWithDefaultRole();
    assertAxisReturns(connection, "Sales", tupleSet1, tupleSet1Expected);

    String tupleSet2Expected =
        "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$10K - $30K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$110K - $130K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$130K - $150K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$150K +], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$30K - $50K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$50K - $70K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$70K - $90K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + tupleSet1Expected;

    assertAxisReturns(connection, "Sales", tupleSet2, tupleSet2Expected);

    assertAxisReturns(connection, "Sales",
        "Union( " + tupleSet2 + ", " + tupleSet1 + ")",
        tupleSet2Expected);
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testArity6TupleUnionAll(Context<?> context) {
    String tupleSet1 = "CrossJoin( [Customers].[Canada].Children, "
        + "CrossJoin( [Time].[1997].firstChild, "
        + "CrossJoin ([Education Level].lastChild,"
        + "CrossJoin ([Yearly Income].lastChild,"
        + "{ (Gender.F, [Marital Status].M ) })) ) )";
    String tupleSet2 = "CrossJoin( [Customers].[Canada].Children, "
        + "CrossJoin( [Time].[1997].firstChild, "
        + "CrossJoin ([Education Level].lastChild,"
        + "CrossJoin ([Yearly Income].children,"
        + "{ (Gender.F, [Marital Status].M ) })) ) )";

    String tupleSet1Expected =
        "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$90K - $110K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}";
    Connection connection = context.getConnectionWithDefaultRole();
    assertAxisReturns(connection, "Sales", tupleSet1, tupleSet1Expected);

    String tupleSet2Expected =
        "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$10K - $30K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$110K - $130K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$130K - $150K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$150K +], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$30K - $50K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$50K - $70K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + "{[Customers].[Customers].[Canada].[BC], [Time].[Time].[1997].[Q1], [Education Level].[Education Level].[Partial High School], [Yearly Income].[Yearly Income].[$70K - $90K], [Gender].[Gender].[F], [Marital Status].[Marital Status].[M]}\n"
        + tupleSet1Expected;
    assertAxisReturns(connection, "Sales", tupleSet2, tupleSet2Expected);

    assertAxisReturns(connection, "Sales",
            "Union( " + tupleSet1 + ", " + tupleSet2 + ", " + "ALL" + ")",
        tupleSet1Expected + "\n" + tupleSet2Expected);
  }

  private class MemberForTest extends RolapMemberBase {
    private String identifer;

    public MemberForTest(String identifer) {
      this.identifer = identifer;
    }

    @Override
    public String getUniqueName() {
      return identifer;
    }

    @Override
    public int hashCode() {
      return 31;
    }
  }
}
