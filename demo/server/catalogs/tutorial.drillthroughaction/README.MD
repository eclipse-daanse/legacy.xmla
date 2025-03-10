# Documentation
### CatalogName : Minimal_Cube_with_DrillThroughAction
### Schema Minimal_Cube_with_DrillThroughAction : 
---
### Cubes :

    Cube

---
#### Cube "Cube":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "Dimension1":

Hierarchies:

    Hierarchy1

##### Hierarchy Hierarchy1:

Tables: "HX_L2,H1_L1"

Levels: "H1_Level1, H1_Level2"

###### Level "H1_Level1" :

    column(s): KEY

###### Level "H1_Level2" :

    column(s): KEY

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
D Dimension1 ""
}
d00["Dimension1"] {
H Hierarchy1 ""
}
h000["Hierarchy1"] {
L H1_Level1 ""
L H1_Level2 ""
}
"c0" ||--|| "d00" : "KEY-KEY"
"d00" ||--|| "h000" : "KEY-KEY"
"d00" ||--|| "h000" : "KEY-KEY"
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"Fact❌"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
Varchar KEY "is null ❌"
}
"H1_L1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"H2_L1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"HX_L2✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int H1L1_KEY "is null (32.0) ✔"
int H2L1_KEY "is null (32.0) ✔"
}

"Fact❌" ||--o{ "HX_L2✔" : "KEY-KEY"
"HX_L2✔" ||--o{ "H1_L1✔" : "H1L1_KEY-KEY"
```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact❌"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
Varchar KEY "is null ❌"
}
```
---
### Cube Matrix for Minimal_Cube_with_DrillThroughAction:
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
"Fact❌"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
Varchar KEY "is null ❌"
}
"H1_L1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"H2_L1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"HX_L2✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int H1L1_KEY "is null (32.0) ✔"
int H2L1_KEY "is null (32.0) ✔"
}

"Fact❌" ||--o{ "HX_L2✔" : "KEY-KEY"
"HX_L2✔" ||--o{ "H1_L1✔" : "H1L1_KEY-KEY"
```
---
