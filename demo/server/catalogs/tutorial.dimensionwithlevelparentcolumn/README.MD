# Documentation
### CatalogName : Minimal_Parent_Child_Hierarchy
### Schema Minimal_Parent_Child_Hierarchy : 
---
### Cubes :

    CubeParentChildOneTopMember

---
#### Cube "CubeParentChildOneTopMember":

    

##### Table: "Fact"

##### Dimensions:
##### Dimension "Dimension1":

Hierarchies:

    Hierarchy1

##### Hierarchy Hierarchy1:

Tables: "Hier_One_Top_Member"

Levels: "Level"

###### Level "Level" :

    column(s): KEY

### Cube "CubeParentChildOneTopMember" diagram:

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
c0["CubeParentChildOneTopMember"]{
M Measure1 ""
D Dimension1 ""
}
d00["Dimension1"] {
H Hierarchy1 ""
}
h000["Hierarchy1"] {
L Level ""
}
"c0" ||--|| "d00" : "DIM_KEY-KEY"
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
"Hier_One_Top_Member✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int PARENT_KEY "is null (32.0) ✔"
}

"Fact✔" ||--o{ "Hier_One_Top_Member✔" : "DIM_KEY-KEY"
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
### Cube Matrix for Minimal_Parent_Child_Hierarchy:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube CubeParentChildOneTopMember: [0.0500, 0.0001]
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
"Hier_One_Top_Member✔"{
int KEY "is null (32.0) ✔"
string NAME "is null (1000000000.0) ✔"
int PARENT_KEY "is null (32.0) ✔"
}

"Fact✔" ||--o{ "Hier_One_Top_Member✔" : "DIM_KEY-KEY"
```
---
