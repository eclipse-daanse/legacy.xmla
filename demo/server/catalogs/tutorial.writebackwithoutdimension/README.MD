# Documentation
### CatalogName : Writeback_without_dimension
### Schema Writeback_without_dimension : 
---
### Cubes :

    C

---
#### Cube "C":

    

##### Table: "FACT"

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
"FACT❌"{
int VAL "is null (32.0) ✔"
int VAL1 "is null (32.0) ✔"
Varchar VALUE "is null (100) ❌"
}
"FACTWB✔"{
int VAL "is null (32.0) ✔"
int VAL1 "is null (32.0) ✔"
string L2 "is null (100.0) ✔"
string ID "is null (100.0) ✔"
string USER "is null (100.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"FACT❌"{
int VAL "is null (32.0) ✔"
int VAL1 "is null (32.0) ✔"
Varchar VALUE "is null (100) ❌"
}
```
---
### Cube Matrix for Writeback_without_dimension:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube C: [0.0000, 0.0001]
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"FACT❌"{
int VAL "is null (32.0) ✔"
int VAL1 "is null (32.0) ✔"
Varchar VALUE "is null (100) ❌"
}
"FACTWB✔"{
int VAL "is null (32.0) ✔"
int VAL1 "is null (32.0) ✔"
string L2 "is null (100.0) ✔"
string ID "is null (100.0) ✔"
string USER "is null (100.0) ✔"
}

```
---
