import greenfoot.*;
import java.util.*;

public class FinchActor extends Actor {
    private HashMap<String, Node> mapNodes;
    private Finch myFinch; // Physical robot

    public FinchActor(HashMap<String, Node> nodes) {
        this.mapNodes = nodes;
        // myFinch = new Finch(); // Initialize physical Finch here
    }

    public void movePath(List<String> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            Node current = mapNodes.get(path.get(i));
            Node next = mapNodes.get(path.get(i+1));
            
            // 1. Calculate turn angle to the next node
            int angle = (int) Math.toDegrees(Math.atan2(next.y - current.y, next.x - current.x));
            setRotation(angle);
            
            // 2. Calculate distance
            double dx = next.x - current.x;
            double dy = next.y - current.y;
            double pixelDist = Math.sqrt(dx*dx + dy*dy);
            double actualCm = pixelDist / 2; // Convert back to CM

            // 3. Move Actor (Simulation)
            move((int)pixelDist);
            Greenfoot.delay(20);

            // 4. Move Physical Finch
            if (myFinch != null) {
                // You could use setTurn/setMove OR followLine
                myFinch.setTurn("Right", angle, 50); 
                followLine(actualCm, 50);
            }
        }
    }
}