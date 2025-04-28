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
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.LevelType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.constant.ConstantLevelCalc;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;

import mondrian.olap.Util;

/**
 * Usage of a {@link org.eclipse.daanse.olap.api.element.Level} as an MDX expression.
 *
 * @author jhyde
 * @since Sep 26, 2005
 */
public class LevelExpressionImpl extends AbstractExpression implements Expression, LevelExpression {
    private final Level level;

    /**
     * Creates a level expression.
     *
     * @param level Level
     * @pre level != null
     */
    public LevelExpressionImpl(Level level) {
        Util.assertPrecondition(level != null, "level != null");
        this.level = level;
    }

    /**
     * Returns the level.
     *
     * @post return != null
     */
    @Override
    public Level getLevel() {
        return level;
    }

    @Override
	public String toString() {
        return level.getUniqueName();
    }

    @Override
	public Type getType() {
        return LevelType.forLevel(level);
    }

    @Override
	public LevelExpressionImpl cloneExp() {
        return new LevelExpressionImpl(level);
    }

    @Override
	public DataType getCategory() {
        return DataType.LEVEL;
    }

    @Override
	public Expression accept(Validator validator) {
        return this;
    }

    @Override
	public LevelCalc accept(ExpressionCompiler compiler) {
        return ConstantLevelCalc.of(level);
    }

    @Override
	public Object accept(QueryComponentVisitor visitor) {
        return visitor.visitLevelExpression(this);
    }

}
