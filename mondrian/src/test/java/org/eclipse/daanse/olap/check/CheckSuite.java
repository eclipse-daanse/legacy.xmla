package org.eclipse.daanse.olap.check;

public class CheckSuite {
	
	static CheckSuite start(String name) { return new CheckSuite(); }

	public CheckExecution startExecution(String uniqueId, String displayName) {
		
		return new CheckExecution();
	}

	public void printResult() {

		
	}
}
