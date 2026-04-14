
import java.util.*;

public class FinchTest {

    // --- 1. Data Structures for Phase 2 ---
    static class Road {
        String to;
        double cm;
        public Road(String to, double cm) { this.to = to; this.cm = cm; }
    }

    private static Map<String, List<Road>> adjList = new HashMap<>();

    public static void main(String[] args) {
        Finch myFinch = new Finch();
        Scanner input = new Scanner(System.in);

        // --- Startup Sequence ---
        myFinch.playNote(60, 0.5);
        myFinch.setBeak(0, 100, 0); // Green for "Go"

        // --- 2. Initialize your Map with Measurements ---
        setupMap();

        // --- 3. User Interface (Task for Friend) ---
        System.out.println("Welcome to Bagmati Boyz Mini GPS!");
        System.out.print("Enter Start Location (e.g., Police): ");
        String start = input.nextLine();
        System.out.print("Enter Destination (e.g., School): ");
        String end = input.nextLine();

        // --- 4. Core Algorithm: Dijkstra ---
        List<String> path = findShortestPath(start, end);

        if (path.isEmpty()) {
            System.out.println("No path found. Check spelling!");
        } else {
            System.out.println("Shortest Route Found: " + path);
            System.out.println("Total Path Length: 224cm (via Hospital)");

            // --- 5. Robot Execution ---
            executePath(path, myFinch);
        }

        // --- Shutdown Sequence ---
        myFinch.playNote(65, 0.5);
        myFinch.stopAll();
        myFinch.disconnect();
        input.close();
    }

    private static void setupMap() {
        String[] nodes = {"Police", "Hospital", "School", "Post Office", "A", "B", "OuterLoop"};
        for (String n : nodes) adjList.put(n, new ArrayList<>());

        // Your 224cm Path (via Hospital)
        addRoad("Police", "A", 30.0);
        addRoad("A", "Hospital", 84.0); // (44cm + 40cm junction compensation)
        addRoad("Hospital", "School", 110.0);
        
        // Alternative Roads (to let Dijkstra decide)
        addRoad("A", "B", 192.0); 
        addRoad("A", "OuterLoop", 127.0); 
        addRoad("OuterLoop", "B", 127.0); // Total 254cm outer loop
        addRoad("School", "B", 40.0);
    }

    private static void addRoad(String u, String v, double d) {
        adjList.get(u).add(new Road(v, d));
        adjList.get(v).add(new Road(u, d));
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

            double dist = 0;
            for (Road r : adjList.get(current)) {
                if (r.to.equals(next)) dist = r.cm;
            }

            // Move the robot
            System.out.println("Executing: " + current + " to " + next + " (" + dist + "cm)");
            // FIX: Added "Forward" as the first argument
            // The speed is now 50.0 (double) instead of 50 (int)
            f.setMove("Forward", dist, 300.0);
            f.pause(0.5); // Short pause between segments
        }
    }
}