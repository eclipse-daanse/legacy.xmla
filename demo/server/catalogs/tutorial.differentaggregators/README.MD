# Documentation
### CatalogName : 01-03_Minimal_Cube_With_One_Measure_And_Different_Aggregators
### Schema 01-03_Minimal_Cube_With_One_Measure_And_Different_Aggregators : 
---
### Cubes :

    CubeDifferentAggregators

---
#### Cube "CubeDifferentAggregators":

    

##### Table: "Fact"

### Cube "CubeDifferentAggregators" diagram:

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
c0["CubeDifferentAggregators"]{
M Measure-Sum ""
M Measure-Min ""
M Measure-Max ""
M Measure-Avg ""
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
}
```
---
### Cube Matrix for 01-03_Minimal_Cube_With_One_Measure_And_Different_Aggregators:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube CubeDifferentAggregators: [0.0000, 0.0001]
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
}

```
---
