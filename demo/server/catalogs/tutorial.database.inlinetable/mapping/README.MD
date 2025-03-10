#Special Table -  Inline Table

The Inline Table is a special Table that is used to hold the data in the mapping. The Inline Table is not a table in the Database, but because it holds the data it can act as a Table. The Server must create at runtime Inline Statements of this Table.


##InlineTable, Row and RowValue

The InlineTable is a virtual table. The table and data does not exist in the datbase and will not be inserted. The inlinetable stores the data in the Row and RowValue elements completely in its definition. The RowValue elements are used to store the values of the columns. The Row element is used to store the values. the RowValue can store the atomic data and the reference to the columns. The InlineTable can have multiple Rows and columns. Inline tables should only be used exceptionally.


```xmi
<roma:InlineTable  id="_table" name="FACT">
  <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
  <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
  <rows>
    <rowValues column="Fact_KEY" value="A"/>
    <rowValues column="Fact_VALUE" value="100"/>
    <rowValues column="Fact_KEY" value="B"/>
    <rowValues column="Fact_VALUE" value="42"/>
  </rows>
  <rows/>
</roma:InlineTable>

```


## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog id="_cat" name="Database - InlineTable" dbschemas="_dbschema"/>
  <roma:DatabaseSchema id="_dbschema">
    <tables xsi:type="roma:InlineTable" id="_table" name="FACT">
      <columns id="Fact_KEY" name="KEY" type="VARCHAR"/>
      <columns id="Fact_VALUE" name="VALUE" type="INTEGER"/>
      <rows>
        <rowValues column="Fact_KEY" value="A"/>
        <rowValues column="Fact_VALUE" value="100"/>
        <rowValues column="Fact_KEY" value="B"/>
        <rowValues column="Fact_VALUE" value="42"/>
      </rows>
      <rows/>
    </tables>
  </roma:DatabaseSchema>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

