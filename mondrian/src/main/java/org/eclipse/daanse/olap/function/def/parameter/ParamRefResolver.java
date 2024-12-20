package org.eclipse.daanse.olap.function.def.parameter;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractMetaDataMultiResolver;
import org.osgi.service.component.annotations.Component;

/**
 * Resolves calls to the <code>ParamRef</code> MDX function.
 */
@Component(service = FunctionResolver.class)
public class ParamRefResolver  extends AbstractMetaDataMultiResolver {

    private static FunctionOperationAtom atom = new FunctionOperationAtom("ParamRef");
    private static String SIGNATURE = "ParamRef(<Name>)";
    private static String DESCRIPTION = "Returns the current value of this parameter. If it is null, returns the default value.";
    
    private static FunctionParameterR[] S = { new FunctionParameterR(DataType.STRING) };
    //"fvS"
    
    private static FunctionMetaData functionMetaData = new FunctionMetaDataR(atom, DESCRIPTION, SIGNATURE,
            DataType.VALUE, S);

    public ParamRefResolver() {
        super(List.of(functionMetaData));
    }


    @Override
    protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData,
            FunctionMetaData fmdTarget) {
        String parameterName = ParameterFunDef.getParameterName(args);
        return new ParameterFunDef(
            functionMetaData, parameterName, null, DataType.UNKNOWN, null,
            null);
    }

}
