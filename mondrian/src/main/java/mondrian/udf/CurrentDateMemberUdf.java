/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara.
// Copyright (C) 2022 Sergei Semenkov
// All rights reserved.
*/
package mondrian.udf;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.MatchType;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.Syntax;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.type.Type;

import aQute.bnd.annotation.spi.ServiceProvider;
import mondrian.olap.Util;
import mondrian.olap.type.HierarchyType;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.StringType;
import mondrian.olap.type.SymbolType;
import mondrian.spi.UserDefinedFunction;
import mondrian.util.Format;

/**
 * User-defined function <code>CurrentDateMember</code>.  Arguments to the
 * function are as follows:
 *
 * <blockquote>
 * <code>
 * CurrentDateMember(&lt;Hierarchy&gt;, &lt;FormatString&gt;[, &lt;Find&gt;)
 * returns &lt;Member&gt;
 * </code>
 * </blockquote>
 *
 * The function returns the member from the specified hierarchy that matches
 * the current date, to the granularity specified by the &lt;FormatString&gt;.
 *
 * The format string conforms to the format string implemented by
 * {@link Format}.
 *
 * @author Zelaine Fong
 */
@ServiceProvider(value = UserDefinedFunction.class)
public class CurrentDateMemberUdf implements UserDefinedFunction {
    private final static String CURRENT_DATE_MEMBER_UDF_DESCRIPTION =
        "Returns the closest or exact member within the specified "
        + "dimension corresponding to the current date, in the format "
        + "specified by the format parameter. "
        + "Format strings are the same as used by the MDX Format function, "
        + "namely the Visual Basic format strings. "
        + "See http://www.apostate.com/programming/vb-format.html.";

    private Object resultDateMember = null;

    @Override
	public Object execute(Evaluator evaluator, Argument[] arguments) {
        if (resultDateMember != null) {
            return resultDateMember;
        }

        // determine the current date
        Object formatArg = arguments[1].evaluateScalar(evaluator);

        final Locale locale = Locale.getDefault();
        final Format format = new Format((String) formatArg, locale);
        String currDateStr = format.format(getDate(evaluator, arguments));

        // determine the match type
        MatchType matchType;
        if (arguments.length == 3) {
            String matchStr = arguments[2].evaluateScalar(evaluator).toString();
            matchType = Enum.valueOf(MatchType.class, matchStr);
        } else {
            matchType = MatchType.EXACT;
        }

        List<Segment> uniqueNames = Util.parseIdentifier(currDateStr);
        resultDateMember =
            evaluator.getSchemaReader().getMemberByUniqueName(
                uniqueNames, false, matchType);
        if (resultDateMember != null) {
            return resultDateMember;
        }

        // if there is no matching member, return the null member for
        // the specified dimension/hierarchy
        Object arg0 = arguments[0].evaluate(evaluator);
        if (arg0 instanceof Hierarchy) {
            resultDateMember = ((Hierarchy) arg0).getNullMember();
        } else {
            resultDateMember =
                ((Dimension) arg0).getHierarchy().getNullMember();
        }
        return resultDateMember;
    }

    /*
     * Package private function created for proper testing.
     */
    Date getDate(Evaluator evaluator, Argument[] arguments) {
        return evaluator.getQueryStartTime();
    }

    @Override
	public String getDescription() {
        return CURRENT_DATE_MEMBER_UDF_DESCRIPTION;
    }

    @Override
	public String getName() {
        return "CurrentDateMember";
    }

    @Override
	public Type[] getParameterTypes() {
        return new Type[] {
            new HierarchyType(null, null),
            StringType.INSTANCE,
            SymbolType.INSTANCE
        };
    }

    @Override
	public List<String> getReservedWords() {
		return List.of("EXACT", "BEFORE", "AFTER");
    }

    @Override
	public Type getReturnType(Type[] parameterTypes) {
        Hierarchy hierarchy =  parameterTypes[0].getHierarchy();
        return (hierarchy == null) ? MemberType.Unknown : MemberType
            .forHierarchy(hierarchy);
    }

    @Override
	public Syntax getSyntax() {
        return Syntax.Function;
    }
}
