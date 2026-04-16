import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.event.*;
import java.util.List;

public class FinchGPSGUI extends JFrame {
    private Finch myFinch;
    private String currentNode = "Police"; // Tracks where the robot is
    private int currentHeading = 0;        // Tracks which way it's facing
    
    private JLabel statusLabel = new JLabel("Status: Ready.");
    private JComboBox<String> orientationBox;
    private MapPanel mapPanel;

    private final Object[][] nodeCoords = {
        {"A", 250, 50}, {"Police", 250, 150}, {"Post Office", 250, 350}, {"B", 250, 450},
        {"E", 100, 50}, {"Hospital", 100, 200}, {"School", 100, 400},
        {"C", 400, 50}, {"D", 400, 450}
    };

    public FinchGPSGUI(Finch f) {
        this.myFinch = f;
        setTitle("Bagmati Boyz GPS Controller");
        setSize(550, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- TOP PANEL: INITIAL ORIENTATION ---
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Initial Orientation:"));
        String[] directions = {"North", "East", "South", "West"};
        orientationBox = new JComboBox<>(directions);
        
        // When you change the dropdown, it updates the robot's internal heading
        orientationBox.addActionListener(e -> {
            String selected = (String) orientationBox.getSelectedItem();
            updateHeadingFromText(selected.toLowerCase());
            mapPanel.repaint();
        });
        
        topPanel.add(orientationBox);
        add(topPanel, BorderLayout.NORTH);

        // --- CENTER: THE MAP ---
        mapPanel = new MapPanel();
        add(mapPanel, BorderLayout.CENTER);

        // --- BOTTOM: STATUS & CALIBRATION ---
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        bottomPanel.add(statusLabel);
        JButton calibrateBtn = new JButton("Calibrate Sensors");
        calibrateBtn.addActionListener(e -> statusLabel.setText("Status: Calibrated."));
        bottomPanel.add(calibrateBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateHeadingFromText(String d) {
        if (d.contains("north")) currentHeading = 0;
        else if (d.contains("east")) currentHeading = 90;
        else if (d.contains("south")) currentHeading = 180;
        else if (d.contains("west")) currentHeading = 270;
        // Also update the static variable in your FinchTest class!
        FinchTest.setInitialHeading(d); 
    }

    class MapPanel extends JPanel {
        public MapPanel() {
            setBackground(new Color(230, 230, 230));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleMapClick(e.getX(), e.getY());
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Draw Roads
            g2.setStroke(new BasicStroke(4));
            g2.setColor(Color.LIGHT_GRAY);
            drawAllRoads(g2);

            // 2. Draw Nodes
            for (Object[] node : nodeCoords) {
                int x = (int) node[1];
                int y = (int) node[2];
                g2.setColor(Color.WHITE);
                g2.fillOval(x - 12, y - 12, 24, 24);
                g2.setColor(Color.DARK_GRAY);
                g2.drawOval(x - 12, y - 12, 24, 24);
                g2.drawString((String)node[0], x + 15, y + 5);
            }

            // 3. DRAW THE ROBOT ICON
            drawRobot(g2);
        }

        private void drawRobot(Graphics2D g2) {
            // Find current node coordinates
            int rx = 0, ry = 0;
            for (Object[] n : nodeCoords) {
                if (n[0].equals(currentNode)) {
                    rx = (int) n[1];
                    ry = (int) n[2];
                }
            }

            // Save the current transform
            AffineTransform old = g2.getTransform();
            
            // Move to robot position and rotate
            g2.translate(rx, ry);
            g2.rotate(Math.toRadians(currentHeading));

            // Draw a simple "Robot" triangle/arrow
            // The tip of the triangle points UP (0 degrees = North)
            int[] xPoints = {0, -10, 10};
            int[] yPoints = {-15, 10, 10};
            g2.setColor(new Color(255, 100, 0)); // Orange robot
            g2.fillPolygon(xPoints, yPoints, 3);
            g2.setColor(Color.BLACK);
            g2.drawPolygon(xPoints, yPoints, 3);

            // Restore transform
            g2.setTransform(old);
        }

        private void drawAllRoads(Graphics2D g2) {
            drawLine(g2, "A", "Police"); drawLine(g2, "Police", "Post Office");
            drawLine(g2, "Post Office", "B"); drawLine(g2, "A", "E");
            drawLine(g2, "E", "Hospital"); drawLine(g2, "Hospital", "School");
            drawLine(g2, "School", "B"); drawLine(g2, "A", "C");
            drawLine(g2, "C", "D"); drawLine(g2, "D", "B");
        }

        private void drawLine(Graphics2D g, String n1, String n2) {
            int x1=0, y1=0, x2=0, y2=0;
            for(Object[] n : nodeCoords) {
                if(n[0].equals(n1)) { x1=(int)n[1]; y1=(int)n[2]; }
                if(n[0].equals(n2)) { x2=(int)n[1]; y2=(int)n[2]; }
            }
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void handleMapClick(int x, int y) {
        for (Object[] node : nodeCoords) {
            if (Math.abs(x - (int)node[1]) < 20 && Math.abs(y - (int)node[2]) < 20) {
                startPathfinding((String)node[0]);
                return;
            }
        }
    }

    private void startPathfinding(String dest) {
        if (dest.equals(currentNode)) return;
        orientationBox.setEnabled(false); // Lock orientation while moving

        new Thread(() -> {
            List<String> path = FinchTest.findShortestPath(currentNode, dest);
            if (!path.isEmpty()) {
                // We simulate the movement node-by-node to update the GUI
                for (int i = 0; i < path.size() - 1; i++) {
                    String from = path.get(i);
                    String to = path.get(i+1);
                    
                    // 1. Determine heading for this leg
                    int targetH = FinchTest.getHeadingBetween(from, to);
                    currentHeading = targetH;
                    
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Moving: " + from + " -> " + to);
                        repaint();
                    });

                    // 2. Actually move the robot
                    FinchTest.executePath(java.util.List.of(from, to), myFinch);
                    
                    currentNode = to;
                    SwingUtilities.invokeLater(this::repaint);
                }
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Arrived at " + dest);
                    orientationBox.setEnabled(true);
                });
            }
        }).start();
    }
}
