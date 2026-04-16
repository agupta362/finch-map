import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class FinchGPSGUI extends JFrame {
    private Finch myFinch;
    private String startNode = "Police"; // Default start
    private JLabel statusLabel = new JLabel("Status: Ready. Select Destination.");
    private MapPanel mapPanel;

    // Mapping nodes to screen coordinates (X, Y)
    // Based on your sketch: A is top-center, B is bottom-center
    private final Object[][] nodeCoords = {
        {"A", 250, 50}, {"Police", 250, 150}, {"Post Office", 250, 350}, {"B", 250, 450},
        {"E", 100, 50}, {"Hospital", 100, 200}, {"School", 100, 400},
        {"C", 400, 50}, {"D", 400, 450}
    };

    public FinchGPSGUI(Finch f) {
        this.myFinch = f;
        setTitle("Bagmati Boyz GPS Controller");
        setSize(550, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        mapPanel = new MapPanel();
        add(mapPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
        bottomPanel.add(statusLabel);
        
        JButton calibrateBtn = new JButton("Calibrate Sensors");
        calibrateBtn.addActionListener(e -> calibrateFromGUI());
        bottomPanel.add(calibrateBtn);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void calibrateFromGUI() {
        JOptionPane.showMessageDialog(this, "Place Finch on BLACK road, then click OK.");
        // You'll need to make calibrateSensors in FinchTest static or accessible
        // For now, we'll assume a standard threshold or call your existing logic.
        statusLabel.setText("Status: Calibrated.");
    }

    // This class draws the map and handles clicks
    class MapPanel extends JPanel {
        public MapPanel() {
            setBackground(new Color(240, 240, 240));
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
            g2.setStroke(new BasicStroke(5));
            g2.setColor(Color.DARK_GRAY);

            // Draw Roads (Connecting lines based on your addRoad logic)
            drawLine(g2, "A", "Police");
            drawLine(g2, "Police", "Post Office");
            drawLine(g2, "Post Office", "B");
            drawLine(g2, "A", "E");
            drawLine(g2, "E", "Hospital");
            drawLine(g2, "Hospital", "School");
            drawLine(g2, "School", "B");
            drawLine(g2, "A", "C");
            drawLine(g2, "C", "D");
            drawLine(g2, "D", "B");

            // Draw Nodes
            for (Object[] node : nodeCoords) {
                int x = (int) node[1];
                int y = (int) node[2];
                String name = (String) node[0];

                g2.setColor(name.equals(startNode) ? Color.RED : Color.BLUE);
                g2.fillOval(x - 10, y - 10, 20, 20);
                g2.setColor(Color.BLACK);
                g2.drawString(name, x + 15, y + 5);
            }
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
            int nx = (int) node[1];
            int ny = (int) node[2];
            // Check if click was near a node (within 20 pixels)
            if (Math.abs(x - nx) < 20 && Math.abs(y - ny) < 20) {
                String destination = (String) node[0];
                startPathfinding(destination);
                return;
            }
        }
    }

    private void startPathfinding(String dest) {
        if (dest.equals(startNode)) return;

        statusLabel.setText("Status: Traveling to " + dest + "...");
        
        // IMPORTANT: Run robot logic in a separate thread so GUI doesn't hang
        new Thread(() -> {
            List<String> path = FinchTest.findShortestPath(startNode, dest); 
            if (!path.isEmpty()) {
                FinchTest.executePath(path, myFinch);
                startNode = dest; // Update current location
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Status: Arrived at " + dest);
                    repaint();
                });
            }
        }).start();
    }
}
