/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*/
package org.eclipse.daanse.olap.api;

public interface ParseRegion {

	/**
	 * Return starting line number (1-based).
	 *
	 * @return 1-based starting line number
	 */
	int getStartLine();

	/**
	 * Return starting column number (1-based).
	 *
	 * @return 1-based starting column number
	 */
	int getStartColumn();

	/**
	 * Return ending line number (1-based).
	 *
	 * @return 1-based ending line number
	 */
	int getEndLine();

	/**
	 * Return ending column number (1-based).
	 *
	 * @return 1-based starting endings column number
	 */
	int getEndColumn();

}