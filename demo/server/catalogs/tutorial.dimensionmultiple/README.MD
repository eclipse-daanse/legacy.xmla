# Documentation
### CatalogName : Minimal_Two_Dimensions
### Schema Minimal_Two_Dimensions : 
---
### Cubes :

    CubeTwoDimensions

---
#### Cube "CubeTwoDimensions":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "Dimension1":

Hierarchies:

    D1H1

##### Hierarchy D1H1:

Tables: "DIM_1"

Levels: "D1H1L1"

###### Level "D1H1L1" :

    column(s): KEY

##### Dimension "Dimension2":

Hierarchies:

    D21H1

##### Hierarchy D21H1:

Tables: "DIM_1"

Levels: "D1H1L1"

###### Level "D1H1L1" :

    column(s): KEY

### Cube "CubeTwoDimensions" diagram:

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
c0["CubeTwoDimensions"]{
M Measure ""
D Dimension1 ""
D Dimension2 ""
}
d00["Dimension1"] {
H D1H1 ""
}
h000["D1H1"] {
L D1H1L1 ""
}
d01["Dimension2"] {
H D21H1 ""
}
h010["D21H1"] {
L D1H1L1 ""
}
"c0" ||--|| "d00" : "DIM1_KEY-KEY"
"d00" ||--|| "h000" : "KEY-KEY"
"c0" ||--|| "d01" : "DIM2_KEY-KEY"
"d01" ||--|| "h010" : "KEY-KEY"
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"DIM_1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"DIM_2✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Fact✔"{
int DIM1_KEY "is null (32.0) ✔"
int DIM2_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}

"Fact✔" ||--o{ "DIM_1✔" : "DIM1_KEY-KEY"
"Fact✔" ||--o{ "DIM_1✔" : "DIM2_KEY-KEY"
```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact✔"{
int DIM1_KEY "is null (32.0) ✔"
int DIM2_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
```
---
### Cube Matrix for Minimal_Two_Dimensions:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube CubeTwoDimensions: [0.1000, 0.0001]
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"DIM_1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"DIM_2✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Fact✔"{
int DIM1_KEY "is null (32.0) ✔"
int DIM2_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}

"Fact✔" ||--o{ "DIM_1✔" : "DIM1_KEY-KEY"
"Fact✔" ||--o{ "DIM_1✔" : "DIM2_KEY-KEY"
```
---
