/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.api;

import java.util.concurrent.Callable;

import org.eclipse.daanse.olap.api.result.Result;

public interface ResultShepherd {

	/**
	 * Executes and shepherds the execution of an Execution instance.
	 * The shepherd will wrap the Execution instance into a Future object
	 * which can be monitored for exceptions. If any are encountered,
	 * two things will happen. First, the user thread will be returned and
	 * the resulting exception will bubble up. Second, the execution thread
	 * will attempt to do a graceful stop of all running SQL statements and
	 * release all other resources gracefully in the background.
	 * @param execution An Execution instance.
	 * @param callable A callable to monitor returning a Result instance.
	 * @throws mondrian.olap.ResourceLimitExceededException if some resource limit specified
	 * in the property file was exceeded
	 * @throws mondrian.olap.QueryCanceledException if query was canceled during execution
	 * @throws mondrian.olap.QueryTimeoutException if query exceeded timeout specified in
	 * the property file
	 * @return A Result object, as supplied by the Callable passed as a
	 * parameter.
	 */
	Result shepherdExecution(Execution execution, Callable<Result> callable);

	void shutdown();

}
