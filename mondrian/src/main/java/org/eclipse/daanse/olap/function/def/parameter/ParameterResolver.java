package org.eclipse.daanse.olap.function.def.parameter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.NumericType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractMetaDataMultiResolver;
import org.osgi.service.component.annotations.Component;

import mondrian.olap.Util;
import mondrian.olap.fun.BuiltinFunTable;
import mondrian.olap.fun.FunUtil;

/**
 * Resolves calls to the <code>Parameter</code> MDX function.
 */
@Component(service = FunctionResolver.class)
public class ParameterResolver extends AbstractMetaDataMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Parameter");
    private static String SIGNATURE = "Parameter(<Name>, <Type>, <DefaultValue>, <Description>, <Set>)";
    private static String DESCRIPTION = "Returns default value of parameter.";

    private static FunctionParameterR[] SySS = { new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.SYMBOL), new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.STRING) };
    private static FunctionParameterR[] SyS = { new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.SYMBOL), new FunctionParameterR(DataType.STRING) };

    private static FunctionParameterR[] SynS = { new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.SYMBOL), new FunctionParameterR(DataType.NUMERIC),
            new FunctionParameterR(DataType.STRING) };
    private static FunctionParameterR[] Syn = { new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.SYMBOL), new FunctionParameterR(DataType.NUMERIC) };

    private static FunctionParameterR[] ShmS = { new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.HIERARCHY), new FunctionParameterR(DataType.MEMBER),
            new FunctionParameterR(DataType.STRING) };
    private static FunctionParameterR[] Shm = { new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.HIERARCHY), new FunctionParameterR(DataType.MEMBER) };

    private static FunctionParameterR[] ShxS = { new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.HIERARCHY), new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.STRING) };
    private static FunctionParameterR[] Shx = { new FunctionParameterR(DataType.STRING),
            new FunctionParameterR(DataType.HIERARCHY), new FunctionParameterR(DataType.SET) };

    // {"fSSySS", "fSSyS", "fnSynS", "fnSyn", "fmShmS", "fmShm","fxShxS", "fxShx"}

    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.STRING, SySS);
    private static FunctionMetaData functionMetaData2 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.STRING, SyS);

    private static FunctionMetaData functionMetaData3 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, SynS);
    private static FunctionMetaData functionMetaData4 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, Syn);

    private static FunctionMetaData functionMetaData5 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.MEMBER, ShmS);
    private static FunctionMetaData functionMetaData6 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.MEMBER, Shm);

    private static FunctionMetaData functionMetaData7 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, ShxS);
    private static FunctionMetaData functionMetaData8 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.SET, Shx);

    private static final List<String> RESERVED_WORDS = List.of("NUMERIC", "STRING");

    @Override
    public List<String> getReservedWords() {
        return RESERVED_WORDS;
    }

    public ParameterResolver() {

        super(List.of(functionMetaData1, functionMetaData2, functionMetaData3, functionMetaData4, functionMetaData5,
                functionMetaData6, functionMetaData7, functionMetaData8));
    }

    @Override
    protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData,
            FunctionMetaData fmdTarget) {
        String parameterName = ParameterFunDef.getParameterName(args);
        Expression typeArg = args[1];
        DataType category;
        Type type = typeArg.getType();
        switch (typeArg.getCategory()) {
        case DIMENSION:
        case HIERARCHY:
        case LEVEL:
            Dimension dimension = type.getDimension();
            if (!ParameterFunDef.isConstant(typeArg)) {
                throw FunUtil.newEvalException(functionMetaData,
                        new StringBuilder("Invalid parameter '").append(parameterName)
                                .append("'. Type must be a NUMERIC, STRING, or a dimension, ")
                                .append("hierarchy or level").toString());
            }
            if (dimension == null) {
                throw FunUtil.newEvalException(functionMetaData, new StringBuilder("Invalid dimension for parameter '")
                        .append(parameterName).append("'").toString());
            }
            type = new MemberType(type.getDimension(), type.getHierarchy(), type.getLevel(), null);
            category = DataType.MEMBER;
            break;

        case SYMBOL:
            String s = (String) ((Literal<?>) typeArg).getValue();
            if (s.equalsIgnoreCase("NUMERIC")) {
                category = DataType.NUMERIC;
                type = NumericType.INSTANCE;
                break;
            } else if (s.equalsIgnoreCase("STRING")) {
                category = DataType.STRING;
                type = StringType.INSTANCE;
                break;
            }
            // fall through and throw error
        default:
            // Error is internal because the function call has already been
            // type-checked.
            throw FunUtil.newEvalException(functionMetaData, new StringBuilder("Invalid type for parameter '")
                    .append(parameterName).append("'; expecting NUMERIC, STRING or a hierarchy").toString());
        }

        // Default value
        Expression exp = args[2];
        Validator validator = Util.createSimpleValidator(BuiltinFunTable.instance());
        final List<Conversion> conversionList = new ArrayList<>();
        String typeName = category.getName().toUpperCase();
        if (!validator.canConvert(2, exp, category, conversionList)) {
            throw FunUtil.newEvalException(functionMetaData, new StringBuilder("Default value of parameter '")
                    .append(parameterName).append("' is inconsistent with its type, ").append(typeName).toString());
        }
        if (exp.getCategory() == DataType.SET && category == DataType.MEMBER) {
            // Default value is a set; take this an indication that
            // the type is 'set of <member type>'.
            type = new SetType(type);
        }
        if (category == DataType.MEMBER) {
            Type expType = exp.getType();
            if (expType instanceof SetType) {
                expType = ((SetType) expType).getElementType();
            }
            if (ParameterResolver.distinctFrom(type.getDimension(), expType.getDimension())
                    || ParameterResolver.distinctFrom(type.getHierarchy(), expType.getHierarchy())
                    || ParameterResolver.distinctFrom(type.getLevel(), expType.getLevel())) {
                throw FunUtil.newEvalException(functionMetaData,
                        new StringBuilder("Default value of parameter '").append(parameterName)
                                .append("' is not consistent with the parameter type '").append(type).toString());
            }
        }

        String parameterDescription = null;
        if (args.length > 3) {
            if (args[3] instanceof Literal && args[3].getCategory() == DataType.STRING) {
                parameterDescription = (String) ((Literal<?>) args[3]).getValue();
            } else {
                throw FunUtil.newEvalException(functionMetaData, new StringBuilder("Description of parameter '")
                        .append(parameterName).append("' must be a string constant").toString());
            }
        }

        return new ParameterFunDef(functionMetaData, parameterName, type, category, exp, parameterDescription);
    }

    private static <T> boolean distinctFrom(T t1, T t2) {
        return t1 != null && t2 != null && !t1.equals(t2);
    }

}