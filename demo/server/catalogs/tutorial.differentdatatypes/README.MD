# Documentation
### CatalogName : Minimal_Cube_With_Measures_Of_Different_Datatypes
### Schema Minimal_Cube_With_Measures_Of_Different_Datatypes : 
---
### Cubes :

    CubeOneNumericMeasureDifferentDataTypes

---
#### Cube "CubeOneNumericMeasureDifferentDataTypes":

    

##### Table: "Fact"

### Cube "CubeOneNumericMeasureDifferentDataTypes" diagram:

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
c0["CubeOneNumericMeasureDifferentDataTypes"]{
M Measure1-Integer ""
M Measure1-Numeric ""
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
double VALUE "is null (100000.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact✔"{
string KEY "is null (1000000000.0) ✔"
double VALUE "is null (100000.0) ✔"
}
```
---
### Cube Matrix for Minimal_Cube_With_Measures_Of_Different_Datatypes:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube CubeOneNumericMeasureDifferentDataTypes: [0.0000, 0.0001]
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
double VALUE "is null (100000.0) ✔"
}

```
---
