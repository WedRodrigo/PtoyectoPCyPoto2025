import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * Clase de prueba para verificar la ejecución paralela de algoritmos de sincronización
 * utilizando MPJ (Message Passing Interface)
 */
public class TestEstacionSolarMPJ {
    
    // Método main desactivado - solo para pruebas internas
    // public static void main(String[] args) {
    public static void testMain(String[] args) {
        // System.out.println("=== Iniciando prueba de Estación Solar con MPJ ===");
        
        try {
            // Crear frame principal
            JFrame frame = new JFrame("Test Estación Solar - Ejecución Paralela MPJ");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.setLayout(new BorderLayout());
            
            // Crear panel de grafo dinámico
            PanelGrafoDinamico panelGrafo = new PanelGrafoDinamico();
            panelGrafo.setPreferredSize(new Dimension(400, 600));
            
            // Crear panel de gráficas (para testing)
            GraficasPanel graficasPanel = new GraficasPanel();
            graficasPanel.resetSeries("Comparativa de Eficiencia (MPI)", 
                                     "Núcleo 1 (Mutex)", 
                                     "Núcleo 2 (Semáforos)", 
                                     "Núcleo 3 (Monitores)", 
                                     "Núcleo 4 (Var. Cond)", 
                                     "Núcleo 5 (Barreras)");
            
            // Crear panel de Estación Solar con ejecución paralela
            // System.out.println("Creando panel de Estación Solar con ejecución paralela...");
            EstacionSolarPanelParalelo panelParalelo = new EstacionSolarPanelParalelo(panelGrafo, graficasPanel);
            
            // Agregar componentes al frame
            frame.add(panelParalelo, BorderLayout.CENTER);
            frame.add(panelGrafo, BorderLayout.EAST);
            
            // Panel de control
            JPanel panelControl = new JPanel();
            panelControl.setLayout(new FlowLayout());
            
            JButton btnDetener = new JButton("Detener Simulación");
            btnDetener.addActionListener(e -> {
                // System.out.println("Deteniendo simulación...");
                panelParalelo.detener();
                JOptionPane.showMessageDialog(frame, "Simulación detenida. Ver consola para estadísticas.", 
                                            "Información", JOptionPane.INFORMATION_MESSAGE);
            });
            
            JButton btnInfo = new JButton("Mostrar Información");
            btnInfo.addActionListener(e -> {
                String info = "=== Información de Ejecución Paralela ===\n\n" +
                             "Esta simulación ejecuta 5 algoritmos de sincronización en paralelo:\n" +
                             "• Monitores (synchronized)\n" +
                             "• Semáforos (java.util.concurrent.Semaphore)\n" +
                             "• Variables de Condición (ReentrantLock + Condition)\n" +
                             "• Mutex (ReentrantLock - exclusión mutua)\n" +
                             "• Barreras (CyclicBarrier - sincronización grupal)\n\n" +
                             "Cada algoritmo se ejecuta en un hilo separado,\n" +
                             "permitiendo comparar su rendimiento en tiempo real.\n\n" +
                             "Características:\n" +
                             "• 8 drones simulados\n" +
                             "• 3 bahías de carga compartidas\n" +
                             "• Sistema de prioridad para drones críticos\n" +
                             "• Gestión de energía solar variable\n" +
                             "• Visualización en tiempo real\n" +
                             "• Comparación de 5 métodos de sincronización";
                
                JOptionPane.showMessageDialog(frame, info, "Información", JOptionPane.INFORMATION_MESSAGE);
            });
            
            panelControl.add(btnInfo);
            panelControl.add(btnDetener);
            
            frame.add(panelControl, BorderLayout.SOUTH);
            
            // Hacer visible el frame
            frame.setVisible(true);
            
            System.out.println("Interfaz creada exitosamente");
            // System.out.println("La simulación está ejecutando 5 algoritmos de sincronización en paralelo");
            // System.out.println("Cada algoritmo procesa solicitudes de recarga de drones");
            // System.out.println("Algoritmos activos: Monitores, Semáforos, Variables de Condición, Mutex, Barreras");
            // System.out.println("Use el botón 'Mostrar Información' para más detalles");
            // System.out.println("Use el botón 'Detener Simulación' para finalizar y ver estadísticas");
            
            // Agregar listener para cerrado adecuado
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // System.out.println("Cerrando aplicación...");
                    panelParalelo.detener();
                    System.exit(0);
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error en la prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
}