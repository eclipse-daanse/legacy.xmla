#Introduction into DataBase Schemas

The purpose of database schemas is to organize tables into logical groups.

In the Daanse server, schemas can be referenced in the catalog. None or only a subset of the existing database schemas may be referenced. This reference is only required if the Daanse server needs to expose the schemas and their tables via an API, such as XMLA.


##Schema without name

The name of a database schema is optional and can be left empty. In such cases, the server will query the default schema of the underlying database.


```xmi
<roma:DatabaseSchema  id="_dbschema2"/>

```

##Schema and attrebutes

It is more secure to explicitly define the schema using the `name` attribute. To provide a clearer description of the schema's contents, you can use the `description` attribute.


```xmi
<roma:DatabaseSchema  id="_dbschema1" description="theDescription" name="foo"/>

```

Schema can be refernced in the catalog. You can see this by checking the `dbSchema attribute` in the catalog.

```xmi
<roma:Catalog  id="_cat" name="Database - Schema" dbschemas="#_dbschema2 #_dbschema1"/>

```


## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog id="_cat" name="Database - Schema" dbschemas="_dbschema2 _dbschema1"/>
  <roma:DatabaseSchema id="_dbschema1" description="theDescription" name="foo">
    <tables xsi:type="roma:PhysicalTable" id="_tab1" name="theTable">
      <columns id="_colt1c1" name="theColumn" type="VARCHAR"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:DatabaseSchema id="_dbschema2">
    <tables xsi:type="roma:PhysicalTable" id="_tab2" name="theTable">
      <columns id="_colt2c1" name="theColumn" type="VARCHAR"/>
    </tables>
  </roma:DatabaseSchema>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [theTable](./data/theTable.csv)

- [theTable](./data/theTable.csv)

- [theTable](./data/theTable.csv)

