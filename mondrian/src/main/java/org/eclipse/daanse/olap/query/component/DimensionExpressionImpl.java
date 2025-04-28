/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.eclipse.daanse.olap.query.component;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.DimensionCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.DimensionType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.constant.ConstantDimensionCalc;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;

import mondrian.olap.Util;

/**
 * Usage of a {@link org.eclipse.daanse.olap.api.element.Dimension} as an MDX expression.
 *
 * @author jhyde
 * @since Sep 26, 2005
 */
public class DimensionExpressionImpl extends AbstractExpression implements Expression, DimensionExpression {
    private final Dimension dimension;

    /**
     * Creates a dimension expression.
     *
     * @param dimension Dimension
     * @pre dimension != null
     */
    public DimensionExpressionImpl(Dimension dimension) {
        Util.assertPrecondition(dimension != null, "dimension != null");
        this.dimension = dimension;
    }

    /**
     * Returns the dimension.
     *
     * @post return != null
     */
    @Override
    public Dimension getDimension() {
        return dimension;
    }

    @Override
	public String toString() {
        return dimension.getUniqueName();
    }

    @Override
	public Type getType() {
        return DimensionType.forDimension(dimension);
    }

    @Override
	public DimensionExpressionImpl cloneExp() {
        return new DimensionExpressionImpl(dimension);
    }

    @Override
	public DataType getCategory() {
        return DataType.DIMENSION;
    }

    @Override
	public Expression accept(Validator validator) {
        return this;
    }

    @Override
	public DimensionCalc accept(ExpressionCompiler compiler) {
        return ConstantDimensionCalc.of(dimension);
    }

    @Override
	public Object accept(QueryComponentVisitor visitor) {
        return visitor.visitDimensionExpression(this);
    }

}
