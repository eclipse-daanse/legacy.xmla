package org.opencube.junit5.context;

import java.util.Optional;
import java.util.concurrent.Semaphore;

import org.eclipse.daanse.jdbc.datasource.pools.api.ConnectionPool;

import org.eclipse.daanse.sql.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;

public interface TestContext extends RolapContext{


	void setDialect(Dialect dialect);
	void setConnectionPool(ConnectionPool connectionPool);
	void setName(String name);
	void setDescription(Optional<String> description);
	void setExpressionCompilerFactory(ExpressionCompilerFactory expressionCompilerFactory);
	void setQueryLimitSemaphore(Semaphore semaphore);
	void setCatalogMappingSupplier(CatalogMappingSupplier catalogMappingSupplier);
}
