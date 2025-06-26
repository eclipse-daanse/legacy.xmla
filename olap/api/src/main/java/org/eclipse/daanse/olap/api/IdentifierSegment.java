/*
 * Licensed to Julian Hyde under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * Julian Hyde licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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

/**
 * Component in a compound identifier. It is described by its name and how
 * the name is quoted.
 *
 * <p>For example, the identifier
 * <code>[Store].USA.[New Mexico].&amp;[45]</code> has four segments:<ul>
 * <li>"Store", {@link Quoting#QUOTED}</li>
 * <li>"USA", {@link Quoting#UNQUOTED}</li>
 * <li>"New Mexico", {@link Quoting#QUOTED}</li>
 * <li>"45", {@link Quoting#KEY}</li>
 * </ul>
 *
 * <p>QUOTED and UNQUOTED segments are represented using a
 * {@link NameSegment NameSegment};
 * KEY segments are represented using a
 * {@link KeySegment KeySegment}.
 *
 * <p>To parse an identifier into a list of segments, use the method
 * {@link org.olap4j.mdx.IdentifierNode#parseIdentifier(String)} and then call
 * {@link org.olap4j.mdx.IdentifierNode#getSegmentList()} on the resulting
 * node.</p>
 *
 * @author jhyde
 */
public sealed interface IdentifierSegment permits KeyIdentifierSegment, NameIdentifierSegment  {

    /**
     * Returns a string representation of this Segment.
     *
     * <p>For example, "[Foo]", "&amp;[123]", "Abc".
     *
     * @return String representation of this Segment
     */
    String toString();

    /**
     * Appends a string representation of this Segment to a StringBuffer.
     *
     * @param buf StringBuffer
     */
    void toString(StringBuilder buf);

    /**
     * Returns the region of the source code which this Segment was created
     * from, if it was created by parsing.
     *
     * @return region of source code
     */
    ParseRegion getRegion();

    /**
     * Returns how this Segment is quoted.
     *
     * @return how this Segment is quoted
     */
    Quoting getQuoting();

    /**
     * Returns the name of this IdentifierSegment.
     * Returns {@code null} if this IdentifierSegment represents a key.
     *
     * @return name of this Segment
     */
    String getName();


}

// End IdentifierSegment.java
