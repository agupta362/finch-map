

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.QuadCurve2D;     
import java.util.List;
import javax.swing.*;

public class FinchGPSGUI extends JFrame {
    private Finch myFinch;
    private String currentNode = "Police"; 
    private int currentHeading = 0;        
    private double robotX = 300; 
    private double robotY = 240; 
    private JLabel statusLabel = new JLabel("Status: Ready.");
    private JComboBox<String> orientationBox;
    private MapPanel mapPanel;

    private final Object[][] nodeCoords = {
        {"A", 300, 80}, {"Police", 300, 240}, {"Post Office", 300, 440}, {"B", 300, 600},
        {"E", 100, 140}, {"Hospital", 100, 240}, {"School", 100, 440}, 
        {"F", 100, 520}, 
        {"C", 500, 140}, {"D", 500, 520}
    };

    public FinchGPSGUI(Finch f) {
        this.myFinch = f;
        setTitle("Bagmati Boyz GPS"); setSize(650, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(); top.setBackground(Color.DARK_GRAY);
        String[] dirs = {"North", "East", "South", "West"};
        orientationBox = new JComboBox<>(dirs);
        orientationBox.addActionListener(e -> updateHeadingFromText(((String)orientationBox.getSelectedItem()).toLowerCase()));
        top.add(new JLabel("Initial Orientation: ")); top.add(orientationBox);
        add(top, BorderLayout.NORTH);

        mapPanel = new MapPanel(); add(mapPanel, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(2, 1));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(statusLabel);
        JButton unlock = new JButton("Emergency Unlock GUI");
        unlock.addActionListener(e -> {
            FinchTest.isMoving = false; // 🚨 Perfectly linked to the FinchTest boolean!
            statusLabel.setText("Status: Unlocked.");
        });
        bottom.add(unlock); add(bottom, BorderLayout.SOUTH);
    }

    private void updateHeadingFromText(String d) {
        if (d.contains("north")) currentHeading = 0;
        else if (d.contains("east")) currentHeading = 90;
        else if (d.contains("south")) currentHeading = 180;
        else if (d.contains("west")) currentHeading = 270;
        FinchTest.setInitialHeading(d); mapPanel.repaint();
    }

    class MapPanel extends JPanel {
        public MapPanel() { 
            setBackground(new Color(90, 170, 90)); 
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { handleMapClick(e.getX(), e.getY()); }
            });
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            drawStraight(g2, "A", "Police", "30cm"); 
            drawStraight(g2, "Police", "Post Office", "142cm");
            drawStraight(g2, "Post Office", "B", "20cm"); 

            drawStraight(g2, "E", "Hospital", "44cm"); 
            drawStraight(g2, "Hospital", "School", "108cm");
            drawStraight(g2, "School", "F", "15cm"); 
            
            drawStraight(g2, "C", "D", "172cm");

            drawCurve(g2, "A", "E", 100, 80, "40cm"); 
            drawCurve(g2, "F", "B", 100, 600, "40cm"); 
            drawCurve(g2, "A", "C", 500, 80, "40cm"); 
            drawCurve(g2, "D", "B", 500, 600, "40cm");

            for (Object[] node : nodeCoords) {
                int x=(int)node[1], y=(int)node[2]; g2.setColor(Color.WHITE); g2.fillOval(x-14, y-14, 28, 28);
                g2.setColor(Color.RED); g2.drawOval(x-14, y-14, 28, 28); g2.setColor(Color.BLACK); g2.drawString((String)node[0], x+18, y+5);
            }
            drawRobot(g2);
        }
        private void drawStraight(Graphics2D g2, String n1, String n2, String lbl) {
            int[] c = getCoords(n1, n2); drawRoadLine(g2, c[0], c[1], c[2], c[3]);
            g2.setColor(Color.YELLOW); g2.drawString(lbl, (c[0]+c[2])/2 + 10, (c[1]+c[3])/2);
        }
        private void drawCurve(Graphics2D g2, String n1, String n2, int cx, int cy, String lbl) {
            int[] c = getCoords(n1, n2); QuadCurve2D q = new QuadCurve2D.Double(c[0], c[1], cx, cy, c[2], c[3]);
            g2.setStroke(new BasicStroke(16)); g2.setColor(Color.DARK_GRAY); g2.draw(q);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[]{10}, 0)); g2.setColor(Color.WHITE); g2.draw(q);
            g2.setColor(Color.YELLOW); g2.drawString(lbl, cx, cy-15);
        }
        private void drawRoadLine(Graphics2D g2, int x1, int y1, int x2, int y2) {
            g2.setStroke(new BasicStroke(16)); g2.setColor(Color.DARK_GRAY); g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[]{10}, 0)); g2.setColor(Color.WHITE); g2.drawLine(x1, y1, x2, y2);
        }
        public int[] getCoords(String n1, String n2) {
            int x1=0, y1=0, x2=0, y2=0;
            for(Object[] n : nodeCoords) { if(n[0].equals(n1)) { x1=(int)n[1]; y1=(int)n[2]; } if(n[0].equals(n2)) { x2=(int)n[1]; y2=(int)n[2]; } }
            return new int[]{x1, y1, x2, y2};
        }
        private void drawRobot(Graphics2D g2) {
            AffineTransform old = g2.getTransform(); g2.translate(robotX, robotY); 
            g2.rotate(Math.toRadians(currentHeading)); int[] xp={0,-12,12}, yp={-18,12,12};
            g2.setColor(new Color(255, 100, 0)); g2.fillPolygon(xp, yp, 3);
            g2.setColor(Color.BLACK); g2.drawPolygon(xp, yp, 3); g2.setTransform(old);
        }
    }

    private void handleMapClick(int x, int y) {
        for (Object[] n : nodeCoords) if (Math.abs(x-(int)n[1])<25 && Math.abs(y-(int)n[2])<25) { startPath((String)n[0]); return; }
    }

    private void startPath(String dest) {
        if (dest.equals(currentNode)) return;
        orientationBox.setEnabled(false);
        statusLabel.setText("Routing to " + dest + "...");
        
        new Thread(() -> {
            List<String> path = FinchTest.findShortestPath(currentNode, dest);
            for (int i=0; i<path.size()-1; i++) {
                String f=path.get(i), t=path.get(i+1);
                int[] c = mapPanel.getCoords(f, t);
                currentHeading = FinchTest.getHeadingBetween(f, t);
                double d = FinchTest.getDistanceBetween(f, t);
                long dur = (long)((d / FinchTest.getSpeedForSegment(f, t)) * 1000);
                Thread rt = new Thread(() -> FinchTest.executePath(List.of(f, t), myFinch)); rt.start();
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis()-start < dur) {
                    double p = (System.currentTimeMillis()-start)/(double)dur;
                    robotX = c[0] + (c[2]-c[0])*p; robotY = c[1] + (c[3]-c[1])*p;
                    SwingUtilities.invokeLater(mapPanel::repaint); try { Thread.sleep(30); } catch (Exception ex) {}
                }
                robotX=c[2]; robotY=c[3]; currentNode=t; SwingUtilities.invokeLater(mapPanel::repaint);
                try { rt.join(); } catch (Exception ex) {}
            }
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Arrived at " + dest);
                orientationBox.setEnabled(true);
            });
        }).start();
    }
}