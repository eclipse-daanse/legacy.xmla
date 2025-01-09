package org.eclipse.daanse.olap.tests;

import org.eclipse.daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory;
import org.eclipse.daanse.olap.rolap.core.BasicContext;
import org.osgi.test.common.annotation.Property;
import org.osgi.test.common.annotation.Property.TemplateArgument;
import org.osgi.test.common.annotation.Property.ValueSource;
import org.osgi.test.common.annotation.config.WithFactoryConfiguration;


public class TestSetup {

    private TestSetup() {
    }

    @DefaultTestSetup
    @WithFoodMartSchemaRecord
    @interface DefaultTestSetupFoodmart {
    }

    @BasicContextNoReferences
    @WithBaseExpressionCompilerFactory
    @interface DefaultTestSetup {
    }

    @WithFactoryConfiguration(factoryPid = BasicContext.PID, location = "?", properties = {
            @Property(key = BasicContext.REF_NAME_DATA_SOURCE
                    + ".target", value = "(test.exec=%s)", templateArguments = @TemplateArgument(source = ValueSource.TestUniqueId)),
            @Property(key = BasicContext.REF_NAME_DATA_SOURCE
                    + ".target", value = "(test.exec=%s)", templateArguments = @TemplateArgument(source = ValueSource.TestUniqueId)),
            @Property(key = BasicContext.REF_NAME_DB_MAPPING_CATALOG_SUPPLIER
                    + ".target", value = "(test.exec=%s)", templateArguments = @TemplateArgument(source = ValueSource.TestUniqueId)),
            @Property(key = BasicContext.REF_NAME_EXPRESSION_COMPILER_FACTORY
                    + ".target", value = "(test.exec=%s)", templateArguments = @TemplateArgument(source = ValueSource.TestUniqueId)) })
    @interface BasicContextNoReferences {
    }

//    @WithFactoryConfiguration(factoryPid = FoodMartRecordDbMappingSchemaProvider.PID, location = "?", properties = {
//            @Property(key = "test.exec", source = ValueSource.TestUniqueId), })
    @interface WithFoodMartSchemaRecord {
    }
    
    
    @WithFactoryConfiguration(factoryPid = BaseExpressionCompilerFactory.PID, location = "?", properties = {
            @Property(key = "test.exec", source = ValueSource.TestUniqueId), })
    @interface WithBaseExpressionCompilerFactory {
    }
    
//    @WithFactoryConfiguration(factoryPid = JdbcStatisticsProvider.PID, location = "?", properties = {
//            @Property(key = "test.exec", source = ValueSource.TestUniqueId), })
//    @interface WithJdbcStatisticsProvider {
//    }

    

}
