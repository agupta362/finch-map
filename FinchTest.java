import java.util.*;
import javax.swing.SwingUtilities;

public class FinchTest {

    // Represents a road connecting two stations
    public static class Road {
        String to; double cm; int heading;
        public Road(String to, double cm, int heading) {
            this.to = to; this.cm = cm; this.heading = heading;
        }
    }

    // Adjacency list to store the map
    public static Map<String, List<Road>> adjList = new HashMap<>();
    
    // Global variables for robot state and sensor thresholds
    public static int currentRobotHeading = 0; 
    public static double lineThreshold = 0;    
    public static double grassThreshold = 0;   
    
    // Volatile so the GUI and background thread can share it safely
    public static volatile boolean isMoving = false;

    // Speed constants for straight lines and curves
    public static final double CM_PER_SEC = 5.13;       
    public static final double CURVE_CM_PER_SEC = 5.37;

    // Nodes that are part of a curved path
    public static final List<String> curveNodes = Arrays.asList("E", "F", "C", "D");

    public static void main(String[] args) {
        Finch myFinch = new Finch();
        Scanner input = new Scanner(System.in);
        
        myFinch.stopAll();
        setupMap(); 

        System.out.println("=== BAGMATI BOYZ: REALITY-SYNCED MASTER ===");
        calibrateSensors(myFinch, input);
        
        // Launch the GUI safely
        SwingUtilities.invokeLater(() -> {
            try {
                FinchGPSGUI gui = new FinchGPSGUI(myFinch);
                gui.setVisible(true);
            } catch (Exception e) {
                System.out.println("GUI Error: " + e.getMessage());
            }
        });
    }

    // Calibrate sensors based on the physical mat colors
    public static void calibrateSensors(Finch f, Scanner in) {
        System.out.println("\n[1] GREEN GRASS + Enter."); in.nextLine();
        double gV = (f.getLine("L") + f.getLine("R")) / 2.0;
        System.out.println("[2] BLACK ROAD + Enter."); in.nextLine();
        double bV = (f.getLine("L") + f.getLine("R")) / 2.0;
        System.out.println("[3] WHITE LINE + Enter."); in.nextLine();
        double wV = (f.getLine("L") + f.getLine("R")) / 2.0;
        
        lineThreshold = (bV + wV) / 2.0;
        grassThreshold = (bV + gV) / 2.0;
    }

    // Get the appropriate speed based on the segment type
    public static double getSpeedForSegment(String curr, String next) {
        if (curveNodes.contains(curr) || curveNodes.contains(next)) return CURVE_CM_PER_SEC;
        return CM_PER_SEC;
    }

    // Adjusts the robot's internal compass after it physically turns along a curved line
    public static int getPhysicalEndHeading(String curr, String next) {
        int targetHeading = getHeadingBetween(curr, next);
        
        if (next.equals("E")) {
            if (curr.equals("A")) return 180; 
            if (curr.equals("Hospital")) return 90; 
        } else if (next.equals("F")) {
            if (curr.equals("School")) return 90; 
            if (curr.equals("B")) return 0; 
        } else if (next.equals("C")) {
            if (curr.equals("A")) return 180; 
            if (curr.equals("D")) return 270; 
        } else if (next.equals("D")) {
            if (curr.equals("C")) return 270; 
            if (curr.equals("B")) return 0; 
        }
        return targetHeading;
    }

    // Executes the calculated path by sending commands to the Finch
    public static void executePath(List<String> path, Finch f) {
        if (isMoving) return;
        isMoving = true;
        try {
            for (int i = 0; i < path.size() - 1; i++) {
                String curr = path.get(i);
                String next = path.get(i + 1);
                Road road = null;
                
                // Find the connecting road
                for (Road r : adjList.get(curr)) if (r.to.equals(next)) road = r;
                if (road == null) continue;

                // Calculate required turn angle
                int targetHeading = road.heading;
                int turn = targetHeading - currentRobotHeading;
                if (turn > 180) turn -= 360;
                if (turn < -180) turn += 360;

                // Perform a hard pivot if needed
                if (turn != 0) {
                    System.out.println("!!! PIVOT at " + curr + ": Turning " + turn + " degrees.");
                    f.setMotors(0, 0); f.pause(0.5);
                    f.setTurn((turn > 0) ? "Right" : "Left", Math.abs(turn), 40.0);
                    f.pause(0.5);
                    currentRobotHeading = targetHeading;
                }

                // Drive the segment
                followLineWithPrecision(f, road.cm, getSpeedForSegment(curr, next));
                
                // Sync internal compass with physical orientation
                currentRobotHeading = getPhysicalEndHeading(curr, next);
            }
        } finally { 
            isMoving = false; 
            f.setMotors(0, 0); 
        } 
    }

    // Drives the robot forward while using sensors to stay on the line
    public static void followLineWithPrecision(Finch f, double cm, double cmPerSec) {
        double speed = 10.0; 
        int grassCount = 0; 
        int lastTurn = 0;
        
        // Calculate driving duration
        long duration = (long)((cm / cmPerSec) * 1000);
        long endTime = System.currentTimeMillis() + duration;
        
        while (System.currentTimeMillis() < endTime) {
            double L = f.getLine("L"); 
            double R = f.getLine("R");
            
            // Emergency stop if the robot drives off the mat onto the grass
            if ((L > grassThreshold && L < lineThreshold) || (R > grassThreshold && R < lineThreshold)) {
                if (++grassCount > 100) { f.setMotors(0, 0); return; }
            } else { 
                grassCount = 0; 
            }
            
            // Adjust motors to straddle the line
            if (L > lineThreshold && R > lineThreshold) { f.setMotors(speed, speed); lastTurn = 0; }
            else if (L > lineThreshold) { f.setMotors(0, speed + 12); lastTurn = 1; }
            else if (R > lineThreshold) { f.setMotors(speed + 12, 0); lastTurn = 2; }
            else {
                // Keep turning if we briefly lost the line
                if (lastTurn == 1) f.setMotors(0, speed + 12);
                else if (lastTurn == 2) f.setMotors(speed + 12, 0);
                else f.setMotors(speed, speed);
            }
            f.pause(0.01);
        }
        f.setMotors(0, 0); 
        f.pause(0.2); 
    }

    // Initializes the map with nodes, distances, and headings
    public static void setupMap() {
        String[] ns = {"Police", "Hospital", "School", "Post Office", "A", "B", "C", "D", "E", "F"};
        for (String n : ns) adjList.put(n, new ArrayList<>());

        addRoad("A", "Police", 30.0, 180);          
        addRoad("Police", "Post Office", 142.0, 180);
        addRoad("Post Office", "B", 20.0, 180);

        addRoad("A", "E", 40.0, 270);        
        addRoad("E", "Hospital", 44.0, 180);  
        addRoad("Hospital", "School", 108.0, 180);
        addRoad("School", "F", 15.0, 180);
        addRoad("F", "B", 40.0, 90);      
        
        addRoad("A", "C", 40.0, 90);          
        addRoad("C", "D", 172.0, 180);        
        addRoad("D", "B", 40.0, 270);        
    }

    // Adds a two-way road to the adjacency list
    public static void addRoad(String u, String v, double d, int h) {
        adjList.get(u).add(new Road(v, d, h));
        adjList.get(v).add(new Road(u, d, (h + 180) % 360)); 
    }

    // Converts text direction to integer heading
    public static void setInitialHeading(String d) {
        String dir = d.toLowerCase();
        if (dir.contains("north")) currentRobotHeading = 0;
        else if (dir.contains("east")) currentRobotHeading = 90;
        else if (dir.contains("south")) currentRobotHeading = 180;
        else if (dir.contains("west")) currentRobotHeading = 270;
    }

    // Gets the heading for a specific road segment
    public static int getHeadingBetween(String u, String v) {
        if (adjList.containsKey(u)) {
            for (Road r : adjList.get(u)) if (r.to.equals(v)) return r.heading;
        }
        return 0;
    }

    // Gets the distance for a specific road segment
    public static double getDistanceBetween(String u, String v) {
        if (adjList.containsKey(u)) {
            for (Road r : adjList.get(u)) if (r.to.equals(v)) return r.cm;
        }
        return 0;
    }

    // Calculates the shortest path using Dijkstra's algorithm
    public static List<String> findShortestPath(String s, String e) {
        Map<String, Double> dists = new HashMap<>(); 
        Map<String, String> prevs = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dists::get));
        
        // Initialize distances
        for (String n : adjList.keySet()) dists.put(n, Double.MAX_VALUE);
        dists.put(s, 0.0); 
        pq.add(s);

        // Process nodes
        while (!pq.isEmpty()) {
            String curr = pq.poll(); 
            if (curr.equals(e)) break; // Stop early if we reached the destination
            
            // Check all neighbors
            for (Road r : adjList.get(curr)) {
                double alt = dists.get(curr) + r.cm;
                
                // If a shorter path is found, update it
                if (alt < dists.get(r.to)) { 
                    dists.put(r.to, alt);       
                    prevs.put(r.to, curr);      
                    pq.add(r.to);               
                }
            }
        }

        // Reconstruct the path backwards
        LinkedList<String> res = new LinkedList<>();
        for (String at = e; at != null; at = prevs.get(at)) {
            res.addFirst(at); 
        }
        return res;
    }
}