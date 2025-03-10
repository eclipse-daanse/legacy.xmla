#Introduction into DataBase

The foundation of all data analysis is the data to be analyzed.
In this case, it is stored in a relational database.


##Catalog

The catalog is the primary object in a Daanse server. It serves as a logical grouping to separate different topics in the context of data storage and analysis. The catalog references a single databaseSchema.


```xmi
<roma:Catalog  id="_cat" name="Database - Intro" dbschemas="#_dbschema"/>

```

##Schema

A catalog can be structured into databaseSchemas to logically group tables. The same table name can exist in multiple schemas.

In Daanse, we represent the database schema, where the data is stored, as a DatabaseSchema element.


```xmi
<roma:DatabaseSchema  id="_dbschema"/>

```

##Table

A schema contains the tables within a database. There are multiple types of tables, which we will explain in further tutorials.

The most common type we use is the physical table. The tables defined in a schema do not have to match all tables in the actual database — only those that should be visible for the server. This provides an additional layer of security.


```xmi
<roma:PhysicalTable  id="_tab" name="TableOne"/>

```

##Column

A table consists of columns, each with a name and a data type.

The columns defined in a table do not have to match all columns in the actual database — only those that should be visible for the server. This provides an additional layer of security.


```xmi
<roma:Column  id="_col" name="ColumnOne" type="VARCHAR"/>

```


## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog id="_cat" name="Database - Intro" dbschemas="_dbschema"/>
  <roma:DatabaseSchema id="_dbschema">
    <tables xsi:type="roma:PhysicalTable" id="_tab" name="TableOne">
      <columns id="_col" name="ColumnOne" type="VARCHAR"/>
    </tables>
  </roma:DatabaseSchema>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [TableOne](./data/TableOne.csv)

