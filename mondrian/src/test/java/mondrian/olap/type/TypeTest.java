/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import mondrian.olap.Connection;
import mondrian.olap.Cube;
import mondrian.olap.Dimension;
import mondrian.olap.Hierarchy;
import mondrian.olap.Id;
import mondrian.olap.Level;
import mondrian.olap.Member;
import mondrian.olap.SchemaReader;
import mondrian.olap.fun.Resolver;

/**
 * Unit test for mondrian type facility.
 *
 * @author jhyde
 * @since Jan 17, 2008
 */

public class TypeTest {

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testConversions(Context foodMartContext) {
        final Connection connection = foodMartContext.createConnection();
        Cube salesCube =
            getCubeWithName("Sales", connection.getSchema().getCubes());
        assertTrue(salesCube != null);
        Dimension customersDimension = null;
        for (Dimension dimension : salesCube.getDimensions()) {
            if (dimension.getName().equals("Customers")) {
                customersDimension = dimension;
            }
        }

        assertTrue(customersDimension != null);
        Hierarchy hierarchy = customersDimension.getHierarchy();
        Member member = hierarchy.getDefaultMember();
        Level level = member.getLevel();
        Type memberType = new MemberType(
            customersDimension, hierarchy, level, member);
        final LevelType levelType =
            new LevelType(customersDimension, hierarchy, level);
        final HierarchyType hierarchyType =
            new HierarchyType(customersDimension, hierarchy);
        final DimensionType dimensionType =
            new DimensionType(customersDimension);
        final StringType stringType = new StringType();
        final ScalarType scalarType = new ScalarType();
        final NumericType numericType = new NumericType();
        final DateTimeType dateTimeType = new DateTimeType();
        final DecimalType decimalType = new DecimalType(10, 2);
        final DecimalType integerType = new DecimalType(7, 0);
        final NullType nullType = new NullType();
        final MemberType unknownMemberType = MemberType.Unknown;
        final TupleType tupleType =
            new TupleType(
                new Type[] {memberType,  unknownMemberType});
        final SetType tupleSetType = new SetType(tupleType);
        final SetType setType = new SetType(memberType);
        final LevelType unknownLevelType = LevelType.Unknown;
        final HierarchyType unknownHierarchyType = HierarchyType.Unknown;
        final DimensionType unknownDimensionType = DimensionType.Unknown;
        final BooleanType booleanType = new BooleanType();
        Type[] types = {
            memberType,
            levelType,
            hierarchyType,
            dimensionType,
            numericType,
            dateTimeType,
            decimalType,
            integerType,
            scalarType,
            nullType,
            stringType,
            booleanType,
            tupleType,
            tupleSetType,
            setType,
            unknownDimensionType,
            unknownHierarchyType,
            unknownLevelType,
            unknownMemberType
        };

        for (Type type : types) {
            // Check that each type is assignable to itself.
            final String desc = type.toString() + ":" + type.getClass();
            assertEquals(type, type.computeCommonType(type, null),desc);

            int[] conversionCount = {0};
            assertEquals(
                type, type.computeCommonType(type, conversionCount),desc);
            assertEquals(0, conversionCount[0]);

            // Check that each scalar type is assignable to nullable with zero
            // conversions.
            if (type instanceof ScalarType) {
                assertEquals(type, type.computeCommonType(nullType, null));
                assertEquals(
                    type, type.computeCommonType(nullType, conversionCount));
                assertEquals(0, conversionCount[0]);
            }
        }

        for (Type fromType : types) {
            for (Type toType : types) {
                Type type = fromType.computeCommonType(toType, null);
                Type type2 = toType.computeCommonType(fromType, null);
                final String desc =
                    "symmetric, from " + fromType + ", to " + toType;
                assertEquals(type, type2, desc);

                int[] conversionCount = {0};
                int[] conversionCount2 = {0};
                type = fromType.computeCommonType(toType, conversionCount);
                type2 = toType.computeCommonType(fromType, conversionCount2);
                if (conversionCount[0] == 0
                    && conversionCount2[0] == 0)
                {
                    assertEquals(type, type2,desc);
                }

                final int toCategory = TypeUtil.typeToCategory(toType);
                final List<Resolver.Conversion> conversions =
                    new ArrayList<Resolver.Conversion>();
                final boolean canConvert =
                    TypeUtil.canConvert(
                        0,
                        fromType,
                        toCategory,
                        conversions);
                if (canConvert && conversions.size() == 0 && type == null) {
                    if (!(fromType == memberType && toType == tupleType
                        || fromType == tupleSetType && toType == setType
                        || fromType == setType && toType == tupleSetType))
                    {
                        Assertions.fail(
                            "can convert from " + fromType + " to " + toType
                            + ", but their most general type is null");
                    }
                }
                if (!canConvert && type != null && type.equals(toType)) {
                	Assertions.fail(
                        "cannot convert from " + fromType + " to " + toType
                        + ", but they have a most general type " + type);
                }
            }
        }
    }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testCommonTypeWhenSetTypeHavingMemberTypeAndTupleType(Context foodMartContext) {
        Connection connection=	foodMartContext.createConnection();
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure(connection));

        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(getMaleChild(connection));

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild(connection));

        TupleType tupleType = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        SetType setTypeWithMember = new SetType(measureMemberType);
        SetType setTypeWithTuple = new SetType(tupleType);

        Type type1 =
            setTypeWithMember.computeCommonType(setTypeWithTuple, null);
        assertNotNull(type1);
        assertTrue(((SetType) type1).getElementType() instanceof TupleType);

        Type type2 =
            setTypeWithTuple.computeCommonType(setTypeWithMember, null);
        assertNotNull(type2);
        assertTrue(((SetType) type2).getElementType() instanceof TupleType);
        assertEquals(type1, type2);
    }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testCommonTypeOfMemberandTupleTypeIsTupleType(Context foodMartContext) {
        Connection connection=	foodMartContext.createConnection();
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure(connection));

        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(getMaleChild(connection));

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild(connection));

        TupleType tupleType = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        Type type1 = measureMemberType.computeCommonType(tupleType, null);
        assertNotNull(type1);
        assertTrue(type1 instanceof TupleType);

        Type type2 = tupleType.computeCommonType(measureMemberType, null);
        assertNotNull(type2);
        assertTrue(type2 instanceof TupleType);
        assertEquals(type1, type2);
    }

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class )
    public void testCommonTypeBetweenTuplesOfDifferentSizesIsATupleType(Context foodMartContext) {
    Connection connection=	foodMartContext.createConnection();
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure(connection));

        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(getMaleChild(connection));

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild(connection));

        TupleType tupleTypeLarger = new TupleType(
            new Type[] {storeMemberType, genderMemberType, measureMemberType});

        TupleType tupleTypeSmaller = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        Type type1 = tupleTypeSmaller.computeCommonType(tupleTypeLarger, null);
        assertNotNull(type1);
        assertTrue(type1 instanceof TupleType);
        assertTrue(((TupleType) type1).elementTypes[0] instanceof MemberType);
        assertTrue(((TupleType) type1).elementTypes[1] instanceof MemberType);
        assertTrue(((TupleType) type1).elementTypes[2] instanceof ScalarType);

        Type type2 = tupleTypeLarger.computeCommonType(tupleTypeSmaller, null);
        assertNotNull(type2);
        assertTrue(type2 instanceof TupleType);
        assertTrue(((TupleType) type2).elementTypes[0] instanceof MemberType);
        assertTrue(((TupleType) type2).elementTypes[1] instanceof MemberType);
        assertTrue(((TupleType) type2).elementTypes[2] instanceof ScalarType);
        assertEquals(type1, type2);
    }

    private MemberType getStoreMemberType(Member storeChild) {
        return new MemberType(
            storeChild.getDimension(),
            storeChild.getDimension().getHierarchy(),
            storeChild.getLevel(),
            storeChild);
    }

    private Member getStoreChild(Connection connection) {
        List<Id.Segment> storeParts = Arrays.<Id.Segment>asList(
            new Id.NameSegment("Store", Id.Quoting.UNQUOTED),
            new Id.NameSegment("All Stores", Id.Quoting.UNQUOTED),
            new Id.NameSegment("USA", Id.Quoting.UNQUOTED),
            new Id.NameSegment("CA", Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader(connection).getMemberByUniqueName(
            storeParts, false);
    }

    private MemberType getMemberTypeHavingMaleChild(Member maleChild) {
        return new MemberType(
            maleChild.getDimension(),
            maleChild.getDimension().getHierarchy(),
            maleChild.getLevel(),
            maleChild);
    }

    private MemberType getMemberTypeHavingMeasureInIt(Member unitSalesMeasure) {
        return new MemberType(
            unitSalesMeasure.getDimension(),
            unitSalesMeasure.getDimension().getHierarchy(),
            unitSalesMeasure.getDimension().getHierarchy().getLevels()[0],
            unitSalesMeasure);
    }

    private Member getMaleChild(Connection connection) {
        List<Id.Segment> genderParts = Arrays.<Id.Segment>asList(
            new Id.NameSegment("Gender", Id.Quoting.UNQUOTED),
            new Id.NameSegment("M", Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader(connection).getMemberByUniqueName(
            genderParts, false);
    }

    private static Member getUnitSalesMeasure(Connection connection) {
        List<Id.Segment> measureParts = Arrays.<Id.Segment>asList(
            new Id.NameSegment("Measures", Id.Quoting.UNQUOTED),
            new Id.NameSegment("Unit Sales", Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader(connection).getMemberByUniqueName(
            measureParts, false);
    }

    private static SchemaReader getSalesCubeSchemaReader(Connection connection) {
        final Cube salesCube = getCubeWithName(
            "Sales",
            getSchemaReader(connection).getCubes());
        return salesCube.getSchemaReader(
        		connection.getRole()).withLocus();
    }

    private static SchemaReader getSchemaReader(Connection connection) {
        return connection.getSchemaReader().withLocus();
    }

    private static Cube getCubeWithName(String cubeName, Cube[] cubes) {
        Cube resultCube = null;
        for (Cube cube : cubes) {
            if (cubeName.equals(cube.getName())) {
                resultCube = cube;
                break;
            }
        }
        return resultCube;
    }
}

// End TypeTest.java

