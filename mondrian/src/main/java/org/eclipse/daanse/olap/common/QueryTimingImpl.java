/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.common;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.daanse.olap.api.QueryTiming;

import mondrian.util.ArrayStack;

/**
 * Provides hooks for recording timing information of components of Query
 * execution.
 *
 * <p>
 * NOTE: This class is experimental and subject to change/removal without
 * notice.
 *
 * <p>
 * Code that executes as part of a Query can call
 * {@link QueryTimingImpl#markStart(String)} before executing, and
 * {@link QueryTimingImpl#markEnd(String)} afterwards, or can track execution
 * times manually and call {@link QueryTimingImpl#markFull(String, Duration)}.
 *
 * <p>
 * To read timing information, add a handler to the statement using
 * {@link mondrian.server.Statement#enableProfiling} and implement the
 * {@link org.eclipse.daanse.olap.api.ProfileHandler#explain(String, QueryTimingImpl)} method.
 *
 * @author jbarnett
 */
public class QueryTimingImpl implements QueryTiming {
	private boolean enabled;
	private final ArrayStack<TimingInfo> currentTimings = new ArrayStack<>();
	// Tracks Query components that are already on the stack so that we don't double
	// count their durations
	private final HashMap<String, Integer> currentTimingDepth = new HashMap<>();
	private final Map<String, DurationCount> timings = new HashMap<>();
	private final Map<String, DurationCount> fullTimings = new HashMap<>();

	/**
	 * Initializes (or re-initializes) a query timing, also setting whether enabled.
	 * All previous stats are removed.
	 *
	 * @param enabled Whether to collect stats in future
	 */
	public void init(boolean enabled) {
		this.enabled = enabled;
		currentTimings.clear();
		timings.clear();
		fullTimings.clear();
	}

	public void done() {
	}

	/**
	 * Marks the start of a Query component's execution.
	 *
	 * @param name Name of the component
	 */
	@Override
	public final void markStart(String name) {
		if (enabled) {
			markStartInternal(name);
		}
	}

	/**
	 * Marks the end of a Query component's execution.
	 *
	 * @param name Name of the component
	 */
	@Override
	public final void markEnd(String name) {
		if (enabled) {
			Instant tstamp = Instant.now();
			markEndInternal(name, tstamp);
		}
	}

	public synchronized final void markFull(String name, Duration duration) {
		if (enabled) {
			markFullInternal(name, duration);
		}
	}

	private void markStartInternal(String name) {
		currentTimings.push(new TimingInfo(name));
		Integer depth = currentTimingDepth.get(name);
		if (depth == null) {
			currentTimingDepth.put(name, 1);
		} else {
			currentTimingDepth.put(name, depth + 1);
		}
	}

	private void markEndInternal(String name, Instant tstamp) {
		if (currentTimings.isEmpty() || !currentTimings.peek().name.equals(name)) {
			throw new IllegalStateException("end but no start for " + name);
		}
		TimingInfo finished = currentTimings.pop();
		assert finished.name.equals(name);
		finished.markEnd(tstamp);

		DurationCount durationCount = timings.get(finished.name);
		if (durationCount == null) {
			durationCount = new DurationCount();
			timings.put(finished.name, durationCount);
		}
		durationCount.count++;
		Integer depth = currentTimingDepth.get(name);
		if (depth == 1) {
			Duration dur = Duration.between(finished.startTime, finished.endTime);
			durationCount.duration=durationCount.duration.plus(dur);
		}
		currentTimingDepth.put(name, depth - 1);

	}

	private void markFullInternal(String name, Duration duration) {
		DurationCount p = fullTimings.get(name);
		if (p == null) {
			p = new DurationCount();
			fullTimings.put(name, p);
		}
		p.count++;
		p.duration=p.duration.plus(duration);
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Query Timing (Cumulative):");
		for (Map.Entry<String, DurationCount> entry : timings.entrySet()) {
			sb.append(Util.NL);
			sb.append(entry.getKey()).append(" invoked ").append(entry.getValue().count).append(" times for total of ")
					.append(entry.getValue().duration.toMillis()).append("ms.  (Avg. ")
					.append(entry.getValue().duration.dividedBy(entry.getValue().count).toMillis())
					.append("ms/invocation)");
		}
		for (Map.Entry<String, DurationCount> entry : fullTimings.entrySet()) {
			if (sb.length() > 0) {
				sb.append(Util.NL);
			}
			sb.append(entry.getKey()).append(" invoked ").append(entry.getValue().count).append(" times for total of ")
					.append(entry.getValue().duration.toMillis()).append("ms.  (Avg. ")
					.append(entry.getValue().duration.dividedBy(entry.getValue().count).toMillis())
					.append("ms/invocation)");
		}
		sb.append(Util.NL);
		return sb.toString();
	}

	private static class TimingInfo {
		private final String name;
		private final Instant startTime;
		private Instant endTime;

		private TimingInfo(String name) {
			this.name = name;
			this.startTime = Instant.now();
		}

		private void markEnd(Instant tstamp) {
			this.endTime = tstamp;
		}
	}

	private static class DurationCount {
		Duration duration= Duration.ZERO;
		long count;
	}
}
