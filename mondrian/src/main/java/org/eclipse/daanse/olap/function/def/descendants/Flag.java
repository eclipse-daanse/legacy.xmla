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
package org.eclipse.daanse.olap.function.def.descendants;

import java.util.List;
import java.util.stream.Stream;

/**
* Enumeration of the flags allowed to the <code>DESCENDANTS</code> function.
*/
public enum Flag {
  SELF( true, false, false, false ),
  AFTER( false, true, false, false ),
  BEFORE( false, false, true, false ),
  BEFORE_AND_AFTER( false, true, true, false ),
  SELF_AND_AFTER( true, true, false, false ),
  SELF_AND_BEFORE( true, false, true, false ),
  SELF_BEFORE_AFTER( true, true, true, false ),
  LEAVES( false, false, false, true );

  final boolean self;
  final boolean after;
  final boolean before;
  final boolean leaves;

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
