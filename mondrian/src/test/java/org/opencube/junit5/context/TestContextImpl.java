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
import org.eclipse.daanse.olap.function.def.aggregate.AggregateResolver;
import org.eclipse.daanse.olap.function.def.aggregate.avg.AvgResolver;
import org.eclipse.daanse.olap.function.def.aggregate.children.AggregateChildrenResolver;
import org.eclipse.daanse.olap.function.def.aggregate.count.CountResolver;
import org.eclipse.daanse.olap.function.def.ancestor.AncestorResolver;
import org.eclipse.daanse.olap.function.def.as.AsAliasResolver;
import org.eclipse.daanse.olap.function.def.cache.CacheFunResolver;
import org.eclipse.daanse.olap.function.def.caption.member.MemberCaptionResolver;
import org.eclipse.daanse.olap.function.def.cast.CaseMatchResolver;
import org.eclipse.daanse.olap.function.def.cast.CaseTestResolver;
import org.eclipse.daanse.olap.function.def.correlation.CorrelationResolver;
import org.eclipse.daanse.olap.function.def.covariance.CovarianceNResolver;
import org.eclipse.daanse.olap.function.def.covariance.CovarianceResolver;
import org.eclipse.daanse.olap.function.def.crossjoin.CrossJoinResolver;
import org.eclipse.daanse.olap.function.def.crossjoin.StarCrossJoinResolver;
import org.eclipse.daanse.olap.function.def.descendants.DescendantsMemberResolver;
import org.eclipse.daanse.olap.function.def.descendants.DescendantsSetResolver;
import org.eclipse.daanse.olap.function.def.dimension.current.CurrentResolver;
import org.eclipse.daanse.olap.function.def.dimension.dimension.DimensionOfDimensionResolver;
import org.eclipse.daanse.olap.function.def.dimension.hierarchy.DimensionOfHierarchyResolver;
import org.eclipse.daanse.olap.function.def.dimension.level.DimensionOfLevelResolver;
import org.eclipse.daanse.olap.function.def.dimension.member.DimensionOfMemberResolver;
import org.eclipse.daanse.olap.function.def.dimensions.numeric.DimensionNumericResolver;
import org.eclipse.daanse.olap.function.def.dimensions.string.DimensionsStringResolver;
import org.eclipse.daanse.olap.function.def.drilldownlevel.DrilldownLevelResolver;
import org.eclipse.daanse.olap.function.def.drilldownleveltopbottom.DrilldownLevelBottomResolver;
import org.eclipse.daanse.olap.function.def.drilldownleveltopbottom.DrilldownLevelTopResolver;
import org.eclipse.daanse.olap.function.def.drilldownmember.DrilldownMemberResolver;
import org.eclipse.daanse.olap.function.def.empty.EmptyExpressionResolver;
import org.eclipse.daanse.olap.function.def.except.ExceptResolver;
import org.eclipse.daanse.olap.function.def.exists.ExistsResolver;
import org.eclipse.daanse.olap.function.def.format.FormatResolver;
import org.eclipse.daanse.olap.function.def.generate.GenerateListResolver;
import org.eclipse.daanse.olap.function.def.generate.GenerateStringResolver;
import org.eclipse.daanse.olap.function.def.headtail.HeadResolver;
import org.eclipse.daanse.olap.function.def.headtail.TailResolver;
import org.eclipse.daanse.olap.function.def.hierarchize.HierarchizeResolver;
import org.eclipse.daanse.olap.function.def.hierarchy.level.LevelHierarchyResolver;
import org.eclipse.daanse.olap.function.def.hierarchy.member.HierarchyCurrentMemberResolver;
import org.eclipse.daanse.olap.function.def.hierarchy.member.MemberHierarchyResolver;
import org.eclipse.daanse.olap.function.def.hierarchy.member.NamedSetCurrentResolver;
import org.eclipse.daanse.olap.function.def.intersect.IntersectResolver;
import org.eclipse.daanse.olap.function.def.lastperiods.LastPeriodsResolver;
import org.eclipse.daanse.olap.function.def.leadlag.LagResolver;
import org.eclipse.daanse.olap.function.def.leadlag.LeadResolver;
import org.eclipse.daanse.olap.function.def.level.member.MemberLevelResolver;
import org.eclipse.daanse.olap.function.def.level.numeric.LevelNumberResolver;
import org.eclipse.daanse.olap.function.def.level.numeric.LevelsNumericPropertyResolver;
import org.eclipse.daanse.olap.function.def.levels.string.LevelsStringPropertyResolver;
import org.eclipse.daanse.olap.function.def.levels.string.LevelsStringResolver;
import org.eclipse.daanse.olap.function.def.linreg.LinRegInterceptResolver;
import org.eclipse.daanse.olap.function.def.linreg.LinRegPointResolver;
import org.eclipse.daanse.olap.function.def.linreg.LinRegR2Resolver;
import org.eclipse.daanse.olap.function.def.linreg.LinRegSlopeResolver;
import org.eclipse.daanse.olap.function.def.linreg.LinRegVarianceResolver;
import org.eclipse.daanse.olap.function.def.logical.IsEmptyFunctionResolver;
import org.eclipse.daanse.olap.function.def.logical.IsEmptyPostfixResolver;
import org.eclipse.daanse.olap.function.def.logical.IsNullResolver;
import org.eclipse.daanse.olap.function.def.logical.IsResolver;
import org.eclipse.daanse.olap.function.def.median.MedianResolver;
import org.eclipse.daanse.olap.function.def.member.AncestorsResolver;
import org.eclipse.daanse.olap.function.def.member.cousin.CousinResolver;
import org.eclipse.daanse.olap.function.def.member.datamember.DataMemberResolver;
import org.eclipse.daanse.olap.function.def.member.defaultmember.DefaultMemberResolver;
import org.eclipse.daanse.olap.function.def.member.firstchild.FirstChildResolver;
import org.eclipse.daanse.olap.function.def.member.firstsibling.FirstSiblingResolver;
import org.eclipse.daanse.olap.function.def.member.lastchild.LastChildResolver;
import org.eclipse.daanse.olap.function.def.member.lastsibling.LastSiblingResolver;
import org.eclipse.daanse.olap.function.def.member.memberorderkey.MemberOrderKeyResolver;
import org.eclipse.daanse.olap.function.def.member.members.MembersResolver;
import org.eclipse.daanse.olap.function.def.member.namedsetcurrentordinal.NamedSetCurrentOrdinalResolver;
import org.eclipse.daanse.olap.function.def.member.nextmember.NextMemberResolver;
import org.eclipse.daanse.olap.function.def.member.parentcalc.ParentResolver;
import org.eclipse.daanse.olap.function.def.member.prevmember.PrevMemberResolver;
import org.eclipse.daanse.olap.function.def.member.strtomember.StrToMemberResolver;
import org.eclipse.daanse.olap.function.def.member.validmeasure.ValidMeasureResolver;
import org.eclipse.daanse.olap.function.def.minmax.MaxResolver;
import org.eclipse.daanse.olap.function.def.minmax.MinResolver;
import org.eclipse.daanse.olap.function.def.nativizeset.NativizeSetResolver;
import org.eclipse.daanse.olap.function.def.nonempty.NonEmptyResolver;
import org.eclipse.daanse.olap.function.def.nonemptycrossjoin.NonEmptyCrossJoinResolver;
import org.eclipse.daanse.olap.function.def.nonstandard.CachedExistsResolver;
import org.eclipse.daanse.olap.function.def.nonstandard.CalculatedChildResolver;
import org.eclipse.daanse.olap.function.def.nonstandard.CastResolver;
import org.eclipse.daanse.olap.function.def.nthquartile.FirstQResolver;
import org.eclipse.daanse.olap.function.def.nthquartile.ThirdQResolver;
import org.eclipse.daanse.olap.function.def.numeric.ordinal.OrdinalResolver;
import org.eclipse.daanse.olap.function.def.numeric.value.ValueResolver;
import org.eclipse.daanse.olap.function.def.openingclosingperiod.ClosingPeriodResolved;
import org.eclipse.daanse.olap.function.def.openingclosingperiod.OpeningPeriodResolved;
import org.eclipse.daanse.olap.function.def.operators.and.AndResolver;
import org.eclipse.daanse.olap.function.def.operators.divide.DivideResolver;
import org.eclipse.daanse.olap.function.def.operators.equal.EqualResolver;
import org.eclipse.daanse.olap.function.def.operators.equal.EqualStringResolver;
import org.eclipse.daanse.olap.function.def.operators.greater.GreaterOrEqualResolver;
import org.eclipse.daanse.olap.function.def.operators.greater.GreaterOrEqualStringResolver;
import org.eclipse.daanse.olap.function.def.operators.greater.GreaterResolver;
import org.eclipse.daanse.olap.function.def.operators.greater.GreaterStringResolver;
import org.eclipse.daanse.olap.function.def.operators.less.LessOrEqualEqualStringResolver;
import org.eclipse.daanse.olap.function.def.operators.less.LessOrEqualResolver;
import org.eclipse.daanse.olap.function.def.operators.less.LessResolver;
import org.eclipse.daanse.olap.function.def.operators.less.LessStringResolver;
import org.eclipse.daanse.olap.function.def.operators.minus.MinusPrefixResolver;
import org.eclipse.daanse.olap.function.def.operators.minus.MinusResolver;
import org.eclipse.daanse.olap.function.def.operators.multiply.MultiplyResolver;
import org.eclipse.daanse.olap.function.def.operators.not.NotPrefixResolver;
import org.eclipse.daanse.olap.function.def.operators.notequal.NotEqualResolver;
import org.eclipse.daanse.olap.function.def.operators.notequal.NotEqualStringResolver;
import org.eclipse.daanse.olap.function.def.operators.or.OrResolver;
import org.eclipse.daanse.olap.function.def.operators.or.OrStringResolver;
import org.eclipse.daanse.olap.function.def.operators.plus.PlusResolver;
import org.eclipse.daanse.olap.function.def.operators.xor.XorResolver;
import org.eclipse.daanse.olap.function.def.order.OrderResolver;
import org.eclipse.daanse.olap.function.def.parallelperiod.ParallelPeriodResolver;
import org.eclipse.daanse.olap.function.def.percentile.PercentileResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.PeriodsToDateResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.MtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.QtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.WtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.YtdMultiResolver;
import org.eclipse.daanse.olap.function.def.rank.RankResolver;
import org.eclipse.daanse.olap.function.def.set.SetResolver;
import org.eclipse.daanse.olap.function.def.set.addcalculatedmembers.AddCalculatedMembersResolver;
import org.eclipse.daanse.olap.function.def.set.ascendants.AscendantsResolver;
import org.eclipse.daanse.olap.function.def.set.children.ChildrenResolver;
import org.eclipse.daanse.olap.function.def.set.distinct.DistinctResolver;
import org.eclipse.daanse.olap.function.def.set.existing.ExistingResolver;
import org.eclipse.daanse.olap.function.def.set.extract.ExtractResolver;
import org.eclipse.daanse.olap.function.def.set.filter.FilterResolver;
import org.eclipse.daanse.olap.function.def.set.level.LevelMembersResolver;
import org.eclipse.daanse.olap.function.def.set.range.RangeResolver;
import org.eclipse.daanse.olap.function.def.set.setitem.SetItemIntResolver;
import org.eclipse.daanse.olap.function.def.set.setitem.SetItemStringResolver;
import org.eclipse.daanse.olap.function.def.set.siblings.SiblingsResolver;
import org.eclipse.daanse.olap.function.def.set.stripcalculatedmembers.StripCalculatedMembersResolver;
import org.eclipse.daanse.olap.function.def.settostr.SetToStrResolver;
import org.eclipse.daanse.olap.function.def.stdev.StddevPResolver;
import org.eclipse.daanse.olap.function.def.stdev.StddevResolver;
import org.eclipse.daanse.olap.function.def.stdev.StdevPResolver;
import org.eclipse.daanse.olap.function.def.stdev.StdevResolver;
import org.eclipse.daanse.olap.function.def.string.LenResolver;
import org.eclipse.daanse.olap.function.def.string.UCaseResolver;
import org.eclipse.daanse.olap.function.def.strtoset.StrToSetResolver;
import org.eclipse.daanse.olap.function.def.strtotuple.StrToTupleResolver;
import org.eclipse.daanse.olap.function.def.subset.SubsetResolver;
import org.eclipse.daanse.olap.function.def.sum.SumResolver;
import org.eclipse.daanse.olap.function.def.toggledrillstate.ToggleDrillStateResolver;
import org.eclipse.daanse.olap.function.def.topbottomcount.BottomCountResolver;
import org.eclipse.daanse.olap.function.def.topbottomcount.TopCountResolver;
import org.eclipse.daanse.olap.function.def.topbottompercentsum.BottomPercentResolver;
import org.eclipse.daanse.olap.function.def.topbottompercentsum.BottomSumResolver;
import org.eclipse.daanse.olap.function.def.topbottompercentsum.TopPercentResolver;
import org.eclipse.daanse.olap.function.def.topbottompercentsum.TopSumResolver;
import org.eclipse.daanse.olap.function.def.tuple.TupleResolver;
import org.eclipse.daanse.olap.function.def.tupleitem.TupleItemResolver;
import org.eclipse.daanse.olap.function.def.tupletostr.TupleToStrResolver;
import org.eclipse.daanse.olap.function.def.union.UnionResolver;
import org.eclipse.daanse.olap.function.def.unorder.UnorderResolver;
import org.eclipse.daanse.olap.function.def.var.VarPResolver;
import org.eclipse.daanse.olap.function.def.var.VarResolver;
import org.eclipse.daanse.olap.function.def.var.VariancePResolver;
import org.eclipse.daanse.olap.function.def.var.VarianceResolver;
import org.eclipse.daanse.olap.function.def.visualtotals.VisualTotalsResolver;
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
	    functionService.addResolver(new MemberHierarchyResolver());
	    functionService.addResolver(new LevelHierarchyResolver());

	    functionService.addResolver(new YtdMultiResolver());
	    functionService.addResolver(new QtdMultiResolver());
	    functionService.addResolver(new MtdMultiResolver());
	    functionService.addResolver(new WtdMultiResolver());
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
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.set.members.MembersResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.set.hierarchy.AllMembersResolver());
        functionService.addResolver(new LevelMembersResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.set.level.AllMembersResolver());
        functionService.addResolver(new StripCalculatedMembersResolver());
        functionService.addResolver(new SiblingsResolver());
        functionService.addResolver(new OrderResolver());
        functionService.addResolver(new ExistingResolver());
        functionService.addResolver(new RangeResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.caption.dimension.CaptionResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.caption.hierarchy.CaptionResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.caption.level.CaptionResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.caption.member.CaptionResolver());
        functionService.addResolver(new MemberCaptionResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.name.dimension.NameResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.name.hierarchy.NameResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.name.level.NameResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.name.member.NameResolver());
        functionService.addResolver(new SetToStrResolver());
        functionService.addResolver(new IsEmptyFunctionResolver());
        functionService.addResolver(new IsEmptyPostfixResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.uniquename.dimension.UniqueNameResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.uniquename.hierarchy.UniqueNameResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.uniquename.level.UniqueNameResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.uniquename.member.UniqueNameResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.uniquename.member.Unique_NameResolver());
        functionService.addResolver(new LevelNumberResolver());
        functionService.addResolver(new PlusResolver());
        functionService.addResolver(new MinusResolver());
        functionService.addResolver(new MultiplyResolver());
        functionService.addResolver(new DivideResolver());
        functionService.addResolver(new OrResolver());
        functionService.addResolver(new OrStringResolver());
        functionService.addResolver(new AndResolver());
        functionService.addResolver(new XorResolver());
        functionService.addResolver(new NotPrefixResolver());
        functionService.addResolver(new EqualStringResolver());
        functionService.addResolver(new NotEqualStringResolver());
        functionService.addResolver(new NotEqualResolver());
        functionService.addResolver(new LessResolver());
        functionService.addResolver(new LessStringResolver());
        functionService.addResolver(new LessOrEqualResolver());
        functionService.addResolver(new LessOrEqualEqualStringResolver());
        functionService.addResolver(new GreaterResolver());
        functionService.addResolver(new GreaterStringResolver());
        functionService.addResolver(new GreaterOrEqualResolver());
        functionService.addResolver(new GreaterOrEqualStringResolver());
        functionService.addResolver(new EqualResolver());
        functionService.addResolver(new StrToSetResolver());
        functionService.addResolver(new DistinctResolver());
        functionService.addResolver(new NamedSetCurrentOrdinalResolver());

        functionService.addResolver(new MinusPrefixResolver());
        functionService.addResolver(new UCaseResolver());
        functionService.addResolver(new LenResolver());
        functionService.addResolver(new CalculatedChildResolver());
        functionService.addResolver(new CachedExistsResolver());
        functionService.addResolver(new CastResolver());
        if (false) { //as in BuiltinFunTable
            functionService.addResolver(new CurrentResolver());
        }

        functionService.addResolver(new AncestorsResolver());
        functionService.addResolver(new IsNullResolver());
        functionService.addResolver(new IsResolver());
        functionService.addResolver(new LagResolver());
        functionService.addResolver(new LeadResolver());
        functionService.addResolver(new ClosingPeriodResolved());
        functionService.addResolver(new OpeningPeriodResolved());
        functionService.addResolver(new ParallelPeriodResolver());
        functionService.addResolver(new StrToMemberResolver());
        functionService.addResolver(new ValidMeasureResolver());
        functionService.addResolver(new AggregateResolver());
        functionService.addResolver(new CorrelationResolver());
        
        functionService.addResolver(new CovarianceResolver());
        functionService.addResolver(new CovarianceNResolver());
        functionService.addResolver(new LinRegInterceptResolver());
        functionService.addResolver(new LinRegPointResolver());
        functionService.addResolver(new LinRegR2Resolver());
        functionService.addResolver(new LinRegSlopeResolver());
        functionService.addResolver(new LinRegVarianceResolver());
        functionService.addResolver(new MaxResolver());
        functionService.addResolver(new MinResolver());
        functionService.addResolver(new MedianResolver());
        
        functionService.addResolver(new PercentileResolver());
        functionService.addResolver(new RankResolver());
        functionService.addResolver(new StddevResolver());
        functionService.addResolver(new StdevResolver());
        
        functionService.addResolver(new StdevPResolver());
        functionService.addResolver(new StddevPResolver());
        functionService.addResolver(new SumResolver());
        functionService.addResolver(new VarianceResolver());
        functionService.addResolver(new VarResolver());
        functionService.addResolver(new VariancePResolver());
        functionService.addResolver(new VarPResolver());
        functionService.addResolver(new BottomCountResolver());
        functionService.addResolver(new TopCountResolver());
        functionService.addResolver(new BottomPercentResolver());
        functionService.addResolver(new BottomSumResolver());
        functionService.addResolver(new TopPercentResolver());
        functionService.addResolver(new TopSumResolver());
        functionService.addResolver(new NonEmptyResolver());
        functionService.addResolver(new StarCrossJoinResolver());
        functionService.addResolver(new CrossJoinResolver());
        
        functionService.addResolver(new NonEmptyCrossJoinResolver());
        functionService.addResolver(new DescendantsMemberResolver());
        functionService.addResolver(new DescendantsSetResolver());
        functionService.addResolver(new DrilldownLevelResolver());
        functionService.addResolver(new DrilldownLevelTopResolver());
        functionService.addResolver(new DrilldownMemberResolver());
        functionService.addResolver(new ExceptResolver());
        functionService.addResolver(new ExistsResolver());
        functionService.addResolver(new GenerateListResolver());
        functionService.addResolver(new GenerateStringResolver());
        functionService.addResolver(new HeadResolver());
        functionService.addResolver(new TailResolver());
        functionService.addResolver(new HierarchizeResolver());
        functionService.addResolver(new IntersectResolver());
        functionService.addResolver(new LastPeriodsResolver());
        functionService.addResolver(new DrilldownLevelBottomResolver());
        
        functionService.addResolver(new UnorderResolver());
        functionService.addResolver(new PeriodsToDateResolver());
        functionService.addResolver(new SubsetResolver());
        functionService.addResolver(new ToggleDrillStateResolver());
        functionService.addResolver(new SetItemStringResolver());
        functionService.addResolver(new SetItemIntResolver());
        functionService.addResolver(new UnionResolver());
        functionService.addResolver(new VisualTotalsResolver());
        //functionService.addResolver(new NativizeSetResolver()); //TODO
        functionService.addResolver(new FormatResolver());
        functionService.addResolver(new SetResolver());
        functionService.addResolver(new TupleToStrResolver());
        functionService.addResolver(new TupleItemResolver());
        functionService.addResolver(new FirstQResolver());
        functionService.addResolver(new ThirdQResolver());
        functionService.addResolver(new StrToTupleResolver());
        functionService.addResolver(new TupleResolver());
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
