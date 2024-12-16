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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.element.Member;

public class RankedMemberList{
    Map<Member, Integer> map = new HashMap<>();

    RankedMemberList( List<Member> members ) {
      int i = -1;
      for ( final Member member : members ) {
        ++i;
        final Integer value = map.put( member, i );
        if ( value != null ) {
          // The list already contained a value for this key -- put
          // it back.
          map.put( member, value );
        }
      }
    }

    int indexOf( Member m ) {
      Integer integer = map.get( m );
      if ( integer == null ) {
        return -1;
      } else {
        return integer;
      }
    }
  }
