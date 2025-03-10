


## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube with level parent child with closure" name="Minimal_Parent_Child_With_Closure" cubes="CubeMultipleHierarchy" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_DIM_KEY" name="DIM_KEY" type="INTEGER"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="Closure" name="Closure">
      <columns id="Closure_PARENT_KEY" name="PARENT_KEY" type="INTEGER"/>
      <columns id="Closure_CHILD_KEY" name="CHILD_KEY" type="INTEGER"/>
      <columns id="Closure_DIST" name="DIST" type="INTEGER"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="Hierarchy" name="Hierarchy">
      <columns id="Hierarchy_KEY" name="KEY" type="INTEGER"/>
      <columns id="Hierarchy_NAME" name="KEY" type="INTEGER"/>
      <columns id="Hierarchy_PARENT_KEY" name="PARENT_KEY" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="HierarchyQuery" table="Hierarchy"/>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:Level id="Level" name="Level" column="Hierarchy_KEY" nameColumn="Hierarchy_NAME" parentColumn="Hierarchy_PARENT_KEY" table="Hierarchy" columnType="Integer" uniqueMembers="true">
    <parentChildLink childColumn="Closure_CHILD_KEY" parentColumn="Closure_PARENT_KEY">
      <table id="ClosureQuery" table="Closure"/>
    </parentChildLink>
  </roma:Level>
  <roma:Hierarchy id="Hierarchy1" name="Hierarchy1" levels="Level" hasAll="true" primaryKey="Hierarchy_KEY" primaryKeyTable="Hierarchy" query="HierarchyQuery"/>
  <roma:StandardDimension id="Dimension1" name="Dimension1" hierarchies="Hierarchy1"/>
  <roma:PhysicalCube id="CubeMultipleHierarchy" name="CubeMultipleHierarchy" query="FactQuery">
    <dimensionConnectors foreignKey="Fact_DIM_KEY" dimension="Dimension1" overrideDimensionName="Dimension1" level="Level"/>
    <measureGroups>
      <measures id="Measure" name="Measure" column="Fact_VALUE"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Closure](./data/Closure.csv)

- [Fact](./data/Fact.csv)

- [Hierarchy](./data/Hierarchy.csv)

