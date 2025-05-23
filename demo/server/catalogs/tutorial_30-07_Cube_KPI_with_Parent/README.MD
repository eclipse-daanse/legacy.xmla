# Documentation
### CatalogName : tutorial_30-07_Cube_KPI_with_Parent
### Schema Minimal_Cubes_With_KPI_with_parent : 

    
		A minimal cube with Kpi with parents - children
		Kpi1 is parent for Kpi2_1, Kpi2_2
		Kpi2_1 is parent for Kpi3

		A KPI has four important properties which are value, goal, status and trend.
		Let's explain this by means of Profit Margin with the below calculation.

    Value: is the actual value of the KPI. This will be a numeric value. For example, this can be the Profit Margin.
      This might not be included in the fact table hence we may have to calculate or derive this column.

    Goal: every organization has a goal for this value. For example, the organization may look at the
      goal of achieving a five percent Profit Margin. Also, sometimes they may have different values for
      different business areas. For example, depending on the product category or sales territory,
      the sales margin goal will differ.

    Status: depending on the KPI value and the KPI goal, the KPI status can be defined.
      For an example, we can say that if the KPI value is greater than the goal it is great if it is not greater
      than the goal, but still greater than zero it is good and if less than zero or running at a loss it is bad.
      This Great, Good or Bad can be displayed to the user by means of a graphical representation such as an arrow,
      traffic lights or a gauge.

    Trend: trend is an optional parameter when defining a KPI, but still an important feature in a KPI.
      For example, you may have a great profit margin, but comparing with last year, it could be less.
      On the other hand, you might have a bad profit margin, but compared to last year it is improving.

    Weight:  The unique name of the member in the measures dimension for the KPI weight.

    StatusGraphic: The default graphical representation of the KPI status.
      (Traffic Light, Road Signs, Gauge - Ascending, Gauge - Descending, Thermometer, Cylinder, Smiley Face)

    TrendGraphic: The default graphical representation of the KPI trend.
      (Standard Arrow, Status Arrow - Ascending, Status Arrow - Descending, Smiley Face)

    DisplayFolder:  The display folder.

		
  
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
KPI Kpi1 "Kpi with time parameters"
KPI Kpi21 "Kpi2_1"
KPI Kpi22 "Kpi2_2"
KPI Kpi3 "Kpi3"
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

Cube Cube: [0.0000, 0.0000]
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
timestamp DATE_KEY "✔"
int VALUE "✔"
int YEAR_ID "✔"
string QTR_ID "✔"
string QTR_NAME "✔"
string MONTH_ID "✔"
string MONTH_NAME "✔"
}

```
---
