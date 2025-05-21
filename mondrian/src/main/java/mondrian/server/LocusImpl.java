/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2020 Hitachi Vantara..  All rights reserved.
*/

package mondrian.server;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Execution;
import org.eclipse.daanse.olap.api.Locus;
import org.eclipse.daanse.olap.api.Statement;

import mondrian.olap.ExecuteDurationUtil;
import mondrian.rolap.RolapConnection;
import mondrian.util.ArrayStack;

import java.util.Optional;

//TODO: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ScopedValue.html
//TODO: https://openjdk.org/jeps/462
/**
 * Point of execution from which a service is invoked.
 */
public class LocusImpl implements Locus {
    private final Execution execution;
    public final String message;
    public final String component;

    private static final ThreadLocal<ArrayStack<Locus>> THREAD_LOCAL =
        ThreadLocal.withInitial(ArrayStack::new);

    /**
     * Creates a Locus.
     *
     * @param execution Execution context
     * @param component Description of a the component executing the query,
     *   generally a method name, e.g. "SqlTupleReader.readTuples"
     * @param message Description of the purpose of this statement, to be
     *   printed if there is an error
     */
    public LocusImpl(
    	Execution execution,
        String component,
        String message)
    {
        if (execution == null) {
            throw new IllegalArgumentException("execution should not be null");
        }

        this.execution = execution;
        this.component = component;
        this.message = message;
    }

    public static void pop(Locus locus) {
        final Locus pop = THREAD_LOCAL.get().pop();
        if (locus != pop) {
            throw new IllegalArgumentException("locus should be equals pop");
        }
    }

    public static void push(Locus locus) {
        THREAD_LOCAL.get().push(locus);
    }

    public static Locus peek() {
        return THREAD_LOCAL.get().peek();
    }

    public static boolean isEmpty() {
      return THREAD_LOCAL.get().isEmpty();
    }

    public static <T> T execute(
        Connection connection,
        String component,
        Action<T> action)
    {
        final Statement statement = connection.getInternalStatement();
        final ExecutionImpl execution = new ExecutionImpl(statement,
                ExecuteDurationUtil.executeDurationValue(connection.getContext()));
        return execute(execution, component, action);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public static <T> T execute(
        Execution execution,
        String component,
        Action<T> action)
    {
        final Locus locus =
            new LocusImpl(
                execution,
                component,
                null);
        LocusImpl.push(locus);
        try {
            return action.execute();
        } finally {
            LocusImpl.pop(locus);
        }
    }

    public final Context getContext() {
        return getExecution().getMondrianStatement().getMondrianConnection().getContext();
    }

    @Override
    public Execution getExecution() {
		return execution;
	}

	public interface Action<T> {
        T execute();
    }
}
