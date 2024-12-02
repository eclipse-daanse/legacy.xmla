/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionTable;
import org.eclipse.daanse.olap.function.core.resolver.ParametersCheckingFunctionDefinitionResolver;

/**
 * Abstract implementation of {@link FunctionTable}.
 *
 * <p>The derived class must implement
 * {@link #defineFunctions(org.eclipse.daanse.olap.api.function.FunctionTable.FunctionTableCollector)} to define
 * each function which will be recognized by this table. This method is called
 * from the constructor, after which point, no further functions can be added.
 */
public abstract class FunTableImpl implements FunctionTable {
	private List<FunctionMetaData> functionMetaDatas;
    /**
     * Maps the {@link OperationAtom} with an upper-case {@link OperationAtom#name()}  to an {@link List} of
     * {@link FunctionResolver} objects for that name.
     */
    private Map<FunctionAtomCompareKey, List<FunctionResolver>> mapNameToResolvers;
    private Set<String> reservedWordSet;
    private List<String> reservedWordList;
    private Set<String> propertyWords;

    /**
     * Creates a FunTableImpl.
     */
    protected FunTableImpl() {
    }

	/**
	 * Initializes the function table.
	 */
	public final void init() {
		final BuilderImpl builder = new BuilderImpl();
		init(builder);

	}

    public final void init(BuilderImpl builder) {
        defineFunctions(builder);
        builder.organizeFunctions();

        // Copy information out of builder into this.
        this.functionMetaDatas = Collections.unmodifiableList(builder.functionMetaDatas);
        this.mapNameToResolvers =
            Collections.unmodifiableMap(builder.mapNameToResolvers);
        this.reservedWordSet = builder.reservedWords;
        final String[] reservedWords =
            builder.reservedWords.toArray(
                new String[builder.reservedWords.size()]);
        Arrays.sort(reservedWords);
        this.reservedWordList =
            Collections.unmodifiableList(Arrays.asList(reservedWords));
        this.propertyWords = Collections.unmodifiableSet(builder.propertyWords);
    }



    @Override
	public List<String> getReservedWords() {
        return reservedWordList;
    }

    @Override
    public Set<String> getPropertyWords() {
        return propertyWords;
    }

    @Override
	public boolean isReservedWord(String s) {
        return reservedWordSet.contains(s.toUpperCase());
    }

    @Override
	public List<FunctionResolver> getResolvers() {
        final List<FunctionResolver> list = new ArrayList<>();
        for (List<FunctionResolver> resolvers : mapNameToResolvers.values()) {
            list.addAll(resolvers);
        }
        return list;
    }

    @Override
	public boolean isProperty(String s) {
        return propertyWords.contains(s.toUpperCase());
    }

    @Override
	public List<FunctionMetaData> getFunctionMetaDatas() {
        return functionMetaDatas;
    }

    @Override
	public List<FunctionResolver> getResolvers(OperationAtom operationAtom) {
    	FunctionAtomCompareKey key = new FunctionAtomCompareKey(operationAtom);
        List<FunctionResolver> resolvers = mapNameToResolvers.get(key);
        if (resolvers == null) {
            resolvers = Collections.emptyList();
        }
        return resolvers;
    }
    public record FunctionAtomCompareKey(String name, Class<?> clazz) {

		public FunctionAtomCompareKey(String name, Class<?> clazz) {
			this.name = name.toUpperCase();
			this.clazz = clazz;
		}

		public FunctionAtomCompareKey(OperationAtom functionAtom) {
			this(functionAtom.name(), functionAtom.getClass());
		}

    };

    /**
     * Implementation of {@link org.eclipse.daanse.olap.api.function.FunctionTable.FunctionTableCollector}.
     * Functions are added to lists each time {@link #define(FunctionResolver)} is
     * called, then {@link #organizeFunctions()} sorts and indexes the map.
     */
    private class BuilderImpl implements FunctionTableCollector {
    	private final List<FunctionMetaData> functionMetaDatas = new ArrayList<>();
        private final List<FunctionResolver> resolverList = new ArrayList<>();
        private final Map<FunctionAtomCompareKey, List<FunctionResolver>>
            mapNameToResolvers =
            new HashMap<>();
        private final Set<String> reservedWords = new HashSet<>();
        private final Set<String> propertyWords = new HashSet<>();

        @Override
		public void define(FunctionDefinition funDef) {
            define(new ParametersCheckingFunctionDefinitionResolver(funDef));
        }

		@Override
		public void define(FunctionResolver resolver) {

			functionMetaDatas.addAll(resolver.getRepresentativeFunctionMetaDatas());

			OperationAtom functionAtom= resolver.getFunctionAtom();

			if (functionAtom instanceof PlainPropertyOperationAtom) {
				propertyWords.add(functionAtom.name().toUpperCase());
			}

			resolverList.add(resolver);
			final List<String> reservedWordsInner = resolver.getReservedWords();
			for (String reservedWord : reservedWordsInner) {
				defineReserved(reservedWord);
			}
		}

        @Override
		public void define(FunctionMetaData functionMetaData) {
           functionMetaDatas.add(functionMetaData);
        }

        @Override
		public void defineReserved(String s) {
            reservedWords.add(s.toUpperCase());
        }

        /**
         * Indexes the collection of functions.
         */
        protected void organizeFunctions() {

        	Comparator<FunctionMetaData> comparatorFMD=new Comparator<FunctionMetaData>() {

				@Override
				public int compare(FunctionMetaData o1, FunctionMetaData o2) {
					//TODO:
					return 0;
				}
			};
            Collections.sort(functionMetaDatas,comparatorFMD);

            // Map upper-case function names to resolvers.
            final List<List<FunctionResolver>> nonSingletonResolverLists =
                new ArrayList<>();
            for (FunctionResolver resolver : resolverList) {
            	FunctionAtomCompareKey key=	 new FunctionAtomCompareKey(resolver.getFunctionAtom());


                List<FunctionResolver> list = mapNameToResolvers.computeIfAbsent(key, k -> new ArrayList<>());
                list.add(resolver);
                if (list.size() == 2) {
                    nonSingletonResolverLists.add(list);
                }
            }

            // Sort lists by signature (skipping singleton lists)
            final Comparator<FunctionResolver> comparator =
                new Comparator<>() {
                    @Override
					public int compare(FunctionResolver o1, FunctionResolver o2) {
                        return o1.getFunctionAtom().getClass().getName().compareTo(o2.getFunctionAtom().getClass().getName());
                    }
                };
            for (List<FunctionResolver> resolverListInner : nonSingletonResolverLists) {
                Collections.sort(resolverListInner, comparator);
            }
        }
    }
}
