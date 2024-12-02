package org.opencube.junit5.context;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.eclipse.daanse.mdx.parser.ccc.MdxParserProviderImpl;
import org.eclipse.daanse.olap.api.ConnectionProps;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.olap.core.BasicContextConfig;
import org.eclipse.daanse.olap.function.core.FunctionServiceImpl;
import org.eclipse.daanse.olap.function.def.aggregate.avg.AvgResolver;
import org.eclipse.daanse.olap.function.def.ancestor.AncestorResolver;
import org.eclipse.daanse.olap.function.def.as.AsAliasResolver;
import org.eclipse.daanse.rolap.mapping.api.CatalogMappingSupplier;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;

import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapConnectionPropsR;
import mondrian.rolap.RolapResultShepherd;
import mondrian.rolap.agg.AggregationManager;
import mondrian.server.NopEventBus;

public class TestContextImpl extends AbstractBasicContext implements TestContext {

	private Dialect dialect;
	private DataSource dataSource;

	private ExpressionCompilerFactory expressionCompilerFactory = new BaseExpressionCompilerFactory();
	private CatalogMappingSupplier catalogMappingSupplier;
	private String name;
	private Optional<String> description = Optional.empty();
    private TestConfig testConfig;
    private Semaphore queryLimimitSemaphore;
    private FunctionService functionService =new FunctionServiceImpl();



	public TestContextImpl() {
        testConfig = new TestConfig();
        this.monitor = new NopEventBus();
	    shepherd = new RolapResultShepherd(testConfig.rolapConnectionShepherdThreadPollingInterval(),testConfig.rolapConnectionShepherdThreadPollingIntervalUnit(),
            testConfig.rolapConnectionShepherdNbThreads());
	    aggMgr = new AggregationManager(this);
	    queryLimimitSemaphore=new Semaphore(testConfig.queryLimit());
	    
	    functionService.addResolver(new AsAliasResolver());
	    functionService.addResolver(new AncestorResolver());
	    functionService.addResolver(new AvgResolver());
	}

	@Override
	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;

	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}


	@Override
	public Optional<String> getDescription() {
		return description;
	}

	@Override
	public CatalogMapping getCatalogMapping() {
		return catalogMappingSupplier.get();
	}


	@Override
	public ExpressionCompilerFactory getExpressionCompilerFactory() {
		return expressionCompilerFactory;
	}

	@Override
	public org.eclipse.daanse.olap.api.Connection getConnection() {
		return getConnection(new RolapConnectionPropsR());
	}

    @Override
    public org.eclipse.daanse.olap.api.Connection getConnection(ConnectionProps props) {
        return new RolapConnection(this, props);
    }

    @Override
    public org.eclipse.daanse.olap.api.Connection getConnection(List<String> roles) {
        return getConnection(new RolapConnectionPropsR(roles,
                true, Locale.getDefault(),
                -1, TimeUnit.SECONDS, Optional.empty(), Optional.empty()));
    }

    @Override
    public Scenario createScenario() {
        return null;
    }

    @Override
    public BasicContextConfig getConfig() {
        return testConfig;
    }

    @Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setDescription(Optional<String> description) {
		this.description = description;
	}

	@Override
	public void setExpressionCompilerFactory(ExpressionCompilerFactory expressionCompilerFactory) {
		this.expressionCompilerFactory = expressionCompilerFactory;
	}


	@Override
	public Semaphore getQueryLimitSemaphore() {
		return queryLimimitSemaphore;
	}

	@Override
	public void setQueryLimitSemaphore(Semaphore queryLimimitSemaphore) {
		this.queryLimimitSemaphore = queryLimimitSemaphore;

	}

	@Override
	public Optional<Map<Object, Object>> getSqlMemberSourceValuePool() {
		return Optional.empty();
	}

    @Override
    public FunctionService getFunctionService() {
        return functionService;
    }

    @Override
    public MdxParserProvider getMdxParserProvider() {
        return new MdxParserProviderImpl();
    }

    public void setFunctionService(FunctionService functionService) {
        this.functionService = functionService;
    }

    @Override
    public String toString() {
    	try {
			return dataSource.getConnection().getMetaData().getURL();
		} catch (SQLException e) {
			e.printStackTrace();

			return dataSource.getClass().getPackageName();
		}
    }

	@Override
	public void setCatalogMappingSupplier(CatalogMappingSupplier catalogMappingSupplier) {
		this.catalogMappingSupplier = catalogMappingSupplier;		
	}

}
