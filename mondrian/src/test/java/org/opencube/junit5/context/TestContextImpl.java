package org.opencube.junit5.context;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
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
import org.eclipse.daanse.olap.function.def.aggregate.children.AggregateChildrenResolver;
import org.eclipse.daanse.olap.function.def.aggregate.count.CountResolver;
import org.eclipse.daanse.olap.function.def.ancestor.AncestorResolver;
import org.eclipse.daanse.olap.function.def.as.AsAliasResolver;
import org.eclipse.daanse.olap.function.def.cache.CacheFunResolver;
import org.eclipse.daanse.olap.function.def.cast.CaseMatchResolver;
import org.eclipse.daanse.olap.function.def.cast.CaseTestResolver;
import org.eclipse.daanse.olap.function.def.dimension.dimension.DimensionOfDimensionResolver;
import org.eclipse.daanse.olap.function.def.dimension.hierarchy.DimensionOfHierarchyResolver;
import org.eclipse.daanse.olap.function.def.dimension.level.DimensionOfLevelResolver;
import org.eclipse.daanse.olap.function.def.dimension.member.DimensionOfMemberResolver;
import org.eclipse.daanse.olap.function.def.dimensions.numeric.DimensionNumericResolver;
import org.eclipse.daanse.olap.function.def.dimensions.string.DimensionsStringResolver;
import org.eclipse.daanse.olap.function.def.member.DataMemberResolver;
import org.eclipse.daanse.olap.function.def.member.DefaultMemberResolver;
import org.eclipse.daanse.olap.function.def.member.FirstChildResolver;
import org.eclipse.daanse.olap.function.def.member.FirstSiblingResolver;
import org.eclipse.daanse.olap.function.def.member.LastChildResolver;
import org.eclipse.daanse.olap.function.def.member.LastSiblingResolver;
import org.eclipse.daanse.olap.function.def.member.MemberOrderKeyResolver;
import org.eclipse.daanse.olap.function.def.member.MembersResolver;
import org.eclipse.daanse.olap.function.def.member.NextMemberResolver;
import org.eclipse.daanse.olap.function.def.member.ParentResolver;
import org.eclipse.daanse.olap.function.def.member.PrevMemberResolver;
import org.eclipse.daanse.olap.function.def.numeric.OrdinalResolver;
import org.eclipse.daanse.olap.function.def.numeric.ValueResolver;
import org.eclipse.daanse.olap.function.def.hierarchy.member.HierarchyCurrentMemberResolver;
import org.eclipse.daanse.olap.function.def.hierarchy.member.MemberHierarchyResolver;
import org.eclipse.daanse.olap.function.def.hierarchy.member.NamedSetCurrentResolver;
import org.eclipse.daanse.olap.function.def.level.member.MemberLevelResolver;
import org.eclipse.daanse.olap.function.def.level.numeric.LevelsNumericPropertyResolver;
import org.eclipse.daanse.olap.function.def.level.string.LevelsStringPropertyResolver;
import org.eclipse.daanse.olap.function.def.level.string.LevelsStringResolver;
import org.eclipse.daanse.olap.function.def.member.CousinResolver;
import org.eclipse.daanse.olap.function.def.hierarchy.level.LevelHierarchyResolver;
import org.eclipse.daanse.olap.function.def.empty.EmptyExpressionResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.MtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.QtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.WtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.YtdMultiResolver;
import org.eclipse.daanse.olap.function.def.set.AddCalculatedMembersResolver;
import org.eclipse.daanse.olap.function.def.set.AscendantsResolver;
import org.eclipse.daanse.olap.function.def.set.ChildrenResolver;
import org.eclipse.daanse.olap.function.def.set.ExtractResolver;
import org.eclipse.daanse.olap.function.def.set.FilterResolver;
import org.eclipse.daanse.olap.function.def.set.SiblingsResolver;
import org.eclipse.daanse.olap.function.def.set.StripCalculatedMembersResolver;
import org.eclipse.daanse.olap.function.def.set.hierarchy.AllMembersResolver;
import org.eclipse.daanse.olap.function.def.set.level.LevelMembersResolver;
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

	    functionService.addResolver(new EmptyExpressionResolver());
	    functionService.addResolver(new DimensionOfHierarchyResolver());
	    functionService.addResolver(new DimensionOfDimensionResolver());
	    functionService.addResolver(new DimensionOfLevelResolver());
	    functionService.addResolver(new DimensionOfMemberResolver());
	    functionService.addResolver(new DimensionNumericResolver());
	    functionService.addResolver(new DimensionsStringResolver());
	    functionService.addResolver(new DimensionsStringResolver());
	    functionService.addResolver(new MemberHierarchyResolver());
	    functionService.addResolver(new LevelHierarchyResolver());

	    functionService.addResolver(new YtdMultiResolver());
	    functionService.addResolver(new QtdMultiResolver());
	    functionService.addResolver(new MtdMultiResolver());
	    functionService.addResolver(new WtdMultiResolver());
	    functionService.addResolver(new MemberLevelResolver());
	    functionService.addResolver(new AggregateChildrenResolver());
        functionService.addResolver(new CaseMatchResolver());
        functionService.addResolver(new CaseTestResolver());
        functionService.addResolver(new CacheFunResolver());
        functionService.addResolver(new LevelsNumericPropertyResolver());
        functionService.addResolver(new LevelsStringPropertyResolver());
        functionService.addResolver(new LevelsStringResolver());
        functionService.addResolver(new MemberLevelResolver());
        functionService.addResolver(new CousinResolver());
        functionService.addResolver(new HierarchyCurrentMemberResolver());
        functionService.addResolver(new NamedSetCurrentResolver());
        functionService.addResolver(new NamedSetCurrentResolver());
        functionService.addResolver(new DataMemberResolver());
        functionService.addResolver(new DefaultMemberResolver());
        functionService.addResolver(new FirstChildResolver());
        functionService.addResolver(new FirstSiblingResolver());
        functionService.addResolver(new LastChildResolver());
        functionService.addResolver(new LastSiblingResolver());
        functionService.addResolver(new MembersResolver());
        functionService.addResolver(new NextMemberResolver());
        functionService.addResolver(new MemberOrderKeyResolver());
        functionService.addResolver(new ParentResolver());
        functionService.addResolver(new PrevMemberResolver());
        functionService.addResolver(new CountResolver());
        functionService.addResolver(new OrdinalResolver());
        functionService.addResolver(new ValueResolver());
        functionService.addResolver(new AddCalculatedMembersResolver());
        functionService.addResolver(new AscendantsResolver());
        functionService.addResolver(new ChildrenResolver());
        functionService.addResolver(new ExtractResolver());
        functionService.addResolver(new FilterResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.set.MembersResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.set.hierarchy.AllMembersResolver());
        functionService.addResolver(new LevelMembersResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.set.level.AllMembersResolver());
        functionService.addResolver(new StripCalculatedMembersResolver());
        functionService.addResolver(new SiblingsResolver());
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
