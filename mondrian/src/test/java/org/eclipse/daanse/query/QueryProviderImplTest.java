package org.eclipse.daanse.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.mdx.model.api.RefreshStatement;
import org.eclipse.daanse.mdx.model.api.SelectStatement;
import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.Axis;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxesClause;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxisClause;
import org.eclipse.daanse.mdx.model.api.select.SelectSlicerAxisClause;
import org.eclipse.daanse.mdx.model.api.select.SelectSubcubeClauseName;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Refresh;
import org.eclipse.daanse.olap.query.base.QueryProviderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class QueryProviderImplTest {

    private QueryProviderImpl queryProviderImpl;
    private StringWriter stringWriter;
    private PrintWriter printWriter;


    @BeforeEach
    protected void setUp() throws Exception {
        queryProviderImpl = new QueryProviderImpl();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

    }

    @Test
    void refreshStatementToQueryQuotedTest() {
        RefreshStatement refreshStatement = mock(RefreshStatement.class);
        NameObjectIdentifier nameObjectIdentifier = mock(NameObjectIdentifier.class);
        when(nameObjectIdentifier.name()).thenReturn("cube");
        when(nameObjectIdentifier.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);
        when(refreshStatement.cubeName()).thenReturn(nameObjectIdentifier);
        Statement statement = mock(Statement.class);
        QueryComponent queryPart = queryProviderImpl.createQuery(statement, refreshStatement, false);
        assertThat(queryPart).isNotNull().isInstanceOf(Refresh.class);
        Refresh refresh = (Refresh) queryPart;
        assertThat(refresh.getCubeName()).isEqualTo("cube");
        assertThat(refresh.getChildren()).isNotNull();
        assertThat(refresh.getChildren()).hasSize(1);
        assertThat(refresh.getChildren()[0]).isEqualTo("cube");
        refresh.unparse(printWriter);
        assertThat(stringWriter.toString()).isNotNull().isEqualTo("REFRESH CUBE [cube]");
    }

    @Test
    void refreshStatementToQueryUnquotedTest() {
        RefreshStatement refreshStatement = mock(RefreshStatement.class);
        NameObjectIdentifier nameObjectIdentifier = mock(NameObjectIdentifier.class);
        when(nameObjectIdentifier.name()).thenReturn("cube");
        when(nameObjectIdentifier.quoting()).thenReturn(ObjectIdentifier.Quoting.UNQUOTED);
        when(refreshStatement.cubeName()).thenReturn(nameObjectIdentifier);
        Statement statement = mock(Statement.class);
        QueryComponent queryPart = queryProviderImpl.createQuery(statement, refreshStatement, false);
        assertThat(queryPart).isNotNull().isInstanceOf(Refresh.class);
        Refresh refresh = (Refresh) queryPart;
        assertThat(refresh.getCubeName()).isEqualTo("cube");
        assertThat(refresh.getChildren()).isNotNull();
        assertThat(refresh.getChildren()).hasSize(1);
        assertThat(refresh.getChildren()[0]).isEqualTo("cube");
        refresh.unparse(printWriter);
        assertThat(stringWriter.toString()).isNotNull().isEqualTo("REFRESH CUBE [cube]");
    }

    @Test
    void selectStatementToQueryTest() {
        String mdx = """
            SELECT [Customer].[Gender].[Gender].Membmers ON COLUMNS,
                     {[Customer].[Customer].[Aaron A. Allen],
                      [Customer].[Customer].[Abigail Clark]} ON ROWS
               FROM [Adventure Works]
               WHERE [Measures].[Internet Sales Amount]
            """;

        SelectStatement selectStatement = mock(SelectStatement.class);
        SelectQueryAxesClause selectQueryAxesClause = mock(SelectQueryAxesClause.class);
        SelectQueryAxisClause selectQueryAxisClause1  = getSelectQueryAxisClause1();
        SelectQueryAxisClause selectQueryAxisClause2  = getSelectQueryAxisClause2();
        SelectSubcubeClauseName selectSubcubeClauseName = mock(SelectSubcubeClauseName.class);
        NameObjectIdentifier nameObjectIdentifier = mock(NameObjectIdentifier.class);
        SelectSlicerAxisClause selectSlicerAxisClause = getSelectSlicerAxisClause();

        when(nameObjectIdentifier.name()).thenReturn("Adventure Works");
        when(nameObjectIdentifier.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);

        when(selectSubcubeClauseName.cubeName()).thenReturn(nameObjectIdentifier);


        when(selectQueryAxesClause.selectQueryAxisClauses())
            .thenAnswer(setupDummyListAnswer(selectQueryAxisClause1, selectQueryAxisClause2));

        when(selectStatement.selectQueryClause()).thenReturn(selectQueryAxesClause);
        when(selectStatement.selectSubcubeClause()).thenReturn(selectSubcubeClauseName);
        when(selectStatement.selectSlicerAxisClause()).thenReturn(Optional.of(selectSlicerAxisClause));
        when(selectStatement.selectCellPropertyListClause()).thenReturn(Optional.empty());

        //TODO
        //QueryPart queryPart = queryProviderImpl.createQuery(selectStatement);
        //queryPart.unparse(printWriter);
        //assertThat(stringWriter.toString()).isNotNull().isEqualTo(mdx);
    }

    private SelectSlicerAxisClause getSelectSlicerAxisClause() {
        SelectSlicerAxisClause selectSlicerAxisClause = mock(SelectSlicerAxisClause.class);
        CompoundId compoundId = mock(CompoundId.class);
        NameObjectIdentifier nameObjectIdentifier1 = mock(NameObjectIdentifier.class);
        NameObjectIdentifier nameObjectIdentifier2 = mock(NameObjectIdentifier.class);

        when(nameObjectIdentifier1.name()).thenReturn("Measures");
        when(nameObjectIdentifier1.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);
        when(nameObjectIdentifier2.name()).thenReturn("Internet Sales Amount");
        when(nameObjectIdentifier2.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);


        when(compoundId.objectIdentifiers()).thenAnswer(
            setupDummyListAnswer(nameObjectIdentifier1, nameObjectIdentifier2));

        when(selectSlicerAxisClause.expression()).thenReturn(compoundId);

        return selectSlicerAxisClause;
    }

    private SelectQueryAxisClause getSelectQueryAxisClause2() {
        SelectQueryAxisClause selectQueryAxisClause = mock(SelectQueryAxisClause.class);
        CallExpression callExpression1 = mock(CallExpression.class);
        CompoundId compoundId1 = mock(CompoundId.class);
        CompoundId compoundId2 = mock(CompoundId.class);
        NameObjectIdentifier nObjectIdentifier1_1 = mock(NameObjectIdentifier.class);
        NameObjectIdentifier nObjectIdentifier2_1 = mock(NameObjectIdentifier.class);
        NameObjectIdentifier nObjectIdentifier3_1 = mock(NameObjectIdentifier.class);

        NameObjectIdentifier nObjectIdentifier1_2 = mock(NameObjectIdentifier.class);
        NameObjectIdentifier nObjectIdentifier2_2 = mock(NameObjectIdentifier.class);
        NameObjectIdentifier nObjectIdentifier3_2 = mock(NameObjectIdentifier.class);

        Axis axis = mock(Axis.class);
        when(axis.ordinal()).thenReturn(1);
        when(axis.named()).thenReturn(true);

        when(nObjectIdentifier1_1.name()).thenReturn("Customer");
        when(nObjectIdentifier1_1.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);
        when(nObjectIdentifier2_1.name()).thenReturn("Customer");
        when(nObjectIdentifier2_1.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);
        when(nObjectIdentifier3_1.name()).thenReturn("Aaron A. Allen");
        when(nObjectIdentifier3_1.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);


        when(compoundId1.objectIdentifiers()).thenAnswer(setupDummyListAnswer(
            nObjectIdentifier1_1, nObjectIdentifier2_1, nObjectIdentifier3_1));

        when(nObjectIdentifier1_2.name()).thenReturn("Customer");
        when(nObjectIdentifier1_2.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);
        when(nObjectIdentifier2_2.name()).thenReturn("Customer");
        when(nObjectIdentifier2_2.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);
        when(nObjectIdentifier3_2.name()).thenReturn("Abigail Clark");
        when(nObjectIdentifier3_2.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);


        when(compoundId2.objectIdentifiers()).thenAnswer(setupDummyListAnswer(
            nObjectIdentifier1_2, nObjectIdentifier2_2, nObjectIdentifier3_2));


        when(callExpression1.operationAtom()).thenReturn(new BracesOperationAtom());
        when(callExpression1.expressions()).thenAnswer(setupDummyListAnswer(compoundId1, compoundId2));

        when(selectQueryAxisClause.nonEmpty()).thenReturn(false);
        when(selectQueryAxisClause.expression()).thenReturn(callExpression1);
        when(selectQueryAxisClause.axis()).thenReturn(axis);

        return selectQueryAxisClause;

    }

    private SelectQueryAxisClause getSelectQueryAxisClause1() {
        SelectQueryAxisClause selectQueryAxisClause = mock(SelectQueryAxisClause.class);
        Axis axis = mock(Axis.class);

        CallExpression callExpression = mock(CallExpression.class);
        NameObjectIdentifier nObjectIdentifier1 = mock(NameObjectIdentifier.class);
        NameObjectIdentifier nObjectIdentifier2 = mock(NameObjectIdentifier.class);
        NameObjectIdentifier nObjectIdentifier3 = mock(NameObjectIdentifier.class);

        when(nObjectIdentifier1.name()).thenReturn("Customer");
        when(nObjectIdentifier1.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);
        when(nObjectIdentifier2.name()).thenReturn("Gender");
        when(nObjectIdentifier2.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);
        when(nObjectIdentifier3.name()).thenReturn("Gender");
        when(nObjectIdentifier3.quoting()).thenReturn(ObjectIdentifier.Quoting.QUOTED);

        CompoundId compoundId = mock(CompoundId.class);
        when(compoundId.objectIdentifiers()).thenAnswer(
            setupDummyListAnswer(nObjectIdentifier1, nObjectIdentifier2, nObjectIdentifier3)
        );
        when(callExpression.operationAtom()).thenReturn(new PlainPropertyOperationAtom("Membmers"));
        when(callExpression.expressions()).thenAnswer(setupDummyListAnswer(compoundId));

        when(axis.ordinal()).thenReturn(0);
        when(axis.named()).thenReturn(true);

        when(selectQueryAxisClause.nonEmpty()).thenReturn(true);
        when(selectQueryAxisClause.expression()).thenReturn(callExpression);
        when(selectQueryAxisClause.axis()).thenReturn(axis);

        return selectQueryAxisClause;
    }

    private static  <N> Answer<List<N>> setupDummyListAnswer(N... values) {
        final List<N> someList = new LinkedList<>(Arrays.asList(values));

        Answer<List<N>> answer = new Answer<>() {
            @Override
            public List<N> answer(InvocationOnMock invocation) throws Throwable {
                return someList;
            }
        };
        return answer;
    }
}
