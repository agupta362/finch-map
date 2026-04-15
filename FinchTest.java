
import java.util.*;

public class FinchTest {

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
    private static int currentRobotHeading; 
    private static final double THRESHOLD = 60.0; 

    public static void main(String[] args) {
        Finch myFinch = new Finch();
        Scanner input = new Scanner(System.in);

        setupMap();

        System.out.println("=== BAGMATI BOYZ: FINAL PRODUCTION CODE ===");
        
        // --- 1. SENSOR GUARD ---
        System.out.println("WAITING: Place Finch on the WHITE LINE...");
        while (true) {
            double L = myFinch.getLine("Left");
            double R = myFinch.getLine("Right");
            System.out.print("\rSensors -> L: " + L + " | R: " + R + " | Target: >" + THRESHOLD + "    ");

            if (L >= THRESHOLD && R >= THRESHOLD) {
                System.out.println("\nLine Detected! System Ready.");
                break; 
            }
            myFinch.setBeak(100, 0, 0); 
            myFinch.pause(0.1);
        }

        myFinch.setBeak(0, 100, 0); 
        myFinch.playNote(60, 0.5);

        // --- 2. INPUT CALIBRATION ---
        System.out.print("\nStarting Location: ");
        String startNode = formatNodeName(input.nextLine());

        System.out.print("Which way is it facing? (North, South, East, West): ");
        setInitialHeading(input.nextLine().toLowerCase());

        System.out.print("Enter Destination: ");
        String endNode = formatNodeName(input.nextLine());

        // --- 3. EXECUTION ---
        List<String> path = findShortestPath(startNode, endNode);

        if (path.isEmpty() || path.size() < 2) {
            System.out.println("❌ ERROR: Path not found between '" + startNode + "' and '" + endNode + "'.");
        } else {
            System.out.println("🚀 Route Found: " + path);
            executePath(path, myFinch);
            victoryDance(myFinch);
        }

        myFinch.stopAll();
        myFinch.disconnect();
        input.close();
    }

    private static String formatNodeName(String raw) {
        if (raw.isEmpty()) return "";
        if (raw.length() == 1) return raw.toUpperCase(); // 'a' -> 'A'
        if (raw.equalsIgnoreCase("post office")) return "Post Office";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }

    private static void setupMap() {
        String[] nodes = {"Police", "Hospital", "School", "Post Office", "A", "B", "C", "D", "E", "F"};
        for (String n : nodes) adjList.put(n, new ArrayList<>());

        // --- TOP AXIS (Westbound 270) ---
        addRoad("Police", "A", 30.0, 270);
        addRoad("A", "E", 40.0, 270);
        addRoad("E", "Hospital", 44.0, 0);

        // --- VERTICAL AXIS (Southbound 180) ---
        addRoad("A", "B", 192.0, 180);             // The long central road
        addRoad("Hospital", "School", 110.0, 180); // The short side road
        addRoad("C", "D", 174.0, 180);

        // --- CONNECTORS ---
        addRoad("A", "C", 40.0, 90);
        addRoad("D", "B", 40.0, 270);
        addRoad("School", "B", 40.0, 90);
        addRoad("Post Office", "B", 20.0, 270);
    }

    private static void addRoad(String u, String v, double d, int headingUV) {
        adjList.get(u).add(new Road(v, d, headingUV));
        adjList.get(v).add(new Road(u, d, (headingUV + 180) % 360));
    }

    private static void setInitialHeading(String dir) {
        if (dir.contains("north")) currentRobotHeading = 0;
        else if (dir.contains("east")) currentRobotHeading = 90;
        else if (dir.contains("south")) currentRobotHeading = 180;
        else if (dir.contains("west")) currentRobotHeading = 270;
    }

    private static List<String> findShortestPath(String start, String end) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> parents = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get));
        
        if (!adjList.containsKey(start)) return new ArrayList<>();
        for (String node : adjList.keySet()) distances.put(node, Double.MAX_VALUE);
        
        distances.put(start, 0.0);
        pq.add(start);

        while (!pq.isEmpty()) {
            String curr = pq.poll();
            if (curr.equals(end)) break;
            for (Road r : adjList.get(curr)) {
                double newDist = distances.get(curr) + r.cm;
                if (newDist < distances.get(r.to)) {
                    distances.put(r.to, newDist);
                    parents.put(r.to, curr);
                    pq.add(r.to);
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
            for (Road r : adjList.get(current)) if (r.to.equals(next)) roadToTake = r;

            // 1. PIVOT
            int nextHeading = roadToTake.heading;
            int turnAngle = nextHeading - currentRobotHeading;
            if (turnAngle > 180) turnAngle -= 360;
            if (turnAngle < -180) turnAngle += 360;

            if (turnAngle != 0) {
                f.setMotors(0, 0); f.pause(0.5);
                f.setTurn((turnAngle > 0) ? "Right" : "Left", Math.abs(turnAngle), 40.0);
                f.pause(0.5);
                currentRobotHeading = nextHeading;
            }

            // 2. MOVE
            System.out.println("Following Line: " + current + " -> " + next + " (" + roadToTake.cm + "cm)");
            followLine(f, roadToTake.cm);
        }
    }

    private static void followLine(Finch f, double distCM) {
        double speed = 25.0; 
        int lostCounter = 0;
        // Adjusted speed multiplier to 7.8 for better distance matching
        long duration = (long)((distCM / 7.8) * 1000); 
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < duration) {
            double left = f.getLine("Left");
            double right = f.getLine("Right");

            if (left > THRESHOLD || right > THRESHOLD) {
                lostCounter = 0;
                if (left > THRESHOLD && right > THRESHOLD) f.setMotors(speed, speed);
                else if (left > THRESHOLD) f.setMotors(speed - 15, speed + 15);
                else f.setMotors(speed + 15, speed - 15);
            } else {
                lostCounter++;
                // High lostCounter (100) allows robot to ignore dashed line gaps
                if (lostCounter > 100) { 
                    System.out.println("⚠️ ERROR: Line Lost.");
                    f.setMotors(0, 0);
                    return; 
                }
                f.setMotors(speed, speed); 
            }
            f.pause(0.01);
        }
        f.setMotors(0, 0); f.pause(0.2);
    }

    private static void victoryDance(Finch f) {
        f.setBeak(0, 0, 100);
        f.setTurn("Right", 360, 100);
    }
}