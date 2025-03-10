A minimal cube with one measurement column and different aggregators

Measurement values in a data cube can be aggregated differently, depending on the chosen aggregation function in the aggregator attribute of the <Measure> tag. Available aggregators are:
- sum: summation of numeric values
- count: number of values
- min: minimal value
- max: maximal value
- avg: average of numeric values
- distinct-count: number of different values (without duplicate values)

In this example cube every measure traces back to the "VALUE" column of the "Fact" database table, but uses another aggregator.



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube with one measurement column and different aggregators" name="01-03_Minimal_Cube_With_One_Measure_And_Different_Aggregators" cubes="CubeDifferentAggregators" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:PhysicalCube id="CubeDifferentAggregators" name="CubeDifferentAggregators" query="FactQuery">
    <measureGroups>
      <measures id="Measure-Sum" name="Measure-Sum" column="Fact_VALUE"/>
      <measures id="Measure-Min" name="Measure-Min" column="Fact_VALUE" aggregator="min"/>
      <measures id="Measure-Max" name="Measure-Max" column="Fact_VALUE" aggregator="max"/>
      <measures id="Measure-Avg" name="Measure-Avg" column="Fact_VALUE" aggregator="avg"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

