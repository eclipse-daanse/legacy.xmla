# Documentation
### CatalogName : Cube_with_virtual_cube_with_kpi
### Schema Cube_with_virtual_cube_with_kpi : 
---
### Cubes :

    Cube1, Cube2, Cube1Cube2Kpi

---
#### Cube "Cube1":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "Cube1Dimension1":

Hierarchies:

    HierarchyWithoutHasAll

##### Hierarchy HierarchyWithoutHasAll:

Tables: "Fact"

Levels: "Level2"

###### Level "Level2" :

    column(s): KEY

---
#### Cube "Cube2":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "Cube2Dimension1":

Hierarchies:

    HierarchyWithoutHasAll

##### Hierarchy HierarchyWithoutHasAll:

Tables: "Fact"

Levels: "Level2"

###### Level "Level2" :

    column(s): KEY

### Cube "Cube1" diagram:

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
c0["Cube1"]{
M MeasureCube1 ""
D Cube1Dimension1 ""
}
d00["Cube1Dimension1"] {
H HierarchyWithoutHasAll ""
}
h000["HierarchyWithoutHasAll"] {
L Level2 ""
}
"c0" ||--|| "d00" : "KEY"
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
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
Integer VALUE_NUMERIC "is null ❌"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact❌"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
Integer VALUE_NUMERIC "is null ❌"
}
```
---
### Cube "Cube2" diagram:

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
c1["Cube2"]{
M MeasureCube2 ""
D Cube2Dimension1 ""
}
d10["Cube2Dimension1"] {
H HierarchyWithoutHasAll ""
}
h100["HierarchyWithoutHasAll"] {
L Level2 ""
}
"c1" ||--|| "d10" : "KEY"
"d10" ||--|| "h100" : "KEY-KEY"
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
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
Integer VALUE_NUMERIC "is null ❌"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact❌"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
Integer VALUE_NUMERIC "is null ❌"
}
```
---
### Virtual Cube "Cube1Cube2Kpi" diagram:

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
c2["Cube1Cube2Kpi"]{
M Cube1_MeasureCube1 ""
M Cube2_MeasureCube2 ""
D Cube1Dimension1 ""
D Cube2Dimension1 ""
CM CalculatedValue ""
CM CalculatedTrend ""
KPI Kpi1 "Kpi with all parameters"
}
d20["Cube1Dimension1"] {
H HierarchyWithoutHasAll ""
}
h200["HierarchyWithoutHasAll"] {
L Level2 ""
}
d21["Cube2Dimension1"] {
H HierarchyWithoutHasAll ""
}
h210["HierarchyWithoutHasAll"] {
L Level2 ""
}
"c2" ||--|| "d20" : "KEY"
"d20" ||--|| "h200" : "KEY-KEY"
"c2" ||--|| "d21" : "KEY"
"d21" ||--|| "h210" : "KEY-KEY"
```
---
### Cube Matrix for Cube_with_virtual_cube_with_kpi:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube Cube1: [0.0500, 0.0001]
Cube Cube2: [0.0500, 0.0001]
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
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
Integer VALUE_NUMERIC "is null ❌"
}

```
---
