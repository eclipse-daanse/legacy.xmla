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
package org.eclipse.daanse.olap.function.def.order;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.fun.sort.Sorter.SorterFlag;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class OrderResolver  extends NoExpressionRequiredFunctionResolver {
    private final List<String> reservedWords;
    static FunctionParameterR[] argTypes;

    public OrderResolver() {

      this.reservedWords = SorterFlag.asReservedWords();
    }

    @Override
    public FunctionDefinition resolve( Expression[] args, Validator validator, List<Conversion> conversions ) {
      OrderResolver.argTypes = new FunctionParameterR[args.length];

      if ( args.length < 2 ) {
        return null;
      }
      // first arg must be a set
      if ( !validator.canConvert( 0, args[0], DataType.SET, conversions ) ) {
        return null;
      }
      OrderResolver.argTypes[0] = new FunctionParameterR(DataType.SET);
      // after fist args, should be: value [, symbol]
      int i = 1;
      while ( i < args.length ) {
        if ( !validator.canConvert( i, args[i], DataType.VALUE, conversions ) ) {
          return null;
        } else {
          OrderResolver.argTypes[i] = new FunctionParameterR(DataType.VALUE);
          i++;
        }
        // if symbol is not specified, skip to the next
        if ( ( i == args.length ) ) {
          // done, will default last arg to ASC
        } else {
          if ( !validator.canConvert( i, args[i], DataType.SYMBOL, conversions ) ) {
            // continue, will default sort flag for prev arg to ASC
          } else {
            OrderResolver.argTypes[i] = new FunctionParameterR(DataType.SYMBOL);
            i++;
          }
        }
      }

      return new OrderFunDef( OrderResolver.argTypes );
    }

    @Override
    public List<String> getReservedWords() {
      if ( reservedWords != null ) {
        return reservedWords;
      }
      return super.getReservedWords();
    }

    @Override
    public OperationAtom getFunctionAtom() {
        return OrderFunDef.functionAtom;
    }
  }
