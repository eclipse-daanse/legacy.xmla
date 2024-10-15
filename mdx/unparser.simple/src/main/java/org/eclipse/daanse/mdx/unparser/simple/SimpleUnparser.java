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
package org.eclipse.daanse.mdx.unparser.simple;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.daanse.mdx.model.api.DMVStatement;
import org.eclipse.daanse.mdx.model.api.DrillthroughStatement;
import org.eclipse.daanse.mdx.model.api.ExplainStatement;
import org.eclipse.daanse.mdx.model.api.MdxStatement;
import org.eclipse.daanse.mdx.model.api.RefreshStatement;
import org.eclipse.daanse.mdx.model.api.ReturnItem;
import org.eclipse.daanse.mdx.model.api.SelectStatement;
import org.eclipse.daanse.mdx.model.api.UpdateStatement;
import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.KeyObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.Literal;
import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NullLiteral;
import org.eclipse.daanse.mdx.model.api.expression.NumericLiteral;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.StringLiteral;
import org.eclipse.daanse.mdx.model.api.expression.SymbolLiteral;
import org.eclipse.daanse.mdx.model.api.expression.operation.AmpersandQuotedPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.CaseOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.CastOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.EmptyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InternalOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PostfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.QuotedPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.Axis;
import org.eclipse.daanse.mdx.model.api.select.CreateCellCalculationBodyClause;
import org.eclipse.daanse.mdx.model.api.select.CreateMemberBodyClause;
import org.eclipse.daanse.mdx.model.api.select.CreateSetBodyClause;
import org.eclipse.daanse.mdx.model.api.select.MeasureBodyClause;
import org.eclipse.daanse.mdx.model.api.select.MemberPropertyDefinition;
import org.eclipse.daanse.mdx.model.api.select.SelectCellPropertyListClause;
import org.eclipse.daanse.mdx.model.api.select.SelectDimensionPropertyListClause;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAsteriskClause;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxesClause;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxisClause;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryClause;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryEmptyClause;
import org.eclipse.daanse.mdx.model.api.select.SelectSlicerAxisClause;
import org.eclipse.daanse.mdx.model.api.select.SelectSubcubeClause;
import org.eclipse.daanse.mdx.model.api.select.SelectSubcubeClauseName;
import org.eclipse.daanse.mdx.model.api.select.SelectSubcubeClauseStatement;
import org.eclipse.daanse.mdx.model.api.select.SelectWithClause;
import org.eclipse.daanse.mdx.unparser.api.UnParser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.converter.Converters;

@Component
@Designate(ocd = SimpleUnparser.Config.class)
public class SimpleUnparser implements UnParser {

    private static final String DELIMITER = "\r\n ";

    @ObjectClassDefinition()
    public interface Config {

        @AttributeDefinition(defaultValue = "false")
        default boolean commentUnusedElements() {
            return true;
        }
    }

    int indent = 0;

    @SuppressWarnings("java:S1068")
    private Config config = null;

    @Activate
    public void activate(Map<String, Object> configMap) {
        modifies(configMap);

    }

    @Deactivate
    public void deActivate(Map<String, Object> configMap) {
        this.config = null;

    }

    @Modified
    public void modifies(Map<String, Object> configMap) {
        this.config = Converters.standardConverter().convert(configMap).to(Config.class);

    }

    public StringBuilder unparseSelectStatement(SelectStatement selectStatement) {
        StringBuilder sb = new StringBuilder();
        if (!selectStatement.selectWithClauses().isEmpty()) {
            sb = sb.append("WITH ");
            sb = sb.append(unparseSelectWithClauses(selectStatement.selectWithClauses()));
            sb = sb.append(" ");
        }

        sb = sb.append("SELECT ");
        sb = sb.append(unparseSelectQueryClause(selectStatement.selectQueryClause()));
        sb = sb.append(" FROM ");
        sb = sb.append(unparseSelectSubcubeClause(selectStatement.selectSubcubeClause()));

        Optional<SelectSlicerAxisClause> ssac = selectStatement.selectSlicerAxisClause();
        if (ssac.isPresent()) {
            sb = sb.append(" ");// whitespace before WHERE
            sb.append(unparseSelectSlicerAxisClause(ssac.get()));
        }

        Optional<SelectCellPropertyListClause> ssplc = selectStatement.selectCellPropertyListClause();
        if (ssplc.isPresent()) {
            sb.append(" ");
            sb = sb.append(unparseSelectCellPropertyListClause(ssplc.get()));
        }

        return sb;

    }

    public StringBuilder unparseSelectCellPropertyListClause(SelectCellPropertyListClause clause) {
        StringBuilder sb = new StringBuilder();

        if (clause.cell()) {

            sb.append("CELL ");
        }
        sb.append(unparseProperties(clause.properties()));
        return sb;
    }

    public StringBuilder unparseProperties(List<String> propertyList) {
        StringBuilder sb = new StringBuilder();
        if (propertyList != null && !propertyList.isEmpty()) {
            sb.append("PROPERTIES ");

            String properties = propertyList.stream().collect(Collectors.joining("\r\n, "));
            sb.append(properties);
        }
        return sb;
    }

    public StringBuilder unparseSelectSlicerAxisClause(SelectSlicerAxisClause clause) {
        StringBuilder sb = new StringBuilder("WHERE ");

        return sb.append(unparseExpression(clause.expression()));
    }

    public StringBuilder unparseSelectSubcubeClause(SelectSubcubeClause clause) {

        if (clause instanceof SelectSubcubeClauseName sscn) {
            return unparseSelectSubcubeClauseName(sscn);
        }
        if (clause instanceof SelectSubcubeClauseStatement sscs) {
            return unparseSelectSubcubeClauseStatement(sscs);
        }

        return null;
    }

    public StringBuilder unparseSelectSubcubeClauseStatement(SelectSubcubeClauseStatement clause) {
        StringBuilder sb = new StringBuilder();

        Optional<SelectSlicerAxisClause> sOptional = clause.selectSlicerAxisClause();

        sb.append(" ( \r\n");
        sb.append("  SELECT \r\n");
        sb.append(unparseSelectQueryClause(clause.selectQueryClause()));
        sb.append(" FROM \r\n");
        sb.append(unparseSelectSubcubeClause(clause.selectSubcubeClause()));

        if (sOptional.isPresent()) {

            sb.append(unparseSelectSlicerAxisClause(sOptional.get()));
        }
        sb.append("\r\n");
        sb.append(" ) \r\n");

        return sb;
    }

    public StringBuilder unparseSelectSubcubeClauseName(SelectSubcubeClauseName clause) {

        return unparseNameObjectIdentifier(clause.cubeName());
    }

    private StringBuilder unparseNameObjectIdentifier(NameObjectIdentifier nameObjectIdentifier) {

        StringBuilder sb = new StringBuilder();
        switch (nameObjectIdentifier.quoting()) {
            case KEY -> sb.append("&").append(nameObjectIdentifier.name());

            case QUOTED -> sb.append("[").append(nameObjectIdentifier.name().replace("]", "]]")).append("]");

            case UNQUOTED -> sb.append(nameObjectIdentifier.name());

        }

        return sb;
    }

    public StringBuilder unparseSelectQueryClause(SelectQueryClause clause) {

        if (clause instanceof SelectQueryAsteriskClause) {
            return unparseSelectQueryAsteriskClause();
        }
        if (clause instanceof SelectQueryAxesClause sqaxc) {
            return unparseSelectQueryAxesClause(sqaxc);

        }
        if (clause instanceof SelectQueryEmptyClause) {
            return unparseSelectQueryEmptyClause();

        }

        return null;
    }

    private StringBuilder unparseSelectQueryEmptyClause() {
        return new StringBuilder();/* empty */
    }

    private StringBuilder unparseSelectQueryAxesClause(SelectQueryAxesClause clause) {

        String ret = clause.selectQueryAxisClauses().stream().map(this::unparseSelectQueryAxisClause)
            .map(Object::toString).collect(Collectors.joining("\r\n,"));
        return new StringBuilder(ret);
    }

    public StringBuilder unparseSelectQueryAxisClause(SelectQueryAxisClause clause) {
        StringBuilder sb = new StringBuilder();

        if (clause.nonEmpty()) {

            sb.append("NON EMPTY ");
        }
        sb.append(unparseExpression(clause.expression()));
        sb.append(" ON ");
        sb.append(unparseAxis(clause.axis()));

        return sb;
    }

    public StringBuilder unparseExpression(MdxExpression expression) {

        if (expression instanceof CallExpression ce) {
            return unparseCallExpression(ce);
        }
        if (expression instanceof Literal l) {
            return unparseLiteral(l);
        }
        if (expression instanceof CompoundId cId) {
            return unparseCompoundId(cId);
        }
        if (expression instanceof ObjectIdentifier oi) {
            return unparseObjectIdentifier(oi);
        }

        return null;
    }

    private StringBuilder unparseObjectIdentifier(ObjectIdentifier objectIdentifier) {

        if (objectIdentifier instanceof KeyObjectIdentifier koi) {

            return unparseKeyObjectIdentifier(koi);
        }
        if (objectIdentifier instanceof NameObjectIdentifier noi) {

            return unparseNameObjectIdentifier(noi);

        }
        return null;
    }

    private StringBuilder unparseKeyObjectIdentifier(KeyObjectIdentifier koi) {
        StringBuilder sb = new StringBuilder();

        String s = koi.nameObjectIdentifiers().stream().map(this::unparseNameObjectIdentifier).map(Object::toString)
            .collect(Collectors.joining("&"));

        sb.append("&").append(s);
        return sb;
    }

    private StringBuilder unparseCompoundId(CompoundId compoundId) {
        String s = compoundId.objectIdentifiers().stream().map(this::unparseObjectIdentifier)
            .collect(Collectors.joining("."));
        return new StringBuilder(s);
    }

    private String unparseCompoundIds(List<? extends CompoundId> compoundIdList) {
        return compoundIdList.stream().map(this::unparseCompoundId).map(Object::toString).collect(Collectors.joining(
            ","));
    }

    private StringBuilder unparseLiteral(Literal literal) {

        if (literal instanceof NullLiteral) {
            return unparseNullLiteral();
        }
        if (literal instanceof NumericLiteral numericLiteral) {
            return unparseNumericLiteral(numericLiteral);

        }
        if (literal instanceof StringLiteral stringLiteral) {
            return unparseStringLiteral(stringLiteral);

        }
        if (literal instanceof SymbolLiteral symbolLiteral) {
            return unparseSymbolLiteral(symbolLiteral);

        }

        return null;
    }

    private StringBuilder unparseSymbolLiteral(SymbolLiteral symbolLiteral) {
        return new StringBuilder(symbolLiteral.value());
    }

    private StringBuilder unparseStringLiteral(StringLiteral stringLiteral) {
        return new StringBuilder(stringLiteral.value());

    }

    private StringBuilder unparseNumericLiteral(NumericLiteral numericLiteral) {
        return new StringBuilder(numericLiteral.value().toString());

    }

    private StringBuilder unparseNullLiteral() {
        return new StringBuilder("NULL");

    }

    private StringBuilder unparseCallExpression(CallExpression callExpression) {
        StringBuilder sb = new StringBuilder();
        String name = callExpression.operationAtom().name();
        List<? extends MdxExpression> expressions = callExpression.expressions();
        String expressionText;
        String object = "";
        if (callExpression.operationAtom() instanceof MethodOperationAtom && !expressions.isEmpty()) {
            expressionText = unparseExpressions(expressions.subList(1, expressions.size()));
            object = unparseExpression(expressions.get(0)).toString();
        } else {
            expressionText = unparseExpressions(expressions);
        }
        switch (callExpression.operationAtom()) {
        	case AmpersandQuotedPropertyOperationAtom _UNNAMED-> sb.append(expressionText).append(".[&").append(name).append("]");
            case BracesOperationAtom _UNNAMED -> sb.append("{").append(expressionText).append("}");
            case CastOperationAtom _UNNAMED-> sb.append("CAST(").append(expressionText.replace(",", " AS ")).append(")");
            case CaseOperationAtom _UNNAMED-> {
            	int size = expressions.size();
            	sb.append("CASE ");
            	sb.append(unparseExpression(expressions.get(0)));
            	
            	for (int i = 1; i < size - 1; i++) {
            		sb.append(" WHEN ");
            		sb.append(unparseExpression(expressions.get(i)));
            		
            	}
            	
            	sb.append(" THEN ");
            	sb.append(unparseExpression(expressions.get(size - 1)));
            	sb.append(" END ");
            	
            }
            case EmptyOperationAtom _UNNAMED-> sb.append("");
            case FunctionOperationAtom _UNNAMED-> sb.append(name).append("(").append(expressionText).append(")");
            case InfixOperationAtom _UNNAMED-> {
            	sb.append(unparseExpression(expressions.get(0)));
            	sb.append(" ");
            	sb.append(name);
            	sb.append(" ");
            	sb.append(unparseExpression(expressions.get(1)));
            }
            case InternalOperationAtom _UNNAMED-> sb.append("$").append(expressionText);
            case MethodOperationAtom _UNNAMED-> sb.append(object).append(".").append(name).append("(").append(expressionText).append(")");
            case ParenthesesOperationAtom _UNNAMED-> sb.append("(").append(expressionText).append(")");
            case PlainPropertyOperationAtom _UNNAMED-> sb.append(expressionText).append(".").append(name);
            case PostfixOperationAtom _UNNAMED-> sb.append(expressionText).append(" ").append(name);
            case PrefixOperationAtom _UNNAMED-> sb.append(name).append(" ").append(expressionText);
            case QuotedPropertyOperationAtom _UNNAMED-> sb.append(expressionText).append(".&").append(name).append("");

        }

        return sb;
    }

    private String unparseExpressions(List<? extends MdxExpression> expressions) {
        return expressions.stream().map(this::unparseExpression).map(Object::toString).collect(Collectors.joining(","));
    }

    private StringBuilder unparseSelectQueryAsteriskClause() {
        return new StringBuilder("*");
    }

    public StringBuilder unparseSelectWithClauses(List<? extends SelectWithClause> clauses) {

        String s = clauses.stream().map(this::unparseSelectWithClause).map(Object::toString)
            .collect(Collectors.joining(DELIMITER));
        return new StringBuilder(s);

    }

    public StringBuilder unparseSelectWithClause(SelectWithClause clause) {
        if (clause instanceof CreateCellCalculationBodyClause) {
            return unparseCreateCellCalculationBodyClause();
        } else if (clause instanceof CreateMemberBodyClause c) {
            return unparseCreateMemberBodyClause(c);
        } else if (clause instanceof CreateSetBodyClause c) {
            return unparseCreateSetBodyClause(c);
        } else if (clause instanceof MeasureBodyClause) {
            return unparseMeasureBodyClause();
        }
        return new StringBuilder();

    }

    public StringBuilder unparseCreateCellCalculationBodyClause() {

        return new StringBuilder();

    }

    public StringBuilder unparseCreateMemberBodyClause(CreateMemberBodyClause clause) {

        StringBuilder sb = new StringBuilder();
        sb.append("MEMBER ");

        sb.append(unparseCompoundId(clause.compoundId())).append(" AS ").append(unparseExpression(clause.expression()));

        if (!clause.memberPropertyDefinitions().isEmpty()) {
            sb.append(" ");

            String ret = clause.memberPropertyDefinitions().stream().map(this::unparseMemberPropertyDefinition)
                .collect(Collectors.joining("\r\n,", ",\r\n ", ""));
            sb.append(ret);
        }

        return sb;

    }

    public StringBuilder unparseMemberPropertyDefinition(MemberPropertyDefinition mpd) {

        StringBuilder sb = new StringBuilder();
        sb.append(unparseObjectIdentifier(mpd.objectIdentifier()));
        sb.append(" = ");
        sb.append(unparseExpression(mpd.expression()));
        return sb;
    }

    public StringBuilder unparseCreateSetBodyClause(CreateSetBodyClause clause) {

        StringBuilder sb = new StringBuilder();
        sb.append("SET ");
        sb.append(unparseCompoundId(clause.compoundId())).append(" AS ").append(unparseExpression(clause.expression()));

        return sb;

    }

    public StringBuilder unparseMeasureBodyClause() {
        return new StringBuilder();

    }

    public StringBuilder unparseDrillthroughStatement(DrillthroughStatement statement) {
        StringBuilder sb = new StringBuilder();
        sb.append("DRILLTHROUGH");

        statement.maxRows().ifPresent(sb.append(DELIMITER).append("MAXROWS").append(" ")::append);

        statement.firstRowSet().ifPresent(sb.append(DELIMITER).append("FIRSTROWSET").append(" ")::append);

        sb.append(DELIMITER).append(unparseSelectStatement(statement.selectStatement()));

        if (statement.returnItems() != null && !statement.returnItems().isEmpty()) {
            sb.append(DELIMITER).append(unparseReturnItems(statement.returnItems()));
        }

        return sb;
    }

    public StringBuilder unparseReturnItems(List<? extends ReturnItem> returnItems) {
        StringBuilder sb = new StringBuilder();
        if (!returnItems.isEmpty()) {
            sb.append("RETURN ");
            sb.append(returnItems.stream().map(r -> unparseCompoundId(r.compoundId())).map(Object::toString)
                .collect(Collectors.joining(",")));
        }
        return sb;
    }

    public StringBuilder unparseExplainStatement(ExplainStatement selectStatement) {
        StringBuilder sb = new StringBuilder();
        sb.append("EXPLAIN PLAN FOR");
        if (selectStatement.mdxStatement() != null) {
            sb.append(DELIMITER).append(unparseMdxStatement(selectStatement.mdxStatement()));
        }
        return sb;
    }

    public StringBuilder unparseDMVStatement(DMVStatement selectStatement) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(DELIMITER);
        sb.append(unparseCompoundIds(selectStatement.columns())).append(DELIMITER);
        sb.append("FROM $SYSTEM.").append(unparseNameObjectIdentifier(selectStatement.table()));
        if (selectStatement.where() != null) {
            sb.append(DELIMITER).append("WHERE ").append(unparseExpression(selectStatement.where()));
        }
        return sb;
    }

    public StringBuilder unparseRefreshStatement(RefreshStatement selectStatement) {
        StringBuilder sb = new StringBuilder();
        if (selectStatement.cubeName() != null) {
            sb.append("REFRESH CUBE ").append(unparseNameObjectIdentifier(selectStatement.cubeName()));
        }
        return sb;
    }

    public StringBuilder unparseUpdateStatement(UpdateStatement updateStatement) {
        StringBuilder sb = new StringBuilder();
        if (updateStatement.cubeName() != null) {
            sb.append("UPDATE CUBE ").append(unparseNameObjectIdentifier(updateStatement.cubeName()));
        }
        return sb;
    }

    public StringBuilder unparseSelectDimensionPropertyListClause(SelectDimensionPropertyListClause clause) {
        StringBuilder sb = new StringBuilder();
        if (clause.properties() != null) {
            sb.append("DIMENSION");
            sb.append(DELIMITER);
            sb.append("PROPERTIES ");
            sb.append(unparseCompoundIds(clause.properties()));
        }
        return sb;
    }

    @Override
    public StringBuilder unparseMdxStatement(MdxStatement mdxStatement) {

        if (mdxStatement instanceof SelectStatement selectStatement) {
            return unparseSelectStatement(selectStatement);
        } else if (mdxStatement instanceof DrillthroughStatement drillthroughStatement) {
            return unparseDrillthroughStatement(drillthroughStatement);
        } else if (mdxStatement instanceof ExplainStatement explainStatement) {
            return unparseExplainStatement(explainStatement);
        } else if (mdxStatement instanceof DMVStatement dMVStatement) {
            return unparseDMVStatement(dMVStatement);
        }

        return new StringBuilder();
    }

    public StringBuilder unparseAxis(Axis axis) {
        return new StringBuilder().append(axis.named() ? axis.name().toUpperCase() : axis.ordinal());

    }

}
