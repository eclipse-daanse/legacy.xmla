# Documentation
### CatalogName : Minimal_Cube_with_cube_dimension_level_with_closure_table
### Schema Minimal_Cube_with_cube_dimension_level_with_closure_table : 
---
### Cubes :

    Cube

---
#### Cube "Cube":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "Employees":

Hierarchies:

    Hierarchy0

##### Hierarchy Hierarchy0:

Tables: "Fact"

Levels: "Employee Id"

###### Level "Employee Id" :

    column(s): employee_id

### Cube "Cube" diagram:

---

```mermaid
%%{init: {
"theme": "default",
"themeCSS": [
    ".er.entityBox {stroke: black;}",
    ".er.attributeBoxEven {stroke: black;}",
    ".er.attributeBoxOdd {stroke: black;}",
    "[id^=entity-c] .er.entityBox { fill: lightgreen;} ",
    "[id^=entity-d] .er.entityBox { fill: powderblue;} ",
    "[id^=entity-h] .er.entityBox { fill: pink;} "
]
}}%%
erDiagram
c0["Cube"]{
M Measure ""
D Employees ""
}
d00["Employees"] {
H _ ""
}
h000["_"] {
L Employee_Id ""
}
"c0" ||--|| "d00" : "employee_id-employee_id"
"d00" ||--|| "h000" : "employee_id-employee_id"
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"Fact✔"{
int employee_id "is null (32.0) ✔"
string full_name "is null (1000000000.0) ✔"
int supervisor_id "is null (32.0) ✔"
string marital_status "is null (1000000000.0) ✔"
string position_title "is null (1000000000.0) ✔"
string gender "is null (1000000000.0) ✔"
int salary "is null (32.0) ✔"
int education_level "is null (32.0) ✔"
string management_role "is null (1000000000.0) ✔"
}
"employee_closure✔"{
int supervisor_id "is null (32.0) ✔"
int employee_id "is null (32.0) ✔"
int distance "is null (32.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact✔"{
int employee_id "is null (32.0) ✔"
string full_name "is null (1000000000.0) ✔"
int supervisor_id "is null (32.0) ✔"
string marital_status "is null (1000000000.0) ✔"
string position_title "is null (1000000000.0) ✔"
string gender "is null (1000000000.0) ✔"
int salary "is null (32.0) ✔"
int education_level "is null (32.0) ✔"
string management_role "is null (1000000000.0) ✔"
}
```
---
### Cube Matrix for Minimal_Cube_with_cube_dimension_level_with_closure_table:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube Cube: [0.0500, 0.0001]
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"Fact✔"{
int employee_id "is null (32.0) ✔"
string full_name "is null (1000000000.0) ✔"
int supervisor_id "is null (32.0) ✔"
string marital_status "is null (1000000000.0) ✔"
string position_title "is null (1000000000.0) ✔"
string gender "is null (1000000000.0) ✔"
int salary "is null (32.0) ✔"
int education_level "is null (32.0) ✔"
string management_role "is null (1000000000.0) ✔"
}
"employee_closure✔"{
int supervisor_id "is null (32.0) ✔"
int employee_id "is null (32.0) ✔"
int distance "is null (32.0) ✔"
}

```
---
