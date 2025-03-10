# Documentation
### CatalogName : Minimal_Cubes_With_Two_Calculated_Members
### Schema Minimal_Cubes_With_Two_Calculated_Members : 
---
### Cubes :

    CubeTwoMeasuresPlusCalculatedMeasures

---
#### Cube "CubeTwoMeasuresPlusCalculatedMeasures":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "Dimension1":

Hierarchies:

    Hierarchy1

##### Hierarchy Hierarchy1:

Tables: "Fact"

Levels: "Level"

###### Level "Level" :

    column(s): KEY

### Cube "CubeTwoMeasuresPlusCalculatedMeasures" diagram:

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
c0["CubeTwoMeasuresPlusCalculatedMeasures"]{
M Measure1-Sum ""
M Measure2-Count ""
D Dimension1 ""
CM CM1 ""
CM CM2 ""
}
d00["Dimension1"] {
H Hierarchy1 ""
}
h000["Hierarchy1"] {
L Level ""
}
"c0" ||--|| "d00" : "KEY-KEY"
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
### Cube Matrix for Minimal_Cubes_With_Two_Calculated_Members:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube CubeTwoMeasuresPlusCalculatedMeasures: [0.0500, 0.0001]
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
