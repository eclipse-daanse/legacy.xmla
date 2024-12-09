package org.eclipse.daanse.olap.function.def.median;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;

public class MedianResolver extends AbstractFunctionDefinitionMultiResolver {
    private static FunctionOperationAtom atom = new FunctionOperationAtom("Median");
    private static String SIGNATURE = "Median(<Set>[, <Numeric Expression>])";
    private static String DESCRIPTION = "Returns the median value of a numeric expression evaluated over a set.";
    private static FunctionParameterR[] x = { new FunctionParameterR(DataType.SET) };
    private static FunctionParameterR[] xn = { new FunctionParameterR(DataType.SET),
            new FunctionParameterR(DataType.NUMERIC) };
    // {"fnx", "fnxn"}

    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, x);
    private static FunctionMetaData functionMetaData1 = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.NUMERIC, xn);

    public MedianResolver() {
        super(List.of(new MedianFunDef(functionMetaData), new MedianFunDef(functionMetaData1)));
    }
}