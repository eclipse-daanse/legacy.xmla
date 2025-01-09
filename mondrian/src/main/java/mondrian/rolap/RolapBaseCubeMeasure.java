/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap;

import java.text.MessageFormat;
import java.util.Map;

import mondrian.olap.exceptions.CastInvalidTypeException;

import org.eclipse.daanse.jdbc.db.dialect.api.Datatype;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.rolap.mapping.api.model.SQLExpressionMapping;

import mondrian.olap.Property;
import mondrian.olap.StringLiteralImpl;
import mondrian.spi.CellFormatter;

/**
 * Measure which is computed from a SQL column (or expression) and which is
 * defined in a non-virtual cube.
 *
 * @see RolapVirtualCubeMeasure
 *
 * @author jhyde
 * @since 24 August, 2006
 */
public class RolapBaseCubeMeasure
    extends RolapMemberBase
    implements RolapStoredMeasure
{

    private final static String unknownAggregator = "Unknown aggregator ''{0}''; valid aggregators are: {1}";

    static enum DataType {
        Integer,
        Numeric,
        String
    }

    /**
     * For SQL generator. Column which holds the value of the measure.
     */
    private final SQLExpressionMapping expression;

    /**
     * For SQL generator. Has values "SUM", "COUNT", etc.
     */
    private final RolapAggregator aggregator;

    private final RolapCube cube;
    private final Map<String, Object> metadata;

    /**
     * Holds the {@link mondrian.rolap.RolapStar.Measure} from which this
     * member is computed. Untyped, because another implementation might store
     * it somewhere else.
     */
    private Object starMeasure;

    private RolapResult.ValueFormatter formatter;

    /**
     * Creates a RolapBaseCubeMeasure.
     *
     * @param cube Cube
     * @param parentMember Parent member
     * @param level Level this member belongs to
     * @param name Name of this member
     * @param caption Caption
     * @param description Description
     * @param formatString Format string
     * @param expression Expression
     * @param aggregatorName Aggregator
     * @param datatype Data type
     * @param metadata metadata
     */
    RolapBaseCubeMeasure(
        RolapCube cube,
        RolapMember parentMember,
        RolapLevel level,
        String name,
        String caption,
        String description,
        String formatString,
        SQLExpressionMapping expression,
        String aggregatorName,
        org.eclipse.daanse.rolap.mapping.api.model.enums.DataType datatype,
        Map<String, Object> metadata)
    {
        super(parentMember, level, name, null, MemberType.MEASURE);
        assert metadata != null;
        this.cube = cube;
        this.metadata = metadata;
        this.caption = caption;
        this.expression = expression;
        if (description != null) {
            setProperty(
                Property.DESCRIPTION_PROPERTY.name,
                description);
        }
        if (formatString == null) {
            formatString = "";
        } else {
            setProperty(
                Property.FORMAT_STRING.name,
                formatString);
        }
        setProperty(
            Property.FORMAT_EXP_PARSED.name,
            StringLiteralImpl.create(formatString));
        setProperty(
            Property.FORMAT_EXP.name,
            formatString);

        // Validate aggregator.
        this.aggregator =
            RolapAggregator.enumeration.getValue(aggregatorName, false);
        if (this.aggregator == null) {
            StringBuilder buf = new StringBuilder();
            for (String aggName : RolapAggregator.enumeration.getNames()) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append('\'');
                buf.append(aggName);
                buf.append('\'');
            }
            throw new OlapRuntimeException(MessageFormat.format(unknownAggregator,
                aggregatorName,
                buf.toString()));
        }

        setProperty(Property.AGGREGATION_TYPE.name, aggregator);
        if (datatype == null) {
            if (aggregator == RolapAggregator.Count
                || aggregator == RolapAggregator.DistinctCount)
            {
                datatype = org.eclipse.daanse.rolap.mapping.api.model.enums.DataType.INTEGER;
            } else {
                datatype = org.eclipse.daanse.rolap.mapping.api.model.enums.DataType.NUMERIC;
            }
        }
        if (RolapBaseCubeMeasure.DataType.valueOf(datatype.getValue()) == null) {
            throw new CastInvalidTypeException(datatype.getValue());
        }
        setProperty(Property.DATATYPE.name, datatype);
    }

    @Override
	public SQLExpressionMapping getMondrianDefExpression() {
        return expression;
    }

    @Override
	public RolapAggregator getAggregator() {
        return aggregator;
    }

    @Override
	public RolapCube getCube() {
        return cube;
    }

    @Override
	public RolapResult.ValueFormatter getFormatter() {
        return formatter;
    }

    public void setFormatter(CellFormatter cellFormatter) {
        this.formatter =
            new RolapResult.CellFormatterValueFormatter(cellFormatter);
    }

    @Override
	public Object getStarMeasure() {
        return starMeasure;
    }

    void setStarMeasure(Object starMeasure) {
        this.starMeasure = starMeasure;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Datatype getDatatype() {
        Object datatype = getPropertyValue(Property.DATATYPE.name);
        try {
            return Datatype.fromValue((String) datatype);
        } catch (ClassCastException e) {
            return Datatype.STRING;
        } catch (IllegalArgumentException e) {
            return Datatype.STRING;
        }
    }
}
