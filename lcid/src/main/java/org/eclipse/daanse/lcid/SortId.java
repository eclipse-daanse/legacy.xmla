/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.lcid;

public enum SortId {

    // 0x0
    SORT_CHINESE_BIG5((byte) 0, new short[] { 0x0404, 0x0c04, 0x1404 }), //
    SORT_CHINESE_PRCP((byte) 0, new short[] { 0x0804, 0x1004 }), //
    SORT_DEFAULT((byte) 0, null), //
    SORT_GEORGIAN_TRADITIONAL((byte) 0, new short[] { 0x0437 }), //
    SORT_HUNGARIAN_DEFAULT((byte) 0, new short[] { 0x040E }), //
    SORT_JAPANESE_XJIS((byte) 0, new short[] { 0x0411 }), //
    SORT_KOREAN_KSC((byte) 0, new short[] { 0x0412 }), //

    // 0x1
    @Deprecated
    SORT_CHINESE_UNICODE((byte) 1, new short[] {}), //
    SORT_GEORGIAN_MODERN((byte) 1, new short[] { 0x0437 }), //
    SORT_GERMAN_PHONE_BOOK((byte) 1, new short[] { 0x0407 }), //
    SORT_HUNGARIAN_TECHNICAL((byte) 1, new short[] { 0x040E }), //
    @Deprecated
    SORT_JAPANESE_UNICODE((byte) 1, new short[] {}), //
    @Deprecated
    SORT_KOREAN_UNICODE((byte) 1, new short[] {}), //

    // 0x2
    SORT_CHINESE_PRC((byte) 2, new short[] { 0x0804, 0x1004 }), //
    SORT_CHINESE_BOPOMOFO((byte) 3, new short[] { 0x0404 }), //

    // 0x4
    SORT_CHINESE_RADICALSTROKE((byte) 4, new short[] { 0x0404, 0x0C04, 0x1404 }), //
    SORT_JAPANESE_RADICALSTROKE((byte) 4, new short[] { 0x0411 });//

    private final byte value;
    private short[] resticteslanguages;

    SortId(byte value, short[] resticteslanguages) {
        this.value = value;
        this.resticteslanguages = resticteslanguages;
    }

    public byte getValue() {
        return value;
    }

    public short[] getResticteslanguages() {
        return resticteslanguages;
    }

    public static byte getValidatedValue(short languageId, SortId sortId) {

        switch (sortId) {
        case SORT_DEFAULT: {

            return sortId.getValue();
        }

        default:

            for (short restriction : sortId.getResticteslanguages()) {
                if (restriction == languageId) {
                    return sortId.getValue();
                }

            }

            throw new IllegalArgumentException(
                    "Unexpected sortId: '" + sortId.name() + "' for languageId '" + languageId + "'");
        }

    }

}
