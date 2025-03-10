#Introduction into Columns

The data of a table are seperated into columns. Each column that should be used, must explicitly defines. Columns that are not relevant for the analysis can be left out.


##Common column fields

A Column has the fields `id,` `name`, `description` and `nullable`. Id is used to reference the column. Name is the name of the Column in the Database-table. Description is a human-readable textual description of the column. Nullable is an boolean flag, that indicates if the column can contain null values.


```xmi
<roma:Column  id="_col1" description="Non nullable Column with description" name="ColumnWithDescription" nullable="true"/>

```

##Types of Columns

In database tables, columns can have various data types to store data efficiently. These types come with additional attributes that provide more detailed descriptions and constraints.

###Varchar

The ColumnSize field in represents the maximum width or precision of a column, depending on its data type. It provides essential information about how much data a column can store


```xmi
<roma:Column  id="_col2" name="ColumnVarchar" type="VARCHAR" columnSize="255"/>

```

###Decimal

DECIMAL type represents Fixed-point number with exact precision (p) and scale (s), used for precise calculations (e.g., financial data).

relevant attributes are:

The DecimalDigits field represents the number of digits to the right of the decimal point for numeric columns in a database. It is typically used for DECIMAL, NUMERIC, FLOAT, REAL, and DOUBLE data types.

The NumPrecRadix field represents the numeric precision radix (or base) used for numeric data types. It indicates whether the precision (COLUMN_SIZE) of a column is based on base 10 (decimal) or base 2 (binary).


```xmi
<roma:Column  id="_col3" name="ColumnDecimal" type="DECIMAL" decimalDigits="2" numPrecRadix="3"/>

```

###Numeric

Numeric are the same as DECIMAL, but some databases treat it as stricter in enforcing precision.

```xmi
<roma:Column  id="_col3" name="ColumnNumeric" type="NUMERIC"/>

```

###Float

Float Approximate floating-point number, implementation-dependent precision, can introduce rounding errors.

```xmi
<roma:Column  id="_col4" name="ColumnFloat" type="FLOAT"/>

```

###Real

REAL are Single-precision (32-bit) floating-point number, less precise than DOUBLE.

```xmi
<roma:Column  id="_col5" name="ColumnReal" type="REAL"/>

```

###Double

DOUBLE (or DOUBLE PRECISION) – Double-precision (64-bit) floating-point number, more accurate than REAL.

```xmi
<roma:Column  id="_col6" name="ColumnDouble" type="DOUBLE"/>

```

###Integer

INTEGER are the whole number type, typically 32-bit, used for exact, non-decimal values.

```xmi
<roma:Column  id="_col7" name="ColumnIneger" type="INTEGER"/>

```


## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog id="_cat" name="Database - ColumnTypes" dbschemas="_dbschema"/>
  <roma:DatabaseSchema id="_dbschema">
    <tables xsi:type="roma:PhysicalTable" id="_tab" name="TableWithColumnTypes">
      <columns id="_col1" description="Non nullable Column with description" name="ColumnWithDescription" nullable="true"/>
      <columns id="_col2" name="ColumnVarchar" type="VARCHAR" columnSize="255"/>
      <columns id="_col3" name="ColumnDecimal" type="DECIMAL" decimalDigits="2" numPrecRadix="3"/>
      <columns id="_col3" name="ColumnNumeric" type="NUMERIC"/>
      <columns id="_col4" name="ColumnFloat" type="FLOAT"/>
      <columns id="_col5" name="ColumnReal" type="REAL"/>
      <columns id="_col6" name="ColumnDouble" type="DOUBLE"/>
      <columns id="_col7" name="ColumnIneger" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

