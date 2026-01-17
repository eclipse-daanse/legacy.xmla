package org.eclipse.daanse.olap.check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.check.model.check.CheckExecutionResult;
import org.eclipse.daanse.olap.check.model.check.CheckResult;
import org.eclipse.daanse.olap.check.runtime.api.CheckExecutor;
import org.eclipse.daanse.olap.check.runtime.impl.CheckExecutorImpl;
import org.eclipse.daanse.rolap.mapping.instance.emf.tutorial.cube.minimal.CheckSuiteSupplier;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CheckExecution {
	private CheckExecutor executor = new CheckExecutorImpl();
	public final List<CheckExecutionResult> RESULTS = Collections.synchronizedList(new ArrayList<>());
	
	public void printResult(ExtensionContext context) {
		System.out.println("Test result for " + context.getDisplayName());
		System.out.println("_____________________________________________");
		for (CheckExecutionResult result : RESULTS) {
			System.out.println(result.getName() + " SuccessCount " + result.getSuccessCount() + " FailureCount " + + result.getFailureCount());
			for (CheckResult chr : result.getCheckResults()) {
				System.out.println("     " + chr.getCheckName() + " Status " + chr.getStatus().getName() + " ExecutionTimeMs " + chr.getExecutionTimeMs());
			}			
		}
		System.out.println("_____________________________________________");
	}

	public void execute(Context<?> context,
			CheckSuiteSupplier checkSuiteSupplier,
			CatalogMappingSupplier catalogSupplier) {
		List<CheckExecutionResult> result  = executor.execute(checkSuiteSupplier.get(), context);
		RESULTS.addAll(result);
	}
}
