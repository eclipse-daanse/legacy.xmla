/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package org.eclipse.daanse.olap.impl;

import java.util.AbstractList;
import java.util.List;

import org.eclipse.daanse.olap.api.IdentifierSegment;
import org.eclipse.daanse.olap.api.ParseRegion;

/**
 * Multi-part identifier.
 *
 * <p>An identifier is immutable.
 *
 * <p>An identifer consists of one or more {@link IdentifierSegment}s. A segment
 * is either:<ul>
 * <li>An unquoted value such as '{@code CA}',
 * <li>A value quoted in brackets, such as '{@code [San Francisco]}', or
 * <li>A key of one or more parts, each of which is prefixed with '&amp;',
 *     such as '{@code &amp;[Key 1]&amp;Key2&amp;[5]}'.
 * </ul>
 *
 * <p>Segment types are indicated by the {@link Quoting} enumeration.
 *
 * <p>A key segment is of type {@link Quoting#KEY}, and has one or more
 * component parts accessed via the
 * {@link IdentifierSegment#getKeyParts()} method. The parts
 * are of type {@link Quoting#UNQUOTED} or {@link Quoting#QUOTED}.
 *
 * <p>A simple example is the identifier {@code Measures.[Unit Sales]}. It
 * has two segments:<ul>
 * <li>Segment #0 is
 *     {@link Quoting#UNQUOTED UNQUOTED},
 *     name "Measures"</li>
 * <li>Segment #1 is
 *     {@link Quoting#QUOTED QUOTED},
 *     name "Unit Sales"</li>
 * </ul>
 *
 * <p>A more complex example illustrates a compound key. The identifier {@code
 * [Customers].[City].&amp;[San Francisco]&amp;CA&amp;USA.&amp;[cust1234]}
 * contains four segments as follows:
 * <ul>
 * <li>Segment #0 is QUOTED, name "Customers"</li>
 * <li>Segment #1 is QUOTED, name "City"</li>
 * <li>Segment #2 is a {@link Quoting#KEY KEY}.
 *     It has 3 sub-segments:
 *     <ul>
 *     <li>Sub-segment #0 is QUOTED, name "San Francisco"</li>
 *     <li>Sub-segment #1 is UNQUOTED, name "CA"</li>
 *     <li>Sub-segment #2 is UNQUOTED, name "USA"</li>
 *     </ul>
 * </li>
 * <li>Segment #3 is a KEY. It has 1 sub-segment:
 *     <ul>
 *     <li>Sub-segment #0 is QUOTED, name "cust1234"</li>
 *     </ul>
 * </li>
 * </ul>
 *
 * @author jhyde
 */
public class IdentifierNode
{

    private final List<IdentifierSegment> segments;

    /**
     * Creates an identifier containing one or more segments.
     *
     * @param segments Array of Segments, each consisting of a name and quoting
     * style
     */
    public IdentifierNode(IdentifierSegment... segments) {
        if (segments.length < 1) {
            throw new IllegalArgumentException();
        }
        this.segments = UnmodifiableArrayList.asCopyOf(segments);
    }


    /**
     * Creates an identifier containing a list of segments.
     *
     * @param segments List of segments
     */
    public IdentifierNode(List<IdentifierSegment> segments) {
        if (segments.size() < 1) {
            throw new IllegalArgumentException();
        }
        this.segments =
            new UnmodifiableArrayList<IdentifierSegment>(
                segments.toArray(
                    new IdentifierSegment[segments.size()]));
    }
    /**
     * Returns string quoted in [...].
     *
     * <p>For example, "San Francisco" becomes
     * "[San Francisco]"; "a [bracketed] string" becomes
     * "[a [bracketed]] string]".
     *
     * @param id Unquoted name
     * @return Quoted name
     */
    static String quoteMdxIdentifier(String id) {
        StringBuilder buf = new StringBuilder(id.length() + 20);
        quoteMdxIdentifier(id, buf);
        return buf.toString();
    }

    /**
     * Returns a string quoted in [...], writing the results to a
     * {@link StringBuilder}.
     *
     * @param id Unquoted name
     * @param buf Builder to write quoted string to
     */
    static void quoteMdxIdentifier(String id, StringBuilder buf) {
        buf.append('[');
        int start = buf.length();
        buf.append(id);
        Olap4jUtil.replace(buf, start, "]", "]]");
        buf.append(']');
    }


    /**
     * Returns a region encompassing the regions of the first through the last
     * of a list of segments.
     *
     * @param segments List of segments
     * @return Region encompassed by list of segments
     */
    static ParseRegion sumSegmentRegions(
        final List<? extends IdentifierSegment> segments)
    {
        return ParseRegionImpl.sum(
            new AbstractList<ParseRegion>() {
                public ParseRegion get(int index) {
                    return segments.get(index).getRegion();
                }

                public int size() {
                    return segments.size();
                }
            });
    }
}

// End IdentifierNode.java
