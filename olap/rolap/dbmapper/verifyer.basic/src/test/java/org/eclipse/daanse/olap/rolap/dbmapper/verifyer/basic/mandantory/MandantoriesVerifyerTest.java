package org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.mandantory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.ACTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.ACTION_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_COLUMN_NAME;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_COLUMN_NAME_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_FOREIGN_KEY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_FOREIGN_KEY_AGG_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_FOREIGN_KEY_FACT_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_LEVEL;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_LEVEL_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_LEVEL_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_MEASURE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_MEASURE_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_MEASURE_FACT_COUNT;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_MEASURE_FACT_COUNT_FACT_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_MEASURE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_TABLE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGG_TABLE_AGG_FACT_COUNT_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.ANNOTATION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.ANNOTATION_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CALCULATED_MEMBER;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CALCULATED_MEMBER_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CALCULATED_MEMBER_PROPERTY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CALCULATED_MEMBER_PROPERTY_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_DIMENSION_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_NAME_MUST_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_USAGE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_USAGE_CUBE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_WITH_NAME_MUST_CONTAIN_;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DIMENSIONS;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DIMENSION_MUST_BE_SET_FOR_CALCULATED_MEMBER;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DRILL_THROUGH_ATTRIBUTE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DRILL_THROUGH_ATTRIBUTE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DRILL_THROUGH_MEASURE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DRILL_THROUGH_MEASURE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.EITHER_A_CLASS_NAME_OR_A_SCRIPT_ARE_REQUIRED;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.ELEMENT_FORMATTER;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.FACT_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.FORMATTER_EITHER_A_CLASS_NAME_OR_A_SCRIPT_ARE_REQUIRED;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.FORMULA;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.FORMULA_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.FORMULA_MUST_BE_SET_FOR_CALCULATED_MEMBER;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.HIERARCHY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.HIERARCHY_TABLE_VALUE_DOES_NOT_CORRESPOND_TO_ANY_JOIN;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.HINT;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.HINT_TYPE_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.JOIN;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.JOIN_LEFT_KEY_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.JOIN_RIGHT_KEY_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.LEVEL;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.LEVEL_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.LEVEL_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.MEASURE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.MEASURE_AGGREGATOR_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.MEASURE_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.MEASURE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.NAMED_SET_FORMULA_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.NAMED_SET_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.NOT_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PARAMETER;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PARAMETER_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PARAMETER_TYPE_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PRIMARY_KEY_TABLE_AND_PRIMARY_KEY_MUST_BE_SET_FOR_JOIN_;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PROPERTY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PROPERTY_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.ROLE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.SCHEMA;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.SCHEMA_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.SQL_DIALECT_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.TABLE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.TABLE_VALUE_DOES_NOT_CORRESPOND_TO_ANY_JOIN;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.TABLE_VALUE_DOES_NOT_CORRESPOND_TO_HIERARCHY_RELATION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.USER_DEFINED_FUNCTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.USER_DEFINED_FUNCTION_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.VIRTUAL_CUBE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.VIRTUAL_CUBE_MUST_CONTAIN_DIMENSIONS;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.VIRTUAL_CUBE_MUST_CONTAIN_MEASURES;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.VIRTUAL_CUBE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.WRITEBACK_ATTRIBUTE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.WRITEBACK_ATTRIBUTE_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.WRITEBACK_ATTRIBUTE_DIMENSION_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.WRITEBACK_MEASURE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.WRITEBACK_MEASURE_COLUMN_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.WRITEBACK_MEASURE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.WRITEBACK_TABLE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.WRITEBACK_TABLE_NAME_MUST_BE_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.description.DescriptionVerifyerTest.setupDummyListAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Action;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.AggColumnName;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.AggForeignKey;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.AggLevel;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.AggMeasure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.AggMeasureFactCount;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.AggName;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.AggTable;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Annotation;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CalculatedMember;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CalculatedMemberProperty;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Closure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Cube;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CubeUsage;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.DrillThroughAction;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.DrillThroughAttribute;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.DrillThroughMeasure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.ElementFormatter;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.ExpressionView;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Formula;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Hierarchy;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Hint;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Join;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Level;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Measure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.NamedSet;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Parameter;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.PrivateDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Property;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Role;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.SQL;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Schema;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Table;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.UserDefinedFunction;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.VirtualCube;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.WritebackAttribute;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.WritebackMeasure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.WritebackTable;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.HideMemberIfEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.InternalTypeEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.LevelTypeEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.TypeEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.VerificationResult;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Verifyer;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class MandantoriesVerifyerTest {

    public static final String COMPONENT_NAME = "org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.mandantory" +
        ".MandantoriesVerifyer";
    @InjectService(filter = "(component.name=" + COMPONENT_NAME + ")")
    Verifyer verifyer;

    Schema schema = mock(Schema.class);
    Cube cube = mock(Cube.class);
    VirtualCube virtualCube = mock(VirtualCube.class);
    PrivateDimension dimension = mock(PrivateDimension.class);
    CalculatedMemberProperty calculatedMemberProperty = mock(CalculatedMemberProperty.class);
    CalculatedMember calculatedMember = mock(CalculatedMember.class);
    Measure measure = mock(Measure.class);
    Hierarchy hierarchy = mock(Hierarchy.class);
    Level level = mock(Level.class);
    org.eclipse.daanse.olap.rolap.dbmapper.model.api.Property property =
        mock(org.eclipse.daanse.olap.rolap.dbmapper.model.api.Property.class);
    NamedSet namedSet = mock(NamedSet.class);
    Parameter parameter = mock(Parameter.class);
    DrillThroughAction drillThroughAction = mock(DrillThroughAction.class);
    Action action = mock(Action.class);
    ElementFormatter elementFormatter = mock(ElementFormatter.class);
    Join join = mock(Join.class);
    Table table = mock(Table.class);
    Formula formula = mock(Formula.class);
    UserDefinedFunction userDefinedFunction = mock(UserDefinedFunction.class);
    WritebackTable writebackTable = mock(WritebackTable.class);
    WritebackAttribute writebackAttribute = mock(WritebackAttribute.class);
    WritebackMeasure writebackMeasure = mock(WritebackMeasure.class);
    DrillThroughMeasure drillThroughMeasure = mock(DrillThroughMeasure.class);
    DrillThroughAttribute drillThroughAttribute = mock(DrillThroughAttribute.class);
    Annotation annotation = mock(Annotation.class);
    Role role = mock(Role.class);
    CubeUsage cubeUsage = mock(CubeUsage.class);
    SQL sql = mock(SQL.class);
    Hint hint = mock(Hint.class);
    AggTable aggTable = mock(AggName.class);
    AggColumnName aggColumnName = mock(AggColumnName.class);
    AggForeignKey aggForeignKey = mock(AggForeignKey.class);
    AggMeasure aggMeasure = mock(AggMeasure.class);
    AggLevel aggLevel = mock(AggLevel.class);
    AggMeasureFactCount measuresFactCount = mock(AggMeasureFactCount.class);

    Level l = new LevelTest("name",
        "table",
        "column",
        "nameColumn",
        "ordinalColumn",
        "parentColumn",
        "nullParentValue",
        TypeEnum.STRING,
        "approxRowCount",
        true,
        LevelTypeEnum.REGULAR,
        HideMemberIfEnum.NEVER,
        "formatter",
        "caption",
        "description",
        "captionColumn",
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(property),
        true,
        InternalTypeEnum.INT,
        null);

    @Test
    void testSchema() {

        when(schema.userDefinedFunctions()).thenAnswer(setupDummyListAnswer(userDefinedFunction));
        when(schema.parameter()).thenAnswer(setupDummyListAnswer(parameter));
        when(schema.roles()).thenAnswer(setupDummyListAnswer(role));

        List<VerificationResult> result = verifyer.verify(schema, null);
        assertThat(result).isNotNull()
            .hasSize(6);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(SCHEMA_NAME_MUST_BE_SET)
            .contains(USER_DEFINED_FUNCTION_NAME_MUST_BE_SET)
            .contains(String.format(EITHER_A_CLASS_NAME_OR_A_SCRIPT_ARE_REQUIRED, NOT_SET))
            .contains(PARAMETER_NAME_MUST_BE_SET)
            .contains(PARAMETER_TYPE_MUST_BE_SET)
            .contains(ROLE_NAME_MUST_BE_SET);

        assertThat(result).extracting(VerificationResult::title)
            .contains(SCHEMA)
            .contains(USER_DEFINED_FUNCTION)
            .contains(PARAMETER);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testCubeAndVirtualCubeAndCalculatedMemberAndAction() {
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(schema.virtualCube()).thenAnswer(setupDummyListAnswer(virtualCube));
        when(cube.calculatedMember()).thenAnswer(setupDummyListAnswer(calculatedMember));
        when(virtualCube.calculatedMember()).thenAnswer(setupDummyListAnswer(calculatedMember));
        when(cube.namedSet()).thenAnswer(setupDummyListAnswer(namedSet));
        when(virtualCube.namedSet()).thenAnswer(setupDummyListAnswer(namedSet));
        when(virtualCube.cubeUsages()).thenAnswer(setupDummyListAnswer(cubeUsage));
        when(cube.action()).thenAnswer(setupDummyListAnswer(action));
        when(cube.writebackTable()).thenAnswer(setupDummyListAnswer(writebackTable));
        when(writebackTable.columns()).thenAnswer(setupDummyListAnswer(writebackAttribute, writebackMeasure));
        when(cube.drillThroughAction()).thenAnswer(setupDummyListAnswer(drillThroughAction));
        when(drillThroughAction.drillThroughElement()).thenAnswer(setupDummyListAnswer(drillThroughMeasure,
            drillThroughAttribute));
        when(drillThroughAction.annotations()).thenAnswer(setupDummyListAnswer(annotation));

        List<VerificationResult> result = verifyer.verify(schema, null);
        assertThat(result).isNotNull()
            .hasSize(28);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(SCHEMA_NAME_MUST_BE_SET)
            .contains(CUBE_NAME_MUST_SET)
            .contains(String.format(FACT_NAME_MUST_BE_SET, NOT_SET))
            .contains(String.format(CUBE_WITH_NAME_MUST_CONTAIN_, NOT_SET, DIMENSIONS))
            .contains(VIRTUAL_CUBE_NAME_MUST_BE_SET)
            .contains(String.format(VIRTUAL_CUBE_MUST_CONTAIN_DIMENSIONS, NOT_SET))
            .contains(String.format(VIRTUAL_CUBE_MUST_CONTAIN_MEASURES, NOT_SET))
            .contains(String.format(VIRTUAL_CUBE_MUST_CONTAIN_MEASURES, NOT_SET))
            .contains(CALCULATED_MEMBER_NAME_MUST_BE_SET)
            .contains(String.format(DIMENSION_MUST_BE_SET_FOR_CALCULATED_MEMBER, NOT_SET))
            .contains(String.format(FORMULA_MUST_BE_SET_FOR_CALCULATED_MEMBER, NOT_SET))
            .contains(NAMED_SET_NAME_MUST_BE_SET)
            .contains(NAMED_SET_FORMULA_MUST_BE_SET)
            .contains(ACTION_NAME_MUST_BE_SET)
            .contains(WRITEBACK_TABLE_NAME_MUST_BE_SET)
            .contains(WRITEBACK_ATTRIBUTE_DIMENSION_MUST_BE_SET)
            .contains(WRITEBACK_ATTRIBUTE_COLUMN_MUST_BE_SET)
            .contains(WRITEBACK_MEASURE_NAME_MUST_BE_SET)
            .contains(WRITEBACK_MEASURE_COLUMN_MUST_BE_SET)
            .contains(DRILL_THROUGH_ATTRIBUTE_NAME_MUST_BE_SET)
            .contains(DRILL_THROUGH_MEASURE_NAME_MUST_BE_SET)
            .contains(ANNOTATION_NAME_MUST_BE_SET)
            .contains(CUBE_USAGE_CUBE_NAME_MUST_BE_SET);

        assertThat(result).extracting(VerificationResult::title)
            .contains(SCHEMA)
            .contains(CUBE)
            .contains(VIRTUAL_CUBE)
            .contains(DIMENSIONS)
            .contains(MEASURE)
            .contains(VIRTUAL_CUBE)
            .contains(CALCULATED_MEMBER)
            .contains(ACTION)
            .contains(WRITEBACK_TABLE)
            .contains(WRITEBACK_ATTRIBUTE)
            .contains(WRITEBACK_MEASURE)
            .contains(DRILL_THROUGH_ATTRIBUTE)
            .contains(DRILL_THROUGH_MEASURE)
            .contains(ANNOTATION)
            .contains(CUBE_USAGE);

        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testCubeAndVirtualCubeAndCalculatedMemberAndFormula() {
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(schema.virtualCube()).thenAnswer(setupDummyListAnswer(virtualCube));
        when(cube.calculatedMember()).thenAnswer(setupDummyListAnswer(calculatedMember));
        when(virtualCube.calculatedMember()).thenAnswer(setupDummyListAnswer(calculatedMember));
        when(calculatedMember.formulaElement()).thenReturn(formula);

        List<VerificationResult> result = verifyer.verify(schema, null);
        assertThat(result).isNotNull()
            .hasSize(14);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(SCHEMA_NAME_MUST_BE_SET)
            .contains(CUBE_NAME_MUST_SET)
            .contains(String.format(FACT_NAME_MUST_BE_SET, NOT_SET))
            .contains(String.format(CUBE_WITH_NAME_MUST_CONTAIN_, NOT_SET, DIMENSIONS))
            .contains(VIRTUAL_CUBE_NAME_MUST_BE_SET)
            .contains(String.format(VIRTUAL_CUBE_MUST_CONTAIN_DIMENSIONS, NOT_SET))
            .contains(String.format(VIRTUAL_CUBE_MUST_CONTAIN_MEASURES, NOT_SET))
            .contains(String.format(VIRTUAL_CUBE_MUST_CONTAIN_MEASURES, NOT_SET))
            .contains(CALCULATED_MEMBER_NAME_MUST_BE_SET)
            .contains(String.format(DIMENSION_MUST_BE_SET_FOR_CALCULATED_MEMBER, NOT_SET))
            .contains(FORMULA_MUST_BE_SET);

        assertThat(result).extracting(VerificationResult::title)
            .contains(SCHEMA)
            .contains(CUBE)
            .contains(VIRTUAL_CUBE)
            .contains(DIMENSIONS)
            .contains(MEASURE)
            .contains(VIRTUAL_CUBE)
            .contains(CALCULATED_MEMBER)
            .contains(FORMULA);

        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testMeasure() {
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(cube.name()).thenReturn("cubeName");
        when(measure.calculatedMemberProperty()).thenAnswer(setupDummyListAnswer(calculatedMemberProperty));

        List<VerificationResult> result = verifyer.verify(schema, null);
        assertThat(result).isNotNull()
            .hasSize(7);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(SCHEMA_NAME_MUST_BE_SET)
            .contains(String.format(FACT_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(CUBE_WITH_NAME_MUST_CONTAIN_, "cubeName", DIMENSIONS))
            .contains(String.format(MEASURE_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_AGGREGATOR_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_COLUMN_MUST_BE_SET, "cubeName"))
            .contains(CALCULATED_MEMBER_PROPERTY_NAME_MUST_BE_SET);
        assertThat(result).extracting(VerificationResult::title)
            .contains(SCHEMA)
            .contains(DIMENSIONS)
            .contains(MEASURE)
            .contains(CALCULATED_MEMBER_PROPERTY);

        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testHierarchyWithJoin() {
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(cube.name()).thenReturn("cubeName");
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(dimension));
        when(dimension.hierarchy()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.level()).thenAnswer(setupDummyListAnswer(level));
        when(hierarchy.relation()).thenReturn(join);
        when(hierarchy.primaryKeyTable()).thenReturn("primaryKeyTable");
        when(join.relation()).thenAnswer(setupDummyListAnswer(table, table));
        when(table.name()).thenReturn("tableName");
        when(table.sql()).thenReturn(sql);
        when(table.hint()).thenAnswer(setupDummyListAnswer(hint));
        when(table.aggTable()).thenAnswer(setupDummyListAnswer(aggTable));
        when(level.memberFormatter()).thenReturn(elementFormatter);        
        when(aggTable.aggIgnoreColumn()).thenAnswer(setupDummyListAnswer(aggColumnName));
        when(aggTable.aggForeignKey()).thenAnswer(setupDummyListAnswer(aggForeignKey));
        when(aggTable.aggMeasure()).thenAnswer(setupDummyListAnswer(aggMeasure));
        when(aggTable.aggLevel()).thenAnswer(setupDummyListAnswer(aggLevel));
        when(aggTable.measuresFactCount()).thenAnswer(setupDummyListAnswer(measuresFactCount));

        List<VerificationResult> result = verifyer.verify(schema, null);
        assertThat(result).isNotNull()
            .hasSize(40);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(SCHEMA_NAME_MUST_BE_SET)
            .contains(String.format(CUBE_DIMENSION_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(FACT_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_AGGREGATOR_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_COLUMN_MUST_BE_SET, "cubeName"))
            .contains(String.format(HIERARCHY_TABLE_VALUE_DOES_NOT_CORRESPOND_TO_ANY_JOIN, NOT_SET))
            .contains(String.format(LEVEL_NAME_MUST_BE_SET, NOT_SET))
            .contains(String.format(LEVEL_COLUMN_MUST_BE_SET, NOT_SET))
            .contains(JOIN_LEFT_KEY_MUST_BE_SET)
            .contains(JOIN_RIGHT_KEY_MUST_BE_SET)
            .contains(FORMATTER_EITHER_A_CLASS_NAME_OR_A_SCRIPT_ARE_REQUIRED)
            .contains(SQL_DIALECT_MUST_BE_SET)
            .contains(HINT_TYPE_MUST_BE_SET)
            .contains(AGG_TABLE_AGG_FACT_COUNT_MUST_BE_SET)
            .contains(AGG_COLUMN_NAME_COLUMN_MUST_BE_SET)
            .contains(AGG_FOREIGN_KEY_FACT_COLUMN_MUST_BE_SET)
            .contains(AGG_FOREIGN_KEY_AGG_COLUMN_MUST_BE_SET)
            .contains(AGG_MEASURE_COLUMN_MUST_BE_SET)
            .contains(AGG_MEASURE_NAME_MUST_BE_SET)
            .contains(AGG_LEVEL_NAME_MUST_BE_SET)
            .contains(AGG_LEVEL_COLUMN_MUST_BE_SET)
            .contains(AGG_MEASURE_FACT_COUNT_FACT_COLUMN_MUST_BE_SET);

        assertThat(result).extracting(VerificationResult::title)
            .contains(SCHEMA)
            .contains(CUBE)
            .contains(MEASURE)
            .contains(HIERARCHY)
            .contains(LEVEL)
            .contains(JOIN)
            .contains(ELEMENT_FORMATTER)
            .contains(SchemaWalkerMessages.SQL)
            .contains(HINT)
            .contains(AGG_TABLE)
            .contains(AGG_COLUMN_NAME)
            .contains(AGG_FOREIGN_KEY)
            .contains(AGG_MEASURE)
            .contains(AGG_LEVEL)
            .contains(AGG_MEASURE_FACT_COUNT);

        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testHierarchyWithoutJoin() {
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(cube.name()).thenReturn("cubeName");
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(dimension));
        when(dimension.hierarchy()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.level()).thenAnswer(setupDummyListAnswer(level));
        when(hierarchy.relation()).thenReturn(join);
        when(level.memberFormatter()).thenReturn(elementFormatter);
        when(level.property()).thenAnswer(setupDummyListAnswer(property));

        List<VerificationResult> result = verifyer.verify(schema, null);
        assertThat(result).isNotNull()
            .hasSize(15);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(SCHEMA_NAME_MUST_BE_SET)
            .contains(String.format(FACT_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_AGGREGATOR_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_COLUMN_MUST_BE_SET, "cubeName"))
            .contains(String.format(PRIMARY_KEY_TABLE_AND_PRIMARY_KEY_MUST_BE_SET_FOR_JOIN_, NOT_SET))
            .contains(String.format(LEVEL_NAME_MUST_BE_SET, NOT_SET))
            .contains(FORMATTER_EITHER_A_CLASS_NAME_OR_A_SCRIPT_ARE_REQUIRED)
            .contains(PROPERTY_COLUMN_MUST_BE_SET);

        assertThat(result).extracting(VerificationResult::title)
            .contains(SCHEMA)
            .contains(CUBE)
            .contains(MEASURE)
            .contains(HIERARCHY)
            .contains(LEVEL)
            .contains(ELEMENT_FORMATTER)
            .contains(PROPERTY)
        ;

        assertThat(result).extracting(VerificationResult::level)
            .contains(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR,
            		org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.WARNING);
    }

    @Test
    void testCheckColumn_With_Table() {
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(cube.name()).thenReturn("cubeName");
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(dimension));
        when(dimension.hierarchy()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.level()).thenAnswer(setupDummyListAnswer(l));
        when(hierarchy.relation()).thenReturn(table);
        when(table.name()).thenReturn("tableName");
        List<VerificationResult> result = verifyer.verify(schema, null);
        assertThat(result).isNotNull()
            .hasSize(20);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(SCHEMA_NAME_MUST_BE_SET)
            .contains(String.format(FACT_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_AGGREGATOR_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_COLUMN_MUST_BE_SET, "cubeName"))
            .contains(PROPERTY_COLUMN_MUST_BE_SET)
            .contains(TABLE_VALUE_DOES_NOT_CORRESPOND_TO_HIERARCHY_RELATION);

        assertThat(result).extracting(VerificationResult::title)
            .contains(SCHEMA)
            .contains(CUBE)
            .contains(MEASURE)
            .contains(LEVEL)
            .contains(TABLE)
            .contains(PROPERTY);

        assertThat(result).extracting(VerificationResult::level)
            .contains(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR,
            		org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.WARNING);
    }

    @Test
    void testCheckColumn_With_Join() {
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(cube.name()).thenReturn("cubeName");
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(dimension));
        when(dimension.hierarchy()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.level()).thenAnswer(setupDummyListAnswer(l));
        when(hierarchy.relation()).thenReturn(join);
        when(join.relation()).thenAnswer(setupDummyListAnswer(table, table));
        when(table.name()).thenReturn("tableName");
        List<VerificationResult> result = verifyer.verify(schema, null);
        assertThat(result).isNotNull()
            .hasSize(36);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(SCHEMA_NAME_MUST_BE_SET)
            .contains(String.format(FACT_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_NAME_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_AGGREGATOR_MUST_BE_SET, "cubeName"))
            .contains(String.format(MEASURE_COLUMN_MUST_BE_SET, "cubeName"))
            .contains(String.format(PRIMARY_KEY_TABLE_AND_PRIMARY_KEY_MUST_BE_SET_FOR_JOIN_, NOT_SET))
            .contains(PROPERTY_COLUMN_MUST_BE_SET)
            .contains(TABLE_VALUE_DOES_NOT_CORRESPOND_TO_ANY_JOIN);

        assertThat(result).extracting(VerificationResult::title)
            .contains(SCHEMA)
            .contains(CUBE)
            .contains(MEASURE)
            .contains(HIERARCHY)
            .contains(LEVEL)
            .contains(PROPERTY);

        assertThat(result).extracting(VerificationResult::level)
            .contains(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR, org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.WARNING);
    }

    public record LevelTest(String name,
                            String table,
                            String column,
                            String nameColumn,
                            String ordinalColumn,
                            String parentColumn,
                            String nullParentValue,
                            TypeEnum type,
                            String approxRowCount,
                            boolean uniqueMembers,
                            LevelTypeEnum levelType,
                            HideMemberIfEnum hideMemberIf,
                            String formatter,
                            String caption,
                            String description,
                            String captionColumn,
                            List<Annotation> annotations,
                            ExpressionView keyExpression,
                            ExpressionView nameExpression,
                            ExpressionView captionExpression,
                            ExpressionView ordinalExpression,
                            ExpressionView parentExpression,
                            Closure closure,
                            List<Property> property,
                            boolean visible,
                            InternalTypeEnum internalType,
                            ElementFormatter memberFormatter
    )
        implements Level {

    }
}
