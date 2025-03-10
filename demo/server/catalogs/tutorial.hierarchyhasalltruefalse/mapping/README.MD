By default, every hierarchy contains a top level called '(All)',
which contains a single member called '(All {hierarchyName})'.
This member is parent of all other members of the hierarchy, and thus represents a grand total.
It is also the default member of the hierarchy; that is, the member which is used for
calculating cell values when the hierarchy is not included on an axis or in the slicer.
The allMemberName and allLevelName attributes override the default names of the all level
and all member.

If the Hierarchy element has hasAll="false", the 'all' level is suppressed.
The default member of that dimension will now be the first member of the first level;
for example, in a Time hierarchy, it will be the first year in the hierarchy.
Changing the default member can be confusing, so you should generally use hasAll="true".



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube with Hierarchy hasAll and without hasAll" name="Minimal_Cube_with_cube_with_Hierarchy_hasAll_and_without_hasAll" cubes="Cube" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="Query" table="Fact"/>
  <roma:Level id="Level" name="Level" column="Fact_KEY"/>
  <roma:Hierarchy id="HierarchyWithHasAll" name="HierarchyWithHasAll" levels="Level" hasAll="true" primaryKey="Fact_KEY" query="Query"/>
  <roma:Hierarchy id="HierarchyWithoutHasAll" name="HierarchyWithoutHasAll" levels="Level" primaryKey="Fact_KEY" query="Query"/>
  <roma:StandardDimension id="Dimension2" name="Dimension2" hierarchies="HierarchyWithoutHasAll"/>
  <roma:StandardDimension id="Dimension1" name="Dimension1" hierarchies="HierarchyWithHasAll"/>
  <roma:PhysicalCube id="Cube" name="Cube" query="Query">
    <dimensionConnectors dimension="Dimension1" overrideDimensionName="Dimension1"/>
    <dimensionConnectors dimension="Dimension2" overrideDimensionName="Dimension2"/>
    <measureGroups>
      <measures id="Measure" name="Measure" column="Fact_VALUE"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

