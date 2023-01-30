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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.ccc.Util.parseSelectQueryAsteriskClause;
import static org.eclipse.daanse.mdx.parser.ccc.Util.parseSelectStatement;

import org.eclipse.daanse.mdx.model.SelectStatement;
import org.eclipse.daanse.mdx.model.select.SelectQueryAsteriskClause;
import org.eclipse.daanse.mdx.model.select.SelectQueryAxesClause;
import org.eclipse.daanse.mdx.model.select.SelectQueryEmptyClause;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SelectQueryClauseTest {

    @Nested
    public class SelectQueryAxesClauseClauseTest {

        @Test
        public void testInStatement() throws MdxParserException {
            String mdx = """
                    SELECT [Customer].[Gender].[Gender].Membmers ON COLUMNS,
                             {[Customer].[Customer].[Aaron A. Allen],
                              [Customer].[Customer].[Abigail Clark]} ON ROWS
                       FROM [c]
                    """;

            SelectStatement selectStatement = parseSelectStatement(mdx);
            System.out.println(selectStatement);
            assertThat(selectStatement).isNotNull();

        }

        @Test
        public void testInClauseMultiple() throws MdxParserException {
            String mdx = """
                    [Customer].[Gender].[Gender].Membmers ON COLUMNS,
                            {[Customer].[Customer].[Aaron A. Allen],
                             [Customer].[Customer].[Abigail Clark]} ON ROWS
                    """;
            SelectQueryAxesClause clause = Util.parseSelectQueryAxesClause(mdx);
            assertThat(clause).isNotNull()
                    .isInstanceOf(SelectQueryAxesClause.class);
        }

        @Test
        public void testInClauseSingle() throws MdxParserException {
            String mdx = "[Customer] ON COLUMNS";
            SelectQueryAxesClause clause = Util.parseSelectQueryAxesClause(mdx);
            assertThat(clause).isNotNull()
                    .isInstanceOf(SelectQueryAxesClause.class);
        }
    }

    @Nested
    public class SelectQueryEmptyClauseTest {
        @Test
        public void testInStatement() throws MdxParserException {
            String mdx = "SELECT FROM [c]";

            SelectStatement selectStatement = parseSelectStatement(mdx);
            assertThat(selectStatement).isNotNull();
            assertThat(selectStatement.selectQueryClause()).isNotNull()
                    .isInstanceOf(SelectQueryEmptyClause.class);

        }
    }

    @Nested
    public class SelectQueryAsteriskClauseTest {

        @Test
        public void testAsteriskInStatement() throws MdxParserException {
            String mdx = "SELECT * FROM [c]";

            SelectStatement selectStatement = parseSelectStatement(mdx);
            assertThat(selectStatement).isNotNull();
            assertThat(selectStatement.selectQueryClause()).isNotNull()
                    .isInstanceOf(SelectQueryAsteriskClause.class);

        }

        @Test
        public void testAsteriskInClause() throws MdxParserException {
            String mdx = "*";
            SelectQueryAsteriskClause clause = parseSelectQueryAsteriskClause(mdx);
            assertThat(clause).isNotNull();
        }

    }
}
