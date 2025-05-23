# Documentation
### CatalogName : tutorial_for_writeback_with_wrong_writeback_attribute
### Schema tutorial_for_writeback_with_wrong_writeback_attribute : 

    
      writeback with fact as table
		
  
### Public Dimensions:

    D1

##### Dimension "D1":

Hierarchies:

    HierarchyWithHasAll

##### Hierarchy HierarchyWithHasAll:

Tables: "L1,L2"

Levels: "L1, L2"

###### Level "L1" :

    column(s): L1

###### Level "L2" :

    column(s): L2

---
### Cubes :

    C

---
#### Cube "C":

    

##### Table: "FACT"

##### Dimensions:
##### Dimension: "D1 -> D1":

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
D D1 ""
}
d00["D1"] {
H HierarchyWithHasAll ""
}
h000["HierarchyWithHasAll"] {
L L1 ""
L L2 ""
}
"c0" ||--|| "d00" : "L2-L2"
"d00" ||--|| "h000" : "L2-L1"
"d00" ||--|| "h000" : "L2-L2"
```
---
### Cube Matrix for tutorial_for_writeback_with_wrong_writeback_attribute:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube C: [0.1000, 0.0000]
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"FACT✔"{
int VAL "✔"
int VAL1 "✔"
string L2 "✔"
}
"FACTWB✔"{
int VAL "✔"
int VAL1 "✔"
string L2 "✔"
string ID "✔"
string USER "✔"
}
"L1✔"{
string L1 "✔"
string L2 "✔"
}
"L2✔"{
string L2 "✔"
}

"FACT✔" ||--o{ "L1✔" : "L2-L2"
"L1✔" ||--o{ "L2✔" : "L2-L2"
```
---
## Validation result for schema tutorial_for_writeback_with_wrong_writeback_attribute
## <span style='color: red;'>ERROR</span> : 
|Type|   |
|----|---|
|SCHEMA|WritebackAttribute dimension must be set|
|SCHEMA|WritebackTable column name must be set|
|SCHEMA|WritebackAttribute column must be set|
|SCHEMA|Dimension with name D1 doesn't have foreign key L22 in dimension D1 in cube with name C for write back|
|SCHEMA|Dimension with name D11 absent in cube with name C for write back|
|DATABASE|Column L22 does not exist in table FACTWB|
## <span style='color: blue;'>WARNING</span> : 
|Type|   |
|----|---|
|DATABASE|Table: Schema must be set|
