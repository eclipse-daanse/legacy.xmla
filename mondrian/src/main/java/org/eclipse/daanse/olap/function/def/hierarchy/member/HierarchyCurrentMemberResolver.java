package org.eclipse.daanse.olap.function.def.hierarchy.member;

import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.resolver.ParametersCheckingFunctionDefinitionResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class HierarchyCurrentMemberResolver extends ParametersCheckingFunctionDefinitionResolver {

    public HierarchyCurrentMemberResolver() {
        super(new HierarchyCurrentMemberFunDef());
    }
}
