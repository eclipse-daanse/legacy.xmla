Data cubes (<Cube>) are defined in an OLAP schema (<Schema>). Within the schema the name of each data cube must be unique.
This example schema contains one cube named "Cube".

A cube is based on a fact table (<Table>) which refers to a database table containing one or more measurements to be aggregated (and optionally further columns defining factual dimensions).
In this case the database table representing the fact table is named "Fact" in the database, which is adressed in the name attribute within the <Table> tag.

Each measurement of the cube is defined in a separate <Measure> element.
The measurement in this example cube is named "Measure" (name attribute). It corresponds to the "VALUE" column (column attribute) in the database table "Fact" and is aggregated by summation (aggregator attribute).
Level is defined in <Level> element.
Property is defined in <Property> element inside <Level> element. Property we can see in cell tooltip in excel



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube with DrillThroughAction" name="Minimal_Cube_with_DrillThroughAction" cubes="Cube" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="H1_L1" name="H1_L1">
      <columns id="H1_L1_VALUE" name="KEY" type="INTEGER"/>
      <columns id="H1_L1_NAME" name="NAME" type="VARCHAR"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="H2_L1" name="H2_L1">
      <columns id="H2_L1_VALUE" name="KEY" type="INTEGER"/>
      <columns id="H2_L1_NAME" name="NAME" type="VARCHAR"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="HX_L2" name="HX_L2">
      <columns id="HX_L2_KEY" name="KEY" type="INTEGER"/>
      <columns id="HX_L2_NAME" name="NAME" type="VARCHAR"/>
      <columns id="HX_L2_H1L1_KEY" name="H1L1_KEY" type="INTEGER"/>
      <columns id="HX_L2_H2L1_KEY" name="H2L1_KEY" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:JoinQuery id="join1">
    <left key="HX_L2_H1L1_KEY">
      <query xsi:type="roma:TableQuery" id="HxL2Query" table="HX_L2"/>
    </left>
    <right key="H1_L1_VALUE">
      <query xsi:type="roma:TableQuery" id="H1L1Query" table="H1_L1"/>
    </right>
  </roma:JoinQuery>
  <roma:Level id="H1_Level2" name="H1_Level2" column="HX_L2_KEY" nameColumn="HX_L2_NAME" table="HX_L2"/>
  <roma:Level id="H1_Level1" name="H1_Level1" column="H1_L1_VALUE" nameColumn="H1_L1_NAME" table="H1_L1"/>
  <roma:Hierarchy id="Hierarchy1" name="Hierarchy1" levels="H1_Level1 H1_Level2" hasAll="true" primaryKey="HX_L2_KEY" primaryKeyTable="HX_L2" query="join1"/>
  <roma:StandardDimension id="Dimension1" name="Dimension1" hierarchies="Hierarchy1"/>
  <roma:PhysicalCube id="Cube" name="Cube" query="FactQuery">
    <dimensionConnectors foreignKey="Fact_KEY" dimension="Dimension1" overrideDimensionName="Dimension1"/>
    <action xsi:type="roma:DrillThroughAction" id="DrillthroughH1L1" name="DrillthroughH1L1" drillThroughMeasure="Measure1" default="true"/>
    <action xsi:type="roma:DrillThroughAction" id="DrillthroughH2L1" name="DrillthroughH2L1" drillThroughMeasure="Measure1">
      <drillThroughAttribute dimension="Dimension1" hierarchy="Hierarchy1" level="H1_Level1"/>
    </action>
    <measureGroups>
      <measures id="Measure1" name="Measure1" column="Fact_VALUE"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

- [H1_L1](./data/H1_L1.csv)

- [H2_L1](./data/H2_L1.csv)

- [HX_L2](./data/HX_L2.csv)

