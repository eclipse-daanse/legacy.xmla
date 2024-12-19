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
package org.eclipse.daanse.olap.function.def.nativizeset;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;

import mondrian.calc.impl.DelegatingTupleList;
import mondrian.calc.impl.TupleCollections;
import mondrian.olap.ResourceLimitExceededException;
import mondrian.olap.Util;

public class CrossJoinAnalyzer {

    private final int arity;
    private final Member[] tempTuple;
    private final List<Member> tempTupleAsList;
    private final int[] nativeIndices;
    private final int resultLimit;

    private final List<Collection<String>> nativeMembers;
    private final ReassemblyGuide reassemblyGuide;
    private final TupleList resultList;

    public CrossJoinAnalyzer(
        TupleList simplifiedList, SubstitutionMap substitutionMap,
        long nativizeMaxResults)
    {
        arity = simplifiedList.getArity();
        tempTuple = new Member[arity];
        tempTupleAsList = Arrays.asList(tempTuple);
        resultLimit = nativizeMaxResults <= 0
                ? Integer.MAX_VALUE
                : (int) Math.min(nativizeMaxResults, Integer.MAX_VALUE);

        resultList = TupleCollections.createList(arity);

        reassemblyGuide = classifyMembers(simplifiedList, substitutionMap);
        nativeMembers = findNativeMembers();
        nativeIndices = findNativeIndices();
    }

    public ReassemblyGuide classifyMembers(
        TupleList simplifiedList,
        SubstitutionMap substitutionMap)
    {
        ReassemblyGuide guide = new ReassemblyGuide(0);

        List<ReassemblyCommand> cmdTuple =
            new ArrayList<>(arity);
        for (List<Member> srcTuple : simplifiedList) {
            cmdTuple.clear();
            for (Member mbr : srcTuple) {
                cmdTuple.add(zz(substitutionMap, mbr));
            }
            guide.addCommandTuple(cmdTuple);
        }
        return guide;
    }

    private ReassemblyCommand zz(
        SubstitutionMap substitutionMap, Member mbr)
    {
        ReassemblyCommand c;
        if (substitutionMap.contains(mbr)) {
            c =
                new ReassemblyCommand(
                    substitutionMap.get(mbr), NativeElementType.LEVEL_MEMBERS);
        } else if (mbr.getName().startsWith(NativizeSetFunDef.SENTINEL_PREFIX)) {
            c =
                new ReassemblyCommand(mbr, NativeElementType.SENTINEL);
        } else {
            NativeElementType nativeType = !isNativeCompatible(mbr)
                ? NativeElementType.NON_NATIVE
                : mbr.getMemberType() == Member.MemberType.REGULAR
                ? NativeElementType.ENUMERATED_VALUE
                : NativeElementType.OTHER_NATIVE;
            c = new ReassemblyCommand(mbr, nativeType);
        }
        return c;
    }

    private List<Collection<String>> findNativeMembers() {
        List<Collection<String>> nativeMembers =
            new ArrayList<>(arity);

        for (int i = 0; i < arity; i++) {
            nativeMembers.add(new LinkedHashSet<>());
        }

        findNativeMembers(reassemblyGuide, nativeMembers);
        return nativeMembers;
    }

    private void findNativeMembers(
        ReassemblyGuide guide,
        List<Collection<String>> nativeMembers)
    {
        List<ReassemblyCommand> commands = guide.getCommands();
        Set<NativeElementType> typesToAdd =
            ReassemblyCommand.getMemberTypes(commands);

        if (typesToAdd.contains(NativeElementType.LEVEL_MEMBERS)) {
            typesToAdd.remove(NativeElementType.ENUMERATED_VALUE);
        }

        int index = guide.getIndex();
        for (ReassemblyCommand command : commands) {
            NativeElementType type = command.getMemberType();
            if (type.isNativeCompatible() && typesToAdd.contains(type)) {
                nativeMembers.get(index).add(command.getElementName());
            }

            if (command.hasNextGuide()) {
                findNativeMembers(command.forNextCol(), nativeMembers);
            }
        }
    }

    private int[] findNativeIndices() {
        int[] indices = new int[arity];
        int nativeColCount = 0;

        for (int i = 0; i < arity; i++) {
            Collection<String> natives = nativeMembers.get(i);
            if (!natives.isEmpty()) {
                indices[nativeColCount++] = i;
            }
        }

        if (nativeColCount == arity) {
            return indices;
        }

        int[] result = new int[nativeColCount];
        System.arraycopy(indices, 0, result, 0, nativeColCount);
        return result;
    }

    private boolean isNativeCompatible(Member member) {
        return member.isParentChildLeaf()
            || (!member.isMeasure()
            && !member.isCalculated() && !member.isAll());
    }

    String getCrossJoinExpression() {
        return formatCrossJoin(nativeMembers);
    }

    private String formatCrossJoin(List<Collection<String>> memberLists) {
        StringBuilder buf = new StringBuilder();

        String left = NativizeSetFunDef.toCsv(memberLists.get(0));
        String right =
            memberLists.size() == 1
            ? ""
            : formatCrossJoin(memberLists.subList(1, memberLists.size()));

        if (left.length() == 0) {
            buf.append(right);
        } else {
            if (right.length() == 0) {
                buf.append("{").append(left).append("}");
            } else {
                buf.append("CrossJoin(")
                    .append("{").append(left).append("},")
                    .append(right).append(")");
            }
        }

        return buf.toString();
    }

    TupleList mergeCalcMembers(TupleList nativeValues) {
        TupleList nativeList =
            adaptList(nativeValues, arity, nativeIndices);

        NativizeSetFunDef.dumpListToLog("native list", nativeList);
        mergeCalcMembers(reassemblyGuide, new Range(nativeList), null);
        NativizeSetFunDef.dumpListToLog("result list", resultList);
        return resultList;
    }

    private void mergeCalcMembers(
        ReassemblyGuide guide, Range range, Set<List<Member>> history)
    {
        int col = guide.getIndex();
        if (col == arity - 1) {
            if (history == null) {
                appendMembers(guide, range);
            } else {
                appendMembers(guide, range, history);
            }
            return;
        }

        for (ReassemblyCommand command : guide.getCommands()) {
            ReassemblyGuide nextGuide = command.forNextCol();
            tempTuple[col] = null;

            switch (command.getMemberType()) {
            case NON_NATIVE:
                tempTuple[col] = command.getMember();
                mergeCalcMembers(
                    nextGuide,
                    range,
                    (history == null
                        ? new HashSet<List<Member>>()
                        : history));
                break;
            case ENUMERATED_VALUE:
                Member value = command.getMember();
                Range valueRange = range.subRangeForValue(value, col);
                if (!valueRange.isEmpty()) {
                    mergeCalcMembers(nextGuide, valueRange, history);
                }
                break;
            case LEVEL_MEMBERS:
                Level level = command.getLevel();
                Range levelRange = range.subRangeForValue(level, col);
                for (Range subRange : levelRange.subRanges(col)) {
                    mergeCalcMembers(nextGuide, subRange, history);
                }
                break;
            case OTHER_NATIVE:
                for (Range subRange : range.subRanges(col)) {
                    mergeCalcMembers(nextGuide, subRange, history);
                }
                break;
            default:
                throw Util.unexpected(command.getMemberType());
            }
        }
    }

    private void appendMembers(ReassemblyGuide guide, Range range) {
        int col = guide.getIndex();

        for (ReassemblyCommand command : guide.getCommands()) {
            switch (command.getMemberType()) {
            case NON_NATIVE:
                tempTuple[col] = command.getMember();
                appendTuple(range.getTuple(), tempTupleAsList);
                break;
            case ENUMERATED_VALUE:
                Member value = command.getMember();
                Range valueRange = range.subRangeForValue(value, col);
                if (!valueRange.isEmpty()) {
                    appendTuple(valueRange.getTuple());
                }
                break;
            case LEVEL_MEMBERS:
            case OTHER_NATIVE:
                for (List<Member> tuple : range.getTuples()) {
                    appendTuple(tuple);
                }
                break;
            default:
                throw Util.unexpected(command.getMemberType());
            }
        }
    }

    private void appendMembers(
        ReassemblyGuide guide, Range range, Set<List<Member>> history)
    {
        int col = guide.getIndex();

        for (ReassemblyCommand command : guide.getCommands()) {
            switch (command.getMemberType()) {
            case NON_NATIVE:
                tempTuple[col] = command.getMember();
                if (range.isEmpty()) {
                    appendTuple(tempTupleAsList, history);
                } else {
                    appendTuple(range.getTuple(), tempTupleAsList, history);
                }
                break;
            case ENUMERATED_VALUE:
                Member value = command.getMember();
                Range valueRange = range.subRangeForValue(value, col);
                if (!valueRange.isEmpty()) {
                    appendTuple(
                        valueRange.getTuple(), tempTupleAsList, history);
                }
                break;
            case LEVEL_MEMBERS:
            case OTHER_NATIVE:
                tempTuple[col] = null;
                for (List<Member> tuple : range.getTuples()) {
                    appendTuple(tuple, tempTupleAsList, history);
                }
                break;
            default:
                throw Util.unexpected(command.getMemberType());
            }
        }
    }

    private void appendTuple(
        List<Member> nonNatives,
        Set<List<Member>> history)
    {
        if (history.add(nonNatives)) {
            appendTuple(nonNatives);
        }
    }

    private void appendTuple(
        List<Member> natives,
        List<Member> nonNatives,
        Set<List<Member>> history)
    {
        List<Member> copy = copyOfTuple(natives, nonNatives);
        if (history.add(copy)) {
            appendTuple(copy);
        }
    }

    private void appendTuple(
        List<Member> natives,
        List<Member> nonNatives)
    {
        appendTuple(copyOfTuple(natives, nonNatives));
    }

    private void appendTuple(List<Member> tuple) {
        resultList.add(tuple);
        checkNativeResultLimit(resultList.size());
    }

    private List<Member> copyOfTuple(
        List<Member> natives,
        List<Member> nonNatives)
    {
        Member[] copy = new Member[arity];
        for (int i = 0; i < arity; i++) {
            copy[i] =
                (nonNatives.get(i) == null)
                    ? natives.get(i)
                    : nonNatives.get(i);
        }
        return Arrays.asList(copy);
    }

    /**
     * Check the resultSize against the result limit setting. Throws
     * LimitExceededDuringCrossjoin exception if limit exceeded.
     * <p/>
     * It didn't seem appropriate to use the existing Mondrian
     * ResultLimit property, since the meaning and use of that
     * property seems to be a bit ambiguous, otherwise we could
     * simply call Util.checkCJResultLimit.
     *
     * @param resultSize Result limit
     * @throws mondrian.olap.ResourceLimitExceededException
     *
     */
    private void checkNativeResultLimit(int resultSize) {
        // Throw an exeption if the size of the crossjoin exceeds the result
        // limit.
        if (resultLimit < resultSize) {
            throw new ResourceLimitExceededException(resultSize, resultLimit);
        }
    }

    public TupleList adaptList(
        final TupleList sourceList,
        final int destSize,
        final int[] destIndices)
    {
        if (sourceList.isEmpty()) {
            return TupleCollections.emptyList(destIndices.length);
        }

        checkNativeResultLimit(sourceList.size());

        TupleList destList =
            new DelegatingTupleList(
                destSize,
                new AbstractList<List<Member>>() {
                    @Override
                    public List<Member> get(int index) {
                        final List<Member> sourceTuple =
                            sourceList.get(index);
                        final Member[] members = new Member[destSize];
                        for (int i = 0; i < destIndices.length; i++) {
                            members[destIndices[i]] = sourceTuple.get(i);
                        }
                        return Arrays.asList(members);
                    }

                    @Override
                    public int size() {
                        return sourceList.size();
                    }
                }
            );

        // The mergeCalcMembers method in this file assumes that the
        // resultList is random access - that calls to get(n) are constant
        // cost, regardless of n. Unfortunately, the TraversalList objects
        // created by HighCardSqlTupleReader are implemented using linked
        // lists, leading to pathologically long run times.
        // This presumes that the ResultStyle is LIST
        if (NativizeSetFunDef.LOGGER.isDebugEnabled()) {
            String sourceListType =
                sourceList.getClass().getSimpleName();
            String sourceElementType =
                String.format("Member[%d]", destSize);
            NativizeSetFunDef.LOGGER.debug(
                String.format(
                    "returning native %s<%s> without copying to new list.",
                    sourceListType,
                    sourceElementType));
        }
        return destList;
    }
}
