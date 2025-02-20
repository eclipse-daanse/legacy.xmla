/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package org.eclipse.daanse.olap.impl;

/**
 * Utility methods common to multiple olap4j driver implementations.
 *
 * <p>
 * This class, and this package as a whole, are part of the olap4j library but
 * the methods are not part of the public olap4j API. The classes exist for the
 * convenience of implementers of olap4j drivers, but their specification and
 * implementation may change at any time.
 * </p>
 *
 * <p>
 * <b>Applications which use the API in this package will not be portable across
 * multiple versions of olap4j</b>. We encourage implementors of drivers to use
 * classes in this package, but not writers of applications.
 * </p>
 *
 * @author jhyde
 * @since Dec 12, 2007
 */
public class Olap4jUtil {



	/**
	 * Returns a string with every occurrence of a seek string replaced with
	 * another.
	 *
	 * @param s       String to act on
	 * @param find    String to find
	 * @param replace String to replace it with
	 * @return The modified string
	 */
	public static String replace(String s, String find, String replace) {
		// let's be optimistic
		int found = s.indexOf(find);
		if (found == -1) {
			return s;
		}
		StringBuilder sb = new StringBuilder(s.length() + 20);
		int start = 0;
		char[] chars = s.toCharArray();
		final int step = find.length();
		if (step == 0) {
			// Special case where find is "".
			sb.append(s);
			replace(sb, 0, find, replace);
		} else {
			for (;;) {
				sb.append(chars, start, found - start);
				if (found == s.length()) {
					break;
				}
				sb.append(replace);
				start = found + step;
				found = s.indexOf(find, start);
				if (found == -1) {
					found = s.length();
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Replaces all occurrences of a string in a buffer with another.
	 *
	 * @param buf     String buffer to act on
	 * @param start   Ordinal within <code>find</code> to start searching
	 * @param find    String to find
	 * @param replace String to replace it with
	 * @return The string buffer
	 */
	public static StringBuilder replace(StringBuilder buf, int start, String find, String replace) {
		// Search and replace from the end towards the start, to avoid O(n ^ 2)
		// copying if the string occurs very commonly.
		int findLength = find.length();
		if (findLength == 0) {
			// Special case where the seek string is empty.
			for (int j = buf.length(); j >= 0; --j) {
				buf.insert(j, replace);
			}
			return buf;
		}
		int k = buf.length();
		while (k > 0) {
			int i = buf.lastIndexOf(find, k);
			if (i < start) {
				break;
			}
			buf.replace(i, i + find.length(), replace);
			// Step back far enough to ensure that the beginning of the section
			// we just replaced does not cause a match.
			k = i - findLength;
		}
		return buf;
	}






}
