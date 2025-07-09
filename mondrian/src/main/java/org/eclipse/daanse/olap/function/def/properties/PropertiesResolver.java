/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.function.def.properties;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Literal;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.osgi.service.component.annotations.Component;

/**
 * Resolves calls to the <code>PROPERTIES</code> MDX function.
 */
@Component(service = FunctionResolver.class)
public class PropertiesResolver  implements FunctionResolver {


        private boolean matches(
            Expression[] args,
            FunctionParameterR[] parameterTypes,
            Validator validator,
            List<Conversion> conversions)
        {
            if (parameterTypes.length != args.length) {
                return false;
            }
            for (int i = 0; i < args.length; i++) {
                if (!validator.canConvert(
                        i, args[i], parameterTypes[i].dataType(), conversions))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public FunctionDefinition resolve(
            Expression[] args,
            Validator validator,
            List<Conversion> conversions)
        {
            if (!matches(args, PropertiesFunDef.PARAMETER_TYPES, validator, conversions)) {
                return null;
            }
            DataType returnType = deducePropertyCategory(args[0], args[1]);


            return new PropertiesFunDef(
                returnType);
        }

        /**
         * Deduces the category of a property. This is possible only if the
         * name is a string literal, and the member's hierarchy is unambigous.
         * If the type cannot be deduced, returns {@link DataType#VALUE}.
         *
         * @param memberExp Expression for the member
         * @param propertyNameExp Expression for the name of the property
         * @return Category of the property
         */
        private DataType deducePropertyCategory(
            Expression memberExp,
            Expression propertyNameExp)
        {
            if (!(propertyNameExp instanceof Literal)) {
                return DataType.VALUE;
            }
            String propertyName =
                (String) ((Literal<?>) propertyNameExp).getValue();
            Hierarchy hierarchy = memberExp.getType().getHierarchy();
            if (hierarchy == null) {
                return DataType.VALUE;
            }
            List<? extends Level> levels = hierarchy.getLevels();
            Property property = Util.lookupProperty(
                levels.getLast(), propertyName);
            if (property == null) {
                // we'll likely get a runtime error
                return DataType.VALUE;
            } else {
                switch (property.getType()) {
                case TYPE_BOOLEAN:
                    return DataType.LOGICAL;
                case TYPE_NUMERIC:
                case TYPE_INTEGER:
                case TYPE_LONG:
                    return DataType.NUMERIC;
                case TYPE_STRING:
                    return DataType.STRING;
                case TYPE_DATE:
                case TYPE_TIME:
                case TYPE_TIMESTAMP:
                    return DataType.DATE_TIME;
                default:
                    throw Util.badValue(property.getType());
                }
            }
        }

        @Override
        public boolean requiresScalarExpressionOnArgument(int k) {
            return true;
        }

        @Override
        public OperationAtom getFunctionAtom() {
            return PropertiesFunDef.functionAtom;
        }

    }
