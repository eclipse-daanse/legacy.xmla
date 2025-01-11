/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2020 Hitachi Vantara..  All rights reserved.
*/
package mondrian.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.daanse.olap.api.BasicContextConfig;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import mondrian.olap.SystemWideProperties;
import mondrian.rolap.RolapConnection;
import mondrian.server.ExecutionImpl;

class CancellationCheckerTest {


    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    private ExecutionImpl excMock = mock(ExecutionImpl.class);

    @Test
    void testCheckCancelOrTimeoutWithIntExecution() {
        int currentIteration = 10;
        prepareCheckCancelOrTimeoutInterval(1);
        CancellationChecker.checkCancelOrTimeout(currentIteration, excMock);
        verify(excMock).checkCancelOrTimeout();
    }

    @Test
    void testCheckCancelOrTimeoutWithLongExecution() {
        long currentIteration = 10L;
        prepareCheckCancelOrTimeoutInterval(1);
        CancellationChecker.checkCancelOrTimeout(currentIteration, excMock);
        verify(excMock).checkCancelOrTimeout();
    }

    @Test
    void testCheckCancelOrTimeoutLongMoreThanIntExecution() {
        long currentIteration = 2147483648L;
        prepareCheckCancelOrTimeoutInterval(1);
        CancellationChecker.checkCancelOrTimeout(currentIteration, excMock);
        verify(excMock).checkCancelOrTimeout();
    }

    @Test
    void testCheckCancelOrTimeoutMaxLongExecution() {
        long currentIteration = 9223372036854775807L;
        prepareCheckCancelOrTimeoutInterval(1);
        CancellationChecker.checkCancelOrTimeout(currentIteration, excMock);
        verify(excMock).checkCancelOrTimeout();
    }

    @Test
    void testCheckCancelOrTimeoutNoExecution_IntervalZero() {
        int currentIteration = 10;
        prepareCheckCancelOrTimeoutInterval(0);
        CancellationChecker.checkCancelOrTimeout(currentIteration, excMock);
        verify(excMock, never()).checkCancelOrTimeout();
    }

    @Test
    void testCheckCancelOrTimeoutNoExecutionEvenIntervalOddIteration() {
        int currentIteration = 3;
        prepareCheckCancelOrTimeoutInterval(10);
        CancellationChecker.checkCancelOrTimeout(currentIteration, excMock);
        verify(excMock, never()).checkCancelOrTimeout();
    }

    private void prepareCheckCancelOrTimeoutInterval(int i) {
        Statement statement = mock(Statement.class);
        RolapConnection rolapConnection = mock(RolapConnection.class);
        Context context = mock(Context.class);
        BasicContextConfig config = mock(BasicContextConfig.class);
        when(config.checkCancelOrTimeoutInterval()).thenReturn(i);
        when(context.getConfig()).thenReturn(config);
        when(rolapConnection.getContext()).thenReturn(context);
        when(statement.getMondrianConnection()).thenReturn(rolapConnection);
        when(excMock.getMondrianStatement()).thenReturn(statement);
    }

}
