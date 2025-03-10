A minimal cube with measures of different datatypes

The datatype of the measuse can be adapted by using the optional datatype attribute of the <Measure> element.
- String
- Numeric
- Integer
- Boolean
- Date
- Time
- Timestamp
If no datatype attribute is defined, the following default datatypes are used:
- if the aggregator "count" or "distinct-count" is used: Integer
- it another aggregator is used: Numeric

In this example cube both measurements are generated from the "VALUE_NUMERIC" column of the "Fact" table, but one is provided with an Intger and the other one with a Numeric datatype.



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube containing the same measurement with two different data types" name="Minimal_Cube_With_Measures_Of_Different_Datatypes" cubes="CubeOneNumericMeasureDifferentDataTypes" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:PhysicalCube id="CubeOneNumericMeasureDifferentDataTypes" name="CubeOneNumericMeasureDifferentDataTypes" query="FactQuery">
    <measureGroups>
      <measures id="Measure1-Integer" name="Measure1-Integer" column="Fact_VALUE" dataType="Integer"/>
      <measures id="Measure1-Numeric" name="Measure1-Numeric" column="Fact_VALUE" dataType="Numeric"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

