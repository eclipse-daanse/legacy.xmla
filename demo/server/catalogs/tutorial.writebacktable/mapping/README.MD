writeback with fact as table



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema with writeback" name="tutorial_for_writeback" cubes="C" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="FACT" name="FACT">
      <columns id="Fact_VAL" name="VAL" type="INTEGER"/>
      <columns id="Fact_VAL1" name="VAL1" type="INTEGER"/>
      <columns id="Fact_L2" name="L2" type="VARCHAR" columnSize="100"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="L1" name="L1">
      <columns id="L1_L1" name="L1" type="VARCHAR" columnSize="100"/>
      <columns id="L1_L2" name="L2" type="VARCHAR" columnSize="100"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="L2" name="L2">
      <columns id="L2_L2" name="L2" type="VARCHAR" columnSize="100"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="FACTWB" name="FACTWB">
      <columns id="Factwb_VAL" name="VAL" type="INTEGER"/>
      <columns id="Factwb_VAL1" name="VAL1" type="INTEGER"/>
      <columns id="factwb_L2" name="L2" type="VARCHAR" columnSize="100"/>
      <columns id="factwb_ID" name="ID" type="VARCHAR" columnSize="100"/>
      <columns id="factwb_USER" name="USER" type="VARCHAR" columnSize="100"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="FACT"/>
  <roma:JoinQuery id="join">
    <left key="L1_L2">
      <query xsi:type="roma:TableQuery" id="l1Query" table="L1"/>
    </left>
    <right key="L2_L2">
      <query xsi:type="roma:TableQuery" id="l2Query" table="L2"/>
    </right>
  </roma:JoinQuery>
  <roma:Level id="L2Level" name="L2" column="L2_L2" table="L2"/>
  <roma:Level id="L1Level" name="L1" column="L1_L1" table="L1"/>
  <roma:Hierarchy id="HierarchyWithHasAll" name="HierarchyWithHasAll" levels="L1Level L2Level" hasAll="true" primaryKey="L1_L2" primaryKeyTable="L1" query="join"/>
  <roma:StandardDimension id="D1" name="D1" hierarchies="HierarchyWithHasAll"/>
  <roma:PhysicalCube id="C" name="C" query="FactQuery">
    <dimensionConnectors foreignKey="L1_L2" dimension="D1" overrideDimensionName="D1" id="D1Connector"/>
    <writebackTable name="FACTWB">
      <writebackAttribute column="Fact_L2" dimensionConnector="D1Connector"/>
      <writebackMeasure column="Fact_VAL" name="Measure1"/>
      <writebackMeasure column="Fact_VAL1" name="Measure2"/>
    </writebackTable>
    <measureGroups>
      <measures id="Measure1" name="Measure1" column="Fact_VAL"/>
      <measures id="Measure2" name="Measure2" column="Fact_VAL1"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [FACT](./data/FACT.csv)

- [FACTWB](./data/FACTWB.csv)

- [L1](./data/L1.csv)

- [L2](./data/L2.csv)

