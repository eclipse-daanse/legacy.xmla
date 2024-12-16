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

public abstract class SortResult {
    /**
     * All values in sorted order; Duplicates are not removed. E.g. Set (15,15,5,0) 10 should be ranked 3.
     *
     * <p>
     * Null values are not present: they would be at the end, anyway.
     */
    final Object[] values;

    public SortResult( Object[] values ) {
      this.values = values;
    }

    public boolean isEmpty() {
      return values == null;
    }

    public void log() {
      if ( values == null ) {
          RankFunDef.LOGGER.debug( "SortResult: empty" );
      } else {
        StringBuilder sb = new StringBuilder();
          sb.append( "SortResult {" );
        for ( int i = 0; i < values.length; i++ ) {
          if ( i > 0 ) {
              sb.append( "\n," );
          }
          Object value = values[i];
            sb.append( value );
        }
        sb.append( "}" );
        String msg = sb.toString();
        RankFunDef.LOGGER.debug(msg);
      }

    }
  }
