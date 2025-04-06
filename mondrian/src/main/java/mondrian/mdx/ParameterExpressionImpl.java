/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.mdx;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.CompilableParameter;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.query.component.expression.AbstractExpression;
import org.eclipse.daanse.olap.util.type.TypeUtil;

import mondrian.olap.Util;

/**
 * MDX expression which is a usage of a {@link org.eclipse.daanse.olap.api.Parameter}.
 *
 * @author jhyde
 */
public class ParameterExpressionImpl extends AbstractExpression implements ParameterExpression {

    private Parameter parameter;

    /**
     * Creates a ParameterExpr.
     *
     * @param parameter Parameter
     */
    public ParameterExpressionImpl(Parameter parameter)
    {
        this.parameter = parameter;
    }

    @Override
	public Type getType() {
        return parameter.getType();
    }

    @Override
	public DataType getCategory() {
        return TypeUtil.typeToCategory(parameter.getType());
    }

    @Override
	public Expression accept(Validator validator) {
        // There must be some Parameter with this name registered with the
        // Query.  After clone(), there will be many copies of the same
        // parameter, and we rely on this method to bring them down to one.
        // So if this object is not the registered vesion, that's fine, go with
        // the other one.  The registered one will be resolved after everything
        // else in the query has been resolved.
        String parameterName = parameter.getName();
        final CatalogReader schemaReader =
            validator.getQuery().getCatalogReader(false);
        Parameter p = schemaReader.getParameter(parameterName);
        if (p == null) {
            this.parameter =
                validator.createOrLookupParam(
                    true,
                    parameter.getName(),
                    parameter.getType(),
                    parameter.getDefaultExp(),
                    parameter.getDescription());
        } else {
            this.parameter = p;
        }
        return this;
    }

    @Override
	public Calc accept(ExpressionCompiler compiler) {
        return ((CompilableParameter) parameter).compile(compiler);
    }

    @Override
	public Object accept(QueryComponentVisitor visitor) {
        return visitor.visitParameterExpression(this);
    }

    @Override
	public ParameterExpressionImpl cloneExp() {
        return new ParameterExpressionImpl(parameter);
    }

    /**
     * Unparses the definition of this Parameter.
     *
     * <p>The first usage of a parameter in a query becomes a call to the
     * <code>Parameter(paramName, description, defaultValue)</code>
     * function, and subsequent usages become calls to
     * <code>ParamRef(paramName)</code>
     *
     * @param pw PrintWriter
     */
    @Override
	public void unparse(PrintWriter pw) {
        // Is this the first time we've seen a statement parameter? If so,
        // we will generate a call to the Parameter() function, to define
        // the parameter.
        final boolean def;
        if (pw instanceof QueryPrintWriter queryPrintWriter
            && parameter.getScope() == Parameter.Scope.Statement)
        {
            def = queryPrintWriter.parameters.add(parameter);
        } else {
            def = false;
        }
        final String name = parameter.getName();
        final Type type = parameter.getType();
        final DataType category = TypeUtil.typeToCategory(type);
        if (def) {
            pw.print(new StringBuilder("Parameter(").append(Util.quoteForMdx(name)).append(", ").toString());
            switch (category) {
            case STRING, NUMERIC:
                pw.print(category.getName().toUpperCase());
                break;
            case MEMBER:
                pw.print(uniqueName(type));
                break;
            case SET:
                Type elementType = ((SetType) type).getElementType();
                pw.print(uniqueName(elementType));
                break;
            default:
                throw new RuntimeException("Bad Category: "+category.getName());
            }
            pw.print(", ");
            final Object value = parameter.getValue();
            if (value == null) {
                parameter.getDefaultExp().unparse(pw);
            } else if (value instanceof String s) {
                pw.print(Util.quoteForMdx(s));
            } else if (value instanceof List list) {
                pw.print("{");
                int i = -1;
                for (Object o : list) {
                    ++i;
                    if (i > 0) {
                        pw.print(", ");
                    }
                    pw.print(o);
                }
                pw.print("}");
            } else {
                pw.print(value);
            }
            final String description = parameter.getDescription();
            if (description != null) {
                pw.print(", " + Util.quoteForMdx(description));
            }
            pw.print(")");
        } else {
            pw.print(new StringBuilder("ParamRef(").append(Util.quoteForMdx(name)).append(")").toString());
        }
    }

    /**
     * Returns the unique name of the level, hierarchy, or dimension of this
     * type, whichever is most specific.
     *
     * @param type Type
     * @return Most specific description of type
     */
    private String uniqueName(Type type) {
        if (type.getLevel() != null) {
            return type.getLevel().getUniqueName();
        } else if (type.getHierarchy() != null) {
            return type.getHierarchy().getUniqueName();
        } else {
            return type.getDimension().getUniqueName();
        }
    }

    // For the purposes of type inference and expression substitution, a
    // parameter is atomic; therefore, we ignore the child member, if any.
    @Override
	public Object[] getChildren() {
        return null;
    }

    /**
     * Returns whether this parameter is equal to another, based upon name,
     * type and value
     */
    @Override
	public boolean equals(Object other) {
        if (!(other instanceof ParameterExpression that)) {
            return false;
        }
        return this.parameter == that.getParameter();
    }

    @Override
	public int hashCode() {
        return parameter.hashCode();
    }

    /**
     * Returns whether the parameter can be modified.
     *
     * @return whether parameter can be modified
     */
    public boolean isModifiable() {
        return true;
    }

    /**
     * Returns the parameter used by this expression.
     *
     * @return parameter used by this expression
     */
    @Override
    public Parameter getParameter() {
        return parameter;
    }
}
