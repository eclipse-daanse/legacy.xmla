/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package org.eclipse.daanse.olap.function.def.aggregate;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.DelegatingTupleList;
import mondrian.calc.impl.TupleCollections;
import mondrian.mdx.UnresolvedFunCallImpl;
import mondrian.olap.ResourceLimitExceededException;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.fun.CacheFunDef;
import mondrian.olap.fun.FunUtil;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapMember;
import mondrian.rolap.RolapStoredMeasure;

/**
 * Abstract base class for all aggregate functions (<code>Aggregate</code>,
 * <code>Sum</code>, <code>Avg</code>, et cetera).
 *
 * @author jhyde
 * @since 2005/8/14
 */
public abstract class AbstractAggregateFunDef extends AbstractFunctionDefinition {

    private final static String iterationLimitExceeded = "Number of iterations exceeded limit of {0,number}";

    public AbstractAggregateFunDef(FunctionMetaData functionMetaData ) {
        super(functionMetaData);
    }

    @Override
	protected Expression validateArgument(
        Validator validator, Expression[] args, int i, DataType category)
    {
        // If expression cache is enabled, wrap first expression (the set)
        // in a function which will use the expression cache.
        if (i == 0 && SystemWideProperties.instance().EnableExpCache) {
            Expression arg = args[0];
            if (FunUtil.worthCaching(arg)) {
                final Expression cacheCall =
                    new UnresolvedFunCallImpl(
								new FunctionOperationAtom(CacheFunDef.NAME), new Expression[] { arg });
                return validator.validate(cacheCall, false);
            }
        }
        return super.validateArgument(validator, args, i, category);
    }

    /**
     * Evaluates the list of members or tuples used in computing the aggregate.
     * If the measure for aggregation has to ignore unrelated dimensions
     * this method will push unrelated dimension members to top level member.
     * This behaviour is driven by the ignoreUnrelatedDimensions property
     * on a base cube usage specified in the virtual cube.Keeps track of the
     * number of iterations that will be required to iterate over the members
     * or tuples needed to compute the aggregate within the current context.
     * In doing so, also determines if the cross product of all iterations
     * across all parent evaluation contexts will exceed the limit set in the
     * properties file.
     *
     * @param tupleListCalc  calculator used to evaluate the member list
     * @param evaluator current evaluation context
     * @return list of evaluated members or tuples
     */
    protected static TupleList evaluateCurrentList(
        TupleListCalc tupleListCalc,
        Evaluator evaluator)
    {
        final int savepoint = evaluator.savepoint();
        TupleList tuples;
        try {
            evaluator.setNonEmpty(false);
            tuples = tupleListCalc.evaluateList(evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        int currLen = tuples.size();
        TupleList dims;
        try {
            dims = AbstractAggregateFunDef.processUnrelatedDimensions(tuples, evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        AbstractAggregateFunDef.crossProd(evaluator, currLen);
        return dims;
    }

    protected TupleIterable evaluateCurrentIterable(
        TupleIteratorCalc tupleIteratorCalc,
        Evaluator evaluator)
    {
        final int savepoint = evaluator.savepoint();
        int currLen = 0;
        TupleIterable iterable;
        try {
            evaluator.setNonEmpty(false);
            iterable = tupleIteratorCalc.evaluateIterable(evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        AbstractAggregateFunDef.crossProd(evaluator, currLen);
        return iterable;
    }

    private static void crossProd(Evaluator evaluator, int currLen) {
        long iterationLimit =
            evaluator.getQuery().getConnection().getContext().getConfig().iterationLimit();
        final int productLen = currLen * evaluator.getIterationLength();
        if (iterationLimit > 0 && productLen > iterationLimit) {
                throw new ResourceLimitExceededException(MessageFormat.format(
                    iterationLimitExceeded, iterationLimit));
        }
        evaluator.setIterationLength(currLen);
    }

    /**
     * Pushes unrelated dimensions to the top level member from the given list
     * of tuples if the ignoreUnrelatedDimensions property is set on the base
     * cube usage in the virtual cube.
     *
     * <p>If IgnoreMeasureForNonJoiningDimension is set to true and
     * ignoreUnrelatedDimensions on CubeUsage is set to false then if a non
     * joining dimension exists in the aggregation list then return an empty
     * list else return the original list.
     *
     * @param tuplesForAggregation is a list of members or tuples used in
     * computing the aggregate
     * @param evaluator Evaluator
     * @return list of members or tuples
     */
    public static TupleList processUnrelatedDimensions(
        TupleList tuplesForAggregation,
        Evaluator evaluator)
    {
        if (tuplesForAggregation.isEmpty()) {
            return tuplesForAggregation;
        }

        RolapMember measure = AbstractAggregateFunDef.getRolapMeasureForUnrelatedDimCheck(
            evaluator, tuplesForAggregation);

        if (measure.isCalculated()) {
            return tuplesForAggregation;
        }

        RolapCube virtualCube = (RolapCube) evaluator.getCube();
        if (virtualCube.isVirtual()) {
            // this should be a safe cast since we've eliminated calcs above
            RolapCube baseCube = ((RolapStoredMeasure)measure).getCube();
            if (baseCube == null) {
                return tuplesForAggregation;
            }
            if (virtualCube.shouldIgnoreUnrelatedDimensions(baseCube.getName()))
            {
                return AbstractAggregateFunDef.ignoreUnrelatedDimensions(
                    tuplesForAggregation, baseCube);
            } else if (evaluator.getQuery().getConnection().getContext().getConfig().ignoreMeasureForNonJoiningDimension())
            {
                return AbstractAggregateFunDef.ignoreMeasureForNonJoiningDimension(
                    tuplesForAggregation, baseCube);
            }
        }
        return tuplesForAggregation;
    }

    /**
     * Returns the measure to use when determining which dimensions
     * are unrelated.  Most of the time this is the measure in context,
     * except 2 cases:
     * 1)  When a measure is included in a compound slicer
     * 2)  When one or more measures are included in the first parameter
     *     of Aggregate.
     *      e.g. Aggregate( Crossjoin( {Time.[1997]}, {measures.[Unit Sales]})
     * In both cases the measure(s) will be present in tuplesForAggregation.
     */
    private static RolapMember getRolapMeasureForUnrelatedDimCheck(
        Evaluator evaluator, TupleList tuplesForAggregation)
    {
        RolapMember measure = (RolapMember)evaluator.getMembers()[0];
        if (tuplesForAggregation != null
            && !tuplesForAggregation.isEmpty())
        {
            // this looks for the measure in the first tuple, with the
            // assumption that there is a single measure in all tuples.
            // This assumption is incorrect in the unusual case where
            // a set of measures is used as the first param in an aggregate
            // function.
            for (Member tupMember : tuplesForAggregation.get(0)) {
                if (tupMember.isMeasure()) {
                    measure = (RolapMember)tupMember;
                }
            }
        }
        return measure;
    }

    /**
     * If a non joining dimension exists in the aggregation list then return
     * an empty list else return the original list.
     *
     * @param tuplesForAggregation is a list of members or tuples used in
     * computing the aggregate
     * @param baseCube the cube to scan for nonjoining dimensions.
     * @return list of members or tuples
     */
    private static TupleList ignoreMeasureForNonJoiningDimension(
        TupleList tuplesForAggregation,
        RolapCube baseCube)
    {
        Set<Dimension> nonJoiningDimensions =
            AbstractAggregateFunDef.nonJoiningDimensions(baseCube, tuplesForAggregation);
        if (!nonJoiningDimensions.isEmpty()) {
            return TupleCollections.emptyList(tuplesForAggregation.getArity());
        }
        return tuplesForAggregation;
    }

    /**
     * Pushes unrelated dimensions to the top level member from the given list
     * of tuples if the ignoreUnrelatedDimensions property is set on the base
     * cube usage in the virtual cube.
     *
     * @param tuplesForAggregation is a list of members or tuples used in
     * computing the aggregate
     * @return list of members or tuples
     */
    private static TupleList ignoreUnrelatedDimensions(
        TupleList tuplesForAggregation,
        RolapCube baseCube)
    {
        Set<Dimension> nonJoiningDimensions =
            AbstractAggregateFunDef.nonJoiningDimensions(baseCube, tuplesForAggregation);
        final Set<List<Member>> processedTuples =
            new LinkedHashSet<>(tuplesForAggregation.size());
        for (List<Member> tuple : tuplesForAggregation) {
            List<Member> tupleCopy = tuple;
            for (int j = 0; j < tuple.size(); j++) {
                final Member member = tuple.get(j);
                if (nonJoiningDimensions.contains(member.getDimension())) {
                    if (tupleCopy == tuple) {
                        // Avoid making a copy until we have to change a tuple.
                        tupleCopy = new ArrayList<>(tuple);
                    }
                    final Hierarchy hierarchy =
                        member.getDimension().getHierarchy();
                    if (hierarchy.hasAll()) {
                        tupleCopy.set(j, hierarchy.getAllMember());
                    } else {
                        tupleCopy.set(j, hierarchy.getDefaultMember());
                    }
                }
            }
            processedTuples.add(tupleCopy);
        }
        return new DelegatingTupleList(
            tuplesForAggregation.getArity(),
            new ArrayList<>(
                processedTuples));
    }

    private static Set<Dimension> nonJoiningDimensions(
        RolapCube baseCube,
        TupleList tuplesForAggregation)
    {
        List<Member> tuple = tuplesForAggregation.get(0);
        return baseCube.nonJoiningDimensions(
            tuple.toArray(new Member[tuple.size()]));
    }
}
