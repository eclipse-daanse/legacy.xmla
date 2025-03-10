#Tables and TableTypes

            ther are several Tables Types. The most common are PhysicalTable, ViewTable, and SystemTable. All of this, and thre upcoming table typed can be used to build cubes on.


##Physical Table

            Physical Tables are the most common Tables.  They are used to store data in the Database.


```xmi
<roma:PhysicalTable  id="_tab" name="TableOne"/>

```

##View Table

            View Tables ate Database views. Virtual tables that are the result of a query inside the underlaying Database. They are used to simplify complex queries and to hide the complexity.


```xmi
<roma:ViewTable  id="_tab" name="ViewOne"/>

```

##System Table

            Sytsem Tables are are used and managed by the underlaying Database Management System. They are used to store metadata and other information about the Database itsselfe. Please be carefill when using System Tables, this may cause secutiry issues.


```xmi
<roma:SystemTable  id="_tab" name="TableOne"/>

```


## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog id="_cat" name="Database - Table" dbschemas="_dbschema"/>
  <roma:DatabaseSchema id="_dbschema">
    <tables xsi:type="roma:PhysicalTable" id="_tab" name="TableOne">
      <columns id="_col" name="ColumnOne" type="VARCHAR"/>
    </tables>
    <tables xsi:type="roma:ViewTable" id="_tab" name="ViewOne">
      <columns id="_col" name="ColumnOne" type="VARCHAR"/>
    </tables>
    <tables xsi:type="roma:SystemTable" id="_tab" name="TableOne">
      <columns id="_col" name="ColumnOne" type="VARCHAR"/>
    </tables>
  </roma:DatabaseSchema>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [TableOne](./data/TableOne.csv)

