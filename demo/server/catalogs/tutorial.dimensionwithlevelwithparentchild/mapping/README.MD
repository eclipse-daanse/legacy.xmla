    A basic OLAP schema with a level

    Data cubes (<Cube>) are defined in an OLAP schema (<Schema>). Within the schema the name of each data cube must be unique.
    This example schema contains one cube named "Cube".

A closure table is a SQL table which contains a record for every employee/supervisor relationship,
regardless of depth. (In mathematical terms, this is called the 'reflexive transitive closure'
of the employee/supervisor relationship.
The distance column is not strictly required, but it makes it easier to populate the table.)



## Definition

This files represent the complete definition of the catalog.

```xmi
<?xml version="1.0" encoding="ASCII"?>
<xmi:XMI xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:roma="https://www.daanse.org/spec/org.eclipse.daanse.rolap.mapping">
  <roma:Catalog description="Schema of a minimal cube with cube dimension level with closure table" name="Minimal_Cube_with_cube_dimension_level_with_closure_table" cubes="Cube" dbschemas="databaseSchema"/>
  <roma:DatabaseSchema id="databaseSchema">
    <tables xsi:type="roma:PhysicalTable" id="Fact" name="Fact">
      <columns id="Fact_employee_id" name="employee_id" type="INTEGER"/>
      <columns id="Fact_full_name" name="full_name" type="VARCHAR"/>
      <columns id="Fact_supervisor_id" name="supervisor_id" type="INTEGER"/>
      <columns id="Fact_marital_status" name="marital_status" type="VARCHAR"/>
      <columns id="Fact_position_title" name="position_title" type="VARCHAR"/>
      <columns id="Fact_gender" name="gender" type="VARCHAR"/>
      <columns id="Fact_salary" name="salary" type="INTEGER"/>
      <columns id="Fact_education_level" name="education_level" type="INTEGER"/>
      <columns id="Fact_management_role" name="management_role" type="VARCHAR"/>
    </tables>
    <tables xsi:type="roma:PhysicalTable" id="employee_closure" name="employee_closure">
      <columns id="employee_closure_supervisor_id" name="supervisor_id" type="INTEGER"/>
      <columns id="employee_closure_employee_id" name="employee_id" type="INTEGER"/>
      <columns id="employee_closure_distance" name="distance" type="INTEGER"/>
    </tables>
  </roma:DatabaseSchema>
  <roma:TableQuery id="FactQuery" table="Fact"/>
  <roma:Level id="EmployeeIdLevel" name="Employee Id" column="Fact_employee_id" nameColumn="Fact_full_name" nullParentValue="0" parentColumn="Fact_supervisor_id" columnType="Numeric" uniqueMembers="true">
    <parentChildLink childColumn="employee_closure_employee_id" parentColumn="employee_closure_supervisor_id">
      <table id="EmployeeClosureQuery" table="employee_closure"/>
    </parentChildLink>
    <memberProperties id="MaritalStatus" name="Marital Status" column="Fact_marital_status"/>
    <memberProperties id="PositionTitle" name="Position Title" column="Fact_position_title"/>
    <memberProperties id="Gender" name="Gender" column="Fact_gender"/>
    <memberProperties id="Salary" name="Salary" column="Fact_salary"/>
    <memberProperties id="EducationLevel" name="Education Level" column="Fact_education_level"/>
    <memberProperties id="ManagementRole" name="Management Role" column="Fact_management_role"/>
  </roma:Level>
  <roma:Hierarchy id="hierarchy" levels="EmployeeIdLevel" allMemberName="All Employees" hasAll="true" primaryKey="Fact_employee_id" query="FactQuery"/>
  <roma:StandardDimension id="Employees" name="Employees" hierarchies="hierarchy"/>
  <roma:PhysicalCube id="Cube" name="Cube" query="FactQuery">
    <dimensionConnectors foreignKey="Fact_employee_id" dimension="Employees" overrideDimensionName="Employees"/>
    <measureGroups>
      <measures id="Measure" name="Measure" column="Fact_salary"/>
    </measureGroups>
  </roma:PhysicalCube>
</xmi:XMI>

```
## csv data


This files represent the data in the tables.

- [Fact](./data/Fact.csv)

- [employee_closure](./data/employee_closure.csv)

