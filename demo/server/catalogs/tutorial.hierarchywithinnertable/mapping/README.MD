    A basic OLAP schema with a level with reference with inner table
Share dimension Dimension1 is defined in a <Dimension> element inside <Schema> element.
Hierarchy is defined in a <Hierarchy> element inside <Dimension> element.
Hierarchy have reference to inner table element
Cube dimension Dimension1 have link on share dimension Dimension1 as source.


    Data cubes (<Cube>) are defined in an OLAP schema (<Schema>). Within the schema the name of each data cube must be unique.
    This example schema contains one cube named "Cube1".

    A cube is based on a fact table (<Table>) which refers to a database table containing one or more measurements to be aggregated (and optionally further columns defining factual dimensions).
    In this case the database table representing the fact table is named "Fact" in the database, which is adressed in the name attribute within the <Table> tag.

    Each measurement of the cube is defined in a separate <Measure> element.
    The measurement in this example cube is named "Measure" (name attribute). It corresponds to the "VALUE" column (column attribute) in the database table "Fact" and is aggregated by summation (aggregator attribute).
    Level is defined in <Level> element.
    Property is defined in <Property> element inside <Level> element. Property we can see in cell tooltip in excel


Catalog with schema with hierarchy with table reference with inner table


## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema with hierarchy with table reference with inner table" name="Cube_with_dimension_with hierarchy_with_inner_table" cubes="Cube" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_DIM_KEY" name="DIM_KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
    </tables>
    <tables xsi:type="roma:InlineTable" name="HT">
      <columns id="HT_KEY" name="KEY" type="VARCHAR"/>
      <columns id="HT_VALUE" name="VALUE" type="NUMERIC"/>
      <columns id="HT_NAME" name="NAME" type="VARCHAR"/>
      <rows>
        <rowValues column="HT_KEY" value="1"/>
        <rowValues column="HT_VALUE" value="100.5"/>
        <rowValues column="HT_NAME" value="name1"/>
      </rows>
      <rows>
        <rowValues column="HT_KEY" value="2"/>
        <rowValues column="HT_VALUE" value="100.2"/>
        <rowValues column="HT_NAME" value="name2"/>
      </rows>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery table="Fact"/>
  <roma:PhysicalCube id="Cube" name="Cube" query="/2">
    <measureGroups>
      <measures id="Measure1" name="Measure1" column="Fact_VALUE"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

