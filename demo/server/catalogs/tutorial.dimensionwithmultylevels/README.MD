# Documentation
### CatalogName : Minimal_Cube_with_cube_dimension_with_multy_level_with_property
### Schema Minimal_Cube_with_cube_dimension_with_multy_level_with_property : 
---
### Cubes :

    Cube

---
#### Cube "Cube":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "Dimension":

Hierarchies:

    HierarchyWithHasAll

##### Hierarchy HierarchyWithHasAll:

Tables: "Fact"

Levels: "Level1, Level2"

###### Level "Level1" :

    column(s): L1

###### Level "Level2" :

    column(s): L2

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
D Dimension ""
}
d00["Dimension"] {
H HierarchyWithHasAll ""
}
h000["HierarchyWithHasAll"] {
L Level1 ""
L Level2 ""
}
"c0" ||--|| "d00" : "KEY"
"d00" ||--|| "h000" : "KEY-L1"
"d00" ||--|| "h000" : "KEY-L2"
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
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
string L1 "is null (100.0) ✔"
int L2 "is null (32.0) ✔"
string prop1 "is null (100.0) ✔"
int prop2 "is null (32.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
string L1 "is null (100.0) ✔"
int L2 "is null (32.0) ✔"
string prop1 "is null (100.0) ✔"
int prop2 "is null (32.0) ✔"
}
```
---
### Cube Matrix for Minimal_Cube_with_cube_dimension_with_multy_level_with_property:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube Cube: [0.1000, 0.0001]
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
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
string L1 "is null (100.0) ✔"
int L2 "is null (32.0) ✔"
string prop1 "is null (100.0) ✔"
int prop2 "is null (32.0) ✔"
}

```
---
