A minimal cube with two measures from different table columns with defaultMeasure
Specifying defaultMeasure in <Cube> element allows users to explicitly specify any base measure as a default Measure.

A data cube can contain multiple measures and they can be taken from different columns of the database fact table.
Each measure must be defined by a separate <Measure> tag, in which the column attribute is used to assign the database table colum that holds the measure's data.
All measures of the cube belong to a default cube dimension called "Measures", which can be adressed within the schema (e.g while adding a calculated measure).

This sample cube contains the two measures "Measure 1", which traces back to the table column "VALUE1", and Measure2", which is derived from the table column "VALUE2".



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube with defaultMeasure" name="Minimal_Cubes_With_Measures_with_defaultMeasure" cubes="CubeTwoMeasures" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE1" name="VALUE1" type="INTEGER"/>
      <columns id="Fact_VALUE2" name="VALUE2" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:PhysicalCube id="CubeTwoMeasures" name="CubeTwoMeasures" defaultMeasure="Measure2" query="FactQuery">
    <measureGroups>
      <measures id="Measure1" name="Measure1" column="Fact_VALUE1"/>
      <measures id="Measure2" name="Measure2" column="Fact_VALUE2"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

