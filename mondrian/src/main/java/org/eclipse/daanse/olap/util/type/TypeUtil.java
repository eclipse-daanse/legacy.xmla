/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2005-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara
 * Copyright (C) 2021-2022 Sergei Semenkov
 * All Rights Reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.util.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.BooleanType;
import org.eclipse.daanse.olap.api.type.CubeType;
import org.eclipse.daanse.olap.api.type.DateTimeType;
import org.eclipse.daanse.olap.api.type.DecimalType;
import org.eclipse.daanse.olap.api.type.DimensionType;
import org.eclipse.daanse.olap.api.type.EmptyType;
import org.eclipse.daanse.olap.api.type.HierarchyType;
import org.eclipse.daanse.olap.api.type.LevelType;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NullType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.ScalarType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.SymbolType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.mdx.UnresolvedFunCallImpl;
//import mondrian.mdx.UnresolvedFunCallImpl;
import mondrian.olap.Util;

/**
 * Utility methods relating to types.
 *
 * @author jhyde
 * @since Feb 17, 2005
 */
public class TypeUtil {

	private TypeUtil() {
		// constructor
	}

	/**
	 * Given a set type, returns the element type. Or its element type, if it is a
	 * set type. And so on.
	 *
	 * @param type Type
	 * @return underlying element type which is not a set type
	 */
	public static Type stripSetType(Type type) {
		while (type instanceof SetType setType) {
			type = setType.getElementType();
		}
		return type;
	}

	/**
	 * Converts a type to a member or tuple type. If it cannot, returns null.
	 *
	 * @param type Type
	 * @return member or tuple type
	 */
	public static Type toMemberOrTupleType(Type type) {
		type = stripSetType(type);
		if (type instanceof TupleType) {
			return type;
		} else {
			return toMemberType(type);
		}
	}

	/**
	 * Converts a type to a member type. If it is a set, strips the set. If it is a
	 * member type, returns the type unchanged. If it is a dimension, hierarchy or
	 * level type, converts it to a member type. If it is a tuple, number, string,
	 * or boolean, returns null.
	 *
	 * @param type Type
	 * @return type as a member type
	 */
	public static MemberType toMemberType(Type type) {
		type = stripSetType(type);
		if (type instanceof MemberType memberType) {
			return memberType;
		} else if (type instanceof DimensionType || type instanceof HierarchyType || type instanceof LevelType) {
			return MemberType.forType(type);
		} else if (type instanceof TupleType tupleType && tupleType.getArity() == 1) {
			return MemberType.forHierarchy(((TupleType) type).getHierarchies().get(0));
		} else {
			return null;
		}
	}

	/**
	 * Returns whether this type is union-compatible with another. In general, to be
	 * union-compatible, types must have the same dimensionality.
	 *
	 * @param type1 First type
	 * @param type2 Second type
	 * @return whether types are union-compatible
	 */
	public static boolean isUnionCompatible(Type type1, Type type2) {
		final MemberType memberType1 = toMemberType(type1);
		final MemberType memberType2 = toMemberType(type2);
		if (memberType1 != null && memberType2 != null) {
			final Hierarchy hierarchy1 = memberType1.getHierarchy();
			final Hierarchy hierarchy2 = memberType2.getHierarchy();
			return equalHierarchy(hierarchy1, hierarchy2);
		}
		if (type1 instanceof TupleType tupleType1) {
			if (type2 instanceof TupleType tupleType2
					&& tupleType1.elementTypes.length == tupleType2.elementTypes.length) {
				for (int i = 0; i < tupleType1.elementTypes.length; i++) {
					if (!isUnionCompatible(tupleType1.elementTypes[i], tupleType2.elementTypes[i])) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * Returns whether two hierarchies are equal.
	 *
	 * @param hierarchy1 First hierarchy
	 * @param hierarchy2 Second hierarchy
	 * @return Whether hierarchies are equal
	 */
	private static boolean equalHierarchy(final Hierarchy hierarchy1, final Hierarchy hierarchy2) {
		return hierarchy1 == null || hierarchy2 == null
				|| hierarchy2.getUniqueName().equals(hierarchy1.getUniqueName());
	}

	/**
	 * Returns whether a value of a given type can be evaluated to a scalar value.
	 *
	 * <p>
	 * The rules are as follows:
	 * <ul>
	 * <li>Clearly boolean, numeric and string expressions can be evaluated.
	 * <li>Member and tuple expressions can be interpreted as a scalar value. The
	 * expression is evaluated to establish the context where a measure can be
	 * evaluated.
	 * <li>Hierarchy and dimension expressions are implicitly converted into the
	 * current member, and evaluated as above.
	 * <li>Level expressions cannot be evaluated
	 * <li>Cube and Set (even sets with a single member) cannot be evaluated.
	 * </ul>
	 *
	 * @param type Type
	 * @return Whether an expression of this type can be evaluated to yield a scalar
	 *         value.
	 */
	public static boolean canEvaluate(Type type) {
		return !(type instanceof SetType || type instanceof CubeType || type instanceof LevelType);
	}

	/**
	 * Returns whether a type is a set type.
	 *
	 * @param type Type
	 * @return Whether a value of this type can be evaluated to yield a set.
	 */
	public static boolean isSet(Type type) {
		return type instanceof SetType;
	}

	public static boolean couldBeMember(Type type) {
		return type instanceof MemberType || type instanceof HierarchyType || type instanceof DimensionType;
	}

	/**
	 * Converts a {@link Type} value to a {@link DataType} ordinal.
	 *
	 * @param type Type
	 * @return category ordinal
	 */
	public static DataType typeToCategory(Type type) {
		if (type instanceof NullType) {
			return DataType.NULL;
		} else if (type instanceof EmptyType) {
			return DataType.EMPTY;
		} else if (type instanceof DateTimeType) {
			return DataType.DATE_TIME;
		} else if (type instanceof DecimalType decimalType && decimalType.getScale() == 0) {
			return DataType.INTEGER;
		} else if (type instanceof NumericType) {
			return DataType.NUMERIC;
		} else if (type instanceof BooleanType) {
			return DataType.LOGICAL;
		} else if (type instanceof DimensionType) {
			return DataType.DIMENSION;
		} else if (type instanceof HierarchyType) {
			return DataType.HIERARCHY;
		} else if (type instanceof MemberType) {
			return DataType.MEMBER;
		} else if (type instanceof LevelType) {
			return DataType.LEVEL;
		} else if (type instanceof SymbolType) {
			return DataType.SYMBOL;
		} else if (type instanceof StringType) {
			return DataType.STRING;
		} else if (type instanceof ScalarType) {
			return DataType.VALUE;
		} else if (type instanceof SetType) {
			return DataType.SET;
		} else if (type instanceof TupleType) {
			return DataType.TUPLE;
		} else {
			throw Util.newInternal("Unknown type " + type);
		}
	}

	/**
	 * Returns a type sufficiently broad to hold any value of several types, but as
	 * narrow as possible. If there is no such type, returns null.
	 *
	 * <p>
	 * The result is equivalent to calling
	 * {@link Type#computeCommonType(Type, int[])} pairwise.
	 *
	 * @param allowConversions Whether to allow implicit conversions
	 * @param types            Array of types
	 * @return Most general type which encompases all types
	 */
	public static Type computeCommonType(boolean allowConversions, Type... types) {
		if (types.length == 0) {
			return null;
		}
		Type type = types[0];
		int[] conversionCount = allowConversions ? new int[] { 0 } : null;
		for (int i = 1; i < types.length; ++i) {
			if (type == null) {
				return null;
			}
			type = type.computeCommonType(types[i], conversionCount);
		}
		return type;
	}

	/**
	 * Returns whether we can convert an argument of a given category to a given
	 * parameter category.
	 *
	 * @param ordinal     argument ordinal
	 * @param fromType    actual argument type
	 * @param to          formal parameter category
	 * @param conversions list of implicit conversions required (out)
	 * @return whether can convert from 'from' to 'to'
	 */
	public static boolean canConvert(int ordinal, Type fromType, DataType to,
			List<FunctionResolver.Conversion> conversions) {
		final DataType from = typeToCategory(fromType);
		if (from == to) {
			return true;
		}
		RuntimeException e = null;
		switch (from) {
		case ARRAY:
			return convertFromArray();
		case DIMENSION:
			return convertFromDimension(ordinal, to, conversions, from, e);
		case HIERARCHY:
			return convertFromHierarchy(ordinal, to, conversions, from);
		case LEVEL:
			return convertFromLevel(ordinal, to, conversions, from);
		case LOGICAL:
			return convertFromLogical(to);
		case MEMBER:
			return convertFromMember(ordinal, to, conversions, from);
		case NUMERIC:
			return convertFromNumeric(ordinal, to, conversions, from);
		case INTEGER:
			return convertFromInteger(to);
		case SET:
			return convertFromSet();
		case STRING:
			return convertFromString(to);
		case DATE_TIME:
			return convertFromDateTime(to);
		case TUPLE:
			return convertFromTuple(ordinal, to, conversions, from);
		case VALUE:
			return convertFromValue(ordinal, to, conversions, from);
		case SYMBOL:
			return convertFromSymbol();
		case NULL:
			return convertFromNull(ordinal, to, conversions, from);
		case EMPTY:
			return convertFromEmpty();
		default:
			throw Util.newInternal(new StringBuilder("unknown category ").append(from).append(" for type ")
					.append(fromType).toString());
		}
	}

	/**
	 * Converts a type to a different category, maintaining as much type information
	 * as possible.
	 *
	 * For example, given <code>LevelType(dimension=Time, hierarchy=unknown,
	 * level=unkown)</code> and category=Hierarchy, returns
	 * <code>HierarchyType(dimension=Time)</code>.
	 *
	 * @param type     Type
	 * @param category Desired category
	 * @return Type after conversion to desired category
	 */
	public static Type castType(Type type, DataType category) {
		switch (category) {
		case LOGICAL:
			return BooleanType.INSTANCE;
		case NUMERIC:
			return NumericType.INSTANCE;// TODO: RIGHT ORDER?
		case INTEGER:
			return new DecimalType(Integer.MAX_VALUE, 0);
		case STRING:
			return StringType.INSTANCE;
		case DATE_TIME:
			return DateTimeType.INSTANCE;
		case SYMBOL:
			return SymbolType.INSTANCE;
		case VALUE:
			return ScalarType.INSTANCE;
		case CUBE:
			if (type instanceof Cube cube) {
				return new CubeType(cube);
			}
			return null;
		case DIMENSION:
			if (type != null) {
				return DimensionType.forType(type);
			}
			return null;
		case HIERARCHY:
			if (type != null) {
				return HierarchyType.forType(type);
			}
			return null;
		case LEVEL:
			if (type != null) {
				return LevelType.forType(type);
			}
			return null;
		case MEMBER:
			if (type != null) {
				final MemberType memberType = TypeUtil.toMemberType(type);
				if (memberType != null) {
					return memberType;
				}
			}
			// Take a wild guess.
			return MemberType.Unknown;
		case TUPLE:
			if (type != null) {
				final Type memberType = TypeUtil.toMemberOrTupleType(type);
				if (memberType != null) {
					return memberType;
				}
			}
			return null;
		case SET:
			if (type != null) {
				final Type memberType = TypeUtil.toMemberOrTupleType(type);
				if (memberType != null) {
					return new SetType(memberType);
				}
			}
			return null;
		case EMPTY:
			return EmptyType.INSTANCE;
		default:
			throw new RuntimeException("Unexpected Category: " + category);
		}
	}

	private static boolean convertFromNull(int ordinal, DataType to, List<FunctionResolver.Conversion> conversions,
			final DataType from) {
		// now null supports members as well as scalars; but scalar is
		// preferred
		if (to.isScalar()) {
			return true;
		} else if (to == DataType.MEMBER) {
			conversions.add(new ConversionImpl(from, to, ordinal, 2, null));
			return true;
		} else {
			return false;
		}
	}

	private static boolean convertFromValue(int ordinal, DataType to, List<FunctionResolver.Conversion> conversions,
			final DataType from) {
		switch (to) {
		case STRING:
		case NUMERIC:
		case LOGICAL:
			conversions.add(new ConversionImpl(from, to, ordinal, 2, null));
			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromTuple(int ordinal, DataType to, List<FunctionResolver.Conversion> conversions,
			final DataType from) {
		switch (to) {
		case NUMERIC:
			conversions.add(new ConversionImpl(from, to, ordinal, 3, null));
			return true;
		case SET:
			conversions.add(new ConversionImpl(from, to, ordinal, 2, null));
			return true;
		case STRING, VALUE:
			// We assume that measures are numeric, so a cast to a string or
			// general value expression is more expensive (cost=4) than a
			// conversion to a numeric expression (cost=3).
			conversions.add(new ConversionImpl(from, to, ordinal, 4, null));
			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromDateTime(DataType to) {
		switch (to) {
		case VALUE:
			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromString(DataType to) {
		switch (to) {
		case VALUE:
			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromInteger(DataType to) {
		switch (to) {
		case VALUE, NUMERIC:

			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromNumeric(int ordinal, DataType to, List<FunctionResolver.Conversion> conversions,
			final DataType from) {
		switch (to) {
		case LOGICAL:
			conversions.add(new ConversionImpl(from, to, ordinal, 2, null));
			return true;
		case VALUE, INTEGER:
			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromMember(int ordinal, DataType to, List<FunctionResolver.Conversion> conversions,
			final DataType from) {
		switch (to) {
		case DIMENSION, HIERARCHY, LEVEL, TUPLE:
			conversions.add(new ConversionImpl(from, to, ordinal, 1, null));
			return true;
		case SET:
			conversions.add(new ConversionImpl(from, to, ordinal, 2, null));
			return true;
		case NUMERIC:
			conversions.add(new ConversionImpl(from, to, ordinal, 3, null));
			return true;
		case VALUE, STRING:
			// We assume that measures are numeric, so a cast to a string or
			// general value expression is more expensive (cost=4) than a
			// conversion to a numeric expression (cost=3).
			conversions.add(new ConversionImpl(from, to, ordinal, 4, null));
			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromLogical(DataType to) {
		return DataType.VALUE == to;
	}

	private static boolean convertFromSet() {
		return false;
	}

	private static boolean convertFromSymbol() {
		return false;
	}

	private static boolean convertFromEmpty() {
		return false;
	}

	private static boolean convertFromArray() {
		return false;
	}

	private static boolean convertFromLevel(int ordinal, DataType to, List<FunctionResolver.Conversion> conversions,
			final DataType from) {
		switch (to) {
		case DIMENSION:
			// It's more difficult to convert to a dimension than a
			// hierarchy. For example, we want '[Store City].CurrentMember'
			// to resolve to <Hierarchy>.CurrentMember rather than
			// <Dimension>.CurrentMember.
			conversions.add(new ConversionImpl(from, to, ordinal, 2, null));
			return true;
		case HIERARCHY, SET:
			conversions.add(new ConversionImpl(from, to, ordinal, 1, null));
			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromHierarchy(int ordinal, DataType to, List<FunctionResolver.Conversion> conversions,
			final DataType from) {
		// Seems funny that you can 'downcast' from a hierarchy, doesn't
		// it? But we add an implicit 'CurrentMember', for example,
		// '[Product].PrevMember' actually means
		// '[Product].CurrentMember.PrevMember'.
		switch (to) {
		case DIMENSION, MEMBER, TUPLE:
			conversions.add(new ConversionImpl(from, to, ordinal, 1, null));
			return true;
		default:
			return false;
		}
	}

	private static boolean convertFromDimension(int ordinal, DataType to, List<FunctionResolver.Conversion> conversions,
			final DataType from, RuntimeException e) {
		// We can go from Dimension to Hierarchy if the dimension has a
		// default hierarchy. From there, we can go to Member or Tuple.
		// Even if the dimension does not have a default hierarchy, we claim
		// now that we can do the conversion, to prevent other overloads
		// from being chosen; we will hit an error either at compile time or
		// at run time.
		switch (to) {
		case MEMBER, TUPLE, HIERARCHY:
			// It is more difficult to convert dimension->hierarchy than
			// hierarchy->dimension
			conversions.add(new ConversionImpl(from, to, ordinal, 2, e));
			return true;
		case LEVEL:
			// It is more difficult to convert dimension->level than
			// dimension->member or dimension->hierarchy->member.
			conversions.add(new ConversionImpl(from, to, ordinal, 3, null));
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns the hierarchies in a set, member, or tuple type.
	 *
	 * @param type Type
	 * @return List of hierarchies
	 */
	public static List<Hierarchy> getHierarchies(Type type) {
		if (type instanceof SetType setType) {
			type = setType.getElementType();
		}
		if (type instanceof TupleType tupleType) {
			List<Hierarchy> hierarchyList = new ArrayList<>();
			for (Type elementType : tupleType.elementTypes) {
				hierarchyList.add(elementType.getHierarchy());
			}
			return hierarchyList;
		} else {
			Hierarchy hierarchy = type.getHierarchy();
			return hierarchy == null ? Collections.<Hierarchy>emptyList() : Collections.singletonList(hierarchy);
		}
	}

	/**
	 * Implementation of
	 * {@link org.eclipse.daanse.olap.api.function.FunctionResolver.Conversion}.
	 */
	private static class ConversionImpl implements FunctionResolver.Conversion {
		final DataType from;
		final DataType to;
		/**
		 * Which argument. Arguments are 0-based, and in particular the 'this' of a call
		 * of member or method call syntax is argument 0. Argument -1 is the return.
		 */
		final int ordinal;

		/**
		 * Score of the conversion. A higher value is more onerous and therefore a call
		 * using such a conversion is less likly to be chosen.
		 */
		final int cost;

		final RuntimeException e;

		/**
		 * Creates a conversion.
		 *
		 * @param from    From type
		 * @param to      To type
		 * @param ordinal Ordinal of argument
		 * @param cost    Cost of conversion
		 * @param e       Exception
		 */
		public ConversionImpl(DataType from, DataType to, int ordinal, int cost, RuntimeException e) {
			this.from = from;
			this.to = to;
			this.ordinal = ordinal;
			this.cost = cost;
			this.e = e;
		}

		@Override
		public int getCost() {
			return cost;
		}

		@Override
		public void checkValid() {
			if (e != null) {
				throw e;
			}
		}

		@Override
		public void apply(Validator validator, List<Expression> args) {
			final Expression arg = args.get(ordinal);
			if ((from == DataType.MEMBER || from == DataType.TUPLE) && to == DataType.SET) {
				final Expression newArg = validator.validate(
						new UnresolvedFunCallImpl(new BracesOperationAtom(), new Expression[] { arg }), false);
				args.set(ordinal, newArg);
			}
		}

		// for debug
		@Override
		public String toString() {
			return new StringBuilder("Conversion(from=").append(from.getName()).append(", to=").append(to.getName())
					.append(", ordinal=").append(ordinal).append(", cost=").append(cost).append(", e=").append(e)
					.append(")").toString();
		}
	}
}
