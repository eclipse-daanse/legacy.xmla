# Documentation
### CatalogName : Minimal_Cube_with_cube_dimension_with_functional_dependency_optimizations
### Schema Minimal_Cube_with_cube_dimension_with_functional_dependency_optimizations : 
---
### Cubes :

    Cube

---
#### Cube "Cube":

    

##### Table: "AUTOMOTIVE_DIM"

##### Dimensions:
##### Dimension "Automotive":

Hierarchies:

    Hierarchy0

##### Hierarchy Hierarchy0:

Tables: "AUTOMOTIVE_DIM"

Levels: "Make, Model, ManufacturingPlant, Vehicle Identification Number, LicensePlateNum"

###### Level "Make" :

    column(s): MAKE_ID

###### Level "Model" :

    column(s): MODEL_ID

###### Level "ManufacturingPlant" :

    column(s): PLANT_ID

###### Level "Vehicle Identification Number" :

    column(s): VEHICLE_ID

###### Level "LicensePlateNum" :

    column(s): VEHICLE_ID

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
M Measure ""
D Automotive ""
}
d00["Automotive"] {
H _ ""
}
h000["_"] {
L Make ""
L Model ""
L ManufacturingPlant ""
L Vehicle_Identification_Number ""
L LicensePlateNum ""
}
"c0" ||--|| "d00" : "AUTO_DIM_ID"
"d00" ||--|| "h000" : "AUTO_DIM_ID-MAKE_ID"
"d00" ||--|| "h000" : "AUTO_DIM_ID-MODEL_ID"
"d00" ||--|| "h000" : "AUTO_DIM_ID-PLANT_ID"
"d00" ||--|| "h000" : "AUTO_DIM_ID-VEHICLE_ID"
"d00" ||--|| "h000" : "AUTO_DIM_ID-VEHICLE_ID"
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"AUTOMOTIVE_DIM✔"{
int AUTO_DIM_ID "is null (32.0) ✔"
int MAKE_ID "is null (32.0) ✔"
string MAKE "is null (100.0) ✔"
int MODEL_ID "is null (32.0) ✔"
string MODEL "is null (100.0) ✔"
int PLANT_ID "is null (32.0) ✔"
string PLANT "is null (100.0) ✔"
int PLANT_STATE_ID "is null (32.0) ✔"
int PLANT_CITY_ID "is null (32.0) ✔"
int VEHICLE_ID "is null (32.0) ✔"
int COLOR_ID "is null (32.0) ✔"
int TRIM_ID "is null (32.0) ✔"
int LICENSE_ID "is null (32.0) ✔"
string LICENSE "is null (100.0) ✔"
int LICENSE_STATE_ID "is null (32.0) ✔"
int PRICE "is null (32.0) ✔"
}

```
---
" Aggregation section:

---
```mermaid
   erDiagram
"AUTOMOTIVE_DIM✔"{
int AUTO_DIM_ID "is null (32.0) ✔"
int MAKE_ID "is null (32.0) ✔"
string MAKE "is null (100.0) ✔"
int MODEL_ID "is null (32.0) ✔"
string MODEL "is null (100.0) ✔"
int PLANT_ID "is null (32.0) ✔"
string PLANT "is null (100.0) ✔"
int PLANT_STATE_ID "is null (32.0) ✔"
int PLANT_CITY_ID "is null (32.0) ✔"
int VEHICLE_ID "is null (32.0) ✔"
int COLOR_ID "is null (32.0) ✔"
int TRIM_ID "is null (32.0) ✔"
int LICENSE_ID "is null (32.0) ✔"
string LICENSE "is null (100.0) ✔"
int LICENSE_STATE_ID "is null (32.0) ✔"
int PRICE "is null (32.0) ✔"
}
```
---
### Cube Matrix for Minimal_Cube_with_cube_dimension_with_functional_dependency_optimizations:
```mermaid
quadrantChart
title Cube Matrix
x-axis small level number --> high level number
y-axis Low row count --> High row count
quadrant-1 Complex
quadrant-2 Deep
quadrant-3 Simple
quadrant-4 Wide

Cube Cube: [0.2500, 0.0001]
```
---
### Database :
---
```mermaid
---
title: Diagram;
---
erDiagram
"AUTOMOTIVE_DIM✔"{
int AUTO_DIM_ID "is null (32.0) ✔"
int MAKE_ID "is null (32.0) ✔"
string MAKE "is null (100.0) ✔"
int MODEL_ID "is null (32.0) ✔"
string MODEL "is null (100.0) ✔"
int PLANT_ID "is null (32.0) ✔"
string PLANT "is null (100.0) ✔"
int PLANT_STATE_ID "is null (32.0) ✔"
int PLANT_CITY_ID "is null (32.0) ✔"
int VEHICLE_ID "is null (32.0) ✔"
int COLOR_ID "is null (32.0) ✔"
int TRIM_ID "is null (32.0) ✔"
int LICENSE_ID "is null (32.0) ✔"
string LICENSE "is null (100.0) ✔"
int LICENSE_STATE_ID "is null (32.0) ✔"
int PRICE "is null (32.0) ✔"
}

```
---
