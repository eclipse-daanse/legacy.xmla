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
import static org.eclipse.daanse.mdx.parser.ccc.Util.parseSelectStatement;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.daanse.mdx.model.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.SelectStatement;
import org.eclipse.daanse.mdx.model.select.SelectSubcubeClauseName;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CubeTest {

    @Nested
    public class SelectSubcubeClauseNameTest {
        @ParameterizedTest
        @ValueSource(strings = { "c", //
                "cube", // Reserved Word but quoted
                "with whitespace", "with [inner]", "1", "." })
        public void testQuoted(String cubeName) throws MdxParserException {
            String mdx = mdxCubeNameQuoted(cubeName);

            SelectStatement selectStatement = parseSelectStatement(mdx);
            assertThat(selectStatement).isNotNull();
            assertThat(selectStatement.selectSubcubeClause()).isNotNull()
                    .isInstanceOfSatisfying(SelectSubcubeClauseName.class, s -> {
                        assertThat(s.cubeName()).isNotNull()
                                .satisfies(n -> {
                                    assertThat(n.name()).isEqualTo(cubeName);
                                    assertThat(n.quoting()).isEqualByComparingTo(Quoting.QUOTED);
                                });
                    });
        }

        @ParameterizedTest
        @ValueSource(strings = { "[]", "[a].[a]" })
        public void testQuotedFail(String cubeName) throws MdxParserException {
            String mdx = mdxCubeName(cubeName);

            assertThrows(MdxParserException.class, () -> parseSelectStatement(mdx));

        }

        @ParameterizedTest
        @ValueSource(strings = { "a", "a1", "aAaAaA", "AaAaAaA" })
        public void testUnquoted(String cubeName) throws MdxParserException {
            String mdx = mdxCubeName(cubeName);

            SelectStatement selectStatement = parseSelectStatement(mdx);
            assertThat(selectStatement).isNotNull();
            assertThat(selectStatement.selectSubcubeClause()).isNotNull()
                    .isInstanceOfSatisfying(SelectSubcubeClauseName.class, s -> {
                        assertThat(s.cubeName()).isNotNull()
                                .satisfies(n -> {
                                    assertThat(n.name()).isEqualTo(cubeName);
                                    assertThat(n.quoting()).isEqualByComparingTo(Quoting.UNQUOTED);
                                });
                    });
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "1", "1 1", "a a", "-", "cube", // Reserved Word
                "CURRENTCUBE"// Reserved Word
        })
        public void testUnquotedFail(String cubeName) throws MdxParserException {
            String mdx = mdxCubeName(cubeName);

            assertThrows(MdxParserException.class, () -> parseSelectStatement(mdx));

            //
        }

        private static String mdxCubeNameQuoted(String cubeName) {
            cubeName = cubeName.replace("]", "]]");
            return mdxCubeName("[" + cubeName + "]");
        }

        private static String mdxCubeName(String cubeName) {
            return "SELECT FROM " + cubeName;
        }
    }

}
