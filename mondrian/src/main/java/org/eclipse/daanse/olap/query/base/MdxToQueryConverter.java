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
package org.eclipse.daanse.olap.query.base;

import mondrian.mdx.UnresolvedFunCallImpl;
import mondrian.olap.CellPropertyImpl;
import mondrian.olap.FormulaImpl;
import mondrian.olap.IdImpl;
import mondrian.olap.MemberPropertyImpl;
import mondrian.olap.NullLiteralImpl;
import mondrian.olap.NumericLiteralImpl;
import mondrian.olap.QueryAxisImpl;
import mondrian.olap.StringLiteralImpl;
import mondrian.olap.SubcubeImpl;
import mondrian.olap.SymbolLiteralImpl;
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
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PostfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.QuotedPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.Axis;
import org.eclipse.daanse.mdx.model.api.select.CreateMemberBodyClause;
import org.eclipse.daanse.mdx.model.api.select.CreateSetBodyClause;
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
import org.eclipse.daanse.olap.api.NameSegment;
import org.eclipse.daanse.olap.api.Quoting;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.SubtotalVisibility;
import org.eclipse.daanse.olap.api.query.component.AxisOrdinal;
import org.eclipse.daanse.olap.api.query.component.CellProperty;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Id;
import org.eclipse.daanse.olap.api.query.component.MemberProperty;
import org.eclipse.daanse.olap.api.query.component.Subcube;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MdxToQueryConverter {

	private MdxToQueryConverter() {
		// constructor
	}

	static List<String> convertColumns(List<? extends ObjectIdentifier> objectIdentifiers) {
		List<String> columns = new ArrayList<>();
		if (objectIdentifiers != null) {
			objectIdentifiers.forEach(oi -> {
				if (oi instanceof KeyObjectIdentifier keyObjectIdentifier) {
					List<? extends NameObjectIdentifier> l = keyObjectIdentifier.nameObjectIdentifiers();
					if (l != null) {
						columns.addAll(l.stream().map(MdxToQueryConverter::convertName).toList());
					}
				}
				if (oi instanceof NameObjectIdentifier nameObjectIdentifier) {
					columns.add(convertName(nameObjectIdentifier));
				}
			});
		}
		return columns;
	}

	static List<CellProperty> convertParameterList(
			Optional<SelectCellPropertyListClause> selectCellPropertyListClauseOptional) {
		if (selectCellPropertyListClauseOptional.isPresent()) {
			SelectCellPropertyListClause selectCellPropertyListClause = selectCellPropertyListClauseOptional.get();
			if (selectCellPropertyListClause.properties() != null) {
				List<Segment> list = selectCellPropertyListClause.properties().stream()
						.map(p -> ((Segment) new IdImpl.NameSegmentImpl(p))).toList();
                return list.stream().map(s -> (CellProperty)new CellPropertyImpl(List.of(s))).toList();
				//return List.of(new CellPropertyImpl(list));
			}
		}
		return List.of();
	}

	static QueryAxisImpl convertQueryAxis(Optional<SelectSlicerAxisClause> selectSlicerAxisClauseOptional) {
		if (selectSlicerAxisClauseOptional.isPresent()) {
			SelectSlicerAxisClause selectSlicerAxisClause = selectSlicerAxisClauseOptional.get();
			MdxExpression expression = selectSlicerAxisClause.expression();
			Expression exp = getExpression(expression);
			boolean nonEmpty = false;
			Id[] dimensionProperties = new Id[0];

			return new QueryAxisImpl(nonEmpty, exp, AxisOrdinal.StandardAxisOrdinal.SLICER, SubtotalVisibility.Undefined,
					dimensionProperties);
		}
		return null;
	}

	static List<QueryAxisImpl> convertQueryAxisList(SelectQueryClause selectQueryClause) {
		switch (selectQueryClause) {
		case SelectQueryAsteriskClause _UNNAMED:
			return selectQueryAsteriskClauseToQueryAxisList();
		case SelectQueryAxesClause selectQueryAxesClause:
			return selectQueryAxesClauseToQueryAxisList(selectQueryAxesClause);
		case SelectQueryEmptyClause _UNNAMES:
			return selectQueryEmptyClauseToQueryAxisList();
		}
	}

	static List<QueryAxisImpl> selectQueryEmptyClauseToQueryAxisList() {
		return List.of();
	}

	static List<QueryAxisImpl> selectQueryAxesClauseToQueryAxisList(SelectQueryAxesClause selectQueryAxesClause) {
		if (selectQueryAxesClause.selectQueryAxisClauses() != null) {
			return selectQueryAxesClause.selectQueryAxisClauses().stream()
					.map(MdxToQueryConverter::selectQueryAxisClauseToQueryAxisList).toList();
		}
		return List.of();
	}

	static QueryAxisImpl selectQueryAxisClauseToQueryAxisList(SelectQueryAxisClause selectQueryAxisClause) {
		Expression exp = getExpression(selectQueryAxisClause.expression());
		AxisOrdinal axisOrdinal = getAxisOrdinal(selectQueryAxisClause.axis());
		List<IdImpl> dimensionProperties = getDimensionProperties(
				selectQueryAxisClause.selectDimensionPropertyListClause());

		return new QueryAxisImpl(selectQueryAxisClause.nonEmpty(), exp, axisOrdinal, SubtotalVisibility.Undefined,
				dimensionProperties.stream().toArray(IdImpl[]::new));
	}

	static List<IdImpl> getDimensionProperties(SelectDimensionPropertyListClause selectDimensionPropertyListClause) {
		if (selectDimensionPropertyListClause != null && selectDimensionPropertyListClause.properties() != null) {
			return selectDimensionPropertyListClause.properties().stream()
					.map(MdxToQueryConverter::getExpressionByCompoundId).toList();
		}
		return List.of();
	}

	static AxisOrdinal getAxisOrdinal(Axis axis) {
		return AxisOrdinal.StandardAxisOrdinal.forLogicalOrdinal(axis.ordinal());
	}

	static List<QueryAxisImpl> selectQueryAsteriskClauseToQueryAxisList() {
		return List.of();
	}

	static Subcube convertSubcube(SelectSubcubeClause selectSubcubeClause) {

		switch (selectSubcubeClause) {
		case SelectSubcubeClauseName selectSubcubeClauseName:
			return getSubcubeBySelectSubcubeClauseName(selectSubcubeClauseName);
		case SelectSubcubeClauseStatement selectSubcubeClauseStatement:
			return getSubcubeBySelectSubcubeClauseStatement(selectSubcubeClauseStatement);
		}
	}

	static Subcube getSubcubeBySelectSubcubeClauseStatement(SelectSubcubeClauseStatement selectSubcubeClauseStatement) {
		Subcube subcube = convertSubcube(selectSubcubeClauseStatement.selectSubcubeClause());
		List<QueryAxisImpl> axes = convertQueryAxisList(selectSubcubeClauseStatement.selectQueryClause());
		String cubeName = null;
		QueryAxisImpl slicerAxis = convertQueryAxis(selectSubcubeClauseStatement.selectSlicerAxisClause());
		return new SubcubeImpl(cubeName, subcube, axes.stream().toArray(QueryAxisImpl[]::new), slicerAxis);
	}

	static Subcube getSubcubeBySelectSubcubeClauseName(SelectSubcubeClauseName selectSubcubeClauseName) {
		NameObjectIdentifier nameObjectIdentifier = selectSubcubeClauseName.cubeName();
		String cubeName = convertName(nameObjectIdentifier);
		return new SubcubeImpl(cubeName, null, null, null);
	}

	static List<Formula> convertFormulaList(List<? extends SelectWithClause> selectWithClauses) {
		List<Formula> formulaList = new ArrayList<>();
		if (selectWithClauses != null) {
			for (SelectWithClause swc : selectWithClauses) {
				if (swc instanceof CreateMemberBodyClause createMemberBodyClause) {
					IdImpl id = getId(createMemberBodyClause.compoundId());
					Expression exp = getExpression(createMemberBodyClause.expression());
                    List<MemberProperty> mpl = convertMemberPropertyList(createMemberBodyClause.memberPropertyDefinitions());
					formulaList.add(new FormulaImpl(id, exp, mpl.toArray(new MemberProperty[0])));
				}
				if (swc instanceof CreateSetBodyClause createSetBodyClause) {
					IdImpl id = getId(createSetBodyClause.compoundId());
					Expression exp = getExpression(createSetBodyClause.expression());
					formulaList.add(new FormulaImpl(id, exp));
				}
			}
		}
		return formulaList;
	}

    private static List<MemberProperty> convertMemberPropertyList(List<? extends MemberPropertyDefinition> memberPropertyDefinitions) {
	    if (memberPropertyDefinitions != null) {
            return memberPropertyDefinitions.stream().map(p -> convertMemberProperty(p)).toList();
        }
	    return List.of();
    }

    private static MemberProperty convertMemberProperty(MemberPropertyDefinition p) {
        String name = getName(p.objectIdentifier());
        Expression exp = getExpression(p.expression());
        return new MemberPropertyImpl(name, exp);
    }

    private static String getName(ObjectIdentifier objectIdentifier) {
	    if (
	        objectIdentifier instanceof KeyObjectIdentifier keyObjectIdentifier
                && keyObjectIdentifier.nameObjectIdentifiers() != null
                && !keyObjectIdentifier.nameObjectIdentifiers().isEmpty()) {
	        return keyObjectIdentifier.nameObjectIdentifiers().get(0).name();
        }
        if (objectIdentifier instanceof NameObjectIdentifier nameObjectIdentifier) {
            return nameObjectIdentifier.name();
        }
        return null;
    }

    static Expression getExpression(MdxExpression expression) {
    	if (expression != null) {
    		switch (expression) {
    		case CompoundId compoundId:
    			return getExpressionByCompoundId(compoundId);
    		case CallExpression callExpression:
    			return getExpressionByCallExpression(callExpression);
    		case Literal literal:
    			return getExpressionByLiteral(literal);
    		case ObjectIdentifier objectIdentifier:
    			return getExpressionByObjectIdentifier(objectIdentifier);
    		}
    	}
    	return null;
	}

	static Expression getExpressionByObjectIdentifier(ObjectIdentifier objectIdentifier) {
		switch (objectIdentifier) {

		case KeyObjectIdentifier keyObjectIdentifier:
			return new IdImpl(getExpressionByKeyObjectIdentifier(keyObjectIdentifier));
		case NameObjectIdentifier nameObjectIdentifier:
			return new IdImpl(getExpressionByNameObjectIdentifier(nameObjectIdentifier));
		}

	}

	static Segment getExpressionByNameObjectIdentifier(NameObjectIdentifier nameObjectIdentifier) {
		return switch (nameObjectIdentifier.quoting()) {
		case QUOTED -> new IdImpl.NameSegmentImpl(nameObjectIdentifier.name(), Quoting.QUOTED);
		case UNQUOTED -> new IdImpl.NameSegmentImpl(nameObjectIdentifier.name(), Quoting.UNQUOTED);
		case KEY -> new IdImpl.KeySegment(new IdImpl.NameSegmentImpl(nameObjectIdentifier.name()));
		};

	}

	static List<Segment> getExpressionByKeyObjectIdentifier(KeyObjectIdentifier keyObjectIdentifier) {
		return keyObjectIdentifier.nameObjectIdentifiers().stream()
				.map(MdxToQueryConverter::getExpressionByNameObjectIdentifier).toList();
	}

	static IdImpl getExpressionByCompoundId(CompoundId compoundId) {
		List<Segment> list = new ArrayList<>();
		compoundId.objectIdentifiers().forEach(it -> {
			if (it instanceof KeyObjectIdentifier keyObjectIdentifier) {
				list.addAll(getExpressionByKeyObjectIdentifier(keyObjectIdentifier));
			}
			if (it instanceof NameObjectIdentifier nameObjectIdentifier) {
				list.add(getExpressionByNameObjectIdentifier(nameObjectIdentifier));
			}
		});
		return new IdImpl(list);
	}

	static Expression getExpressionByLiteral(Literal literal) {

		switch (literal) {
		case NumericLiteral numericLiteral:
			return NumericLiteralImpl.create(numericLiteral.value());
		case StringLiteral stringLiteral:
			return StringLiteralImpl.create(stringLiteral.value());
		case NullLiteral _UNNAMED: // TODO: use Unnamed Pattern _
			return NullLiteralImpl.nullValue;
		case SymbolLiteral symbolLiteral:
			return SymbolLiteralImpl.create(symbolLiteral.value());
		}
	}

	static Expression getExpressionByCallExpression(CallExpression callExpression) {
		Expression x;
		Expression y;
		List<Expression> list;
		OperationAtom oa = callExpression.operationAtom();

		switch (oa) {
		case InfixOperationAtom ioa:
			x = getExpression(callExpression.expressions().get(0));
			y = getExpression(callExpression.expressions().get(1));
			return new UnresolvedFunCallImpl(ioa, new Expression[] { x, y });
		case PrefixOperationAtom poa:
			x = getExpression(callExpression.expressions().get(0));
			return new UnresolvedFunCallImpl(poa, new Expression[] { x });
		case FunctionOperationAtom foa:
			list = getExpressionList(callExpression.expressions());
			return new UnresolvedFunCallImpl(foa, list.stream().toArray(Expression[]::new));
		case PlainPropertyOperationAtom ppoa:
			x = getExpression(callExpression.expressions().get(0));
			return new UnresolvedFunCallImpl(ppoa, new Expression[] { x });
		case QuotedPropertyOperationAtom qpoa:
			x = getExpression(callExpression.expressions().get(0));
			return new UnresolvedFunCallImpl(qpoa, new Expression[] { x });
		case AmpersandQuotedPropertyOperationAtom aqpoa:
			x = getExpression(callExpression.expressions().get(0));
			return new UnresolvedFunCallImpl(aqpoa, new Expression[] { x });
		case MethodOperationAtom moa:
			list = getExpressionList(callExpression.expressions());
			return new UnresolvedFunCallImpl(moa, list.stream().toArray(Expression[]::new));
		case BracesOperationAtom broa:
			list = getExpressionList(callExpression.expressions());
			return new UnresolvedFunCallImpl(broa, list.stream().toArray(Expression[]::new));
		case ParenthesesOperationAtom parenoa:
			list = getExpressionList(callExpression.expressions());
			return new UnresolvedFunCallImpl(parenoa, list.stream().toArray(Expression[]::new));
		case InternalOperationAtom intoa:
			list = getExpressionList(callExpression.expressions());
			return new UnresolvedFunCallImpl(intoa, list.stream().toArray(Expression[]::new));
		case EmptyOperationAtom eoa:
			return new UnresolvedFunCallImpl(eoa, new Expression[] {});
		case PostfixOperationAtom posgos:
			x = getExpression(callExpression.expressions().get(0));
			return new UnresolvedFunCallImpl(posgos, new Expression[] { x });
		case CaseOperationAtom coa:
			list = getExpressionList(callExpression.expressions());
			return new UnresolvedFunCallImpl(coa, list.stream().toArray(Expression[]::new));
		case CastOperationAtom cast:
			list = getExpressionList(callExpression.expressions());
			return new UnresolvedFunCallImpl(new CastOperationAtom(), list.stream().toArray(Expression[]::new));
		}
	}

	static List<Expression> getExpressionList(List<? extends MdxExpression> expressions) {
		if (expressions != null) {
			return expressions.stream().map(MdxToQueryConverter::getExpression).toList();
		}
		return List.of();
	}

	static IdImpl getId(CompoundId compoundId) {
		List<Segment> segmentList = getIdSegmentList(compoundId.objectIdentifiers());
		return new IdImpl(segmentList);
	}

	static List<Segment> getIdSegmentList(List<? extends ObjectIdentifier> objectIdentifiers) {
		List<Segment> segmentList = new ArrayList<>();
		if (objectIdentifiers != null) {
			for (ObjectIdentifier oi : objectIdentifiers) {
				segmentList.add(getIdSegment(oi));
			}
		}
		return segmentList;
	}

	static Segment getIdSegment(ObjectIdentifier objectIdentifier) {

		switch (objectIdentifier) {
		case KeyObjectIdentifier keyObjectIdentifier:
			return new IdImpl.KeySegment(getNameSegmentList(keyObjectIdentifier.nameObjectIdentifiers()));
		case NameObjectIdentifier nameObjectIdentifier:
			return new IdImpl.NameSegmentImpl(nameObjectIdentifier.name(), getQuoting(nameObjectIdentifier.quoting()));
		}
	}

	static List<NameSegment> getNameSegmentList(List<? extends NameObjectIdentifier> nameObjectIdentifiers) {
		List<NameSegment> res = new ArrayList<>();
		if (nameObjectIdentifiers != null) {
			for (NameObjectIdentifier noi : nameObjectIdentifiers) {
				res.add(new IdImpl.NameSegmentImpl(noi.name(), getQuoting(noi.quoting())));
			}
		}
		return res;
	}

	static Quoting getQuoting(ObjectIdentifier.Quoting quoting) {
		switch (quoting) {
		case QUOTED:
			return Quoting.QUOTED;
		case UNQUOTED:
			return Quoting.UNQUOTED;
		case KEY:
			return Quoting.KEY;
		default:
			return Quoting.UNQUOTED;
		}
	}

	static String convertName(NameObjectIdentifier nameObjectIdentifier) {
		switch (nameObjectIdentifier.quoting()) {
		case QUOTED:
			return nameObjectIdentifier.name();
		case UNQUOTED:
			return nameObjectIdentifier.name();
		case KEY:
			return key(nameObjectIdentifier.name());
		default:
			return nameObjectIdentifier.name();
		}
	}

	static String quoted(String name) {
		StringBuilder sb = new StringBuilder();
		return sb.append("[").append(name).append("]").toString();
	}

	static String key(String name) {
		StringBuilder sb = new StringBuilder();
		return sb.append("&").append(name).append("]").toString();
	}

}
