Cube with multiple hierarchy



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube with multiple hierarchies" name="Minimal_Cube_with_cube_multiple_hierarchy" cubes="CubeMultipleHierarchy" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_DIM_KEY" name="DIM_KEY" type="INTEGER"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="H1_L1" name="H1_L1"/>
    <tables xsi:type="roma:PhysicalTable" id="H2_L1" name="H2_L1">
      <columns id="H1_L1_KEY" name="KEY" type="INTEGER"/>
      <columns id="H1_L1_NAME" name="NAME" type="VARCHAR"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="HX_L2" name="HX_L2">
      <columns id="HX_L2_KEY" name="KEY" type="INTEGER"/>
      <columns id="HX_L2_NAME" name="NAME" type="VARCHAR"/>
      <columns id="HX_L2_H1L1_KEY" name="H1L1_KEY" type="INTEGER"/>
      <columns id="HX_L2_H2L1_KEY" name="H2L1_KEY" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:Column id="H2_L1_NAME" name="NAME" type="VARCHAR"/>
  <roma:Column id="H2_L1_KEY" name="KEY" type="INTEGER"/>
  <roma:TableQuery id="H1L1Query" table="H1_L1"/>
  <roma:TableQuery id="HxL2Query" table="HX_L2"/>
  <roma:TableQuery id="H2L1Query" table="H2_L1"/>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:JoinQuery id="joinQuery2">
    <left key="HX_L2_H2L1_KEY" query="HxL2Query"/>
    <right key="H2_L1_KEY" query="H2L1Query"/>
  </roma:JoinQuery>
  <roma:JoinQuery id="joinQuery1">
    <left key="HX_L2_KEY" query="HxL2Query"/>
    <right key="H1_L1_KEY" query="H1L1Query"/>
  </roma:JoinQuery>
  <roma:Level id="H2_Level1" name="H2_Level1" column="H2_L1_KEY" nameColumn="H2_L1_NAME" table="H2_L1"/>
  <roma:Level id="H2_Level2" name="H2_Level2" column="HX_L2_KEY" nameColumn="HX_L2_NAME" table="HX_L2"/>
  <roma:Level id="H1_Level2" name="H1_Level2" column="HX_L2_KEY" nameColumn="HX_L2_NAME" table="HX_L2"/>
  <roma:Level id="H1_Level1" name="H1_Level1" column="H1_L1_KEY" nameColumn="H1_L1_NAME" table="H1_L1"/>
  <roma:Hierarchy id="Hierarchy2" name="Hierarchy2" levels="H2_Level1 H2_Level2" hasAll="true" primaryKey="HX_L2_KEY" primaryKeyTable="HX_L2" query="joinQuery2"/>
  <roma:Hierarchy id="Hierarchy1" name="Hierarchy1" levels="H1_Level1 H1_Level2" hasAll="true" primaryKey="HX_L2_KEY" primaryKeyTable="HX_L2" query="joinQuery1"/>
  <roma:StandardDimension id="Dimension1" name="Dimension1" hierarchies="Hierarchy1 Hierarchy2"/>
  <roma:PhysicalCube id="CubeMultipleHierarchy" name="CubeMultipleHierarchy" query="FactQuery">
    <dimensionConnectors foreignKey="Fact_DIM_KEY" dimension="Dimension1" overrideDimensionName="Dimension1"/>
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

