# Documentation
### CatalogName : Minimal_Cubes_With_Measures
### Schema Minimal_Cubes_With_Measures : 
---
### Cubes :

    CubeTwoMeasures

---
#### Cube "CubeTwoMeasures":

    

##### Table: "Fact"

### Cube "CubeTwoMeasures" diagram:

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
c0["CubeTwoMeasures"]{
M Measure1 ""
M Measure2 ""
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
string KEY "is null (1000000000.0) ✔"
int VALUE1 "is null (32.0) ✔"
int VALUE2 "is null (32.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE1 "is null (32.0) ✔"
int VALUE2 "is null (32.0) ✔"
}
```
---
### Cube Matrix for Minimal_Cubes_With_Measures:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube CubeTwoMeasures: [0.0000, 0.0001]
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
int VALUE1 "is null (32.0) ✔"
int VALUE2 "is null (32.0) ✔"
}

```
---
