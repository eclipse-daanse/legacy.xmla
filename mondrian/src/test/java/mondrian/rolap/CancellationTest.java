/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opencube.junit5.TestUtil.cubeByName;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.productMembersPotScrubbersPotsAndPans;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.function.def.crossjoin.CrossJoinFunDef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.fun.CrossJoinTest;
import mondrian.server.ExecutionImpl;
import mondrian.server.LocusImpl;

import java.util.Optional;

class CancellationTest {

    @BeforeEach
    public void beforeEach() {

    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    /**
     * Creates a cell region, runs a query, then flushes the cache.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonEmptyListCancellation(Context context) throws OlapRuntimeException {
        // tests that cancellation/timeout is checked in
        // CrossJoinFunDef.nonEmptyList
        ((TestConfig)context.getConfig()).setCheckCancelOrTimeoutInterval(1);
        CrossJoinFunDefTester crossJoinFunDef =
                new CrossJoinFunDefTester(new CrossJoinTest.NullFunDef().getFunctionMetaData());
        Result result =
            executeQuery(context.getConnectionWithDefaultRole(), "select store.[store name].members on 0 from sales");
        Evaluator eval = ((RolapResult) result).getEvaluator(new int[]{0});
        TupleList list = new UnaryTupleList();
        for (Position pos : result.getAxes()[0].getPositions()) {
            list.add(pos);
        }
        ExecutionImpl exec = spy(new ExecutionImpl(eval.getQuery().getStatement(), Optional.empty()));
        eval.getQuery().getStatement().start(exec);
        CrossJoinFunDef.nonEmptyList(eval, list, null, crossJoinFunDef.getCtag());
        // checkCancelOrTimeout should be called once
        // for each tuple since phase interval is 1
        verify(exec, times(list.size())).checkCancelOrTimeout();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testMutableCrossJoinCancellation(Context context) throws OlapRuntimeException {
        // tests that cancellation/timeout is checked in
        // CrossJoinFunDef.mutableCrossJoin
        ((TestConfig)context.getConfig()).setCheckCancelOrTimeoutInterval(1);
        Connection connection = context.getConnectionWithDefaultRole();
        RolapCube salesCube = (RolapCube) cubeByName(
             connection,
            "Sales");
        SchemaReader salesCubeSchemaReader =
            salesCube.getSchemaReader(
                    connection.getRole()).withLocus();

        TupleList productMembers =
            productMembersPotScrubbersPotsAndPans(salesCubeSchemaReader);

        String selectGenders = "select Gender.members on 0 from sales";
        Result genders = executeQuery(connection, selectGenders);

        Evaluator gendersEval =
            ((RolapResult) genders).getEvaluator(new int[]{0});
        TupleList genderMembers = new UnaryTupleList();
        for (Position pos : genders.getAxes()[0].getPositions()) {
            genderMembers.add(pos);
        }

        ExecutionImpl execution =
            spy(new ExecutionImpl(genders.getQuery().getStatement(), Optional.empty()));
        TupleList mutableCrossJoinResult =
            mutableCrossJoin(productMembers, genderMembers, execution);

        gendersEval.getQuery().getStatement().start(execution);

        // checkCancelOrTimeout should be called once
        // for each tuple from mutableCrossJoin since phase interval is 1
        // plus once for each productMembers item
        // since it gets through SqlStatement.execute
        int expectedCallsQuantity =
            mutableCrossJoinResult.size() + productMembers.size();
        verify(execution, times(expectedCallsQuantity)).checkCancelOrTimeout();
    }

    private TupleList mutableCrossJoin(
        final TupleList list1, final TupleList list2, final ExecutionImpl execution)
        {
            return LocusImpl.execute(
                execution, "CancellationTest",
                new LocusImpl.Action<TupleList>() {
                    @Override
					public TupleList execute() {
                        return CrossJoinFunDef.mutableCrossJoin(list1, list2);
                    }
                });
        }

    class CrossJoinFunDefTester extends CrossJoinFunDef {
        public CrossJoinFunDefTester(FunctionMetaData functionMetaData) {
            super(functionMetaData);
        }

        //@Override
		//public TupleList nonEmptyList(
        //    Evaluator evaluator,
        //    TupleList list,
        //    ResolvedFunCall call)
        //{
        //    return super.nonEmptyList(evaluator, list, call);
        //}
    }
}
