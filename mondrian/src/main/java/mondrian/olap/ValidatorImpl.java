/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2017 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.olap;



import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.FunctionCall;
import org.eclipse.daanse.olap.api.query.component.MemberProperty;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionPrinter;
import org.eclipse.daanse.olap.query.base.Expressions;
import org.eclipse.daanse.olap.util.type.TypeUtil;

import mondrian.mdx.ResolvedFunCallImpl;
import mondrian.mdx.UnresolvedFunCallImpl;
import mondrian.olap.exceptions.MdxMemberExpIsSetException;
import mondrian.olap.exceptions.UnknownParameterException;
import mondrian.util.ArrayStack;

/**
 * Default implementation of {@link org.eclipse.daanse.olap.api.Validator}.
 *
 * <p>Uses a stack to help us guess the type of our parent expression
 * before we've completely resolved our children -- necessary,
 * unfortunately, when figuring out whether the "*" operator denotes
 * multiplication or crossjoin.
 *
 * <p>Keeps track of which nodes have already been resolved, so we don't
 * try to resolve nodes which have already been resolved. (That would not
 * be wrong, but can cause resolution to be an <code>O(2^N)</code>
 * operation.)
 *
 * <p>The concrete implementing class needs to implement
 * {@link #getQuery()} and {@link #defineParameter(Parameter)}.
 *
 * @author jhyde
 */
abstract class ValidatorImpl implements Validator {
    protected final ArrayStack<QueryComponent> stack = new ArrayStack<>();
    private final FunctionService functionService;
    private final Map<QueryComponent, QueryComponent> resolvedNodes =
        new HashMap<>();
    private static final QueryComponent placeHolder = NumericLiteralImpl.zero;
    private final static String noFunctionMatchesSignature = "No function matches signature ''{0}''";
    private final static String moreThanOneFunctionMatchesSignature =
        "More than one function matches signature ''{0}''; they are: {1}";

    /**
     * Creates a ValidatorImpl.
     *
     * @param funTable Function table
     *
     * @param resolvedIdentifiers map of already resolved Ids
     * @pre funTable != null
     */
    protected ValidatorImpl(
    		FunctionService funTable, Map<QueryComponent, QueryComponent> resolvedIdentifiers)
    {
        Util.assertPrecondition(funTable != null, "funTable != null");
        this.functionService = funTable;
        resolvedNodes.putAll(resolvedIdentifiers);
    }

    @Override
	public Expression validate(Expression exp, boolean scalar) {
        Expression resolved;
        try {
            resolved = (Expression) resolvedNodes.get(exp);
        } catch (ClassCastException e) {
            // A classcast exception will occur if there is a String
            // placeholder in the map. This is an internal error -- should
            // not occur for any query, valid or invalid.
            throw Util.newInternal(
                e,
                new StringBuilder("Infinite recursion encountered while validating '")
                .append(Util.unparse(exp)).append("'").toString());
        }
        if (resolved == null) {
            try {
                stack.push((QueryComponent) exp);
                // To prevent recursion, put in a placeholder while we're
                // resolving.
                resolvedNodes.put((QueryComponent) exp, placeHolder);
                resolved = exp.accept(this);
                Util.assertTrue(resolved != null);
                resolvedNodes.put((QueryComponent) exp, (QueryComponent) resolved);
            } finally {
            	if (!stack.isEmpty()) {
            		stack.pop();
            	}
            }
        }

        if (scalar) {
            final Type type = resolved.getType();
            if (!TypeUtil.canEvaluate(type)) {
                String exprString = Util.unparse(resolved);
                throw new MdxMemberExpIsSetException(exprString);
            }
        }

        return resolved;
    }

    @Override
	public void validate(ParameterExpression parameterExpr) {
        ParameterExpression resolved =
            (ParameterExpression) resolvedNodes.get(parameterExpr);
        if (resolved != null) {
            return; // already resolved
        }
        try {
            stack.push(parameterExpr);
            resolvedNodes.put(parameterExpr, placeHolder);
            resolved = (ParameterExpression) parameterExpr.accept(this);
            assert resolved != null;
            resolvedNodes.put(parameterExpr, resolved);
        } finally {
            stack.pop();
        }
    }

    @Override
	public void validate(MemberProperty memberProperty) {
        MemberProperty resolved =
            (MemberProperty) resolvedNodes.get(memberProperty);
        if (resolved != null) {
            return; // already resolved
        }
        try {
            stack.push(memberProperty);
            resolvedNodes.put(memberProperty, placeHolder);
            memberProperty.resolve(this);
            resolvedNodes.put(memberProperty, memberProperty);
        } finally {
            stack.pop();
        }
    }

    @Override
	public void validate(QueryAxis axis) {
        final QueryAxisImpl resolved = (QueryAxisImpl) resolvedNodes.get(axis);
        if (resolved != null) {
            return; // already resolved
        }
        try {
            stack.push(axis);
            resolvedNodes.put(axis, placeHolder);
            axis.resolve(this);
            resolvedNodes.put(axis, axis);
        } finally {
            stack.pop();
        }
    }

    @Override
	public void validate(Formula formula) {
        final Formula resolved = (Formula) resolvedNodes.get(formula);
        if (resolved != null) {
            return; // already resolved
        }
        try {
            stack.push(formula);
            resolvedNodes.put(formula, placeHolder);
            formula.accept(this);
            resolvedNodes.put(formula, formula);
        } finally {
            stack.pop();
        }
    }

    @Override
	public FunctionDefinition getDef(
        Expression[] args,
        OperationAtom operationAtom)
    {
        // Compute signature first. It makes debugging easier.
		final String signature = FunctionPrinter.getSignature(operationAtom, DataType.UNKNOWN,
				Expressions.categoriesOf(args));

        // Resolve function by its upper-case name first.  If there is only one
        // function with that name, stop immediately.  If there is more than
        // function, use some custom method, which generally involves looking
        // at the type of one of its arguments.
        List<FunctionResolver> resolvers = functionService.getResolvers(operationAtom);
        assert resolvers != null;

        final List<FunctionResolver.Conversion> conversionList =
            new ArrayList<>();
        int minConversionCost = Integer.MAX_VALUE;
        List<FunctionDefinition> matchDefs = new ArrayList<>();
        List<FunctionResolver.Conversion> matchConversionList = null;
        for (FunctionResolver resolver : resolvers) {
            conversionList.clear();
            FunctionDefinition def = resolver.resolve(args, this, conversionList);
            if (def != null) {
                int conversionCost = sumConversionCost(conversionList);
                if (conversionCost < minConversionCost) {
                    minConversionCost = conversionCost;
                    matchDefs.clear();
                    matchDefs.add(def);
                    matchConversionList =
                        new ArrayList<>(conversionList);
                } else if (conversionCost == minConversionCost) {
                    matchDefs.add(def);
                } else {
                    // ignore this match -- it required more coercions than
                    // other overloadings we've seen
                }
            }
        }
        switch (matchDefs.size()) {
        case 0:
            throw new OlapRuntimeException(MessageFormat.format(noFunctionMatchesSignature,
                signature));
        case 1:
            break;
        default:
            final StringBuilder buf = new StringBuilder();
            for (FunctionDefinition matchDef : matchDefs) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append(matchDef.getSignature());
            }
            throw new OlapRuntimeException(MessageFormat.format(
                moreThanOneFunctionMatchesSignature,
                    signature,
                    buf.toString()));
        }

        final FunctionDefinition matchDef = matchDefs.get(0);
        for (FunctionResolver.Conversion conversion : matchConversionList) {
            conversion.checkValid();
            conversion.apply(this, Arrays.asList(args));
        }

        return matchDef;
    }

    @Override
	public boolean alwaysResolveFunDef() {
        return false;
    }

    private int sumConversionCost(
        List<FunctionResolver.Conversion> conversionList)
    {
        int cost = 0;
        for (FunctionResolver.Conversion conversion : conversionList) {
            cost += conversion.getCost();
        }
        return cost;
    }

    @Override
	public boolean canConvert(
        int ordinal, Expression fromExp, DataType to, List<FunctionResolver.Conversion> conversions)
    {
        return TypeUtil.canConvert(
            ordinal,
            fromExp.getType(),
            to,
            conversions);
    }

    @Override
	public boolean requiresExpression() {
        return requiresExpression(stack.size() - 1);
    }

    private boolean requiresExpression(int n) {
        if (n < 1) {
            return false;
        }
        final Object parent = stack.get(n - 1);
        if (parent instanceof Formula formula) {
            return formula.isMember();
        } else if (parent instanceof ResolvedFunCallImpl funCall) {
            if (funCall.getFunDef().getFunctionMetaData().operationAtom() instanceof ParenthesesOperationAtom) {
                return requiresExpression(n - 1);
            } else {
                int k = whichArg(funCall, (Expression) stack.get(n));
                if (k < 0) {
                    // Arguments of call have mutated since call was placed
                    // on stack. Presumably the call has already been
                    // resolved correctly, so the answer we give here is
                    // irrelevant.
                    return false;
                }
                final FunctionDefinition funDef = funCall.getFunDef();
                final DataType[] parameterTypes = funDef.getFunctionMetaData().parameterDataTypes();
                return parameterTypes[k] != DataType.SET;
            }
        } else if (parent instanceof UnresolvedFunCallImpl funCall) {
            if (funCall.getOperationAtom() instanceof ParenthesesOperationAtom
                || funCall.getOperationAtom().name().equals("*"))
            {
                return requiresExpression(n - 1);
            } else {
                int k = whichArg(funCall, (Expression) stack.get(n));
                if (k < 0) {
                    // Arguments of call have mutated since call was placed
                    // on stack. Presumably the call has already been
                    // resolved correctly, so the answer we give here is
                    // irrelevant.
                    return false;
                }
                return requiresExpression(funCall, k);
            }
        } else {
            return false;
        }
    }

    /**
     * Returns whether the <code>k</code>th argument to a function call
     * has to be an expression.
     */
    boolean requiresExpression(
        UnresolvedFunCallImpl funCall,
        int k)
    {
        // The function call has not been resolved yet. In fact, this method
        // may have been invoked while resolving the child. Consider this:
        //   CrossJoin([Measures].[Unit Sales] * [Measures].[Store Sales])
        //
        // In order to know whether to resolve '*' to the multiplication
        // operator (which returns a scalar) or the crossjoin operator
        // (which returns a set) we have to know what kind of expression is
        // expected.
        List<FunctionResolver> resolvers = functionService.getResolvers(funCall.getOperationAtom());
        for (FunctionResolver resolver : resolvers) {
            if (!resolver.requiresScalarExpressionOnArgument(k)) {
                // This resolver accepts a set in this argument position,
                // therefore we don't REQUIRE a scalar expression.
                return false;
            }
        }
        return true;
    }

    @Override
	public FunctionService getFunctionService() {
        return functionService;
    }

    @Override
	public Parameter createOrLookupParam(
        boolean definition,
        String name,
        Type type,
        Expression defaultExp,
        String description)
    {
        final CatalogReader schemaReader = getQuery().getCatalogReader(false);
        Parameter param = schemaReader.getParameter(name);

        if (definition) {
            if (param != null) {
                if (param.getScope() == Parameter.Scope.Statement) {
                    ParameterImpl paramImpl = (ParameterImpl) param;
                    paramImpl.setDescription(description);
                    paramImpl.setDefaultExp(defaultExp);
                    paramImpl.setType(type);
                }
                return param;
            }
            param = new ParameterImpl(
                name,
                defaultExp, description, type);

            // Append it to the list of known parameters.
            defineParameter(param);
            return param;
        } else {
            if (param != null) {
                return param;
            }
            throw new UnknownParameterException(name);
        }
    }

    private int whichArg(final FunctionCall node, final Expression arg) {
        final Expression[] children = node.getArgs();
        for (int i = 0; i < children.length; i++) {
            if (children[i] == arg) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Defines a parameter.
     *
     * @param param Parameter
     */
    protected abstract void defineParameter(Parameter param);
}

// End ValidatorImpl.java

