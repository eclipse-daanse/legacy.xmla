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
package org.eclipse.daanse.olap.function.def.rank;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.element.Member;

public class TupleSortResult extends SortResult {
    /**
     * The precomputed rank associated with all tuples
     */
    final Map<List<Member>, Integer> rankMap;

    public TupleSortResult( Object[] values, Map<List<Member>, Integer> rankMap ) {
      super( values );
      this.rankMap = rankMap;
    }

    public Integer rankOf( Member[] tuple ) {
      return rankMap.get( Arrays.asList( tuple ) );
    }
  }
