import javax.swing.*;
import java.awt.*;

public class TestEstacionSolar {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test Estación Solar");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            
            // Create a simple PanelGrafoDinamico for testing
            PanelGrafoDinamico panelGrafo = new PanelGrafoDinamico();
            
            // Create the EstacionSolarPanel with Monitores synchronization
            EstacionSolarPanel estacionPanel = new EstacionSolarPanel("Monitores", panelGrafo);
            
            // Add components to frame
            frame.setLayout(new BorderLayout());
            frame.add(estacionPanel, BorderLayout.CENTER);
            frame.add(panelGrafo, BorderLayout.EAST);
            
            frame.setVisible(true);
            
            // Test for 10 seconds then stop
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            estacionPanel.detener();
            System.out.println("Test completed successfully!");
        });
    }
}