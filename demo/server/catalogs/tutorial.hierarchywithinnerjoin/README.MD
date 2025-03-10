# Documentation
### CatalogName : Cube_with_hierarchy_with_table_reference_with_inner_join
### Schema Cube_with_hierarchy_with_table_reference_with_inner_join : 
---
### Cubes :

    Cube

---
#### Cube "Cube":

    

##### Table: "Fact"

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
M Measure1 ""
}
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
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
"Level_1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Level_2✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L1_KEY "is null (32.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact✔"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
```
---
### Cube Matrix for Cube_with_hierarchy_with_table_reference_with_inner_join:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube Cube: [0.0000, 0.0001]
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
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
"Level_1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Level_2✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L1_KEY "is null (32.0) ✔"
}

```
---
