# Autonomous Finch GPS Navigator

A self-driving GPS system for a physical Finch robot. You pick a destination on the digital map, the app finds the shortest route, and the robot drives itself there.

## What it does

* Interactive map UI where you click a destination like Hospital or School
* Shortest-path routing with Dijkstra's algorithm
* Autonomous driving with infrared sensors to stay on the line
* Live map animation that follows the robot's movement
* Curve vs straightaway speed handling
* Failsafe stop if the robot leaves the track for too long

## How it works

1. The track is modeled as a weighted graph in code.
2. When you click a destination, Dijkstra finds the shortest path.
3. The Finch uses a dual IR sensor loop to stay centered on the white line.
4. The map UI updates while the robot moves, including slower motion on curves.

## Tech stack

* Java
* Finch Robot (BirdBrain Technologies)
* Java Swing and Graphics2D
* Graph theory, multithreading, event-driven UI, hardware/software integration

## How to run

From the project directory (files are in the repo root):

```bash
# Windows
del *.class
javac *.java
java FinchTest
```

## Using the interface

1. Calibrate the light sensors for grass, black road, and white line.
2. Choose the robot's current facing direction from the dropdown.
3. Click a red station on the map.
4. Watch the path calculation and autonomous drive.
