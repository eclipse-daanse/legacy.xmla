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
package org.eclipse.daanse.olap.function.def.linreg;

import java.util.List;

public class Value {
    private List<?> xs;
    private List<?> ys;
    /**
     * The intercept for the linear regression model. Initialized following a call
     * to accuracy.
     */
    double intercept;

    /**
     * The slope for the linear regression model. Initialized following a call to
     * accuracy.
     */
    double slope;

    /** the coefficient of determination */
    double rSquared = Double.MAX_VALUE;

    /** variance = sum square diff mean / n - 1 */
    double variance = Double.MAX_VALUE;

    Value(double intercept, double slope, List<?> xs, List<?> ys) {
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
        return new StringBuilder("LinReg.Value: slope of ").append(slope).append(" and an intercept of ")
                .append(intercept).append(". That is, y=").append(intercept).append((slope > 0.0 ? " +" : " "))
                .append(slope).append(" * x.").toString();
    }

}
