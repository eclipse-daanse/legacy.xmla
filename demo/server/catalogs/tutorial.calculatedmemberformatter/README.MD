# Documentation
### CatalogName : Minimal_Cube_With_CalculatedMember_CellFormatter
### Schema Minimal_Cube_With_CalculatedMember_CellFormatter : 
---
### Cubes :

    CubeCalculatedMemberFormater

---
#### Cube "CubeCalculatedMemberFormater":

    

##### Table: "Fact"

### Cube "CubeCalculatedMemberFormater" diagram:

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
c0["CubeCalculatedMemberFormater"]{
M Measure1 ""
CM CM1 ""
CM CM2 ""
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
### Cube Matrix for Minimal_Cube_With_CalculatedMember_CellFormatter:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube CubeCalculatedMemberFormater: [0.0000, 0.0001]
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
