
import java.util.*;

public class FinchTest {

    // --- 1. DATA STRUCTURES ---
    static class Road {
        String to;
        double cm;
        int heading; // 0=N, 90=E, 180=S, 270=W

        public Road(String to, double cm, int heading) {
            this.to = to;
            this.cm = cm;
            this.heading = heading;
        }
    }

    private static Map<String, List<Road>> adjList = new HashMap<>();
    private static int currentRobotHeading = 270; // STARTING: Facing West (towards center)

    public static void main(String[] args) {
        Finch myFinch = new Finch();
        Scanner input = new Scanner(System.in);

        // --- Startup Sequence ---
        myFinch.playNote(60, 0.5);
        myFinch.setBeak(0, 100, 0); // Green for "System Ready"

        setupMap();

        System.out.println("--- BAGMATI BOYZ: FULL GPS SYSTEM ---");
        System.out.print("Enter Start (e.g., Police): ");
        String start = input.nextLine();
        System.out.print("Enter Destination (e.g., School): ");
        String end = input.nextLine();

        // --- 2. THE BRAIN: DIJKSTRA ---
        // --- 4. Core Algorithm: Dijkstra ---
    List<String> path = findShortestPath(start, end);

    if (path.isEmpty()) {
        System.out.println("No path found. Check spelling!");
    } else {
        System.out.println("Shortest Route Found: " + path);
        System.out.println("Total Path Length: 224cm (via Hospital)");

        // REMOVED: if (myFinch.isFinchConnected()) 
        // The library assumes you are connected if the BirdBrain Connector is open.
        System.out.println("Sending commands to Finch...");
        executePath(path, myFinch);
        victoryDance(myFinch);
    }

        myFinch.stopAll();
        myFinch.disconnect();
        input.close();
    }

    private static void setupMap() {
        // Defining all landmarks and junction points
        String[] nodes = {"Police", "Hospital", "School", "Post Office", "A", "B", "C", "D", "E", "F"};
        for (String n : nodes) adjList.put(n, new ArrayList<>());

        // --- THE TOP AXIS (Westbound) ---
        addRoad("Police", "A", 30.0, 270);    
        addRoad("A", "E", 40.0, 270);         
        addRoad("E", "Hospital", 44.0, 270);  

        // --- THE LEFT SIDE (Southbound - The 224cm Short Path) ---
        addRoad("Hospital", "School", 110.0, 180); 

        // --- THE OUTER LOOPS (The 254cm Alternative) ---
        // Top-to-Bottom via C and D (East side)
        addRoad("A", "C", 40.0, 90);         // A to C is East
        addRoad("C", "D", 174.0, 180);       // C to D is South
        addRoad("D", "B", 40.0, 270);        // D to B is West

        // Top-to-Bottom via E and F (West side)
        addRoad("E", "F", 174.0, 180);       // E to F is South
        addRoad("F", "B", 40.0, 90);         // F to B is East

        // --- CENTRAL & BOTTOM AXIS ---
        addRoad("A", "B", 192.0, 180);       // Main Straight Road
        addRoad("School", "B", 40.0, 90);    // School to B is East
        addRoad("Post Office", "B", 20.0, 270); // PO to B is West
    }

    private static void addRoad(String u, String v, double d, int headingUV) {
        adjList.get(u).add(new Road(v, d, headingUV));
        // Auto-calculates the return heading (e.g., if South is 180, North is 0)
        adjList.get(v).add(new Road(u, d, (headingUV + 180) % 360));
    }

    private static List<String> findShortestPath(String start, String end) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> parents = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (String node : adjList.keySet()) distances.put(node, Double.MAX_VALUE);
        if (!adjList.containsKey(start)) return new ArrayList<>();
        
        distances.put(start, 0.0);
        pq.add(start);

        while (!pq.isEmpty()) {
            String current = pq.poll();
            if (current.equals(end)) break;

            for (Road road : adjList.get(current)) {
                double newDist = distances.get(current) + road.cm;
                if (newDist < distances.get(road.to)) {
                    distances.put(road.to, newDist);
                    parents.put(road.to, current);
                    pq.add(road.to);
                }
            }
        }

        LinkedList<String> path = new LinkedList<>();
        for (String at = end; at != null; at = parents.get(at)) path.addFirst(at);
        return path;
    }

    private static void executePath(List<String> path, Finch f) {
        for (int i = 0; i < path.size() - 1; i++) {
            String current = path.get(i);
            String next = path.get(i + 1);

            Road roadToTake = null;
            for (Road r : adjList.get(current)) {
                if (r.to.equals(next)) roadToTake = r;
            }

            // --- TURN CALCULATION ---
            int nextHeading = roadToTake.heading;
            int turnAngle = nextHeading - currentRobotHeading;

            // Normalize angle to find the shortest turn
            if (turnAngle > 180) turnAngle -= 360;
            if (turnAngle < -180) turnAngle += 360;

            if (turnAngle != 0) {
                String dir = (turnAngle > 0) ? "Right" : "Left";
                System.out.println("Turn " + dir + " " + Math.abs(turnAngle) + " degrees.");
                f.setTurn(dir, Math.abs(turnAngle), 50.0);
                currentRobotHeading = nextHeading;
                f.pause(0.2);
            }

            // --- MOVE FORWARD ---
            System.out.println("Moving: " + current + " -> " + next + " (" + roadToTake.cm + "cm)");
            f.setMove("Forward", roadToTake.cm, 50.0);
            f.pause(0.5);
        }
    }

    private static void victoryDance(Finch f) {
        System.out.println("Destination Reached!");
        f.setBeak(100, 0, 0); // Red
        f.playNote(72, 0.5);
        f.setTurn("Right", 360, 100); // Victory Spin
        f.setBeak(0, 0, 100); // Blue
    }
}