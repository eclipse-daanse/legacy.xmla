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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.api;


//todo: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ScopedValue.html
//TODO: https://openjdk.org/jeps/462
public interface Locus {

	Execution getExecution();

	Context getContext();

}
