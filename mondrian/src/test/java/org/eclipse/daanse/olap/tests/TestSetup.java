/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.tests;

import org.eclipse.daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory;
import org.eclipse.daanse.rolap.core.BasicContext;
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
            @Property(key = BasicContext.REF_NAME_CATALOG_MAPPING_SUPPLIER
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
