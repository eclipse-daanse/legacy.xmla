# Documentation
### CatalogName : Minimal_Single_Hierarchy_Hidden_Members_with_IfBlankName
### Schema Minimal_Single_Hierarchy_Hidden_Members_with_IfBlankName : 
---
### Cubes :

    HiddenMembersIfBlankName, HiddenMembersIfBlankName

---
#### Cube "HiddenMembersIfBlankName":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "DimensionMembersHiddenIfBlankName":

Hierarchies:

    Hierarchy1

##### Hierarchy Hierarchy1:

Tables: "Level_2_NULL,Level_1"

Levels: "Level1, Level2"

###### Level "Level1" :

    column(s): KEY

###### Level "Level2" :

    column(s): KEY

---
#### Cube "HiddenMembersIfBlankName":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "DimensionMembersHiddenIfBlankName":

Hierarchies:

    Hierarchy1

##### Hierarchy Hierarchy1:

Tables: "Level_2_NULL,Level_1"

Levels: "Level1, Level2"

###### Level "Level1" :

    column(s): KEY

###### Level "Level2" :

    column(s): KEY

### Cube "HiddenMembersIfBlankName" diagram:

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
c0["HiddenMembersIfBlankName"]{
M Measure1 ""
D DimensionMembersHiddenIfBlankName ""
}
d00["DimensionMembersHiddenIfBlankName"] {
H Hierarchy1 ""
}
h000["Hierarchy1"] {
L Level1 ""
L Level2 ""
}
"c0" ||--|| "d00" : "DIM_KEY-KEY"
"d00" ||--|| "h000" : "KEY-KEY"
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
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
"Fact_Multiple✔"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
"Level_1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Level_1_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Level_2_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L1_KEY "is null (32.0) ✔"
}
"Level_2_NULL✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L1_KEY "is null (32.0) ✔"
}
"Level_3_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L2_KEY "is null (32.0) ✔"
}

"Fact✔" ||--o{ "Level_2_NULL✔" : "DIM_KEY-KEY"
"Level_2_NULL✔" ||--o{ "Level_1✔" : "L1_KEY-KEY"
"Fact✔" ||--o{ "Level_2_NULL✔" : "DIM_KEY-KEY"
"Level_2_NULL✔" ||--o{ "Level_1✔" : "L1_KEY-KEY"
```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact✔"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
```
---
### Cube "HiddenMembersIfBlankName" diagram:

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
c1["HiddenMembersIfBlankName"]{
M Measure1 ""
D DimensionMembersHiddenIfBlankName ""
}
d10["DimensionMembersHiddenIfBlankName"] {
H Hierarchy1 ""
}
h100["Hierarchy1"] {
L Level1 ""
L Level2 ""
}
"c1" ||--|| "d10" : "DIM_KEY-KEY"
"d10" ||--|| "h100" : "KEY-KEY"
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
"Fact✔"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
"Fact_Multiple✔"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
"Level_1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Level_1_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Level_2_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L1_KEY "is null (32.0) ✔"
}
"Level_2_NULL✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L1_KEY "is null (32.0) ✔"
}
"Level_3_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L2_KEY "is null (32.0) ✔"
}

"Fact✔" ||--o{ "Level_2_NULL✔" : "DIM_KEY-KEY"
"Level_2_NULL✔" ||--o{ "Level_1✔" : "L1_KEY-KEY"
"Fact✔" ||--o{ "Level_2_NULL✔" : "DIM_KEY-KEY"
"Level_2_NULL✔" ||--o{ "Level_1✔" : "L1_KEY-KEY"
```
---
" Aggregation section:

---
```mermaid
   erDiagram
"Fact✔"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
```
---
### Cube Matrix for Minimal_Single_Hierarchy_Hidden_Members_with_IfBlankName:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube HiddenMembersIfBlankName: [0.1000, 0.0001]
Cube HiddenMembersIfBlankName: [0.1000, 0.0001]
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
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
"Fact_Multiple✔"{
int DIM_KEY "is null (32.0) ✔"
int VALUE "is null (32.0) ✔"
}
"Level_1✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Level_1_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
}
"Level_2_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L1_KEY "is null (32.0) ✔"
}
"Level_2_NULL✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L1_KEY "is null (32.0) ✔"
}
"Level_3_Multiple✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int L2_KEY "is null (32.0) ✔"
}

"Fact✔" ||--o{ "Level_2_NULL✔" : "DIM_KEY-KEY"
"Level_2_NULL✔" ||--o{ "Level_1✔" : "L1_KEY-KEY"
"Fact✔" ||--o{ "Level_2_NULL✔" : "DIM_KEY-KEY"
"Level_2_NULL✔" ||--o{ "Level_1✔" : "L1_KEY-KEY"
```
---
