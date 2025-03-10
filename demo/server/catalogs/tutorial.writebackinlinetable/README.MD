# Documentation
### CatalogName : tutorial_for_writeback_with_fact_InlineTable
### Schema tutorial_for_writeback_with_fact_InlineTable : 
---
### Cubes :

    C

---
#### Cube "C":

    

##### Table: ""

##### Dimensions:
##### Dimension "D1":

Hierarchies:

    HierarchyWithHasAll

##### Hierarchy HierarchyWithHasAll:

Tables: "L1,L2"

Levels: "L1, L2"

###### Level "L1" :

    column(s): L1

###### Level "L2" :

    column(s): L2

### Cube "C" diagram:

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
c0["C"]{
M Measure1 ""
M Measure2 ""
D D1 ""
}
d00["D1"] {
H HierarchyWithHasAll ""
}
h000["HierarchyWithHasAll"] {
L L1 ""
L L2 ""
}
"c0" ||--|| "d00" : "L2-L2"
"d00" ||--|| "h000" : "L2-L1"
"d00" ||--|| "h000" : "L2-L2"
```
---
### Cube Matrix for tutorial_for_writeback_with_fact_InlineTable:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube C: [0.1000, 0.0005]
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"FACTWB✔"{
int VAL "is null (32.0) ✔"
int VAL1 "is null (32.0) ✔"
string L2 "is null (100.0) ✔"
string ID "is null (100.0) ✔"
string USER "is null (100.0) ✔"
}
"L1✔"{
string L1 "is null (100.0) ✔"
string L2 "is null (100.0) ✔"
}
"L2✔"{
string L2 "is null (100.0) ✔"
}
"FACT❌"{
Integer VAL "is null ❌"
Integer VAL1 "is null ❌"
Varchar L2 "is null (100) ❌"
}

"FACT❌" ||--o{ "L1✔" : "L2-L2"
"L1✔" ||--o{ "L2✔" : "L2-L2"
```
---
