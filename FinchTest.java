
import java.util.*;

public class FinchTest {
    static class Road {
        String to; 
        double cm; 
        int heading; // 0=N, 90=E, 180=S, 270=W
        public Road(String to, double cm, int heading) { 
            this.to = to; this.cm = cm; this.heading = heading; 
        }
    }

    private static Map<String, List<Road>> adjList = new HashMap<>();
    private static int currentRobotHeading; 
    private static double dynamicThreshold = 0; 

    public static void main(String[] args) {
        Finch myFinch = new Finch();
        Scanner input = new Scanner(System.in);
        setupMap();

        System.out.println("=== BAGMATI BOYZ: ORIENTATION-AWARE GPS ===");
        
        // --- 1. SENSOR CALIBRATION (Fixes "Ghosting" on black road) ---
        calibrateSensors(myFinch, input);

        // --- 2. INITIALIZATION & ORIENTATION ---
        System.out.print("\nStart Location (e.g., Police): ");
        String start = formatNode(input.nextLine());
        
        // This accounts for which side the car is facing at the start
        System.out.print("Which way is the robot facing RIGHT NOW? (North, South, East, West): ");
        setInitialHeading(input.nextLine().toLowerCase());

        System.out.print("Where are you going? (Destination): ");
        String end = formatNode(input.nextLine());

        // --- 3. PATHFINDING & EXECUTION ---
        List<String> path = findShortestPath(start, end);

        if (path.isEmpty() || path.size() < 2) {
            System.out.println("❌ ERROR: Path not found. Check spelling!");
        } else {
            System.out.println("✅ Path Calculated: " + path);
            executePath(path, myFinch);
            
        }

        myFinch.stopAll();
        myFinch.disconnect();
    }

    private static void calibrateSensors(Finch f, Scanner in) {
        System.out.println("\n[CALIBRATION] Place Finch on the BLACK ROAD. Press Enter.");
        in.nextLine();
        double blackFloor = (f.getLine("Left") + f.getLine("Right")) / 2.0;
        
        System.out.println("[CALIBRATION] Place Finch on the WHITE LINE. Press Enter.");
        in.nextLine();
        double whiteLine = (f.getLine("Left") + f.getLine("Right")) / 2.0;

        dynamicThreshold = (blackFloor + whiteLine) / 2.0;
        System.out.println(">>> Threshold set to: " + dynamicThreshold);
    }

    private static void setupMap() {
        String[] nodes = {"Police", "Hospital", "School", "Post Office", "A", "B", "C", "D", "E", "F"};
        for (String n : nodes) adjList.put(n, new ArrayList<>());

        // --- THE SPINE (Vertical) ---
        // Heading 180 = South (Towards School), Heading 0 = North (Towards A)
        addRoad("A", "Police", 30.0, 180);           
        addRoad("Police", "Post Office", 142.0, 180); 
        addRoad("Post Office", "B", 20.0, 180);

        // --- TOP ROAD (Horizontal Curves) ---
        addRoad("A", "E", 40.0, 270);         // West
        addRoad("E", "Hospital", 44.0, 270);  // West

        // --- LEFT SIDE ROAD (Vertical) ---
        addRoad("Hospital", "School", 110.0, 180); 
        addRoad("School", "B", 40.0, 90);     // Connector East to Spine

        // --- OUTER LOOP (Curves) ---
        addRoad("A", "C", 40.0, 90);          // East Curve (Corrected to 40cm)
        addRoad("C", "D", 174.0, 180);        // South Straight
        addRoad("D", "B", 40.0, 270);         // West Curve back to B
    }

    private static void addRoad(String u, String v, double d, int h) {
        adjList.get(u).add(new Road(v, d, h));
        adjList.get(v).add(new Road(u, d, (h + 180) % 360)); // Auto-creates return path
    }

    private static void executePath(List<String> path, Finch f) {
        for (int i = 0; i < path.size() - 1; i++) {
            String curr = path.get(i);
            String next = path.get(i + 1);
            Road road = null;
            for (Road r : adjList.get(curr)) if (r.to.equals(next)) road = r;

            // --- THE TURN LOGIC (Accounts for starting direction) ---
            int targetHeading = road.heading;
            int turnAngle = targetHeading - currentRobotHeading;

            if (turnAngle > 180) turnAngle -= 360;
            if (turnAngle < -180) turnAngle += 360;

            if (turnAngle != 0) {
                System.out.println(">>> Turning " + turnAngle + " degrees to face " + next);
                f.setMotors(0, 0); f.pause(0.5);
                f.setTurn((turnAngle > 0) ? "Right" : "Left", Math.abs(turnAngle), 40.0);
                f.pause(0.5);
                currentRobotHeading = targetHeading; // Update current facing direction
            }

            // --- THE MOVEMENT ---
            System.out.println(">>> Following Line: " + curr + " to " + next);
            followLine(f, road.cm);
        }
    }

    private static void followLine(Finch f, double cm) {
        double speed = 20.0; // Lower speed = Sharper curve detection
        int lost = 0;
        long endTime = System.currentTimeMillis() + (long)((cm / 7.5) * 1000); 

        while (System.currentTimeMillis() < endTime) {
            double L = f.getLine("Left");
            double R = f.getLine("Right");

            if (L > dynamicThreshold || R > dynamicThreshold) {
                lost = 0;
                if (L > dynamicThreshold && R > dynamicThreshold) f.setMotors(speed, speed);
                else if (L > dynamicThreshold) f.setMotors(speed - 18, speed + 18); // Veer Left
                else f.setMotors(speed + 18, speed - 18); // Veer Right
            } else {
                // If it loses the line (Black road or Green grass)
                if (++lost > 70) { 
                    System.out.println("⚠️ ERROR: Off-track. Stopping.");
                    f.setMotors(0, 0); return; 
                }
                f.setMotors(speed, speed); 
            }
            f.pause(0.01);
        }
        f.setMotors(0, 0);
    }

    // --- UTILITIES ---
    private static void setInitialHeading(String d) {
        if (d.contains("north")) currentRobotHeading = 0;
        else if (d.contains("east")) currentRobotHeading = 90;
        else if (d.contains("south")) currentRobotHeading = 180;
        else if (d.contains("west")) currentRobotHeading = 270;
    }

    private static String formatNode(String s) {
        if (s.equalsIgnoreCase("post office")) return "Post Office";
        if (s.length() < 2) return s.toUpperCase();
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private static List<String> findShortestPath(String s, String e) {
        Map<String, Double> dists = new HashMap<>();
        Map<String, String> prevs = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dists::get));
        for (String n : adjList.keySet()) dists.put(n, Double.MAX_VALUE);
        if (!adjList.containsKey(s)) return new ArrayList<>();
        dists.put(s, 0.0); pq.add(s);
        while (!pq.isEmpty()) {
            String curr = pq.poll();
            if (curr.equals(e)) break;
            for (Road r : adjList.get(curr)) {
                double alt = dists.get(curr) + r.cm;
                if (alt < dists.get(r.to)) { dists.put(r.to, alt); prevs.put(r.to, curr); pq.add(r.to); }
            }
        }
        LinkedList<String> res = new LinkedList<>();
        for (String at = e; at != null; at = prevs.get(at)) res.addFirst(at);
        return res;
    }

    
}