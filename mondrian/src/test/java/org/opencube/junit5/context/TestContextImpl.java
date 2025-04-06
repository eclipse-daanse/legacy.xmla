package org.opencube.junit5.context;

import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
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
import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.ConnectionProps;
import org.eclipse.daanse.olap.api.aggregator.Aggregator;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.olap.core.LoggingEventBus;
import org.eclipse.daanse.olap.function.core.FunctionServiceImpl;
import org.eclipse.daanse.olap.function.core.resolver.NullReservedWordsResolver;
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
import org.eclipse.daanse.olap.function.def.coalesceempty.CoalesceEmptyResolver;
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
import org.eclipse.daanse.olap.function.def.excel.acos.AcosResolver;
import org.eclipse.daanse.olap.function.def.excel.acos.AcoshResolver;
import org.eclipse.daanse.olap.function.def.excel.asin.AsinhResolver;
import org.eclipse.daanse.olap.function.def.excel.atan2.Atan2Resolver;
import org.eclipse.daanse.olap.function.def.excel.atan2.AtanhResolver;
import org.eclipse.daanse.olap.function.def.excel.cosh.CoshResolver;
import org.eclipse.daanse.olap.function.def.excel.degrees.DegreesResolver;
import org.eclipse.daanse.olap.function.def.excel.log10.Log10Resolver;
import org.eclipse.daanse.olap.function.def.excel.mod.ModResolver;
import org.eclipse.daanse.olap.function.def.excel.pi.PiResolver;
import org.eclipse.daanse.olap.function.def.excel.power.PowerResolver;
import org.eclipse.daanse.olap.function.def.excel.radians.RadiansResolver;
import org.eclipse.daanse.olap.function.def.excel.sinh.SinhResolver;
import org.eclipse.daanse.olap.function.def.excel.sqrtpi.SqrtPiResolver;
import org.eclipse.daanse.olap.function.def.excel.tanh.TanhResolver;
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
import org.eclipse.daanse.olap.function.def.member.defaultmember.NonFunctionDefaultMemberResolver;
import org.eclipse.daanse.olap.function.def.member.firstchild.FirstChildResolver;
import org.eclipse.daanse.olap.function.def.member.firstsibling.FirstSiblingResolver;
import org.eclipse.daanse.olap.function.def.member.lastchild.LastChildResolver;
import org.eclipse.daanse.olap.function.def.member.lastsibling.LastSiblingResolver;
import org.eclipse.daanse.olap.function.def.member.memberorderkey.MemberOrderKeyResolver;
import org.eclipse.daanse.olap.function.def.member.members.MembersResolver;
import org.eclipse.daanse.olap.function.def.member.members.NonFunctionMembersResolver;
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
import org.eclipse.daanse.olap.function.def.parameter.ParamRefResolver;
import org.eclipse.daanse.olap.function.def.parameter.ParameterResolver;
import org.eclipse.daanse.olap.function.def.percentile.PercentileResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.PeriodsToDateResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.MtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.QtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.WtdMultiResolver;
import org.eclipse.daanse.olap.function.def.periodstodate.xtd.YtdMultiResolver;
import org.eclipse.daanse.olap.function.def.properties.PropertiesResolver;
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
import org.eclipse.daanse.olap.function.def.udf.currentdatemember.CurrentDateMemberResolver;
import org.eclipse.daanse.olap.function.def.udf.currentdatestring.CurrentDateStringResolver;
import org.eclipse.daanse.olap.function.def.udf.in.InResolver;
import org.eclipse.daanse.olap.function.def.udf.lastnonempty.LastNonEmptyResolver;
import org.eclipse.daanse.olap.function.def.udf.matches.MatchesResolver;
import org.eclipse.daanse.olap.function.def.udf.nullvalue.NullValueResolver;
import org.eclipse.daanse.olap.function.def.udf.val.ValResolver;
import org.eclipse.daanse.olap.function.def.union.UnionResolver;
import org.eclipse.daanse.olap.function.def.unorder.UnorderResolver;
import org.eclipse.daanse.olap.function.def.var.VarPResolver;
import org.eclipse.daanse.olap.function.def.var.VarResolver;
import org.eclipse.daanse.olap.function.def.var.VariancePResolver;
import org.eclipse.daanse.olap.function.def.var.VarianceResolver;
import org.eclipse.daanse.olap.function.def.vba.abs.AbsResolver;
import org.eclipse.daanse.olap.function.def.vba.asc.AscBResolver;
import org.eclipse.daanse.olap.function.def.vba.asc.AscResolver;
import org.eclipse.daanse.olap.function.def.vba.asc.AscWResolver;
import org.eclipse.daanse.olap.function.def.vba.atn.AtnResolver;
import org.eclipse.daanse.olap.function.def.vba.cbool.CBoolResolver;
import org.eclipse.daanse.olap.function.def.vba.cbyte.CByteResolver;
import org.eclipse.daanse.olap.function.def.vba.cdate.CDateResolver;
import org.eclipse.daanse.olap.function.def.vba.cdbl.CDblResolver;
import org.eclipse.daanse.olap.function.def.vba.chr.ChrBResolver;
import org.eclipse.daanse.olap.function.def.vba.chr.ChrResolver;
import org.eclipse.daanse.olap.function.def.vba.chr.ChrWResolver;
import org.eclipse.daanse.olap.function.def.vba.cint.CIntResolver;
import org.eclipse.daanse.olap.function.def.vba.cos.CosResolver;
import org.eclipse.daanse.olap.function.def.vba.date.DateResolver;
import org.eclipse.daanse.olap.function.def.vba.dateadd.DateAddResolver;
import org.eclipse.daanse.olap.function.def.vba.datediff.DateDiffResolver;
import org.eclipse.daanse.olap.function.def.vba.datepart.DatePartResolver;
import org.eclipse.daanse.olap.function.def.vba.dateserial.DateSerialResolver;
import org.eclipse.daanse.olap.function.def.vba.datevalue.DateValueResolver;
import org.eclipse.daanse.olap.function.def.vba.day.DayResolver;
import org.eclipse.daanse.olap.function.def.vba.ddb.DDBResolver;
import org.eclipse.daanse.olap.function.def.vba.exp.ExpResolver;
import org.eclipse.daanse.olap.function.def.vba.fix.FixResolver;
import org.eclipse.daanse.olap.function.def.vba.formatcurrency.FormatCurrencyResolver;
import org.eclipse.daanse.olap.function.def.vba.formatdatetime.FormatDateTimeResolver;
import org.eclipse.daanse.olap.function.def.vba.formatnumber.FormatNumberResolver;
import org.eclipse.daanse.olap.function.def.vba.formatpercent.FormatPercentResolver;
import org.eclipse.daanse.olap.function.def.vba.fv.FVResolver;
import org.eclipse.daanse.olap.function.def.vba.hex.HexResolver;
import org.eclipse.daanse.olap.function.def.vba.hour.HourResolver;
import org.eclipse.daanse.olap.function.def.vba.instr.InStrResolver;
import org.eclipse.daanse.olap.function.def.vba.instrrev.InStrRevResolver;
import org.eclipse.daanse.olap.function.def.vba.integer.IntResolver;
import org.eclipse.daanse.olap.function.def.vba.ipmt.IPmtResolver;
import org.eclipse.daanse.olap.function.def.vba.irr.IRRResolver;
import org.eclipse.daanse.olap.function.def.vba.isarray.IsArrayResolver;
import org.eclipse.daanse.olap.function.def.vba.isdate.IsDateResolver;
import org.eclipse.daanse.olap.function.def.vba.iserror.IsErrorResolver;
import org.eclipse.daanse.olap.function.def.vba.ismissing.IsMissingResolver;
import org.eclipse.daanse.olap.function.def.vba.isnumeric.IsNumericResolver;
import org.eclipse.daanse.olap.function.def.vba.isobject.IsObjectResolver;
import org.eclipse.daanse.olap.function.def.vba.lcase.LCaseResolver;
import org.eclipse.daanse.olap.function.def.vba.left.LeftResolver;
import org.eclipse.daanse.olap.function.def.vba.log.LogResolver;
import org.eclipse.daanse.olap.function.def.vba.ltrim.LTrimResolver;
import org.eclipse.daanse.olap.function.def.vba.mid.MidResolver;
import org.eclipse.daanse.olap.function.def.vba.minute.MinuteResolver;
import org.eclipse.daanse.olap.function.def.vba.mirr.MIRRResolver;
import org.eclipse.daanse.olap.function.def.vba.month.MonthResolver;
import org.eclipse.daanse.olap.function.def.vba.monthname.MonthNameResolver;
import org.eclipse.daanse.olap.function.def.vba.now.NowResolver;
import org.eclipse.daanse.olap.function.def.vba.nper.NPerResolver;
import org.eclipse.daanse.olap.function.def.vba.npv.NPVResolver;
import org.eclipse.daanse.olap.function.def.vba.pmt.PmtResolver;
import org.eclipse.daanse.olap.function.def.vba.ppmt.PPmtResolver;
import org.eclipse.daanse.olap.function.def.vba.pv.PVResolver;
import org.eclipse.daanse.olap.function.def.vba.rate.RateResolver;
import org.eclipse.daanse.olap.function.def.vba.replace.ReplaceResolver;
import org.eclipse.daanse.olap.function.def.vba.round.RoundResolver;
import org.eclipse.daanse.olap.function.def.vba.rtrim.RTrimResolver;
import org.eclipse.daanse.olap.function.def.vba.second.SecondResolver;
import org.eclipse.daanse.olap.function.def.vba.sgn.SgnResolver;
import org.eclipse.daanse.olap.function.def.vba.sin.SinResolver;
import org.eclipse.daanse.olap.function.def.vba.sln.SLNResolver;
import org.eclipse.daanse.olap.function.def.vba.space.SpaceResolver;
import org.eclipse.daanse.olap.function.def.vba.sqr.SqrResolver;
import org.eclipse.daanse.olap.function.def.vba.strcomp.StrCompResolver;
import org.eclipse.daanse.olap.function.def.vba.string.StringResolver;
import org.eclipse.daanse.olap.function.def.vba.strreverse.StrReverseResolver;
import org.eclipse.daanse.olap.function.def.vba.syd.SYDResolver;
import org.eclipse.daanse.olap.function.def.vba.tan.TanResolver;
import org.eclipse.daanse.olap.function.def.vba.time.TimeResolver;
import org.eclipse.daanse.olap.function.def.vba.timer.TimerResolver;
import org.eclipse.daanse.olap.function.def.vba.timeserial.TimeSerialResolver;
import org.eclipse.daanse.olap.function.def.vba.timevalue.TimeValueResolver;
import org.eclipse.daanse.olap.function.def.vba.trim.TrimResolver;
import org.eclipse.daanse.olap.function.def.vba.typename.TypeNameResolver;
import org.eclipse.daanse.olap.function.def.vba.weekday.WeekdayResolver;
import org.eclipse.daanse.olap.function.def.vba.weekdayname.WeekdayNameResolver;
import org.eclipse.daanse.olap.function.def.vba.year.YearResolver;
import org.eclipse.daanse.olap.function.def.visualtotals.VisualTotalsResolver;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.aggregator.AvgAggregator;
import org.eclipse.daanse.rolap.aggregator.CountAggregator;
import org.eclipse.daanse.rolap.aggregator.DistinctCountAggregator;
import org.eclipse.daanse.rolap.aggregator.MaxAggregator;
import org.eclipse.daanse.rolap.aggregator.MinAggregator;
import org.eclipse.daanse.rolap.aggregator.SumAggregator;
import org.eclipse.daanse.rolap.mapping.api.CatalogMappingSupplier;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.sql.guard.api.SqlGuardFactory;

import mondrian.rolap.RolapCatalogCache;
import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapConnectionPropsR;
import mondrian.rolap.RolapResultShepherd;
import mondrian.rolap.agg.AggregationManager;

public class TestContextImpl extends AbstractBasicContext implements TestContext, RolapContext {

    private Dialect dialect;
    private DataSource dataSource;

    private ExpressionCompilerFactory expressionCompilerFactory = new BaseExpressionCompilerFactory();
    private CatalogMappingSupplier catalogMappingSupplier;
    private String name;
    private Optional<String> description = Optional.empty();
    private Semaphore queryLimimitSemaphore;
    private FunctionService functionService = new FunctionServiceImpl();
    private List<Aggregator> primaryAggregators = List.of(SumAggregator.INSTANCE, CountAggregator.INSTANCE,
            DistinctCountAggregator.INSTANCE, MinAggregator.INSTANCE, MaxAggregator.INSTANCE, AvgAggregator.INSTANCE);
    
    public TestContextImpl() {
        this.eventBus = new LoggingEventBus();
        shepherd = new RolapResultShepherd(getConfigValue(ConfigConstants.ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL, ConfigConstants.ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_DEFAULT_VALUE, Long.class),
                getConfigValue(ConfigConstants.ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_UNIT, ConfigConstants.ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_UNIT_DEFAULT_VALUE, TimeUnit.class),
                getConfigValue(ConfigConstants.ROLAP_CONNECTION_SHEPHERD_NB_THREADS, ConfigConstants.ROLAP_CONNECTION_SHEPHERD_NB_THREADS_DEFAULT_VALUE, Integer.class));
        aggMgr = new AggregationManager(this);
        schemaCache = new RolapCatalogCache(this);
        queryLimimitSemaphore = new Semaphore(getConfigValue(ConfigConstants.QUERY_LIMIT, ConfigConstants.QUERY_LIMIT_DEFAULT_VALUE ,Integer.class));
        functionService.addResolver(new NullReservedWordsResolver());
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
        if (false) { // as in BuiltinFunTable
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
        functionService.addResolver(new NativizeSetResolver());
        functionService.addResolver(new FormatResolver());
        functionService.addResolver(new SetResolver());
        functionService.addResolver(new TupleToStrResolver());
        functionService.addResolver(new TupleItemResolver());
        functionService.addResolver(new FirstQResolver());
        functionService.addResolver(new ThirdQResolver());
        functionService.addResolver(new StrToTupleResolver());
        functionService.addResolver(new TupleResolver());

        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifBooleanResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifDimensionResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifHierarchyResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifLevelResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifMemberResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifNumericResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifSetResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifStringResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.iif.IifTupleResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.count.CountResolver());
        functionService.addResolver(new CoalesceEmptyResolver());
        functionService.addResolver(new PropertiesResolver());
        functionService.addResolver(new ParameterResolver());
        functionService.addResolver(new ParamRefResolver());
        functionService.addResolver(new NonFunctionDefaultMemberResolver());
        functionService.addResolver(new NonFunctionMembersResolver());

        // excel functions
        functionService.addResolver(new AcoshResolver());
        functionService.addResolver(new AcosResolver());
        functionService.addResolver(new AsinhResolver());
        functionService.addResolver(new CoshResolver());
        functionService.addResolver(new Atan2Resolver());
        functionService.addResolver(new AtanhResolver());
        functionService.addResolver(new CoshResolver());
        functionService.addResolver(new DegreesResolver());
        functionService.addResolver(new Log10Resolver());
        functionService.addResolver(new ModResolver());
        functionService.addResolver(new PiResolver());
        functionService.addResolver(new RadiansResolver());
        functionService.addResolver(new SinhResolver());
        functionService.addResolver(new SqrtPiResolver());
        functionService.addResolver(new TanhResolver());
        functionService.addResolver(new PowerResolver());

        // Vba
        functionService.addResolver(new CBoolResolver());
        functionService.addResolver(new CByteResolver());
        functionService.addResolver(new CDblResolver());
        functionService.addResolver(new CDateResolver());
        functionService.addResolver(new IsDateResolver());
        functionService.addResolver(new CIntResolver());
        functionService.addResolver(new FixResolver());
        functionService.addResolver(new HexResolver());
        functionService.addResolver(new IntResolver());

        functionService.addResolver(new DateAddResolver());
        functionService.addResolver(new DateDiffResolver());
        functionService.addResolver(new DatePartResolver());
        functionService.addResolver(new DateResolver());
        functionService.addResolver(new DateSerialResolver());
        functionService.addResolver(new DateValueResolver());
        functionService.addResolver(new DayResolver());
        functionService.addResolver(new HourResolver());
        functionService.addResolver(new MinuteResolver());
        functionService.addResolver(new MonthResolver());
        functionService.addResolver(new NowResolver());
        functionService.addResolver(new SecondResolver());
        functionService.addResolver(new TimeResolver());
        functionService.addResolver(new TimeSerialResolver());
        functionService.addResolver(new TimeValueResolver());
        functionService.addResolver(new TimerResolver());
        functionService.addResolver(new YearResolver());
        functionService.addResolver(new WeekdayResolver());
        functionService.addResolver(new DDBResolver());
        functionService.addResolver(new FVResolver());
        functionService.addResolver(new IPmtResolver());
        functionService.addResolver(new NPerResolver());
        functionService.addResolver(new PPmtResolver());
        functionService.addResolver(new PmtResolver());
        functionService.addResolver(new PVResolver());

        functionService.addResolver(new RateResolver());
        functionService.addResolver(new SLNResolver());
        functionService.addResolver(new SYDResolver());
        functionService.addResolver(new IsArrayResolver());
        functionService.addResolver(new IsErrorResolver());
        functionService.addResolver(new IsMissingResolver());
        functionService.addResolver(new org.eclipse.daanse.olap.function.def.vba.isnull.IsNullResolver());
        functionService.addResolver(new IsNumericResolver());
        functionService.addResolver(new IsObjectResolver());
        functionService.addResolver(new TypeNameResolver());
        functionService.addResolver(new AbsResolver());
        functionService.addResolver(new AtnResolver());
        functionService.addResolver(new CosResolver());
        functionService.addResolver(new LogResolver());
        functionService.addResolver(new SinResolver());
        functionService.addResolver(new SgnResolver());
        functionService.addResolver(new SqrResolver());
        functionService.addResolver(new TanResolver());
        functionService.addResolver(new RoundResolver());
        functionService.addResolver(new AscResolver());
        functionService.addResolver(new AscBResolver());
        functionService.addResolver(new AscWResolver());
        functionService.addResolver(new ChrResolver());
        functionService.addResolver(new ChrBResolver());
        functionService.addResolver(new ChrWResolver());
        functionService.addResolver(new FormatCurrencyResolver());
        functionService.addResolver(new ExpResolver());

        functionService.addResolver(new FormatNumberResolver());
        functionService.addResolver(new FormatDateTimeResolver());
        functionService.addResolver(new FormatPercentResolver());
        functionService.addResolver(new InStrResolver());
        functionService.addResolver(new InStrRevResolver());
        functionService.addResolver(new LCaseResolver());
        functionService.addResolver(new LTrimResolver());
        functionService.addResolver(new LeftResolver());
        functionService.addResolver(new MidResolver());
        functionService.addResolver(new MonthNameResolver());
        functionService.addResolver(new RTrimResolver());
        functionService.addResolver(new ReplaceResolver());

        functionService.addResolver(new org.eclipse.daanse.olap.function.def.vba.right.RightResolver());
        functionService.addResolver(new SpaceResolver());
        functionService.addResolver(new StrCompResolver());
        functionService.addResolver(new StrReverseResolver());
        functionService.addResolver(new StringResolver());
        functionService.addResolver(new TrimResolver());
        functionService.addResolver(new WeekdayNameResolver());
        functionService.addResolver(new IRRResolver());
        functionService.addResolver(new MIRRResolver());
        functionService.addResolver(new NPVResolver());

        // UDF
        functionService.addResolver(new InResolver());
        functionService.addResolver(new MatchesResolver());
        functionService.addResolver(new LastNonEmptyResolver());
        functionService.addResolver(new NullValueResolver());
        functionService.addResolver(new ValResolver());
        functionService.addResolver(new CurrentDateStringResolver());
        functionService.addResolver(new CurrentDateMemberResolver());
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
    public org.eclipse.daanse.olap.api.Connection getConnectionWithDefaultRole() {
        return getConnection(new RolapConnectionPropsR());
    }

    @Override
    public org.eclipse.daanse.olap.api.Connection getConnection(ConnectionProps props) {
        return new RolapConnection(this, props);
    }

    @Override
    public org.eclipse.daanse.olap.api.Connection getConnection(List<String> roles) {
        return getConnection(new RolapConnectionPropsR(roles, true, Locale.getDefault(), Duration.ofSeconds(-1),
                Optional.empty(), Optional.empty()));
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

    @Override
    public List<String> getAccessRoles() {
        return List.of();
    }

    @Override
    public Optional<SqlGuardFactory> getSqlGuardFactory() {
        return Optional.empty();
    }

    public void setConfigValue(String key, Object value) {
        if (configuration == null) {
            configuration = new HashMap<String, Object>();
        }
        configuration.put(key, value);
    }
    
    public void setCellBatchSize(Integer cellBatchSize) {
        setConfigValue(ConfigConstants.CELL_BATCH_SIZE, cellBatchSize);
    }

    public void setSegmentCacheManagerNumberSqlThreads(Integer segmentCacheManagerNumberSqlThreads) {
        setConfigValue(ConfigConstants.SEGMENT_CACHE_MANAGER_NUMBER_SQL_THREADS, segmentCacheManagerNumberSqlThreads);
    }

    public void setSolveOrderMode(String solveOrderMode) {
        setConfigValue(ConfigConstants.SOLVE_ORDER_MODE, solveOrderMode);
    }

    public void setDisableCaching(boolean disableCaching) {
        setConfigValue(ConfigConstants.DISABLE_CACHING, disableCaching);
    }

    public void setEnableGroupingSets(boolean enableGroupingSets) {
        setConfigValue(ConfigConstants.ENABLE_GROUPING_SETS, enableGroupingSets);
    }

    public void setCompoundSlicerMemberSolveOrder(int compoundSlicerMemberSolveOrder) {
        setConfigValue(ConfigConstants.COMPOUND_SLICER_MEMBER_SOLVE_ORDER, compoundSlicerMemberSolveOrder);
    }

    public void setEnableDrillThrough(boolean enableDrillThrough) {
        setConfigValue(ConfigConstants.ENABLE_DRILL_THROUGH, enableDrillThrough);
    }

    public void setEnableNativeFilter(boolean enableNativeFilter) {
        setConfigValue(ConfigConstants.ENABLE_NATIVE_FILTER, enableNativeFilter);
    }

    public void setEnableNativeCrossJoin(boolean enableNativeCrossJoin) {
        setConfigValue(ConfigConstants.ENABLE_NATIVE_CROSS_JOIN, enableNativeCrossJoin);
    }

    public void setEnableNativeTopCount(boolean enableNativeTopCount) {
        setConfigValue(ConfigConstants.ENABLE_NATIVE_TOP_COUNT, enableNativeTopCount);
    }

    public void setEnableInMemoryRollup(boolean enableInMemoryRollup) {
        setConfigValue(ConfigConstants.ENABLE_IN_MEMORY_ROLLUP, enableInMemoryRollup);
    }

    public void setExpandNonNative(boolean expandNonNative) {
        setConfigValue(ConfigConstants.EXPAND_NON_NATIVE, expandNonNative);
    }

    public void setGenerateAggregateSql(boolean generateAggregateSql) {
        setConfigValue(ConfigConstants.GENERATE_AGGREGATE_SQL, generateAggregateSql);
    }

    public void setIgnoreInvalidMembersDuringQuery(boolean ignoreInvalidMembersDuringQuery) {
        setConfigValue(ConfigConstants.IGNORE_INVALID_MEMBERS_DURING_QUERY, ignoreInvalidMembersDuringQuery);
    }

    public void setIgnoreMeasureForNonJoiningDimension(boolean ignoreMeasureForNonJoiningDimension) {
        setConfigValue(ConfigConstants.IGNORE_MEASURE_FOR_NON_JOINING_DIMENSION, ignoreMeasureForNonJoiningDimension);
    }

    public void setIterationLimit(int iterationLimit) {
        setConfigValue(ConfigConstants.ITERATION_LIMIT, iterationLimit);
    }

    public void setLevelPreCacheThreshold(int levelPreCacheThreshold) {
        setConfigValue(ConfigConstants.LEVEL_PRE_CACHE_THRESHOLD, levelPreCacheThreshold);
    }

    public void setReadAggregates(boolean readAggregates) {
        setConfigValue(ConfigConstants.READ_AGGREGATES, readAggregates);
    }

    public void setAlertNativeEvaluationUnsupported(String alertNativeEvaluationUnsupported) {
        setConfigValue(ConfigConstants.ALERT_NATIVE_EVALUATION_UNSUPPORTED, alertNativeEvaluationUnsupported);
    }

    public void setCrossJoinOptimizerSize(int crossJoinOptimizerSize) {
        setConfigValue(ConfigConstants.CROSS_JOIN_OPTIMIZER_SIZE, crossJoinOptimizerSize);
    }

    public void setCurrentMemberWithCompoundSlicerAlert(String currentMemberWithCompoundSlicerAlert) {
        setConfigValue(ConfigConstants.CURRENT_MEMBER_WITH_COMPOUND_SLICER_ALERT, currentMemberWithCompoundSlicerAlert);
    }

    public void setIgnoreInvalidMembers(boolean ignoreInvalidMembers) {
        setConfigValue(ConfigConstants.IGNORE_INVALID_MEMBERS, ignoreInvalidMembers);
    }

    public void setMaxEvalDepth(int maxEvalDepth) {
        setConfigValue(ConfigConstants.MAX_EVAL_DEPTH, maxEvalDepth);
    }

    public void setCheckCancelOrTimeoutInterval(int checkCancelOrTimeoutInterval) {
        setConfigValue(ConfigConstants.CHECK_CANCEL_OR_TIMEOUT_INTERVAL, checkCancelOrTimeoutInterval);
    }

    public void setWarnIfNoPatternForDialect(String warnIfNoPatternForDialect) {
        setConfigValue(ConfigConstants.WARN_IF_NO_PATTERN_FOR_DIALECT, warnIfNoPatternForDialect);
    }

    public void setUseAggregates(boolean useAggregates) {
        setConfigValue(ConfigConstants.USE_AGGREGATES, useAggregates);
    }

    public void setQueryTimeout(int queryTimeout) {
        setConfigValue(ConfigConstants.QUERY_TIMEOUT, queryTimeout);
    }

    public void setOptimizePredicates(boolean optimizePredicates) {
        setConfigValue(ConfigConstants.OPTIMIZE_PREDICATES, optimizePredicates);
    }

    public void setNullDenominatorProducesNull(boolean nullDenominatorProducesNull) {
        setConfigValue(ConfigConstants.NULL_DENOMINATOR_PRODUCES_NULL, nullDenominatorProducesNull);
    }


    public void setNativizeMinThreshold(int nativizeMinThreshold) {
        setConfigValue(ConfigConstants.NATIVIZE_MIN_THRESHOLD, nativizeMinThreshold);
    }

    public void setNativizeMaxResults(int nativizeMaxResults) {
        setConfigValue(ConfigConstants.NATIVIZE_MAX_RESULTS, nativizeMaxResults);
    }

    public void setGenerateFormattedSql(boolean generateFormattedSql) {
        setConfigValue(ConfigConstants.GENERATE_FORMATTED_SQL, generateFormattedSql);
    }

    @Override
    public Optional<Aggregator> getAggregator(String aggregatorName) {
        return primaryAggregators.stream().filter(a->aggregatorName.equals(a.getName())).findAny();
    }
}
