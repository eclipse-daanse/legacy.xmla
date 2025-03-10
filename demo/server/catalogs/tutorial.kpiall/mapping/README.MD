    A minimal cube with Kpi with all kpi properties

    A KPI has four important properties which are value, goal, status and trend.
    Let's explain this by means of Profit Margin with the below calculation.

Value: is the actual value of the KPI. This will be a numeric value. For example, this can be the Profit Margin.
    This might not be included in the fact table hence we may have to calculate or derive this column.

Goal: every organization has a goal for this value. For example, the organization may look at the
    goal of achieving a five percent Profit Margin. Also, sometimes they may have different values for
    different business areas. For example, depending on the product category or sales territory,
    the sales margin goal will differ.

Status: depending on the KPI value and the KPI goal, the KPI status can be defined.
    For an example, we can say that if the KPI value is greater than the goal it is great if it is not greater
    than the goal, but still greater than zero it is good and if less than zero or running at a loss it is bad.
    This Great, Good or Bad can be displayed to the user by means of a graphical representation such as an arrow,
    traffic lights or a gauge.

Trend: trend is an optional parameter when defining a KPI, but still an important feature in a KPI.
    For example, you may have a great profit margin, but comparing with last year, it could be less.
    On the other hand, you might have a bad profit margin, but compared to last year it is improving.

Weight:  The unique name of the member in the measures dimension for the KPI weight.

StatusGraphic: The default graphical representation of the KPI status.
    (Traffic Light, Road Signs, Gauge - Ascending, Gauge - Descending, Thermometer, Cylinder, Smiley Face)

TrendGraphic: The default graphical representation of the KPI trend.
    (Standard Arrow, Status Arrow - Ascending, Status Arrow - Descending, Smiley Face)

DisplayFolder:  The display folder.



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Minimal Cubes With KPI with all properties" name="Minimal_Cubes_With_KPI_all_Properties" cubes="CubeKPI" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
      <columns id="Fact_VALUE_NUMERIC" name="VALUE_NUMERIC" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:PhysicalCube id="CubeKPI" name="CubeKPI" query="FactQuery">
    <calculatedMembers id="CalculatedValue" name="CalculatedValue" visible="false" formula="[Measures].[Measure1-Sum] / [Measures].[Measure2-Count]"/>
    <calculatedMembers id="CalculatedGoal" name="CalculatedGoal" visible="false" formula="[Measures].[Measure1-Sum] / [Measures].[Measure2-Count]"/>
    <calculatedMembers id="CalculatedStatus" name="CalculatedStatus" visible="false" formula="[Measures].[Measure1-Sum] / [Measures].[Measure2-Count]"/>
    <calculatedMembers id="CalculatedTrend" name="CalculatedTrend" visible="false" formula="[Measures].[Measure1-Sum] / [Measures].[Measure2-Count]"/>
    <kpis id="Kpi1" description="Kpi with all parameters" name="Kpi1" displayFolder="Kpi1Folder1\Kpi1Folder2" associatedMeasureGroupID="Kpi1MeasureGroupID" value="[Measures].[CalculatedValue]" goal="[Measures].[CalculatedGoal]" status="[Measures].[CalculatedStatus]" trend="[Measures].[CalculatedTrend]" weight="[Measures].[CalculatedValue]" trendGraphic="Smiley Face" statusGraphic="Cylinder" currentTimeMember="[Measures].[CalculatedValue]"/>
    <measureGroups>
      <measures id="Measure1-Sum" name="Measure1-Sum" column="Fact_VALUE"/>
      <measures id="Measure2-Count" name="Measure2-Count" column="Fact_VALUE" aggregator="count"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

