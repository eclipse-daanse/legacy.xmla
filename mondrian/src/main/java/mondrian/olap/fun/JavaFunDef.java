/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Syntax;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.FunctionMetaDataR;

import mondrian.calc.impl.GenericCalc;
import mondrian.olap.Util;

/**
 * MDX function which is implemented by a Java method. When the function is
 * executed, the method is invoked via reflection.
 *
 * @author wgorman, jhyde
 * @since Jan 5, 2008
*/
public class JavaFunDef extends AbstractFunctionDefinition {
	
	private static final Map<Class, DataType> mapClazzToCategory = Map.ofEntries(
			Map.entry(String.class, DataType.STRING), //
			Map.entry(Double.class, DataType.NUMERIC), //
			Map.entry(double.class, DataType.NUMERIC), //
			Map.entry(Integer.class, DataType.INTEGER), //
			Map.entry(int.class, DataType.INTEGER), //
			Map.entry(boolean.class, DataType.LOGICAL), //
			Map.entry(Object.class, DataType.VALUE), //
			Map.entry(Date.class, DataType.DATE_TIME), //
			Map.entry(float.class, DataType.NUMERIC), //
			Map.entry(long.class, DataType.NUMERIC), //
			Map.entry(double[].class, DataType.ARRAY), //
			Map.entry(char.class, DataType.STRING), //
			Map.entry(byte.class, DataType.INTEGER));


    private final Method method;

    /**
     * Creates a JavaFunDef.
     *
     * @param name Name
     * @param desc Description
     * @param syntax Syntax
     * @param returnCategory Return type
     * @param paramCategories Parameter types
     * @param method Java method which implements this function
     */
	public JavaFunDef(FunctionMetaData functionMetaDataEmpty, Method method) {
		super(functionMetaDataEmpty);
		this.method = method;
	}

    @Override
	public Calc compileCall(
        ResolvedFunCall call,
        ExpressionCompiler compiler)
    {
        final Calc[] calcs = new Calc[getFunctionMetaData().parameterDataTypes().length];
        final Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < calcs.length;i++) {
            calcs[i] =
                JavaFunDef.compileTo(
                    compiler, call.getArgs()[i], parameterTypes[i]);
        }
        return new JavaMethodCalc(call, calcs, method);
    }

    private static DataType getCategory(Class clazz) {
        return JavaFunDef.mapClazzToCategory.get(clazz);
    }

    private static DataType getReturnCategory(Method m) {
        return JavaFunDef.getCategory(m.getReturnType());
    }

    private static DataType[] getParameterCategories(Method m) {
    	DataType[] arr = new DataType[m.getParameterTypes().length];
        for (int i = 0; i < m.getParameterTypes().length; i++) {
            arr[i] = JavaFunDef.getCategory(m.getParameterTypes()[i]);
        }
        return arr;
    }

    private static FunctionDefinition generateFunDef(final Method method) {
        String name =
            getAnnotation(
                method, FunctionName.class, method.getName());
        String desc =
            getAnnotation(
                method, Description.class, "");
        Syntax syntax =
            getAnnotation(
                method, SyntaxDef.class, Syntax.Function);

        DataType returnCategory = JavaFunDef.getReturnCategory(method);

        DataType[] paramCategories = JavaFunDef.getParameterCategories(method);

        
    	OperationAtom functionAtom = syntax.getOperationAtom(name);

        FunctionMetaData functionMetaDataEmpty = new FunctionMetaDataR(functionAtom,
        		desc, "", returnCategory, paramCategories
    			);
        return new JavaFunDef(functionMetaDataEmpty, method);
    }

    /**
     * Scans a java class and returns a list of function definitions, one for
     * each static method which is suitable to become an MDX function.
     *
     * @param clazz Class
     * @return List of function definitions
     */
    public static List<FunctionDefinition> scan(Class clazz) {
        List<FunctionDefinition> list = new ArrayList<>();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())
                && !method.getName().equals("main"))
            {
                list.add(JavaFunDef.generateFunDef(method));
            }
        }
        return list;
    }

    /**
     * Compiles an expression to a calc of the required result type.
     *
     * <p>Since the result of evaluating the calc will be passed to the method
     * using reflection, it is important that the calc returns
     * <em>precisely</em> the correct type: if a method requires an
     * <code>int</code>, you can pass an {@link Integer} but not a {@link Long}
     * or {@link Float}.
     *
     * <p>If it can be determined that the underlying calc will never return
     * null, generates an optimal form with one fewer object instantiation.
     *
     * @param compiler Compiler
     * @param exp Expression to compile
     * @param clazz Desired class
     * @return compiled expression
     */
    private static Calc compileTo(ExpressionCompiler compiler, Expression exp, Class clazz) {
        if (clazz == String.class) {
            return compiler.compileString(exp);
        } else if (clazz == Date.class) {
            return compiler.compileDateTime(exp);
        } else if (clazz == boolean.class) {
            return compiler.compileBoolean(exp);
        } else if (clazz == byte.class) {
            final IntegerCalc integerCalc = compiler.compileInteger(exp);
            if (integerCalc.getResultStyle() == ResultStyle.VALUE_NOT_NULL) {
                // We know that the calculation will never return a null value,
                // so generate optimized code.
                return new AbstractCalc2(exp, integerCalc) {
                    @Override
					public Object evaluate(Evaluator evaluator) {

						Integer i = integerCalc.evaluate(evaluator);
						if (i == null) {
							return null;
						}
						return i.byteValue();
					}
                };
            } else {
                return new AbstractCalc2(exp, integerCalc) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
                        Integer i = (Integer) integerCalc.evaluate(evaluator);
                        return i == null ? null : (byte) i.intValue();
                    }
                };
            }
        } else if (clazz == char.class) {
            final StringCalc stringCalc = compiler.compileString(exp);
            return new AbstractCalc2(exp, stringCalc) {
                @Override
				public Object evaluate(Evaluator evaluator) {
                    final String string =
                        stringCalc.evaluate(evaluator);
                    return
                        Character.valueOf(
                            string == null
                            || string.length() < 1
                                ? (char) 0
                                : string.charAt(0));
                }
            };
        } else if (clazz == short.class) {
            final IntegerCalc integerCalc = compiler.compileInteger(exp);
            if (integerCalc.getResultStyle() == ResultStyle.VALUE_NOT_NULL) {
                return new AbstractCalc2(exp, integerCalc) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
						Integer i = integerCalc.evaluate(evaluator);
						if (i == null) {
							return null;
						}
						return i.shortValue();
					}
                };
            } else {
                return new AbstractCalc2(exp, integerCalc) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
                        Integer i = (Integer) integerCalc.evaluate(evaluator);
                        return i == null ? null : (short) i.intValue();
                    }
                };
            }
        } else if (clazz == int.class) {
            return compiler.compileInteger(exp);
        } else if (clazz == long.class) {
            final IntegerCalc integerCalc = compiler.compileInteger(exp);
            if (integerCalc.getResultStyle() == ResultStyle.VALUE_NOT_NULL) {
                return new AbstractCalc2(exp, integerCalc) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
						Integer i = integerCalc.evaluate(evaluator);
						if (i == null) {
							return null;
						}
						return i.longValue();
					}
                };
            } else {
                return new AbstractCalc2(exp, integerCalc) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
                        Integer i = (Integer) integerCalc.evaluate(evaluator);
                        return i == null ? null : (long) i.intValue();
                    }
                };
            }
        } else if (clazz == float.class) {
            final DoubleCalc doubleCalc = compiler.compileDouble(exp);
            if (doubleCalc.getResultStyle() == ResultStyle.VALUE_NOT_NULL) {
                return new AbstractCalc2(exp, doubleCalc) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
                        Double v = (Double) doubleCalc.evaluate(evaluator);
                        return v == null ? null : v.floatValue();
                    }
                };
            } else {
                return new AbstractCalc2(exp, doubleCalc) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
                        return  doubleCalc.evaluate(evaluator).floatValue();
                    }
                };
            }
        } else if (clazz == double.class) {
            return compiler.compileDouble(exp);
        } else if (clazz == Object.class) {
            return compiler.compileScalar(exp, false);
        } else {
            throw Util.newInternal("expected primitive type, got " + clazz);
        }
    }

    /**
     * Annotation which allows you to tag a Java method with the name of the
     * MDX function it implements.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface FunctionName
    {
        public abstract String value();
    }

    /**
     * Annotation which allows you to tag a Java method with the description
     * of the MDX function it implements.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Description
    {
        public abstract String value();
    }

    /**
     * Annotation which allows you to tag a Java method with the signature of
     * the MDX function it implements.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Signature
    {
        public abstract String value();
    }

    /**
     * Annotation which allows you to tag a Java method with the syntax of the
     * MDX function it implements.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface SyntaxDef
    {
        public abstract Syntax value();
    }

    /**
     * Base class for adapter calcs that convert arguments into the precise
     * type needed.
     */
    private abstract static class AbstractCalc2 extends AbstractProfilingNestedCalc {
        /**
         * Creates an AbstractCalc2.
         *
         * @param exp Source expression
         * @param calc Child compiled expression
         */
        protected AbstractCalc2(Expression exp, Calc calc) {
            super(exp.getType(), new Calc[] {calc});
        }
    }

    /**
     * Calc which calls a Java method.
     */
    private static class JavaMethodCalc extends GenericCalc {
        private final Method method;
        private final Object[] args;

        /**
         * Creates a JavaMethodCalc.
         *
         * @param call Function call being implemented
         * @param calcs Calcs for arguments of function call
         * @param method Method to call
         */
        public JavaMethodCalc(
            ResolvedFunCall call, Calc[] calcs, Method method)
        {
            super(call.getType(), calcs);
            this.method = method;
            this.args = new Object[calcs.length];
        }

        @Override
		public Object evaluate(Evaluator evaluator) {
            final Calc[] calcs = getChildCalcs();
            for (int i = 0; i < args.length; i++) {
                args[i] = calcs[i].evaluate(evaluator);
                if (args[i] == null || Objects.equals(args[i], FunUtil.DOUBLE_NULL)) {
                    return Util.nullValue;
                }
            }
            try {
                return method.invoke(null, args);
            } catch (IllegalAccessException e) {
                throw FunUtil.newEvalException(e);
            } catch (InvocationTargetException e) {
                throw FunUtil.newEvalException(e.getCause());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().equals("argument type mismatch")) {
                    StringBuilder buf =
                        new StringBuilder(
                            "argument type mismatch: parameters (");
                    int k = 0;
                    for (Class<?> parameterType : method.getParameterTypes()) {
                        if (k++ > 0) {
                            buf.append(", ");
                        }
                        buf.append(parameterType.getName());
                    }
                    buf.append("), actual (");
                    k = 0;
                    for (Object arg : args) {
                        if (k++ > 0) {
                            buf.append(", ");
                        }
                        buf.append(
                            arg == null
                                ? "null"
                                : arg.getClass().getName());
                    }
                    buf.append(")");
                    throw Util.newInternal(buf.toString());
                } else {
                    throw e;
                }
            }
        }
    }
    
    public static <T> T getAnnotation(
            Method method, Class<? extends Annotation> annotationClass, T defaultValue)
        {
    	
            try {
                if (method.isAnnotationPresent(annotationClass)) {
                    final Annotation annotation =
                        method.getAnnotation(annotationClass);
                    final Method method1 =
                        annotation.getClass().getMethod("value");
                    return (T) method1.invoke(annotation);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException  e) {
                return defaultValue;
            }
            return defaultValue;
        }
}
