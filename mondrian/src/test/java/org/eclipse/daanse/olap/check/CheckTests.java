package org.eclipse.daanse.olap.check;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(CheckExtension.class)
public class CheckTests {
	
	@Test
	void test1(CheckExecution checkExecution, Context<?> context, org.eclipse.daanse.rolap.mapping.instance.emf.tutorial.cube.minimal.CheckSuiteSupplier checkSuiteSupplier, org.eclipse.daanse.rolap.mapping.instance.emf.tutorial.cube.minimal.CatalogSupplier catalogSupplier) {
		checkExecution.execute(context, checkSuiteSupplier, catalogSupplier);
	}

	@Test
	void test2(CheckExecution checkExecution, Context<?> context, org.eclipse.daanse.rolap.mapping.instance.emf.tutorial.cube.minimal.CheckSuiteSupplier checkSuiteSupplier, org.eclipse.daanse.rolap.mapping.instance.emf.tutorial.cube.minimal.CatalogSupplier catalogSupplier) {
		checkExecution.execute(context, checkSuiteSupplier, catalogSupplier);
	}

}
