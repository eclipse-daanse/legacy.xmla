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
package org.eclipse.daanse.olap.function.def.periodstodate.xtd;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.element.LevelType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;


@Component(service = FunctionResolver.class)
public class MtdMultiResolver extends AbstractFunctionDefinitionMultiResolver {

	private static OperationAtom atom = new FunctionOperationAtom("Mtd");

	private static FunctionMetaData functionMetaDataWithMember = new FunctionMetaDataR(atom,
			"A shortcut function for the PeriodsToDate function that specifies the level to be Month.", "Mtd(<Member>)",
			DataType.SET, new FunctionParameterR[] { new FunctionParameterR( DataType.MEMBER ) });

	private static FunctionMetaData functionMetaDataWithoutMember = new FunctionMetaDataR(atom,
			"A shortcut function for the PeriodsToDate function that specifies the level to be Month.", "Mtd()",
			DataType.SET, new FunctionParameterR[] { });

	public MtdMultiResolver() {
		super(List.of(new XtdFunDef(functionMetaDataWithMember, LevelType.TIME_MONTHS),
				new XtdFunDef(functionMetaDataWithoutMember, LevelType.TIME_MONTHS)));
	}

}
