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
package org.eclipse.daanse.olap.api;

import java.time.Duration;

public interface QueryTiming {

	/**
	 * Marks the start of a Query component's execution.
	 *
	 * @param name Name of the component
	 */
	void markStart(String name);

	/**
	 * Marks the end of a Query component's execution.
	 *
	 * @param name Name of the component
	 */
	void markEnd(String name);

	/**
	 * Marks the duration of a Query component's execution.
	 *
	 * markFull is synchronized because it may be called from either Actor's spawn
	 * thread or RolapResultShepherd thread
	 *
	 * @param name     Name of the component
	 * @param duration Duration of the execution
	 */

	void markFull(String string, Duration duration);

}