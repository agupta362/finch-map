Autonomous Finch GPS Navigator
What is this?
This is a custom, self-driving GPS system built for a physical Finch robot. Give the robot a destination on the interactive digital map, and it will automatically calculate the shortest route and physically drive itself there.

Key Features
Interactive Map: A custom digital interface where you can click a destination (like the Hospital or School), and the robot will route itself there.

Smart Pathfinding: Automatically calculates the absolute shortest path across the map's grid using Dijkstra's Algorithm.

Autonomous Driving: Uses real-time infrared sensor data to physically steer the robot, keep it centered on the line, and execute precise turns at intersections.

Live Sync: The digital map animates exactly as the physical robot moves, using custom physics timers to slow down on curves and speed up on straightaways.

How It Works
The Math: The physical track is mapped out in the code as a "weighted graph." When a user clicks a destination, Dijkstra's algorithm finds the fastest route by mapping the distance between nodes.

The Sensors: The Finch robot uses a dual-sensor control loop. It "straddles" the white line, constantly adjusting its left and right wheel motors to stay on track. If it detects green grass for too long, it triggers an emergency failsafe stop.

The Physics: Curves are mathematically different from straightaways. The system has a built-in physics engine that recognizes when the robot is entering a curve, smoothly adjusting its speed and updating the "mental compass" so it knows exactly what direction it is facing when it finishes the turn.

Tech Stack
Language: Java

Hardware: Finch Robot (by BirdBrain Technologies)

Interface: Java Swing & Graphics2D (for custom bezier curve rendering)

Concepts: Multithreading, Graph Theory, Event-Driven UI, Hardware/Software Integration

How to Run It
Open your terminal, navigate to the parent directory, and run these three commands to compile and launch the project:

Bash
# 1. Clear out old compiled files
del birdbraintechnologies\*.class

# 2. Compile all Java files
javac birdbraintechnologies\*.java

# 3. Run the main program
java birdbraintechnologies.FinchTest
Using the Interface
Run the program and follow the terminal prompts to calibrate the robot's light sensors (Grass, Black Road, White Line).

Once the map window opens, select the direction your physical robot is currently facing from the top dropdown menu.

Click any red station on the map (e.g., Hospital, Post Office, School).

Watch the robot calculate the route and drive
