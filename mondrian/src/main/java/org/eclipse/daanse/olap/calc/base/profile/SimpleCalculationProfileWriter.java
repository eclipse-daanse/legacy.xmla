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

package org.eclipse.daanse.olap.calc.base.profile;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.calc.profile.CalcEvaluationProfile;
import org.eclipse.daanse.olap.api.calc.profile.CalculationProfile;

public class SimpleCalculationProfileWriter {
	private static final String SPACE = " ";
	private static final int DEFAULT_INDENT = 4;
	private final PrintWriter printWriter;
	private final int indent;
	private int currentIdentPrefixLength;

	private String currentIndentPrefix = "";

	public SimpleCalculationProfileWriter(PrintWriter printWriter) {
		this(printWriter, DEFAULT_INDENT);
	}

	public SimpleCalculationProfileWriter(PrintWriter printWriter, int indent) {
		this.printWriter = printWriter;
		this.indent = indent;
	}

	public void write(CalculationProfile profile) {
		write(profile, false);
	}

	private void write(CalculationProfile profile, boolean indentRequired) {

		indentIf(indentRequired);
		printCalculationProfile(profile);
		outdentIf(indentRequired);
	}

	private void printCalculationProfile(CalculationProfile profile) {
		printName(profile);
		printOpen();
		printType(profile);
		printResultStyle(profile);
		printSubProfiles(profile);
		printCallTime(profile);
		printAdditionalValues(profile);
		printClose();
		printChilds(profile);
	}

	private void outdentIf(boolean doIndent) {
		if (doIndent) {
			outdent();
		}
	}

	private void indentIf(boolean doIndent) {
		if (doIndent) {
			indent();
		}
	}

	private void printChilds(CalculationProfile profile) {
		profile.childProfiles().forEach((CalculationProfile childCalcProfile) -> {
			write(childCalcProfile, true);
		});

	}

	private void printClose() {
		printWriter.print(")");
		printWriter.println();
	}

	private void printOpen() {
		printWriter.print("(");
	}

	private void printAdditionalValues(CalculationProfile profile) {
		profile.additionalValues().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			printWriter.print(", ");
			printWriter.print(entry.getKey());
			printWriter.print("=");
			printWriter.print(entry.getValue());
		});
	}

	private void printCallTime(CalculationProfile profile) {
		printWriter.print("callMillis");
		printWriter.print("=");
		printWriter.print(profile.duration().toMillis());
	}

	private void printSubProfiles(CalculationProfile profile) {
		List<CalcEvaluationProfile> evaluationProfiles = profile.evaluationProfiles();
		printWriter.print("callCount");
		printWriter.print("=");
		printWriter.print(evaluationProfiles.size());
		printWriter.print(", ");
	}

	private void printResultStyle(CalculationProfile profile) {
		printWriter.print("resultStyle");
		printWriter.print("=");
		printWriter.print(profile.resultStyle());
		printWriter.print(", ");
	}

	private void printType(CalculationProfile profile) {
		printWriter.print("type");
		printWriter.print("=");
		printWriter.print(profile.type());
		printWriter.print(", ");
	}

	private void printName(CalculationProfile profile) {
		printWriter.print(currentIndentPrefix);
		printWriter.print(profile.clazz().getName());

	}

	private void indent() {
		currentIdentPrefixLength += indent;
		calculateCurrentIndentPrefix();
	}

	private void outdent() {
		currentIdentPrefixLength -= indent;
		calculateCurrentIndentPrefix();
	}

	private void calculateCurrentIndentPrefix() {
		currentIndentPrefix = SPACE.repeat(currentIdentPrefixLength);
	}

}
