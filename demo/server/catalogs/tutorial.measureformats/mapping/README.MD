A minimal cube with two measures formatted by different format strings

The displayed cell format of measures can be adapted by setting a formatString attribute. Further details see https://mondrian.pentaho.com/api/mondrian/util/Format.html.

In this sample cube the three measures derive from the same table column in the database, but have different expressions in the format String attribute.



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube containing the same measurement with two different formats" name="Minimal_Cube_With_Measures_Of_Different_formats" cubes="CubeOneNumericMeasureDifferentFormatStrings" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:PhysicalCube id="CubeOneNumericMeasureDifferentFormatStrings" name="CubeOneNumericMeasureDifferentFormatStrings" query="FactQuery">
    <measureGroups>
      <measures id="Measure1" name="Measure1" formatString="#,##0.00" column="Fact_VALUE"/>
      <measures id="Measure2" name="Measure2" formatString="#,##0" column="Fact_VALUE"/>
      <measures id="Measure3" name="Measure3" formatString="#,##0." column="Fact_VALUE"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

