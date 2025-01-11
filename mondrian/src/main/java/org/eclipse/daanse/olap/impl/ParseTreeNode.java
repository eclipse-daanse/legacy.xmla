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

import org.eclipse.daanse.olap.api.ParseRegion;

/**
 * Node in a parse tree representing a parsed MDX statement.
 *
 * <p>To convert a parse tree to an MDX string, use a {@link ParseTreeWriter}
 * and the  method.
 *
 * @author jhyde
 * @since Jun 4, 2007
 */
public interface ParseTreeNode {

    /**
     * Returns the region of the source code which this node was created from,
     * if it was created by parsing.
     *
     * <p>A non-leaf node's region will encompass the regions of all of its
     * children. For example, a the region of a function call node
     * <code>Crossjoin([Gender], {[Store].[USA]})</code> stretches from
     * the first character of the function name to the closing parenthesis.
     *
     * <p>Region may be null, if the node was created programmatically, not
     * from a piece of source code.
     *
     * @return Region of the source code this node was created from, if it was
     * created by parsing
     */
    ParseRegion getRegion();



}

// End ParseTreeNode.java
