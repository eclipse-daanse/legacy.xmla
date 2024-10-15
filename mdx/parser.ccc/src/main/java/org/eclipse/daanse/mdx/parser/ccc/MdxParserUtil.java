/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.mdx.parser.ccc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.operation.AmpersandQuotedPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.QuotedPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.record.expression.CallExpressionR;
import org.eclipse.daanse.mdx.model.record.expression.CompoundIdR;

public class MdxParserUtil {
	private MdxParserUtil() {
	}

	public static String stripQuotes(String s, String prefix, String suffix, String quoted) {
		if (!(s.startsWith(prefix) && s.endsWith(suffix))) {
            throw new IllegalArgumentException("Invalid quotes: " + s);
        }
		s = s.substring(prefix.length(), s.length() - suffix.length());
		s = s.replace(quoted, suffix);
		return s;
	}

	public static MdxExpression createCall(MdxExpression left, ObjectIdentifier objectIdentifier,
			List<MdxExpression> expressions, Set<String> propertyWords) {
		final String name = objectIdentifier instanceof NameObjectIdentifier nameObjectIdentifier
				?  nameObjectIdentifier.name()
				: null;
		if (expressions != null) {
			if (left != null) {
				// Method syntax: "x.foo(arg1, arg2)" or "x.foo()"
				expressions.add(0, left);
				return new CallExpressionR(new MethodOperationAtom(name) , expressions);
			} else {
				// Function syntax: "foo(arg1, arg2)" or "foo()"
				return new CallExpressionR(new FunctionOperationAtom(name), expressions);
			}
		} else {
			// Member syntax: "foo.bar"
			// or property syntax: "foo.RESERVED_WORD"

			OperationAtom operationAtom;
			boolean call = false;
			switch (objectIdentifier.quoting()) {
			case UNQUOTED:
				operationAtom = new PlainPropertyOperationAtom(name);
				if (name != null && propertyWords.contains(name.toUpperCase())) {
                    call = true;
                }
				break;
			case QUOTED:
				operationAtom = new QuotedPropertyOperationAtom(name);
				break;
			default:
				operationAtom = new AmpersandQuotedPropertyOperationAtom(name);
				break;
			}
			if (left instanceof CompoundId compoundIdLeft && !call) {
				List<ObjectIdentifier> newObjectIdentifiers = new ArrayList<>(
						(compoundIdLeft).objectIdentifiers());
				newObjectIdentifiers.add(objectIdentifier);
				return new CompoundIdR(newObjectIdentifiers);
			} else if (left == null) {
				return new CompoundIdR(List.of(objectIdentifier));
			} else {
				return new CallExpressionR(operationAtom, List.of(left));
			}
		}
	}
}
