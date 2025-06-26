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
package org.eclipse.daanse.olap.api.result;

public enum Datatype {
    // The following values exactly match VARENUM
    // in Automation and may be used in VARIANT.

    INTEGER(3, "DBTYPE_I4", "A four-byte, signed integer: INTEGER"),

    DOUBLE(5, "DBTYPE_R8", "A double-precision floating-point value: Double"),

    CURRENCY(
        6,
        "DBTYPE_CY",
        "A currency value: LARGE_INTEGER, Currency is a fixed-point number with "
            + "four digits to the right of the decimal point. It is stored in an "
            + "eight-byte signed integer, scaled by 10,000."),

    BOOLEAN(
        11,
        "DBTYPE_BOOL",
        "A Boolean value stored in the same way as in Automation: VARIANT_BOOL; "
            + "0 means false and ~0 (bitwise, the value is not 0; that is, all bits "
            + "are set to 1) means true."),

    /**
     * Used by SQL Server for value.
     */
    VARIANT(12, "DBTYPE_VARIANT", "An Automation VARIANT"),

    /**
     * Used by SQL Server for font size.
     */
    UNSIGNED_SHORT(18, "DBTYPE_UI2", "A two-byte, unsigned integer"),

    /**
     * Used by SQL Server for colors, font flags and cell ordinal.
     */
    UNSIGNED_INTEGER(19, "DBTYPE_UI4", "A four-byte, unsigned integer"),

    // The following values exactly match VARENUM
    // in Automation but cannot be used in VARIANT.

    LARGE_INTEGER(
        20,
        "DBTYPE_I8",
        "An eight-byte, signed integer: LARGE_INTEGER"),

    // The following values are not in VARENUM in OLE.

    STRING(
        130,
        "DBTYPE_WSTR",
        "A null-terminated Unicode character string: wchar_t[length]; If "
            + "DBTYPE_WSTR is used by itself, the number of bytes allocated "
            + "for the string, including the null-termination character, is "
            + "specified by cbMaxLen in the DBBINDING structure. If "
            + "DBTYPE_WSTR is combined with DBTYPE_BYREF, the number of bytes "
            + "allocated for the string, including the null-termination character, "
            + "is at least the length of the string plus two. In either case, the "
            + "actual length of the string is determined from the bound length "
            + "value. The maximum length of the string is the number of allocated "
            + "bytes divided by sizeof(wchar_t) and truncated to the nearest "
            + "integer."),

    /**
     * Used by SAP BW. Represents a Character
     */
    ACCP(1000, "ACCP", "SAP BW Character"),

    /**
     * Used by SAP BW. Represents a CHAR
     */
    CHAR(1001, "CHAR", "SAP BW CHAR"),

    /**
     * Used by SAP BW. Represents a CHAR
     */
    CUKY(1002, "CUKY", "SAP BW CHAR"),

    /**
     * Used by SAP BW. Represents a Currency - Packed decimal, Integer
     */
    CURR(1003, "CURR", "SAP BW Currency - Packed decimal, Integer"),

    /**
     * Used by SAP BW. Represents a Date
     */
    DATS(1004, "DATS", "SAP BW Date"),

    /**
     * Used by SAP BW. Represents a Decimal
     */
    DEC(1005, "DEC", "SAP BW Decimal"),

    /**
     * Used by SAP BW. Represents a Point
     */
    FLTP(1006, "FLTP", "SAP BW Floating Point"),

    /**
     * Used by SAP BW. Represents a Byte
     */
    INT1(1007, "INT1", "SAP BW Byte"),

    /**
     * Used by SAP BW. Represents a Small integer
     */
    INT2(1008, "INT2", "SAP BW Small integer"),

    /**
     * Used by SAP BW. Represents an Integer
     */
    INT4(1009, "INT4", "SAP BW Integer"),

    /**
     * Used by SAP BW. Represents a Text
     */
    LCHR(1010, "LCHR", "SAP BW Text"),

    /**
     * Used by SAP BW. Represents a Numeric
     */
    NUMC(1011, "NUMC", "SAP BW Numeric"),

    /**
     * Used by SAP BW. Represents a Tiny Int
     */
    PREC(1012, "PREC", "SAP BW Tiny Int"),

    /**
     * Used by SAP BW. Represents a QUAN Integer
     */
    QUAN(1013, "QUAN", "SAP BW QUAN Integer"),

    /**
     * Used by SAP BW. Represents a String
     */
    SSTR(1014, "SSTR", "SAP BW String"),

    /**
     * Used by SAP BW. Represents a Long String
     */
    STRG(1015, "STRG", "SAP BW Long String"),

    /**
     * Used by SAP BW. Represents a Time
     */
    TIMS(1016, "TIMS", "SAP BW Time"),

    /**
     * Used by SAP BW. Represents a Varchar
     */
    VARC(1017, "VARC", "SAP BW Varchar"),

    /**
     * Used by SAP BW. Represents a Long String for Units
     */
    UNIT(1018, "UNIT", "SAP BW Long String for Units");

    private final int xmlaOrdinal;
    private String dbTypeIndicator;
    private String description;

    Datatype(
        int xmlaOrdinal,
        String dbTypeIndicator,
        String description)
    {
        this.xmlaOrdinal = xmlaOrdinal;
        this.dbTypeIndicator = dbTypeIndicator;
        this.description = description;
    }

    /**
     * The internal name of this Datatype.
     * Might not be unique across Datatype instances.
     */
    public String xmlaName() {
        return dbTypeIndicator;
    }

    /**
     * Human readable description of a Datatype instance.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Unique identifier of a Datatype instance.
     */
    public int xmlaOrdinal() {
        return xmlaOrdinal;
    }

}
