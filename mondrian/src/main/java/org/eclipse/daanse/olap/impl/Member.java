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

/**
 * <code>Member</code> is a data value in an OLAP Dimension.
 *
 * @author jhyde
 * @since Aug 22, 2006
 */
public interface Member extends MetadataElement {

    /**
     * Enumeration of tree operations which can be used when querying
     * members.
     *
     * <p>Some of the values are as specified by XMLA.
     * For example, XMLA specifies MDTREEOP_CHILDREN with ordinal 1,
     * which corresponds to the value {@link #CHILDREN}.
     *
     * @see org.olap4j.OlapDatabaseMetaData#getMembers
     */
    public enum TreeOp {
        /**
         * Tree operation which returns only the immediate children.
         */
        CHILDREN(
            1,
            "Tree operation which returns only the immediate children."),

        /**
         * Tree operation which returns members on the same level.
         */
        SIBLINGS(
            2,
            "Tree operation which returns members on the same level."),

        /**
         * Tree operation which returns only the immediate parent.
         */
        PARENT(
            4,
            "Tree operation which returns only the immediate parent."),

        /**
         * Tree operation which returns itself in the list of returned rows.
         */
        SELF(
            8,
            "Tree operation which returns itself in the list of returned "
                + "rows."),

        /**
         * Tree operation which returns all of the descendants.
         */
        DESCENDANTS(
            16,
            "Tree operation which returns all of the descendants."),

        /**
         * Tree operation which returns all of the ancestors.
         */
        ANCESTORS(
            32,
            "Tree operation which returns all of the ancestors.");

        private final int xmlaOrdinal;
        private String description;

        private TreeOp(int xmlaOrdinal, String description) {
            this.xmlaOrdinal = xmlaOrdinal;
            this.description = description;
        }

        public String xmlaName() {
            return "MDTREEOP_" + name();
        }

        public String getDescription() {
            return description;
        }

        public int xmlaOrdinal() {
            return xmlaOrdinal;
        }
    }
}

// End Member.java
