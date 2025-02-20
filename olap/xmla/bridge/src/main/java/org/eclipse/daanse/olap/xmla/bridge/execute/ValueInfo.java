/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.xmla.bridge.execute;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.daanse.xmla.api.common.properties.XsdType;

/**
 * Takes a DataType String (null, Integer, Numeric or non-null)
 * and Value Object (Integer, Double, String, other) and
 * canonicalizes them to XSD data type and corresponding object.
 * <p>
 * If the input DataType is Integer, then it attempts to return
 * an XSD_INT with value java.lang.Integer (and failing that an
 * XSD_LONG (java.lang.Long) or XSD_INTEGER (java.math.BigInteger)).
 * Worst case is the value loses precision with any integral
 * representation and must be returned as a decimal type (Double
 * or java.math.BigDecimal).
 * <p>
 * If the input DataType is Decimal, then it attempts to return
 * an XSD_DOUBLE with value java.lang.Double (and failing that an
 * XSD_DECIMAL (java.math.BigDecimal)).
 */
class ValueInfo {

    public static final int XSD_INT_MAX_INCLUSIVE = 2147483647;
    public static final int XSD_INT_MIN_INCLUSIVE = -2147483648;

    /**
     * Returns XSD_INT, XSD_DOUBLE, XSD_STRING or null.
     *
     * @param dataType null, Integer, Numeric or non-null.
     * @return Returns the suggested XSD type for a given datatype
     */
    static String getValueTypeHint(final String dataType) {
        if (dataType != null) {
            return (dataType.equals("Integer"))
                ? XsdType.XSD_INTEGER
                : ((dataType.equals("Numeric"))
                ? XsdType.XSD_DOUBLE
                : XsdType.XSD_STRING);
        } else {
            return null;
        }
    }

    String valueType;
    Object value;
    boolean isDecimal;

    ValueInfo(final String dataType, final Object inputValue) {
        final String valueTypeHint = getValueTypeHint(dataType);

        // This is a hint: should it be a string, integer or decimal type.
        // In the following, if the hint is integer, then there is
        // an attempt that the value types
        // be XSD_INT, XST_LONG, or XSD_INTEGER (but they could turn
        // out to be XSD_DOUBLE or XSD_DECIMAL if precision is loss
        // with the integral formats). It the hint is a decimal type
        // (double, float, decimal), then a XSD_DOUBLE or XSD_DECIMAL
        // is returned.
        if (valueTypeHint != null) {
            // The value type is a hint. If the value can be
            // converted to the data type without precision loss, ok;
            // otherwise value data type must be adjusted.

            if (valueTypeHint.equals(XsdType.XSD_STRING)) {
                // For String types, nothing to do.
                this.valueType = valueTypeHint;
                this.value = inputValue;
                this.isDecimal = false;

            } else if (valueTypeHint.equals(XsdType.XSD_INTEGER)) {
                // If valueTypeHint is XSD_INT, then see if value can be
                // converted to (first choice) integer, (second choice),
                // long and (last choice) BigInteger - otherwise must
                // use double/decimal.

                // Most of the time value ought to be an Integer so
                // try it first
                if (inputValue instanceof Integer) {
                    // For integer, its already the right type
                    this.valueType = valueTypeHint;
                    this.value = inputValue;
                    this.isDecimal = false;

                } else if (inputValue instanceof Byte) {
                    this.valueType = XsdType.XSD_BYTE;
                    this.value = inputValue;
                    this.isDecimal = false;

                } else if (inputValue instanceof Short) {
                    this.valueType = XsdType.XSD_SHORT;
                    this.value = inputValue;
                    this.isDecimal = false;

                } else if (inputValue instanceof Long) {
                    // See if it can be an integer or long
                    long lval = (Long) inputValue;
                    setValueAndType(lval);

                } else if (inputValue instanceof BigInteger bi) {
                    // See if it can be an integer or long
                    long lval = bi.longValue();
                    if (bi.equals(BigInteger.valueOf(lval))) {
                        // It can be converted from BigInteger to long
                        // without loss of precision.
                        setValueAndType(lval);
                    } else {
                        // It can not be converted to a long.
                        this.valueType = XsdType.XSD_INTEGER_LONG;
                        this.value = inputValue;
                        this.isDecimal = false;
                    }

                } else if (inputValue instanceof Float f) {
                    // See if it can be an integer or long
                    long lval = f.longValue();
                    if (f.equals(Float.valueOf(lval))) {
                        // It can be converted from double to long
                        // without loss of precision.
                        setValueAndType(lval);

                    } else {
                        // It can not be converted to a long.
                        this.valueType = XsdType.XSD_FLOAT;
                        this.value = inputValue;
                        this.isDecimal = true;
                    }

                } else if (inputValue instanceof Double d) {
                    // See if it can be an integer or long
                    long lval = d.longValue();
                    if (d.equals(Double.valueOf(lval))) {
                        // It can be converted from double to long
                        // without loss of precision.
                        setValueAndType(lval);

                    } else {
                        // It can not be converted to a long.
                        this.valueType = XsdType.XSD_DOUBLE;
                        this.value = inputValue;
                        this.isDecimal = true;
                    }

                } else if (inputValue instanceof BigDecimal bd) {
                    try {
                        // Can it be converted to a long
                        // Throws ArithmeticException on conversion failure.
                        // The following line is only available in
                        // Java5 and above:
                        //long lval = bd.longValueExact();
                        long lval = bd.longValue();

                        setValueAndType(lval);
                    } catch (ArithmeticException ex) {
                        // No, it can not be converted to long

                        try {
                            // Can it be an integer
                            BigInteger bi = bd.toBigIntegerExact();
                            this.valueType = XsdType.XSD_INTEGER_LONG;
                            this.value = bi;
                            this.isDecimal = false;
                        } catch (ArithmeticException ex1) {
                            // OK, its a decimal
                            this.valueType = XsdType.XSD_DECIMAL;
                            this.value = inputValue;
                            this.isDecimal = true;
                        }
                    }

                } else if (inputValue instanceof Number) {
                    // Don't know what Number type we have here.
                    // Note: this could result in precision loss.
                    this.value = ((Number) inputValue).longValue();
                    this.valueType = valueTypeHint;
                    this.isDecimal = false;

                } else {
                    // Who knows what we are dealing with,
                    // hope for the best?!?
                    this.valueType = valueTypeHint;
                    this.value = inputValue;
                    this.isDecimal = false;
                }

            } else if (valueTypeHint.equals(XsdType.XSD_DOUBLE)) {
                // The desired type is double.

                // Most of the time value ought to be an Double so
                // try it first
                if (inputValue instanceof Double) {
                    // For Double, its already the right type
                    this.valueType = valueTypeHint;
                    this.value = inputValue;
                    this.isDecimal = true;

                } else if (inputValue instanceof Byte
                    || inputValue instanceof Short
                    || inputValue instanceof Integer
                    || inputValue instanceof Long)
                {
                    // Convert from byte/short/integer/long to double
                    this.value = ((Number) inputValue).doubleValue();
                    this.valueType = valueTypeHint;
                    this.isDecimal = true;

                } else if (inputValue instanceof Float) {
                    this.value = inputValue;
                    this.valueType = XsdType.XSD_FLOAT;
                    this.isDecimal = true;

                } else if (inputValue instanceof BigDecimal bd) {
                    double dval = bd.doubleValue();
                    // make with same scale as Double
                    try {
                        BigDecimal bd2 =BigDecimal.valueOf(dval);
                        // Can it be a double
                        // Must use compareTo - see BigDecimal.equals
                        if (bd.compareTo(bd2) == 0) {
                            this.valueType = XsdType.XSD_DOUBLE;
                            this.value = dval;
                        } else {
                            this.valueType = XsdType.XSD_DECIMAL;
                            this.value = inputValue;
                        }
                    } catch (NumberFormatException ex) {
                        this.valueType = XsdType.XSD_DECIMAL;
                        this.value = inputValue;
                    }
                    this.isDecimal = true;

                } else if (inputValue instanceof BigInteger bi) {
                    // See if it can be an integer or long
                    long lval = bi.longValue();
                    if (bi.equals(BigInteger.valueOf(lval))) {
                        // It can be converted from BigInteger to long
                        // without loss of precision.
                        setValueAndType(lval);
                    } else {
                        // It can not be converted to a long.
                        this.valueType = XsdType.XSD_INTEGER_LONG;
                        this.value = inputValue;
                        this.isDecimal = true;
                    }

                } else if (inputValue instanceof Number) {
                    // Don't know what Number type we have here.
                    // Note: this could result in precision loss.
                    this.value = ((Number) inputValue).doubleValue();
                    this.valueType = valueTypeHint;
                    this.isDecimal = true;

                } else {
                    // Who knows what we are dealing with,
                    // hope for the best?!?
                    this.valueType = valueTypeHint;
                    this.value = inputValue;
                    this.isDecimal = true;
                }
            }
        } else {
            // There is no valueType "hint", so just get it from the value.
            if (inputValue instanceof String) {
                this.valueType = XsdType.XSD_STRING;
                this.value = inputValue;
                this.isDecimal = false;

            } else if (inputValue instanceof Integer) {
                this.valueType = XsdType.XSD_INTEGER;
                this.value = inputValue;
                this.isDecimal = false;

            } else if (inputValue instanceof Byte b) {
                this.valueType = XsdType.XSD_BYTE;
                this.value = b.intValue();
                this.isDecimal = false;

            } else if (inputValue instanceof Short s) {
                this.valueType = XsdType.XSD_SHORT;
                this.value = s.intValue();
                this.isDecimal = false;

            } else if (inputValue instanceof Long) {
                // See if it can be an integer or long
                setValueAndType((Long) inputValue);

            } else if (inputValue instanceof BigInteger bi) {
                // See if it can be an integer or long
                long lval = bi.longValue();
                if (bi.equals(BigInteger.valueOf(lval))) {
                    // It can be converted from BigInteger to long
                    // without loss of precision.
                    setValueAndType(lval);
                } else {
                    // It can not be converted to a long.
                    this.valueType = XsdType.XSD_INTEGER_LONG;
                    this.value = inputValue;
                    this.isDecimal = false;
                }

            } else if (inputValue instanceof Float) {
                this.valueType = XsdType.XSD_FLOAT;
                this.value = inputValue;
                this.isDecimal = true;

            } else if (inputValue instanceof Double) {
                this.valueType = XsdType.XSD_DOUBLE;
                this.value = inputValue;
                this.isDecimal = true;

            } else if (inputValue instanceof BigDecimal bd) {
                double dval = bd.doubleValue();
                // make with same scale as Double
                try {
                    BigDecimal bd2 =  BigDecimal.valueOf(dval);
                    // Can it be a double
                    // Must use compareTo - see BigDecimal.equals
                    if (bd.compareTo(bd2) == 0) {
                        this.valueType = XsdType.XSD_DOUBLE;
                        this.value = dval;
                    } else {
                        this.valueType = XsdType.XSD_DECIMAL;
                        this.value = inputValue;
                    }
                } catch (NumberFormatException ex) {
                    this.valueType = XsdType.XSD_DECIMAL;
                    this.value = inputValue;
                }
                this.isDecimal = true;

            } else if (inputValue instanceof Number) {
                // Don't know what Number type we have here.
                // Note: this could result in precision loss.
                this.value = ((Number) inputValue).longValue();
                this.valueType = XsdType.XSD_LONG;
                this.isDecimal = false;

            } else if (inputValue instanceof Boolean) {
                this.value = inputValue;
                this.valueType = XsdType.XSD_BOOLEAN;
                this.isDecimal = false;
            } else {
                // Who knows what we are dealing with,
                // hope for the best?!?
                this.valueType = XsdType.XSD_STRING;
                this.value = inputValue;
                this.isDecimal = false;
            }
        }
    }
    private void setValueAndType(long lval) {
        if (! isValidXsdInt(lval)) {
            // No, it can not be a integer, must be a long
            this.valueType = XsdType.XSD_LONG;
            this.value = lval;
        } else {
            // Its an integer.
            this.valueType = XsdType.XSD_INTEGER;
            this.value = (int) lval;
        }
        this.isDecimal = false;
    }

    private boolean isValidXsdInt(long l) {
        return (l <= XSD_INT_MAX_INCLUSIVE) && (l >= XSD_INT_MIN_INCLUSIVE);
    }

}
