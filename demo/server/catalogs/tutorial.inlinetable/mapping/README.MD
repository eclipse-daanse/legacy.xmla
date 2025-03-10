A minimal cube based on an inline table

An inline table is a virtual table which is completely defined within the schema file instead of using an existing database table. Inline tables should only be used exceptionally.
The <InlineTable> element consists of 2 main components:
- the column definition (<ColumnDefs>) and
- the table data held in the table rows (<Rows>).
Within the <ColumnDefs> tag, every table column is represented by a <ColumnDef> element which defines the column name (name attribute) and  datatype (type attribute).
Each row containing the data must be defined separately by a <Row> element within the <Rows> tag of the inline table.
For each table cell within a row, a separate <Value> element must be created which contains the column name in the name attribute and the value as the element's content.

In this example schema, an inline table named "Fact" is used as the fact table of the cube. Therefore, the <InlineTable> element is used instead of the <Table> element.
It consists of the string column "KEY" and the numeric column "VALUE" (<ColumnDef> within <ColumnDefs>).
The table has one row which holds the text "A" in the "KEY" column and the value 100.5 in the "VALUE" column (<Row> & <Value> within <Rows>)
As the inline table is the only dataset the cube refers to, no external database is needed.



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:InlineTableQuery id="FactQuery" alias="Fact" table="Fact"/>
  <roma:Catalog description="Schema of a minimal cube consisting of one measurement and based on an virtual inline table" name="CubeOneMeasureInlineTable" cubes="CubeOneMeasure" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:InlineTable" id="Fact" name="Fact">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
      <rows>
        <rowValues column="Fact_KEY" value="A"/>
        <rowValues column="Fact_VALUE" value="100.5"/>
      </rows>
    </tables>
  </roma:DatabaseSchema>
  <roma:PhysicalCube id="CubeOneMeasure" name="CubeOneMeasure" query="FactQuery">
    <measureGroups>
      <measures id="Measure-Sum" name="Measure-Sum" column="Fact_VALUE"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

