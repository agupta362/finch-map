import greenfoot.*;
import java.util.*;

public class FinchActor extends Actor {
    private HashMap<String, Node> mapNodes;
    private Finch myFinch; 

    public FinchActor(HashMap<String, Node> nodes) {
        this.mapNodes = nodes;
        try {
            myFinch = new Finch(); // This connects to the real robot
        } catch (Exception e) {
            System.out.println("Finch not connected, simulation only mode.");
        }
    }

    // FIXED: Removed the 'Finch f' parameter because we use 'myFinch' variable instead
    public void followLine(double distance, double speed) {
        if (myFinch == null) return;
        
        int threshold = 50; 
        long startTime = System.currentTimeMillis();
        double duration = (distance / speed) * 1000; 

        while (System.currentTimeMillis() - startTime < duration) {
            int left = myFinch.getLine("Left");
            int right = myFinch.getLine("Right");

            if (left > threshold && right > threshold) {
                myFinch.setMotors(speed, speed); 
            } else if (left < threshold && right > threshold) {
                myFinch.setMotors(speed, speed * 0.2); 
            } else if (right < threshold && left > threshold) {
                myFinch.setMotors(speed * 0.2, speed); 
            } else {
                myFinch.setMotors(speed * 0.7, speed * 0.7); 
            }
        }
        myFinch.stop();
    }

    public void movePath(List<String> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            Node current = mapNodes.get(path.get(i));
            Node next = mapNodes.get(path.get(i+1));
            
            if (current == null || next == null) continue;

            // 1. Calculate turn
            int angle = (int) Math.toDegrees(Math.atan2(next.y - current.y, next.x - current.x));
            setRotation(angle);
            
            // 2. Calculate distance
            double dx = next.x - current.x;
            double dy = next.y - current.y;
            double pixelDist = Math.sqrt(dx*dx + dy*dy);
            double actualCm = pixelDist / 2; 

            // 3. Move Actor (Simulation)
            move((int)pixelDist);
            Greenfoot.delay(20);

            // 4. Move Physical Finch
            if (myFinch != null) {
                // Adjusting the turn and calling the local followLine
                myFinch.setTurn("Right", angle, 50); 
                followLine(actualCm, 50); // Calls the fixed method above
            }
        }
    }
}
