/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueDoubleCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.olap.Util;
import mondrian.olap.fun.FunUtil.SetWrapper;

/**
 * Abstract base class for definitions of linear regression functions.
 *
 * @see InterceptFunDef
 * @see PointFunDef
 * @see R2FunDef
 * @see SlopeFunDef
 * @see VarianceFunDef
 *
 * <h2>Correlation coefficient</h2>
 * <p><i>Correlation coefficient</i></p>
 *
 * <p>The correlation coefficient, r, ranges from -1 to  + 1. The
 * nonparametric Spearman correlation coefficient, abbreviated rs, has
 * the same range.</p>
 *
 * <table border="1" cellpadding="6" cellspacing="0">
 *   <tr>
 *     <td>Value of r (or rs)</td>
 *     <td>Interpretation</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">r= 0</td>
 *
 *     <td>The two variables do not vary together at all.</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">0 &gt; r &gt; 1</td>
 *     <td>
 *       <p>The two variables tend to increase or decrease together.</p>
 *     </td>
 *   </tr>
 *   <tr>
 *     <td valign="top">r = 1.0</td>
 *     <td>
 *       <p>Perfect correlation.</p>
 *     </td>
 *   </tr>
 *
 *   <tr>
 *     <td valign="top">-1 &gt; r &gt; 0</td>
 *     <td>
 *       <p>One variable increases as the other decreases.</p>
 *     </td>
 *   </tr>
 *
 *   <tr>
 *     <td valign="top">r = -1.0</td>
 *     <td>
 *       <p></p>
 *       <p>Perfect negative or inverse correlation.</p>
 *     </td>
 *   </tr>
 * </table>
 *
 * <p>If r or rs is far from zero, there are four possible explanations:</p>
 * <p>The X variable helps determine the value of the Y variable.</p>
 * <ul>
 *   <li>The Y variable helps determine the value of the X variable.
 *   <li>Another variable influences both X and Y.
 *   <li>X and Y don't really correlate at all, and you just
 *       happened to observe such a strong correlation by chance. The P value
 *       determines how often this could occur.
 * </ul>
 * <p><i>r2 </i></p>
 *
 * <p>Perhaps the best way to interpret the value of r is to square it to
 * calculate r2. Statisticians call this quantity the coefficient of
 * determination, but scientists call it r squared. It is has a value
 * that ranges from zero to one, and is the fraction of the variance in
 * the two variables that is shared. For example, if r2=0.59, then 59% of
 * the variance in X can be explained by variation in Y. &nbsp;Likewise,
 * 59% of the variance in Y can be explained by (or goes along with)
 * variation in X. More simply, 59% of the variance is shared between X
 * and Y.</p>
 *
 * <p>(<a href="http://www.graphpad.com/articles/interpret/corl_n_linear_reg/correlation.htm">Source</a>).
 *
 * <p>Also see: <a href="http://mathworld.wolfram.com/LeastSquaresFitting.html">least squares fitting</a>.
 */


public abstract class LinReg extends AbstractFunctionDefinition {
    /** Code for the specific function. */
    final int regType;

    public static final int POINT = 0;
    public static final int R2 = 1;
    public static final int INTERCEPT = 2;
    public static final int SLOPE = 3;
    public static final int VARIANCE = 4;

    static final FunctionResolver InterceptResolver =
        new ReflectiveMultiResolver(
            "LinRegIntercept",
            "LinRegIntercept(<Set>, <Numeric Expression>[, <Numeric Expression>])",
            "Calculates the linear regression of a set and returns the value of b in the regression line y = ax + b.",
            new String[]{"fnxn", "fnxnn"},
            InterceptFunDef.class);

    static final FunctionResolver PointResolver =
        new ReflectiveMultiResolver(
            "LinRegPoint",
            "LinRegPoint(<Numeric Expression>, <Set>, <Numeric Expression>[, <Numeric Expression>])",
            "Calculates the linear regression of a set and returns the value of y in the regression line y = ax + b.",
            new String[]{"fnnxn", "fnnxnn"},
            PointFunDef.class);

    static final FunctionResolver SlopeResolver =
        new ReflectiveMultiResolver(
            "LinRegSlope",
            "LinRegSlope(<Set>, <Numeric Expression>[, <Numeric Expression>])",
            "Calculates the linear regression of a set and returns the value of a in the regression line y = ax + b.",
            new String[]{"fnxn", "fnxnn"},
            SlopeFunDef.class);

    static final FunctionResolver R2Resolver =
        new ReflectiveMultiResolver(
            "LinRegR2",
            "LinRegR2(<Set>, <Numeric Expression>[, <Numeric Expression>])",
            "Calculates the linear regression of a set and returns R2 (the coefficient of determination).",
            new String[]{"fnxn", "fnxnn"},
            R2FunDef.class);

    static final FunctionResolver VarianceResolver =
        new ReflectiveMultiResolver(
            "LinRegVariance",
            "LinRegVariance(<Set>, <Numeric Expression>[, <Numeric Expression>])",
            "Calculates the linear regression of a set and returns the variance associated with the regression line y = ax + b.",
            new String[]{"fnxn", "fnxnn"},
            VarianceFunDef.class);


    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        final DoubleCalc yCalc = compiler.compileDouble(call.getArg(1));
        final DoubleCalc xCalc =
            call.getArgCount() > 2
            ? compiler.compileDouble(call.getArg(2))
            : new CurrentValueDoubleCalc(call.getType());
        return new LinRegCalc(call, tupleListCalc, yCalc, xCalc, regType);
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Helper
    //
    /////////////////////////////////////////////////////////////////////////
    static class Value {
        private List xs;
        private List ys;
        /**
         * The intercept for the linear regression model. Initialized
         * following a call to accuracy.
         */
        double intercept;

        /**
         * The slope for the linear regression model. Initialized following a
         * call to accuracy.
         */
        double slope;

         /** the coefficient of determination */
        double rSquared = Double.MAX_VALUE;

        /** variance = sum square diff mean / n - 1 */
        double variance = Double.MAX_VALUE;

        Value(double intercept, double slope, List xs, List ys) {
            this.intercept = intercept;
            this.slope = slope;
            this.xs = xs;
            this.ys = ys;
        }

        public double getIntercept() {
            return this.intercept;
        }

        public double getSlope() {
            return this.slope;
        }

        public double getRSquared() {
            return this.rSquared;
        }

        /**
         * strength of the correlation
         *
         * @param rSquared Strength of the correlation
         */
        public void setRSquared(double rSquared) {
            this.rSquared = rSquared;
        }

        public double getVariance() {
            return this.variance;
        }

        public void setVariance(double variance) {
            this.variance = variance;
        }

        @Override
		public String toString() {
            return new StringBuilder("LinReg.Value: slope of ")
                .append(slope)
                .append(" and an intercept of ").append(intercept)
                .append(". That is, y=")
                .append(intercept)
                .append((slope > 0.0 ? " +" : " "))
                .append(slope)
                .append(" * x.").toString();
        }
    }

    /**
     * Definition of the <code>LinRegIntercept</code> MDX function.
     *
     * <p>Synopsis:
     *
     * <blockquote><code>LinRegIntercept(&lt;Numeric Expression&gt;,
     * &lt;Set&gt;, &lt;Numeric Expression&gt;[, &lt;Numeric
     * Expression&gt;])</code></blockquote>
     */
    public static class InterceptFunDef extends LinReg {
        public InterceptFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData, LinReg.INTERCEPT);
        }
    }

    /**
     * Definition of the <code>LinRegPoint</code> MDX function.
     *
     * <p>Synopsis:
     *
     * <blockquote><code>LinRegPoint(&lt;Numeric Expression&gt;,
     * &lt;Set&gt;, &lt;Numeric Expression&gt;[, &lt;Numeric
     * Expression&gt;])</code></blockquote>
     */
    public static class PointFunDef extends LinReg {
        public PointFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData, LinReg.POINT);
        }

        @Override
		public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            final DoubleCalc xPointCalc =
                compiler.compileDouble(call.getArg(0));
            final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(1));
            final DoubleCalc yCalc = compiler.compileDouble(call.getArg(2));
            final DoubleCalc xCalc =
                call.getArgCount() > 3
                ? compiler.compileDouble(call.getArg(3))
                : new CurrentValueDoubleCalc(call.getType());
            return new PointCalc(
                call, xPointCalc, tupleListCalc, yCalc, xCalc);
        }
    }

    private static class PointCalc extends AbstractProfilingNestedDoubleCalc {
        private final DoubleCalc xPointCalc;
        private final TupleListCalc tupleListCalc;
        private final DoubleCalc yCalc;
        private final DoubleCalc xCalc;

        public PointCalc(
            ResolvedFunCall call,
            DoubleCalc xPointCalc,
            TupleListCalc tupleListCalc,
            DoubleCalc yCalc,
            DoubleCalc xCalc)
        {
            super(call.getType(), new Calc[]{xPointCalc, tupleListCalc, yCalc, xCalc});
            this.xPointCalc = xPointCalc;
            this.tupleListCalc = tupleListCalc;
            this.yCalc = yCalc;
            this.xCalc = xCalc;
        }

        @Override
		public Double evaluate(Evaluator evaluator) {
            Double xPoint = xPointCalc.evaluate(evaluator);
            Value value = LinReg.process(evaluator, tupleListCalc, yCalc, xCalc);
            if (value == null) {
                return FunUtil.DOUBLE_NULL;
            }
            // use first arg to generate y position
            return xPoint * value.getSlope() + value.getIntercept();
        }
    }

    /**
     * Definition of the <code>LinRegSlope</code> MDX function.
     *
     * <p>Synopsis:
     *
     * <blockquote><code>LinRegSlope(&lt;Numeric Expression&gt;,
     * &lt;Set&gt;, &lt;Numeric Expression&gt;[, &lt;Numeric
     * Expression&gt;])</code></blockquote>
     */
    public static class SlopeFunDef extends LinReg {
        public SlopeFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData, LinReg.SLOPE);
        }
    }

    /**
     * Definition of the <code>LinRegR2</code> MDX function.
     *
     * <p>Synopsis:
     *
     * <blockquote><code>LinRegR2(&lt;Numeric Expression&gt;,
     * &lt;Set&gt;, &lt;Numeric Expression&gt;[, &lt;Numeric
     * Expression&gt;])</code></blockquote>
     */
    public static class R2FunDef extends LinReg {
        public R2FunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData, LinReg.R2);
        }
    }

    /**
     * Definition of the <code>LinRegVariance</code> MDX function.
     *
     * <p>Synopsis:
     *
     * <blockquote><code>LinRegVariance(&lt;Numeric Expression&gt;,
     * &lt;Set&gt;, &lt;Numeric Expression&gt;[, &lt;Numeric
     * Expression&gt;])</code></blockquote>
     */
    public static class VarianceFunDef extends LinReg {
        public VarianceFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData, LinReg.VARIANCE);
        }
    }

    protected static void debug(String type, String msg) {
        // comment out for no output
        // RME
    }


    protected LinReg(FunctionMetaData functionMetaData, int regType) {
        super(functionMetaData);
        this.regType = regType;
    }

    protected static LinReg.Value process(
        Evaluator evaluator,
        TupleListCalc tupleListCalc,
        DoubleCalc yCalc,
        DoubleCalc xCalc)
    {
        final int savepoint = evaluator.savepoint();
        TupleList members;
        try {
            evaluator.setNonEmpty(false);
            members = tupleListCalc.evaluateList(evaluator);
        } finally {
            evaluator.restore(savepoint);
        }
        SetWrapper[] sws;
        try {
            sws =
                FunUtil.evaluateSet(
                    evaluator, members, new DoubleCalc[] {yCalc, xCalc});
        } finally {
            evaluator.restore(savepoint);
        }
        SetWrapper swY = sws[0];
        SetWrapper swX = sws[1];

        if (swY.errorCount > 0) {
            LinReg.debug("LinReg.process", "ERROR error(s) count ="  + swY.errorCount);
            // TODO: throw exception
            return null;
        } else if (swY.v.isEmpty()) {
            return null;
        }

        return LinReg.linearReg(swX.v, swY.v);
    }

    public static LinReg.Value accuracy(LinReg.Value value) {
        // for variance
        double sumErrSquared = 0.0;

        double sumErr = 0.0;

        // for r2
        // data
        double sumY = 0.0;
        // predicted

        // Obtain the forecast values for this model
        List yfs = LinReg.forecast(value);

        // Calculate the Sum of the Absolute Errors
        Iterator ity = value.ys.iterator();
        Iterator ityf = yfs.iterator();
        while (ity.hasNext()) {
            // Get next data point
            Double dy = (Double) ity.next();
            if (dy == null) {
                continue;
            }
            Double dyf = (Double) ityf.next();
            if (dyf == null) {
                continue;
            }

            double y = dy.doubleValue();
            double yf = dyf.doubleValue();

            // Calculate error in forecast, and update sums appropriately

            // the y residual or error
            double error = yf - y;

            sumErr += error;
            sumErrSquared += error * error;

            sumY += y;
        }


        // Initialize the accuracy indicators
        int n = value.ys.size();

        // Variance
        // The estimate the value of the error variance is a measure of
        // variability of the y values about the estimated line.
        // http://home.ubalt.edu/ntsbarsh/Business-stat/opre504.htm
        // s2 = SSE/(n-2) = sum (y - yf)2 /(n-2)
        if (n > 2) {
            double variance = sumErrSquared / (n - 2);

            value.setVariance(variance);
        }

        // R2
        // R2 = 1 - (SSE/SST)
        // SSE = sum square error = Sum((error-MSE)*(error-MSE))
        // MSE = mean error = Sum(error)/n
        // SST = sum square y diff = Sum((y-MST)*(y-MST))
        // MST = mean y = Sum(y)/n
        double mse = sumErr / n;
        double mst = sumY / n;
        double sse = 0.0;
        double sst = 0.0;
        ity = value.ys.iterator();
        ityf = yfs.iterator();
        while (ity.hasNext()) {
            // Get next data point
            Double dy = (Double) ity.next();
            if (dy == null) {
                continue;
            }
            Double dyf = (Double) ityf.next();
            if (dyf == null) {
                continue;
            }

            double y = dy.doubleValue();
            double yf = dyf.doubleValue();

            double error = yf - y;
            sse += (error - mse) * (error - mse);
            sst += (y - mst) * (y - mst);
        }
        if (sst != 0.0) {
            double rSquared =  1 - (sse / sst);

            value.setRSquared(rSquared);
        }


        return value;
    }

    public static LinReg.Value linearReg(List xlist, List ylist) {
        // y and x have same number of points
        int size = ylist.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXX = 0.0;
        double sumXY = 0.0;

        LinReg.debug("LinReg.linearReg", "ylist.size()=" + ylist.size());
        LinReg.debug("LinReg.linearReg", "xlist.size()=" + xlist.size());
        int n = 0;
        for (int i = 0; i < size; i++) {
            Object yo = ylist.get(i);
            Object xo = xlist.get(i);
            if ((yo == null) || (xo == null)) {
                continue;
            }
            n++;
            double y = ((Double) yo).doubleValue();
            double x = ((Double) xo).doubleValue();

            LinReg.debug("LinReg.linearReg", new StringBuilder(" ").append(i).append(" (")
                .append(x).append(",").append(y).append(")").toString());
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumXY += x * y;
        }

        double xMean = n == 0 ? 0 : sumX / n;
        double yMean = n == 0 ? 0 : sumY / n;

        LinReg.debug("LinReg.linearReg", "yMean=" + yMean);
        LinReg.debug(
            "LinReg.linearReg",
            "(n*sumXX - sumX*sumX)=" + (n * sumXX - sumX * sumX));
        // The regression line is the line that minimizes the variance of the
        // errors. The mean error is zero; so, this means that it minimizes the
        // sum of the squares errors.
        double slope = (n * sumXX - sumX * sumX) == 0 ? 0 : (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = yMean - slope * xMean;

        LinReg.Value value = new LinReg.Value(intercept, slope, xlist, ylist);
        LinReg.debug("LinReg.linearReg", "value=" + value);

        return value;
    }


    public static List forecast(LinReg.Value value) {
        List yfs = new ArrayList(value.xs.size());

        Iterator it = value.xs.iterator();
        while (it.hasNext()) {
            Double d = (Double) it.next();
            // If the value is missing we still must put a place
            // holder in the y axis, otherwise there is a discontinuity
            // between the data and the fit.
            if (d == null) {
                yfs.add(null);
            } else {
                double x = d.doubleValue();
                double yf = value.intercept + value.slope * x;
                yfs.add(Double.valueOf(yf));
            }
        }

        return yfs;
    }

    private static class LinRegCalc extends AbstractProfilingNestedDoubleCalc {
        private final TupleListCalc tupleListCalc;
        private final DoubleCalc yCalc;
        private final DoubleCalc xCalc;
        private final int regType;

        public LinRegCalc(
            ResolvedFunCall call,
            TupleListCalc tupleListCalc,
            DoubleCalc yCalc,
            DoubleCalc xCalc,
            int regType)
        {
            super(call.getType(), new Calc[]{tupleListCalc, yCalc, xCalc});
            this.tupleListCalc = tupleListCalc;
            this.yCalc = yCalc;
            this.xCalc = xCalc;
            this.regType = regType;
        }

        @Override
		public Double evaluate(Evaluator evaluator) {
            Value value = LinReg.process(evaluator, tupleListCalc, yCalc, xCalc);
            if (value == null) {
                return FunUtil.DOUBLE_NULL;
            }
            switch (regType) {
            case INTERCEPT:
                return value.getIntercept();
            case SLOPE:
                return value.getSlope();
            case VARIANCE:
                return value.getVariance();
            case R2:
                return value.getRSquared();
            default:
            case POINT:
                throw Util.newInternal("unexpected value " + regType);
            }
        }
    }
}
