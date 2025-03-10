# Documentation
### CatalogName : Minimal_Cubes_With_KPI_with_parent
### Schema Minimal_Cubes_With_KPI_with_parent : 
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
M Measure-Sum1 ""
M Measure-Sum2 ""
M Measure-Sum3 ""
M Measure-Sum4 ""
KPI Kpi1 "Kpi1"
KPI Kpi21 "Kpi2_1"
KPI Kpi22 "Kpi2_2"
KPI Kpi3 "Kpi3"
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
int VALUE "is null (32.0) ✔"
double VALUE_NUMERIC "is null (100000.0) ✔"
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
double VALUE_NUMERIC "is null (100000.0) ✔"
}
```
---
### Cube Matrix for Minimal_Cubes_With_KPI_with_parent:
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
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
double VALUE_NUMERIC "is null (100000.0) ✔"
}

```
---
