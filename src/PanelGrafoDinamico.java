import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class PanelGrafoDinamico extends JPanel {

    // Usamos ConcurrentHashMap para seguridad entre hilos
    private final ConcurrentHashMap<String, Nodo> nodos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Flecha> flechas = new ConcurrentHashMap<>();
    private String tituloProblema = "Grafo de Asignación de Recursos";

    public PanelGrafoDinamico() {
        setBackground(new Color(255, 255, 224));
    }

    /**
     * Prepara los nodos iniciales para un problema específico.
     */
    public void inicializarGrafo(String problema) {
        nodos.clear();
        flechas.clear();
        tituloProblema = "Grafo: " + problema;

        switch (problema) {
            case "ProductorConsumidor":
                nodos.put("P1", new Nodo(100, 200, "Proceso", "P1 (Prod)"));
                nodos.put("R1", new Nodo(300, 200, "Recurso", "R1 (Tanque)"));
                nodos.put("P2", new Nodo(500, 200, "Proceso", "P2 (Cons)"));
                break;
            
            case "CenaFilosofos":
                int numFilosofos = 5;
                int centroX = 350, centroY = 250, radioP = 200, radioR = 100;
                for (int i = 0; i < numFilosofos; i++) {
                    double angulo = Math.toRadians(i * (360.0 / numFilosofos) - 90);
                    // Nodos de Procesos (Filósofos)
                    nodos.put("P" + i, new Nodo(
                        (int) (centroX + Math.cos(angulo) * radioP) - 30,
                        (int) (centroY + Math.sin(angulo) * radioP) - 30,
                        "Proceso", "P" + i
                    ));
                    // Nodos de Recursos (Tenedores)
                    double anguloR = Math.toRadians((i + 0.5) * (360.0 / numFilosofos) - 90);
                    nodos.put("R" + i, new Nodo(
                        (int) (centroX + Math.cos(anguloR) * radioR) - 30,
                        (int) (centroY + Math.sin(anguloR) * radioR) - 30,
                        "Recurso", "R" + i
                    ));
                }
                break;

            case "BarberoDormilon":
                nodos.put("P-B", new Nodo(150, 150, "Proceso", "Barbero"));
                nodos.put("R-Silla", new Nodo(300, 150, "Recurso", "Silla"));
                nodos.put("R-Espera", new Nodo(300, 300, "Recurso", "Espera (4)"));
                // Los clientes (P-C1, P-C2...) se crearán dinámicamente
                break;

            case "EscritorLector":
                nodos.put("R1", new Nodo(300, 200, "Recurso", "Pizarra"));
                // Los lectores (P-L) y escritores (P-E) se crean dinámicamente
                break;

            case "Fumadores":
                nodos.put("P-A", new Nodo(300, 100, "Proceso", "Agente"));
                nodos.put("R-M", new Nodo(300, 250, "Recurso", "Mesa"));
                nodos.put("P0", new Nodo(100, 400, "Proceso", "F0 (Tab)"));
                nodos.put("P1", new Nodo(300, 400, "Proceso", "F1 (Pap)"));
                nodos.put("P2", new Nodo(500, 400, "Proceso", "F2 (Fos)"));
                break;

            case "EstacionSolar":
                for (int i = 0; i < 5; i++) {
                    nodos.put("D" + i, new Nodo(80 + i * 120, 380, "Proceso", "D" + i));
                }
                nodos.put("E", new Nodo(320, 150, "Recurso", "Energía"));
                nodos.put("AE", new Nodo(520, 150, "Proceso", "Admin E"));
                nodos.put("B0", new Nodo(320, 250, "Recurso", "Bahías (3)"));
                break;

            case "Vacio":
            default:
                tituloProblema = "Grafo de Asignación de Recursos";
                break;
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    /**
     * Método seguro para hilos para crear un nodo si no existe.
     */
    public void crearNodoSiNoExiste(String id, int x, int y, String tipo, String etiqueta) {
        nodos.putIfAbsent(id, new Nodo(x, y, tipo, etiqueta));
        SwingUtilities.invokeLater(this::repaint);
    }

    /**
     * Método para agregar o actualizar un nodo con información.
     */
    public void agregarNodo(String id, String informacion) {
        Nodo nodo = nodos.get(id);
        if (nodo != null) {
            nodo.setInformacion(informacion);
        }
        SwingUtilities.invokeLater(this::repaint);
    }

    /**
     * Dibuja una flecha de solicitud (Proceso -> Recurso)
     */
    public void setFlechaSolicitud(String idProceso, String idRecurso) {
        Nodo p = nodos.get(idProceso);
        Nodo r = nodos.get(idRecurso);
        if (p != null && r != null) {
            String clave = idProceso + "->" + idRecurso; 
            flechas.put(clave, new Flecha(p, r));
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    /**
     * Dibuja una flecha de asignación (Recurso -> Proceso)
     * y elimina la flecha de solicitud si existía.
     */
    public void setFlechaAsignacion(String idProceso, String idRecurso) {
        Nodo p = nodos.get(idProceso);
        Nodo r = nodos.get(idRecurso);
        if (p != null && r != null) {
            flechas.remove(idProceso + "->" + idRecurso); // Elimina P->R
            String clave = idRecurso + "->" + idProceso; // Añade R->P
            flechas.put(clave, new Flecha(r, p));
            SwingUtilities.invokeLater(this::repaint);
        }
    }

    /**
     * Elimina todas las flechas (solicitud o asignación) entre un proceso y un recurso.
     */
    public void removerFlechas(String idProceso, String idRecurso) {
        flechas.remove(idProceso + "->" + idRecurso); // Elimina P->R
        flechas.remove(idRecurso + "->" + idProceso); // Elimina R->P
        SwingUtilities.invokeLater(this::repaint);
    }
    
    /**
     * Elimina un nodo del grafo (ej. un cliente que se va).
     */
    public void removerNodo(String id) {
        nodos.remove(id);
        // Eliminar flechas asociadas (simplificado)
        flechas.keySet().removeIf(key -> key.contains(id));
        SwingUtilities.invokeLater(this::repaint);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dibujar título
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString(tituloProblema, 20, 30);

        // Dibujar flechas
        for (Flecha f : flechas.values()) {
            f.dibujar(g2d);
        }

        // Dibujar nodos
        for (Nodo n : nodos.values()) {
            n.dibujar(g2d);
        }
    }
}
