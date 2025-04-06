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
package org.eclipse.daanse.olap.api.result;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public interface Property {

    String getName();

    Datatype getDatatype();

    Set<Property.TypeFlag> getType();

    String getCaption();



    /**
     * Enumeration of aspects of the type of a Property. In particular, whether
     * it belongs to a member or a cell.
     *
     * <p>The values are as specified by XMLA for the PROPERTY_TYPE attribute
     * of the MDSCHEMA_PROPERTIES data set.
     * For example, XMLA specifies that the value 9 (0x1 | 0x8) means that a
     * property belongs to a member and is a binary large object (BLOB).
     * In this case, {@link Property#getType} will return the {@link Set}
     * {{@link #MEMBER}, {@link #BLOB}}.
     */
    enum TypeFlag {
        /**
         * Identifies a property of a member. This property can be used in the
         * DIMENSION PROPERTIES clause of the SELECT statement.
         */
        MEMBER(1),

        /**
         * Identifies a property of a cell. This property can be used in the
         * CELL PROPERTIES clause that occurs at the end of the SELECT
         * statement.
         */
        CELL(2),

        /**
         * Identifies an internal property.
         */
        SYSTEM(4),

        /**
         * Identifies a property which contains a binary large object (blob).
         */
        BLOB(8);

        private final int xmlaOrdinal;

        public static final Set<Property.TypeFlag> CELL_TYPE_FLAG =
            Collections.unmodifiableSet(
            		EnumSet.of(Property.TypeFlag.CELL));
        public static final Set<Property.TypeFlag> MEMBER_TYPE_FLAG =
            Collections.unmodifiableSet(
            		EnumSet.of(Property.TypeFlag.MEMBER));



        private TypeFlag(int xmlaOrdinal) {
            this.xmlaOrdinal = xmlaOrdinal;
        }

        public String xmlaName() {
            return "MDPROP_" + name();
        }

        public String getDescription() {
            return null;
        }

        public int xmlaOrdinal() {
            return xmlaOrdinal;
        }

    }


    /**
     * Enumeration of the system properties available for every .
     *
     * <p>The following properties are mandatory for members:<ul>
     * <li>{@link #CATALOG_NAME}</li>
     * <li>{@link #SCHEMA_NAME}</li>
     * <li>{@link #CUBE_NAME}</li>
     * <li>{@link #DIMENSION_UNIQUE_NAME}</li>
     * <li>{@link #HIERARCHY_UNIQUE_NAME}</li>
     * <li>{@link #LEVEL_UNIQUE_NAME}</li>
     * <li>{@link #LEVEL_NUMBER}</li>
     * <li>{@link #MEMBER_UNIQUE_NAME}</li>
     * <li>{@link #MEMBER_NAME}</li>
     * <li>{@link #MEMBER_TYPE}</li>
     * <li>{@link #MEMBER_GUID}</li>
     * <li>{@link #MEMBER_CAPTION}</li>
     * <li>{@link #MEMBER_ORDINAL}</li>
     * <li>{@link #CHILDREN_CARDINALITY}</li>
     * <li>{@link #PARENT_LEVEL}</li>
     * <li>{@link #PARENT_UNIQUE_NAME}</li>
     * <li>{@link #PARENT_COUNT}</li>
     * <li>{@link #DESCRIPTION}</li>
     * </ul>
     */
    enum StandardMemberProperty implements Property {

        /**
         * Definition of the property which
         * holds the name of the current catalog.
         */
        CATALOG_NAME(
            Datatype.STRING,
            false,
            null,
            "Optional. The name of the catalog to which this member belongs. "
                + "NULL if the provider does not support catalogs."),

        /**
         * Definition of the property which
         * holds the name of the current schema.
         */
        SCHEMA_NAME(
            Datatype.STRING,
            false,
            null,
            "Optional. The name of the schema to which this member belongs. "
                + "NULL if the provider does not support schemas."),

        /**
         * Definition of the property which
         * holds the name of the current cube.
         */
        CUBE_NAME(
            Datatype.STRING,
            false,
            null, "Required. Name of the cube to which this member belongs."),

        /**
         * Definition of the property which
         * holds the unique name of the current dimension.
         */
        DIMENSION_UNIQUE_NAME(
            Datatype.STRING,
            false,
            null,
            "Required. Unique name of the dimension to which this member "
                + "belongs. For providers that generate unique names by "
                + "qualification, each component of this name is delimited."),

        /**
         * Definition of the property which
         * holds the unique name of the current hierarchy.
         */
        HIERARCHY_UNIQUE_NAME(
            Datatype.STRING,
            false,
            null,
            "Required. Unique name of the hierarchy. If the member belongs to "
                + "more than one hierarchy, there is one row for each hierarchy "
                + "to which it belongs. For providers that generate unique names "
                + "by qualification, each component of this name is delimited."),

        /**
         * Definition of the property which
         * holds the unique name of the current level.
         */
        LEVEL_UNIQUE_NAME(
            Datatype.STRING,
            false,
            null,
            "Required. Unique name of the level to which the member belongs. "
                + "For providers that generate unique names by qualification, "
                + "each component of this name is delimited."),

        /**
         * Definition of the property which
         * holds the ordinal of the current level.
         */
        LEVEL_NUMBER(
            Datatype.UNSIGNED_INTEGER,
            false,
            null,
            "Required. The distance of the member from the root of the "
                + "hierarchy. The root level is zero."),

        /**
         * Definition of the property which
         * holds the ordinal of the current member.
         */
        MEMBER_ORDINAL(
            Datatype.UNSIGNED_INTEGER,
            false,
            null,
            "Required. Ordinal number of the member. Sort rank of the member "
                + "when members of this dimension are sorted in their natural "
                + "sort order. If providers do not have the concept of natural "
                + "ordering, this should be the rank when sorted by MEMBER_NAME."),

        /**
         * Definition of the property which
         * holds the name of the current member.
         */
        MEMBER_NAME(
            Datatype.STRING,
            false,
            null,
            "Required. Name of the member."),

        /**
         * Definition of the property which
         * holds the unique name of the current member.
         */
        MEMBER_UNIQUE_NAME(
            Datatype.STRING,
            false,
            null,
            "Required. Unique name of the member. For providers that generate "
                + "unique names by qualification, each component of this name is "
                + "delimited."),

        /**
         * Definition of the property which
         * holds the type of the member.
         */
        MEMBER_TYPE(
            Datatype.STRING,
            false,
            null,
            "Required. Type of the member. Can be one of the following values: "
                + "MDMEMBER_Datatype.TYPE_REGULAR, MDMEMBER_Datatype.TYPE_ALL, "
                + "MDMEMBER_Datatype.TYPE_FORMULA, MDMEMBER_Datatype.TYPE_MEASURE, "
                + "MDMEMBER_Datatype.TYPE_UNKNOWN. MDMEMBER_Datatype.TYPE_FORMULA "
                + "takes precedence over MDMEMBER_Datatype.TYPE_MEASURE. "
                + "Therefore, if there is a formula (calculated) member on the "
                + "Measures dimension, it is listed as "
                + "MDMEMBER_Datatype.TYPE_FORMULA."),

        /**
         * Definition of the property which
         * holds the GUID of the member
         */
        MEMBER_GUID(
            Datatype.STRING,
            false,
            null,
            "Optional. Member GUID. NULL if no GUID exists."),

        /**
         * Definition of the property which
         * holds the label or caption associated with the member, or the
         * member's name if no caption is defined.
         */
        MEMBER_CAPTION(
            Datatype.STRING,
            false,
            null,
            "Required. A label or caption associated with the member. Used "
                + "primarily for display purposes. If a caption does not exist, "
                + "MEMBER_NAME is returned."),

        /**
         * Definition of the property which holds the
         * number of children this member has.
         */
        CHILDREN_CARDINALITY(
            Datatype.UNSIGNED_INTEGER,
            false,
            null,
            "Required. Number of children that the member has. This can be an "
                + "estimate, so consumers should not rely on this to be the exact "
                + "count. Providers should return the best estimate possible."),

        /**
         * Definition of the property which holds the
         * distance from the root of the hierarchy of this member's parent.
         */
        PARENT_LEVEL(
            Datatype.UNSIGNED_INTEGER,
            false,
            null,
            "Required. The distance of the member's parent from the root level "
                + "of the hierarchy. The root level is zero."),

        /**
         * Definition of the property which holds the
         * Name of the current catalog.
         */
        PARENT_UNIQUE_NAME(
            Datatype.STRING,
            false,
            null,
            "Required. Unique name of the member's parent. NULL is returned "
                + "for any members at the root level. For providers that generate "
                + "unique names by qualification, each component of this name is "
                + "delimited."),

        /**
         * Definition of the property which holds the
         * number of parents that this member has. Generally 1, or 0
         * for root members.
         */
        PARENT_COUNT(
            Datatype.UNSIGNED_INTEGER,
            false,
            null,
            "Required. Number of parents that this member has."),

        /**
         * Definition of the property which holds the
         * description of this member.
         */
        DESCRIPTION(
            Datatype.STRING,
            false,
            null,
            "Optional. A human-readable description of the member."),

        /**
         * Definition of the internal property which holds the
         * name of the system property which determines whether to show a member
         * (especially a measure or calculated member) in a user interface such
         * as JPivot.
         */
        $visible(
            Datatype.BOOLEAN,
            true,
            null,
            null),

        /**
         * Definition of the internal property which holds the
         * value of the member key in the original data type. MEMBER_KEY is for
         * backward-compatibility.  MEMBER_KEY has the same value as KEY0 for
         * non-composite keys, and MEMBER_KEY property is null for composite
         * keys.
         */
        MEMBER_KEY(
            Datatype.VARIANT,
            true,
            null,
            "Optional. The value of the member key. Null for composite keys."),

        /**
         * Definition of the boolean property that indicates whether
         * a member is a placeholder member for an empty position in a
         * dimension hierarchy.
         */
        IS_PLACEHOLDERMEMBER(
            Datatype.BOOLEAN,
            false,
            null,
            "Required. Whether the member is a placeholder member for an empty "
                + "position in a dimension hierarchy."),

        /**
         * Definition of the property that indicates whether the member is a
         * data member.
         */
        IS_DATAMEMBER(
            Datatype.BOOLEAN,
            false,
            null,
            "Required. whether the member is a data member"),

        /**
         * Definition of the property which
         * holds the level depth of a member.
         *
         * <p>Caution: Level depth of members in parent-child hierarchy isn't
         * from their levels.  It's calculated from the underlying data
         * dynamically.
         */
        DEPTH(
            Datatype.UNSIGNED_INTEGER,
            true,
            null,
            "The level depth of a member"),

        /**
         * Definition of the property which
         * holds the DISPLAY_INFO required by XML/A.
         *
         * <p>Caution: This property's value is calculated based on a specified
         * MDX query, so its value is dynamic at runtime.
         */
        DISPLAY_INFO(
            Datatype.UNSIGNED_INTEGER,
            false,
            null,
            "Display instruction of a member for XML/A"),

        /**
         * Definition of the property which
         * holds the value of a cell. Is usually numeric (since most measures
         * are numeric) but is occasionally another type.
         */
        VALUE(
            Datatype.VARIANT,
            false,
            null,
            "The unformatted value of the cell.");

        private final Datatype type;
        private final String description;
        private final boolean internal;

        private StandardMemberProperty(
            Datatype type,
            boolean internal,
            Class<? extends Enum> enumClazz,
            String description)
        {
//            assert ordinal == ordinal();
            this.internal = internal;
            this.type = type;
            this.description = description;
        }

        public String getName() {
            return name();
        }

        public String getUniqueName() {
            return name();
        }

        public String getCaption() {
            // NOTE: This caption will be the same in all locales, since
            // StandardMemberProperty has no way of deducing the current
            // connection. Providers that wish to localize the caption of
            // built-in properties should create a wrapper around
            // StandardMemberProperty that is aware of the current connection or
            // locale.
            return name();
        }

        public String getDescription() {
            // NOTE: This description will be the same in all locales, since
            // StandardMemberProperty has no way of deducing the current
            // connection. Providers that wish to localize the description of
            // built-in properties should create a wrapper around
            // StandardCellProperty that is aware of the current connection or
            // locale.
            return description;
        }

        public Datatype getDatatype() {
            return type;
        }

        public Set<TypeFlag> getType() {
            return TypeFlag.MEMBER_TYPE_FLAG;
        }

        public boolean isInternal() {
            return internal;
        }

        public boolean isVisible() {
            return !internal;
        }
    }


    /**
     * Enumeration of the system properties available for every
     * {@link org.olap4j.Cell}.
     *
     * <p>The following propertiess are mandatory for cells:<ul>
     * <li>{@link #BACK_COLOR}</li>
     * <li>{@link #CELL_EVALUATION_LIST}</li>
     * <li>{@link #CELL_ORDINAL}</li>
     * <li>{@link #FORE_COLOR}</li>
     * <li>{@link #FONT_NAME}</li>
     * <li>{@link #FONT_SIZE}</li>
     * <li>{@link #FONT_FLAGS}</li>
     * <li>{@link #FORMAT_STRING}</li>
     * <li>{@link #FORMATTED_VALUE}</li>
     * <li>{@link #NON_EMPTY_BEHAVIOR}</li>
     * <li>{@link #SOLVE_ORDER}</li>
     * <li>{@link #VALUE}</li>
     * </ul>
     */
    enum StandardCellProperty implements Property {
        BACK_COLOR(
            Datatype.STRING,
            false,
            "The background color for displaying the VALUE or FORMATTED_VALUE "
                + "property. For more information, see FORE_COLOR and BACK_COLOR "
                + "Contents."),

        CELL_EVALUATION_LIST(
            Datatype.STRING,
            false,
            "The semicolon-delimited list of evaluated formulas applicable to "
                + "the cell, in order from lowest to highest solve order. For more "
                + "information about solve order, see Understanding Pass Order and "
                + "Solve Order"),

        CELL_ORDINAL(
            Datatype.UNSIGNED_INTEGER,
            false,
            "The ordinal number of the cell in the dataset."),

        FORE_COLOR(
            Datatype.STRING,
            false,
            "The foreground color for displaying the VALUE or FORMATTED_VALUE "
                + "property. For more information, see FORE_COLOR and BACK_COLOR "
                + "Contents."),

        FONT_NAME(
            Datatype.STRING,
            false,
            "The font to be used to display the VALUE or FORMATTED_VALUE "
                + "property."),

        FONT_SIZE(
            Datatype.STRING,
            false,
            "Font size to be used to display the VALUE or FORMATTED_VALUE "
                + "property."),

        FONT_FLAGS(
            Datatype.UNSIGNED_INTEGER,
            false,
            "The bitmask detailing effects on the font. The value is the "
                + "result of a bitwise OR operation of one or more of the "
                + "following constants: MDFF_BOLD  = 1, MDFF_ITALIC = 2, "
                + "MDFF_UNDERLINE = 4, MDFF_STRIKEOUT = 8. For example, the value "
                + "5 represents the combination of bold (MDFF_BOLD) and underline "
                + "(MDFF_UNDERLINE) font effects."),

        /**
         * Definition of the property which
         * holds the formatted value of a cell.
         */
        FORMATTED_VALUE(
            Datatype.STRING,
            false,
            "The character string that represents a formatted display of the "
                + "VALUE property."),

        /**
         * Definition of the property which
         * holds the format string used to format cell values.
         */
        FORMAT_STRING(
            Datatype.STRING,
            false,
            "The format string used to create the FORMATTED_VALUE property "
                + "value. For more information, see FORMAT_STRING Contents."),

        NON_EMPTY_BEHAVIOR(
            Datatype.STRING,
            false,
            "The measure used to determine the behavior of calculated members "
                + "when resolving empty cells."),

        /**
         * Definition of the property which
         * determines the solve order of a calculated member with respect to
         * other calculated members.
         */
        SOLVE_ORDER(
            Datatype.INTEGER,
            false,
            "The solve order of the cell."),

        /**
         * Definition of the property which
         * holds the value of a cell. Is usually numeric (since most measures
         * are numeric) but is occasionally another type.
         */
        VALUE(
            Datatype.VARIANT,
            false,
            "The unformatted value of the cell."),

        /**
         * Definition of the property which
         * holds the datatype of a cell. Valid values are "String",
         * "Numeric", "Integer". The property's value derives from the
         * "datatype" attribute of the "Measure" element; if the
         * datatype attribute is not specified, the datatype is
         * "Numeric" by default, except measures whose aggregator is
         * "Count", whose datatype is "Integer".
         */
        DATATYPE(
            Datatype.STRING,
            false,
            "The datatype of the cell."),

        LANGUAGE(
            Datatype.UNSIGNED_INTEGER,
            false,
            "The locale where the FORMAT_STRING will be applied. LANGUAGE is "
                + "usually used for currency conversion."),

        ACTION_TYPE(
            Datatype.INT4,
            false,
            "A bitmask that indicates which types of actions exist on the "
                + "cell."),

        UPDATEABLE(
            Datatype.UNSIGNED_INTEGER,
            false,
            "A value that indicates whether the cell can be updated.");

        /**
         * The datatype of the property.
         */
        private final Datatype type;
        private final String description;
        private final boolean internal;

        private StandardCellProperty(
            Datatype type,
            boolean internal,
            String description)
        {
            this.type = type;
            this.internal = internal;
            this.description = description;
        }

        public Datatype getDatatype() {
            return type;
        }

        public Set<TypeFlag> getType() {
            return TypeFlag.CELL_TYPE_FLAG;
        }

        public String getName() {
            return name();
        }

        public String getUniqueName() {
            return name();
        }

        public String getCaption() {
            // NOTE: This caption will be the same in all locales, since
            // StandardCellProperty has no way of deducing the current
            // connection. Providers that wish to localize the caption of
            // built-in properties should create a wrapper around
            // StandardCellProperty that is aware of the current connection or
            // locale.
            return name();
        }

        public String getDescription() {
            // NOTE: This description will be the same in all locales, since
            // StandardCellProperty has no way of deducing the current
            // connection. Providers that wish to localize the description of
            // built-in properties should create a wrapper around
            // StandardCellProperty that is aware of the current connection or
            // locale.
            return description;
        }

        public boolean isInternal() {
            return internal;
        }

        public boolean isVisible() {
            return !internal;
        }
    }

}
