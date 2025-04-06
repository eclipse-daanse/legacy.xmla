package org.opencube.junit5.context;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.rolap.mapping.api.CatalogMappingSupplier;

public interface TestContext extends Context{


	void setDialect(Dialect dialect);
	void setDataSource(DataSource dialect);
	void setName(String name);
	void setDescription(Optional<String> description);
	void setExpressionCompilerFactory(ExpressionCompilerFactory expressionCompilerFactory);
    Connection getConnection(List<String> roles);
	void setQueryLimitSemaphore(Semaphore semaphore);
	void setCatalogMappingSupplier(CatalogMappingSupplier catalogMappingSupplier);
}
