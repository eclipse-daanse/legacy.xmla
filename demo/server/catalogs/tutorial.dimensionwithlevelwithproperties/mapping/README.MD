A basic OLAP schema with a level with property

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
  <roma:Catalog description="Schema of a minimal cube with cube dimension level with property" name="Minimal_Cube_with_cube_dimension_level_with_property" cubes="Cube" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
      <columns id="Fact_L" name="L" type="VARCHAR" columnSize="100"/>
      <columns id="Fact_prop" name="prop" type="VARCHAR" columnSize="100"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:Level id="Level" name="Level" column="Fact_L">
    <memberProperties id="Prop" name="Prop" column="Fact_prop"/>
  </roma:Level>
  <roma:Hierarchy id="HierarchyWithHasAll" name="HierarchyWithHasAll" levels="Level" hasAll="true" primaryKey="Fact_KEY" query="FactQuery"/>
  <roma:StandardDimension id="Dimension" name="Dimension" hierarchies="HierarchyWithHasAll"/>
  <roma:PhysicalCube id="Cube" name="Cube" query="FactQuery">
    <dimensionConnectors dimension="Dimension" overrideDimensionName="Dimension"/>
    <measureGroups>
      <measures id="Measure" name="Measure" column="Fact_VALUE"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

