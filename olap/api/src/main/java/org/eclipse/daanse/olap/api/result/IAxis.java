package org.eclipse.daanse.olap.api.result;

import java.util.Locale;

public interface IAxis {

    /**
     * Abbreviation for {@link IAxis.Standard#FILTER}.
     */
    IAxis.Standard FILTER = IAxis.Standard.FILTER;

    /**
     * Abbreviation for {@link IAxis.Standard#COLUMNS}.
     */
    IAxis.Standard COLUMNS = IAxis.Standard.COLUMNS;

    /**
     * Abbreviation for {@link IAxis.Standard#ROWS}.
     */
    IAxis.Standard ROWS = IAxis.Standard.ROWS;

    /**
     * Abbreviation for {@link IAxis.Standard#PAGES}.
     */
    IAxis.Standard PAGES = IAxis.Standard.PAGES;

    /**
     * Abbreviation for {@link IAxis.Standard#CHAPTERS}.
     */
    IAxis.Standard SECTIONS = IAxis.Standard.SECTIONS;

    /**
     * Abbreviation for {@link IAxis.Standard#FILTER}.
     */
    IAxis.Standard CHAPTERS = IAxis.Standard.CHAPTERS;

    /**
     * Returns the name of this axis, e.g. "COLUMNS", "FILTER", "AXIS(17)".
     *
     * @return Name of the axis
     */
    String name();

    /**
     * Returns whether this is the filter (slicer) axis.
     *
     * @return whether this is the filter axis
     */
    boolean isFilter();


    /**
     * Returns the ordinal which is to be used for retrieving this axis from
     * the {@link org.olap4j.CellSet#getAxes()}, or retrieving its
     * coordinate from {@link Cell#getCoordinateList()}.
     *
     * <p>For example:
     * <ul>
     * <li>-1 {@link IAxis.Standard#FILTER FILTER}</li>
     * <li>0 {@link IAxis.Standard#COLUMNS COLUMNS}</li>
     * <li>1 {@link IAxis.Standard#ROWS ROWS}</li>
     * <li>2 {@link IAxis.Standard#PAGES PAGES}</li>
     * <li>3 {@link IAxis.Standard#CHAPTERS CHAPTERS}</li>
     * <li>4 {@link IAxis.Standard#SECTIONS SECTIONS}</li>
     * <li>5 {@link IAxis.Standard#SECTIONS SECTIONS}</li>
     * <li>6 AXES(6)</li>
     * <li>123 AXES(123)</li>
     * </ul>
     *
     * @return ordinal of this axis
     */
    int axisOrdinal();

    /**
     * Returns localized name for this Axis.
     *
     * <p>Examples: "FILTER", "ROWS", "COLUMNS", "AXIS(10)".
     *
     * @param locale Locale for which to give the name
     * @return localized name for this Axis
     */
    String getCaption(Locale locale);

    /**
     * Enumeration of standard, named axes descriptors.
     */
    public enum Standard implements IAxis {
        /**
         * Filter axis, also known as the slicer axis, and represented by the
         * WHERE clause of an MDX query.
         */
        FILTER,

        /** COLUMNS axis, also known as X axis and AXIS(0). */
        COLUMNS,

        /** ROWS axis, also known as Y axis and AXIS(1). */
        ROWS,

        /** PAGES axis, also known as AXIS(2). */
        PAGES,

        /** CHAPTERS axis, also known as AXIS(3). */
        CHAPTERS,

        /** SECTIONS axis, also known as AXIS(4). */
        SECTIONS;

        public int axisOrdinal() {
            return ordinal() - 1;
        }

        public boolean isFilter() {
            return this == FILTER;
        }

        public String getCaption(Locale locale) {
            // TODO: localize
            return name();
        }
    }

    /**
     * Container class for various Axis factory methods.
     */
    class Factory {
        private static final IAxis.Standard[] STANDARD_VALUES = IAxis.Standard.values();

        /**
         * Returns the axis with a given ordinal.
         *
         * <p>For example, {@code forOrdinal(0)} returns the COLUMNS axis;
         * {@code forOrdinal(-1)} returns the SLICER axis;
         * {@code forOrdinal(100)} returns AXIS(100).
         *
         * @param ordinal Axis ordinal
         * @return Axis whose ordinal is as given
         */
        public static IAxis forOrdinal(final int ordinal) {
            if (ordinal < -1) {
                throw new IllegalArgumentException(
                    "Axis ordinal must be -1 or higher");
            }
            if (ordinal + 1 < STANDARD_VALUES.length) {
                return STANDARD_VALUES[ordinal + 1];
            }
            return new IAxis() {
                public String toString() {
                    return name();
                }

                public String name() {
                    return "AXIS(" + ordinal + ")";
                }

                public boolean isFilter() {
                    return false;
                }

                public int axisOrdinal() {
                    return ordinal;
                }

                public String getCaption(Locale locale) {
                    // TODO: localize
                    return name();
                }
            };
        }
    }
}
