package org.opencube.junit5.context;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.db.statistics.api.StatisticsProvider;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.rolap.dbmapper.provider.api.DatabaseMappingSchemaProvider;

public interface TestContext extends Context{
	
	
	void setDialect(Dialect dialect);
	void setDataSource(DataSource dialect);
	void setDatabaseMappingSchemaProviders(List<DatabaseMappingSchemaProvider> databaseMappingSchemaProviders);
	void setName(String name);
	void setDescription(Optional<String> description);
	void setExpressionCompilerFactory(ExpressionCompilerFactory expressionCompilerFactory);
	void setStatisticsProvider(StatisticsProvider statisticsProvider);


}