/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package mondrian.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.impl.RectangularCellSetFormatter;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

public class TestExcel {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void test(Context context) {
        String mdxQueryString = """
                SELECT NON EMPTY CrossJoin(Hierarchize(AddCalculatedMembers({DrilldownLevel({[Store].[All Stores]})})),Hierarchize(AddCalculatedMembers({DrilldownLevel({[Position].[All Position]})}))) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [HR] WHERE ([Measures].[Count]) CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
                """;

        QueryComponent queryComponent = context.getConnectionWithDefaultRole().parseStatement(mdxQueryString);

        assertThat(queryComponent).isInstanceOf(Query.class);
        if (queryComponent instanceof Query query) {
            Statement statement = context.getConnectionWithDefaultRole().createStatement();
            CellSet cellSet = statement.executeQuery(query);

            System.out.println(cellSet);
            
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter= new PrintWriter(stringWriter);
            new RectangularCellSetFormatter(true).format(cellSet, printWriter);

            
            System.out.println(stringWriter);
            System.out.println("...");
        }

    }
}
