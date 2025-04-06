/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.calc.base;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.profile.CalcEvaluationProfile;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;
import org.eclipse.daanse.olap.api.calc.profile.ProfilingCalc;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.base.profile.CalcEvaluationProfileR;
import org.eclipse.daanse.olap.calc.base.profile.CalcProfileR;

public abstract class AbstractProfilingCalc<T> implements ProfilingCalc<T> {

	private Type type;

	private Instant firstEvalStart = null;
	private Instant lastEvalEnd = null;

	private final List<CalcEvaluationProfile> evaluations = new ArrayList<CalcEvaluationProfile>();

	/**
	 * Abstract Implementation of {@link ProfilingCalc} that generated a
	 * {@link CalculationProfile} while calling
	 * {@link #evaluateWithProfile(Evaluator)))
	 *
	 * @param Type
	 * @param name
	 */
	public AbstractProfilingCalc(Type type) {
		this.type = type;
	}

	@Override
	public T evaluateWithProfile(Evaluator evaluator) {
		Instant startEval = Instant.now();
		if (firstEvalStart == null) {
			firstEvalStart = startEval;
		}
		final T evalResult = evaluate(evaluator);

		Instant endEval = Instant.now();
		lastEvalEnd = endEval;

		profileEvaluation(startEval, endEval, evalResult);
		return evalResult;
	}

	protected void profileEvaluation(Instant evaluationStart, Instant evaluationEnd, T evaluationResult) {

		CalcEvaluationProfile evaluationProfile = new CalcEvaluationProfileR(evaluationStart, evaluationEnd,
				evaluationResult, Map.of());
		evaluations.add(evaluationProfile);

	}

	protected Map<String, Object> profilingProperties(Map<String, Object> properties) {
		return properties;
	}

	public CalculationProfile getCalculationProfile() {
		final List<CalculationProfile> childProfiles = getChildProfiles();
		Map<String, Object> profilingProperties = profilingProperties(new HashMap<String, Object>());
		return new CalcProfileR(this.getClass(), getType(), getResultStyle(), Optional.ofNullable(firstEvalStart),
				Optional.ofNullable(lastEvalEnd), profilingProperties, evaluations, childProfiles);
	}

	List<CalculationProfile> getChildProfiles() {
		return List.of();
	}

	@Override
	public Type getType() {
		return type;
	}
	
	 protected void requiresType(Class<? extends Type> typeClass) {
		Type type = getType();
		if (!typeClass.isInstance(type)) {
			throw new RuntimeException("Expecting Type " + typeClass + " but was " + type);
		}

	}

}
