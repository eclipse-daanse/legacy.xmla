# Documentation
### CatalogName : Minimal_Virtual_Cubes_With_Measures
### Schema Minimal_Virtual_Cubes_With_Measures : 
---
### Cubes :

    Cube1, Cube2, VirtualCubeMeasureOnly

---
#### Cube "Cube1":

    

##### Table: "C1_Fact"

---
#### Cube "Cube2":

    

##### Table: "C2_Fact"

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
M C1-Measure-Sum ""
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
"C1_Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
}
"C2_Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"C1_Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
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
M C2-Measure-Sum ""
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
"C1_Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
}
"C2_Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"C2_Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
}
```
---
### Virtual Cube "VirtualCubeMeasureOnly" diagram:

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
c2["VirtualCubeMeasureOnly"]{
M Cube1_C1-Measure-Sum ""
M Cube2_C2-Measure-Sum ""
CM Calculation1 ""
}
```
---
### Cube Matrix for Minimal_Virtual_Cubes_With_Measures:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube Cube1: [0.0000, 0.0001]
Cube Cube2: [0.0000, 0.0001]
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"C1_Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
}
"C2_Fact✔"{
string KEY "is null (1000000000.0) ✔"
int VALUE "is null (32.0) ✔"
}

```
---
