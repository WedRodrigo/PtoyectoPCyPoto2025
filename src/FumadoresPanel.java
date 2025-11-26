// Archivo: FumadoresPanel.java
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.List;
import java.util.ArrayList;

// No implementa ninguna interfaz extra, solo hereda de JPanel
public class FumadoresPanel extends JPanel implements Simulable {
    private static final int TABACO = 0;
    private static final int PAPEL = 1;
    private static final int FOSFOROS = 2;
    private final String[] nombresIngredientes = {"Tabaco", "Papel", "Fósforos"};
    private final Random random = new Random();

    private volatile boolean[] ingredientesEnMesa = new boolean[3];
    private volatile boolean[] fumadorFumando = {false, false, false};
    private volatile String agentePuso = "Esperando";

    private MonitorMesa monitorMesaMutex;
    private MonitorMesaSemaforo monitorMesaSemaforo;
    private MonitorMesaCondicion monitorMesaCondicion;
    private MonitorMesaMonitores monitorMesaMonitores;
    private final String tipoSincronizacion;

    // Referencia al panel de grafo dinámico (nombre corregido)
    private final PanelGrafoDinamico panelGrafo;

    // Lista para gestionar los hilos de la simulación
    private List<Thread> simulationThreads = new ArrayList<>();

    // Constructor corregido
    public FumadoresPanel(String tipoSincronizacion, PanelGrafoDinamico panelGrafo) {
        this.tipoSincronizacion = tipoSincronizacion;
        this.panelGrafo = panelGrafo; // Guardar la referencia correcta

        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(240, 240, 240));

        // La inicialización del grafo (creación de nodos) se hace en panelGrafo.inicializarGrafo("Fumadores")
        // No es necesario hacerlo aquí explícitamente.

        // Lógica de selección de monitor sin cambios
        if ("Semáforo".equals(tipoSincronizacion)) {
            monitorMesaSemaforo = new MonitorMesaSemaforo(this, panelGrafo); // Pasar panelGrafo
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            monitorMesaCondicion = new MonitorMesaCondicion(this, panelGrafo); // Pasar panelGrafo
        } else if ("Monitores".equals(tipoSincronizacion)) {
            monitorMesaMonitores = new MonitorMesaMonitores(this, panelGrafo); // Pasar panelGrafo
        } else { // Mutex
            monitorMesaMutex = new MonitorMesa(this, panelGrafo); // Pasar panelGrafo
        }
        iniciarSimulacion();
    }

    // Método para detener los hilos de la simulación
    public void detener() {
        for (Thread t : simulationThreads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        simulationThreads.clear();
        System.out.println("Simulación de Fumadores detenida.");
        // Opcional: Limpiar grafo al detener
        // if (panelGrafo != null) {
        //     panelGrafo.inicializarGrafo("Vacio");
        // }
    }

    // IniciarSimulacion sin cambios en la lógica principal, solo pasa panelGrafo si es necesario
    private void iniciarSimulacion() {
        simulationThreads.clear(); // Limpiar hilos anteriores

        if ("Semáforo".equals(tipoSincronizacion)) {
            AgenteSemaforo agente = new AgenteSemaforo(monitorMesaSemaforo);
            simulationThreads.add(new Thread(agente));
            FumadorSemaforo f0 = new FumadorSemaforo(0, TABACO, monitorMesaSemaforo);
            simulationThreads.add(new Thread(f0));
            FumadorSemaforo f1 = new FumadorSemaforo(1, PAPEL, monitorMesaSemaforo);
            simulationThreads.add(new Thread(f1));
            FumadorSemaforo f2 = new FumadorSemaforo(2, FOSFOROS, monitorMesaSemaforo);
            simulationThreads.add(new Thread(f2));

        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            AgenteCondicion agente = new AgenteCondicion(monitorMesaCondicion);
            simulationThreads.add(new Thread(agente));
            FumadorCondicion f0 = new FumadorCondicion(0, TABACO, monitorMesaCondicion);
            simulationThreads.add(new Thread(f0));
            FumadorCondicion f1 = new FumadorCondicion(1, PAPEL, monitorMesaCondicion);
            simulationThreads.add(new Thread(f1));
            FumadorCondicion f2 = new FumadorCondicion(2, FOSFOROS, monitorMesaCondicion);
            simulationThreads.add(new Thread(f2));

        } else if ("Monitores".equals(tipoSincronizacion)) {
            AgenteMonitores agente = new AgenteMonitores(monitorMesaMonitores);
            simulationThreads.add(new Thread(agente));
            FumadorMonitores f0 = new FumadorMonitores(0, TABACO, monitorMesaMonitores);
            simulationThreads.add(new Thread(f0));
            FumadorMonitores f1 = new FumadorMonitores(1, PAPEL, monitorMesaMonitores);
            simulationThreads.add(new Thread(f1));
            FumadorMonitores f2 = new FumadorMonitores(2, FOSFOROS, monitorMesaMonitores);
            simulationThreads.add(new Thread(f2));

        } else { // Mutex
            Agente agente = new Agente(monitorMesaMutex);
            simulationThreads.add(new Thread(agente));
            Fumador f0 = new Fumador(0, TABACO, monitorMesaMutex);
            simulationThreads.add(new Thread(f0));
            Fumador f1 = new Fumador(1, PAPEL, monitorMesaMutex);
            simulationThreads.add(new Thread(f1));
            Fumador f2 = new Fumador(2, FOSFOROS, monitorMesaMutex);
            simulationThreads.add(new Thread(f2));
        }

        // Iniciar todos los hilos
        for (Thread t : simulationThreads) {
            t.start();
        }
    }

    // --- Métodos de UI (sin cambios) ---
    public void actualizarEstado(boolean[] mesa, boolean[] fumando, String agentePuso) {
        SwingUtilities.invokeLater(() -> {
            System.arraycopy(mesa, 0, this.ingredientesEnMesa, 0, 3);
            System.arraycopy(fumando, 0, this.fumadorFumando, 0, 3);
            this.agentePuso = agentePuso;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centroX = getWidth() / 2;
        int centroY = getHeight() / 2;

        int anchoMesa = 400;
        int altoMesa = 200;
        int mesaX = centroX - anchoMesa/2;
        int mesaY = centroY - altoMesa/2 - 50;

        g2d.setColor(new Color(150, 90, 45));
        g2d.fillRoundRect(mesaX, mesaY, anchoMesa, altoMesa, 30, 30);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(mesaX, mesaY, anchoMesa, altoMesa, 30, 30);

        int areaIngredientesX = mesaX + 20;
        int areaIngredientesY = mesaY + 20;
        int areaIngredientesW = anchoMesa - 40;
        int areaIngredientesH = altoMesa - 40;
        g2d.setColor(new Color(230, 230, 230));
        g2d.fillRect(areaIngredientesX, areaIngredientesY, areaIngredientesW, areaIngredientesH);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(areaIngredientesX, areaIngredientesY, areaIngredientesW, areaIngredientesH);

        g2d.drawString("Puestos: " + agentePuso, areaIngredientesX + 10, areaIngredientesY + 20);

        int posT = areaIngredientesX + 30;
        int posP = areaIngredientesX + 150;
        int posF = areaIngredientesX + 270;

        int posY = areaIngredientesY + 50;

        if (ingredientesEnMesa[TABACO]) {
            dibujarIngrediente(g2d, TABACO, posT, posY);
        }
        if (ingredientesEnMesa[PAPEL]) {
            dibujarIngrediente(g2d, PAPEL, posP, posY);
        }
        if (ingredientesEnMesa[FOSFOROS]) {
            dibujarIngrediente(g2d, FOSFOROS, posF, posY);
        }

        int radio = 250;
        int[] angulos = {240, 120, 60};

        for (int i = 0; i < 3; i++) {
            double angulo = Math.toRadians(angulos[i]);
            int fumadorX = (int) (centroX + Math.cos(angulo) * radio);
            int fumadorY = (int) (centroY + Math.sin(angulo) * radio);

            String ingredienteFaltante = nombresIngredientes[i];

            Color colorFumador = fumadorFumando[i] ? Color.GREEN.darker() : new Color(0, 100, 200);
            g2d.setColor(colorFumador);
            g2d.fillOval(fumadorX - 25, fumadorY - 25, 50, 50);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("F" + (i), fumadorX - 5, fumadorY + 5);

            int offsetTextoX = 0;
            int offsetTextoY = 0;

            if (i == 0) {
                offsetTextoX = 100;
                offsetTextoY = 0;
            } else if (i == 1) {
                offsetTextoX = -120;
                offsetTextoY = -55;
            } else {
                offsetTextoX = 60;
                offsetTextoY = -55;
            }

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14));
            g2d.drawString("FALTANTE: " + ingredienteFaltante, fumadorX + offsetTextoX, fumadorY + offsetTextoY);
            g2d.drawString(fumadorFumando[i] ? "FUMANDO" : "ESPERANDO MESA", fumadorX + offsetTextoX, fumadorY + offsetTextoY + 20);

            if (fumadorFumando[i]) {
                dibujarCigarrillo(g2d, fumadorX, fumadorY);
            }
        }
    }

    private void dibujarIngrediente(Graphics2D g2d, int id, int x, int y) {
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        switch (id) {
            case TABACO:
                g2d.setColor(new Color(130, 80, 50));
                g2d.fillRoundRect(x, y, 90, 30, 5, 5);
                g2d.setColor(new Color(150, 100, 70));
                g2d.drawString("Tabaco", x + 5, y + 20);
                break;
            case PAPEL:
                g2d.setColor(Color.WHITE);
                g2d.fillRect(x, y, 90, 30);
                g2d.setColor(Color.GRAY);
                g2d.drawRect(x, y, 90, 30);
                g2d.setColor(Color.BLACK);
                g2d.drawString("Papel", x + 25, y + 20);
                break;
            case FOSFOROS:
                g2d.setColor(Color.RED);
                g2d.fillRect(x, y, 30, 50);
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(x + 30, y, 60, 50);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, 90, 50);
                g2d.drawString("Fósforos", x + 5, y + 30);
                break;
        }
    }

    private void dibujarCigarrillo(Graphics2D g2d, int x, int y) {
        int cx = x + 30;
        int cy = y - 5;

        g2d.setColor(new Color(255, 190, 120));
        g2d.fillRect(cx - 5, cy, 5, 10);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(cx, cy, 35, 10);
        g2d.setColor(Color.ORANGE.darker());
        g2d.fillOval(cx + 35, cy, 10, 10);
        g2d.setColor(new Color(150, 150, 150, 150));
        g2d.setFont(new Font("Arial", Font.ITALIC, 20));
        g2d.drawString("~", cx + 50, cy - 5);
        g2d.drawString("~~", cx + 65, cy - 15);
    }

    // ====================================================================
    // CLASES DE SINCRONIZACIÓN (Corregidas para usar panelGrafo)
    // ====================================================================

    // --- MONITORES ---
    private class MonitorMesaMonitores {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition agenteCond = lock.newCondition();
        private final Condition[] fumadorCond = {lock.newCondition(), lock.newCondition(), lock.newCondition()};
        private boolean[] disponibles = new boolean[3];
        private final FumadoresPanel panel;
        private final PanelGrafoDinamico panelGrafo;

        public MonitorMesaMonitores(FumadoresPanel panel, PanelGrafoDinamico panelGrafo) {
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }

        public void ponerIngredientes() throws InterruptedException {
            panelGrafo.setFlechaSolicitud("P-A", "R-M"); // Agente solicita la mesa
            lock.lock();
            try {
                while (disponibles[TABACO] || disponibles[PAPEL] || disponibles[FOSFOROS]) {
                    panelGrafo.removerFlechas("P-A", "R-M"); // Quita solicitud mientras espera
                    agenteCond.await();
                    panelGrafo.setFlechaSolicitud("P-A", "R-M"); // Vuelve a solicitar al despertar
                }
                panelGrafo.setFlechaAsignacion("P-A", "R-M"); // Agente obtiene la mesa

                int faltante = random.nextInt(3);
                int ingr1 = (faltante + 1) % 3;
                int ingr2 = (faltante + 2) % 3;
                disponibles[ingr1] = true;
                disponibles[ingr2] = true;
                panel.actualizarEstado(disponibles, panel.fumadorFumando,
                                       nombresIngredientes[ingr1] + " y " + nombresIngredientes[ingr2]);

                fumadorCond[faltante].signal();
            } finally {
                // El agente libera la mesa inmediatamente después de señalar al fumador
                panelGrafo.removerFlechas("P-A", "R-M");
                lock.unlock();
            }
        }

        public void tomarIngredientes(int idFumador, int faltante) throws InterruptedException {
            lock.lock();
            try {
                int ingr1 = (faltante + 1) % 3;
                int ingr2 = (faltante + 2) % 3;
                String fumadorId = "P" + idFumador;

                panelGrafo.setFlechaSolicitud(fumadorId, "R-M"); // Fumador solicita

                while (!disponibles[ingr1] || !disponibles[ingr2]) {
                     panelGrafo.removerFlechas(fumadorId, "R-M"); // Quita solicitud mientras espera
                     fumadorCond[faltante].await();
                     panelGrafo.setFlechaSolicitud(fumadorId, "R-M"); // Vuelve a solicitar
                }
                panelGrafo.setFlechaAsignacion(fumadorId, "R-M"); // Fumador obtiene la mesa

                disponibles[ingr1] = false;
                disponibles[ingr2] = false;
                panel.fumadorFumando[idFumador] = true;
                panel.actualizarEstado(disponibles, panel.fumadorFumando, "Mesa vacía");

            } finally {
                lock.unlock();
                 // El fumador MANTIENE la "mesa" (el lock implícito) mientras fuma
            }
        }

        public void terminarFumar(int idFumador) {
            lock.lock();
            try {
                panel.fumadorFumando[idFumador] = false;
                panel.actualizarEstado(disponibles, panel.fumadorFumando, "Esperando");
                panelGrafo.removerFlechas("P" + idFumador, "R-M"); // Fumador libera la mesa
                agenteCond.signal(); // Avisa al agente que la mesa está libre
            } finally {
                lock.unlock();
            }
        }
    }
    // (AgenteMonitores y FumadorMonitores sin cambios funcionales, solo usan el monitor corregido)
    private class AgenteMonitores implements Runnable {
        private final MonitorMesaMonitores monitor;
        public AgenteMonitores(MonitorMesaMonitores monitor) { this.monitor = monitor; }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    monitor.ponerIngredientes();
                    Thread.sleep(random.nextInt(2000) + 1000);
                }
            } catch (InterruptedException e) { System.out.println("Agente (Monitor) detenido."); }
            // Opcional: limpiar flechas al detener
            // finally { panelGrafo.removerFlechas("P-A", "R-M"); }
        }
    }
    private class FumadorMonitores implements Runnable {
        private final int id;
        private final int ingredienteFaltante;
        private final MonitorMesaMonitores monitor;
        public FumadorMonitores(int id, int ingredienteFaltante, MonitorMesaMonitores monitor) {
            this.id = id; this.ingredienteFaltante = ingredienteFaltante; this.monitor = monitor;
        }
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    monitor.tomarIngredientes(id, ingredienteFaltante);
                    Thread.sleep(random.nextInt(3000) + 2000); // Fuma
                    monitor.terminarFumar(id);
                }
            } catch (InterruptedException e) { System.out.println("Fumador (Monitor) " + id + " detenido."); }
            // Opcional: limpiar flechas al detener
            // finally { panelGrafo.removerFlechas("P" + id, "R-M"); }
        }
    }


    // --- VARIABLE DE CONDICIÓN ---
    // (La lógica es idéntica a Monitores, solo cambian los nombres de las clases)
    private class MonitorMesaCondicion {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition agenteCond = lock.newCondition();
        private final Condition[] fumadorCond = {lock.newCondition(), lock.newCondition(), lock.newCondition()};
        private boolean[] disponibles = new boolean[3];
        private final FumadoresPanel panel;
        private final PanelGrafoDinamico panelGrafo;

        public MonitorMesaCondicion(FumadoresPanel panel, PanelGrafoDinamico panelGrafo) {
             this.panel = panel; this.panelGrafo = panelGrafo;
        }
         public void ponerIngredientes() throws InterruptedException {
            panelGrafo.setFlechaSolicitud("P-A", "R-M");
            lock.lock();
            try {
                while (disponibles[TABACO] || disponibles[PAPEL] || disponibles[FOSFOROS]) {
                    panelGrafo.removerFlechas("P-A", "R-M");
                    agenteCond.await();
                    panelGrafo.setFlechaSolicitud("P-A", "R-M");
                }
                panelGrafo.setFlechaAsignacion("P-A", "R-M");
                int faltante = random.nextInt(3);
                int ingr1 = (faltante + 1) % 3; int ingr2 = (faltante + 2) % 3;
                disponibles[ingr1] = true; disponibles[ingr2] = true;
                panel.actualizarEstado(disponibles, panel.fumadorFumando,
                                       nombresIngredientes[ingr1] + " y " + nombresIngredientes[ingr2]);
                fumadorCond[faltante].signal();
            } finally {
                panelGrafo.removerFlechas("P-A", "R-M");
                lock.unlock();
            }
        }
        public void tomarIngredientes(int idFumador, int faltante) throws InterruptedException {
            lock.lock();
            try {
                int ingr1 = (faltante + 1) % 3; int ingr2 = (faltante + 2) % 3;
                String fumadorId = "P" + idFumador;
                panelGrafo.setFlechaSolicitud(fumadorId, "R-M");
                while (!disponibles[ingr1] || !disponibles[ingr2]) {
                     panelGrafo.removerFlechas(fumadorId, "R-M");
                     fumadorCond[faltante].await();
                     panelGrafo.setFlechaSolicitud(fumadorId, "R-M");
                }
                panelGrafo.setFlechaAsignacion(fumadorId, "R-M");
                disponibles[ingr1] = false; disponibles[ingr2] = false;
                panel.fumadorFumando[idFumador] = true;
                panel.actualizarEstado(disponibles, panel.fumadorFumando, "Mesa vacía");
            } finally { lock.unlock(); }
        }
        public void terminarFumar(int idFumador) {
            lock.lock();
            try {
                panel.fumadorFumando[idFumador] = false;
                panel.actualizarEstado(disponibles, panel.fumadorFumando, "Esperando");
                panelGrafo.removerFlechas("P" + idFumador, "R-M");
                agenteCond.signal();
            } finally { lock.unlock(); }
        }
    }
    private class AgenteCondicion implements Runnable {
        private final MonitorMesaCondicion monitor;
        public AgenteCondicion(MonitorMesaCondicion monitor) { this.monitor = monitor; }
        @Override
        public void run() {
            try { while (!Thread.currentThread().isInterrupted()) { monitor.ponerIngredientes(); Thread.sleep(random.nextInt(2000) + 1000); }
            } catch (InterruptedException e) { System.out.println("Agente (Condicion) detenido."); }
        }
    }
    private class FumadorCondicion implements Runnable {
        private final int id; private final int ingredienteFaltante; private final MonitorMesaCondicion monitor;
        public FumadorCondicion(int id, int ingredienteFaltante, MonitorMesaCondicion monitor) {
            this.id = id; this.ingredienteFaltante = ingredienteFaltante; this.monitor = monitor; }
        @Override
        public void run() {
            try { while (!Thread.currentThread().isInterrupted()) { monitor.tomarIngredientes(id, ingredienteFaltante); Thread.sleep(random.nextInt(3000) + 2000); monitor.terminarFumar(id); }
            } catch (InterruptedException e) { System.out.println("Fumador (Condicion) " + id + " detenido."); }
        }
    }

    // --- MUTEX ---
    private class MonitorMesa {
        private final ReentrantLock mutex = new ReentrantLock();
        private boolean[] disponibles = new boolean[3];
        private final FumadoresPanel panel;
        private final PanelGrafoDinamico panelGrafo;

        public MonitorMesa(FumadoresPanel panel, PanelGrafoDinamico panelGrafo) {
             this.panel = panel; this.panelGrafo = panelGrafo;
        }

        public void ponerIngredientes() throws InterruptedException {
            panelGrafo.setFlechaSolicitud("P-A", "R-M");
            mutex.lock();
            panelGrafo.setFlechaAsignacion("P-A", "R-M");
            try {
                // Con Mutex, no podemos esperar eficientemente, hacemos busy-waiting simulado
                while (disponibles[TABACO] || disponibles[PAPEL] || disponibles[FOSFOROS]) {
                    panelGrafo.removerFlechas("P-A", "R-M"); // Suelta mientras "espera"
                    mutex.unlock();
                    Thread.sleep(100); // Espera activa corta
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    panelGrafo.setFlechaSolicitud("P-A", "R-M"); // Vuelve a solicitar
                    mutex.lock();
                    panelGrafo.setFlechaAsignacion("P-A", "R-M"); // Intenta tomar de nuevo
                }

                int faltante = random.nextInt(3);
                int ingr1 = (faltante + 1) % 3; int ingr2 = (faltante + 2) % 3;
                disponibles[ingr1] = true; disponibles[ingr2] = true;
                panel.actualizarEstado(disponibles, panel.fumadorFumando,
                                       nombresIngredientes[ingr1] + " y " + nombresIngredientes[ingr2]);
            } finally {
                panelGrafo.removerFlechas("P-A", "R-M"); // Agente siempre libera la mesa al final
                if (mutex.isHeldByCurrentThread()) { // Asegurarse de liberar
                   mutex.unlock();
                }
            }
        }

        public boolean tomarIngredientes(int idFumador, int faltante) throws InterruptedException {
            String fumadorId = "P" + idFumador;
            int ingr1 = (faltante + 1) % 3;
            int ingr2 = (faltante + 2) % 3;

            panelGrafo.setFlechaSolicitud(fumadorId, "R-M");
            mutex.lock();
            try {
                if (disponibles[ingr1] && disponibles[ingr2]) {
                    panelGrafo.setFlechaAsignacion(fumadorId, "R-M"); // Fumador obtiene la mesa
                    disponibles[ingr1] = false;
                    disponibles[ingr2] = false;
                    panel.fumadorFumando[idFumador] = true;
                    panel.actualizarEstado(disponibles, panel.fumadorFumando, "Mesa vacía");
                    return true; // Mantiene el lock mientras fuma
                } else {
                    panelGrafo.removerFlechas(fumadorId, "R-M"); // No pudo tomar, quita solicitud
                    return false; // Libera el lock
                }
            } finally {
                 // Solo liberar si no se tomaron los ingredientes
                 if (!panel.fumadorFumando[idFumador] && mutex.isHeldByCurrentThread()) {
                     mutex.unlock();
                 }
            }
        }

        public void terminarFumar(int idFumador) {
           // Asume que el fumador aún tiene el lock
            try {
                panel.fumadorFumando[idFumador] = false;
                panel.actualizarEstado(disponibles, panel.fumadorFumando, "Esperando");
                panelGrafo.removerFlechas("P" + idFumador, "R-M"); // Fumador libera la mesa
            } finally {
                 if (mutex.isHeldByCurrentThread()) {
                    mutex.unlock();
                 }
            }
        }
    }
    // (Agente y Fumador (Mutex) sin cambios funcionales)
     private class Agente implements Runnable {
        private final MonitorMesa monitorMesa;
        public Agente(MonitorMesa monitorMesa) { this.monitorMesa = monitorMesa; }
        @Override
        public void run() {
            try { while (!Thread.currentThread().isInterrupted()) { monitorMesa.ponerIngredientes(); Thread.sleep(random.nextInt(2000) + 1000); }
            } catch (InterruptedException e) { System.out.println("Agente (Mutex) detenido."); }
        }
    }
    private class Fumador implements Runnable {
        private final int id; private final int ingredienteFaltante; private final MonitorMesa monitorMesa;
        public Fumador(int id, int ingredienteFaltante, MonitorMesa monitorMesa) {
            this.id = id; this.ingredienteFaltante = ingredienteFaltante; this.monitorMesa = monitorMesa; }
        @Override
        public void run() {
            try { while (!Thread.currentThread().isInterrupted()) {
                    if (monitorMesa.tomarIngredientes(id, ingredienteFaltante)) {
                        Thread.sleep(random.nextInt(3000) + 2000); // Fuma
                        monitorMesa.terminarFumar(id);
                    } else { Thread.sleep(100); /* Espera activa */ }
                }
            } catch (InterruptedException e) { System.out.println("Fumador (Mutex) " + id + " detenido."); }
        }
    }


    // --- SEMÁFORO ---
    private class MonitorMesaSemaforo {
        private final Semaphore agenteSemaforo = new Semaphore(1); // Controla acceso del agente a la mesa
        private final Semaphore[] fumadorSemaforo = {new Semaphore(0), new Semaphore(0), new Semaphore(0)}; // Cada fumador espera aquí
        private final boolean[] ingredientesMesa = new boolean[3];
        private final FumadoresPanel panel;
        private final PanelGrafoDinamico panelGrafo;

        public MonitorMesaSemaforo(FumadoresPanel panel, PanelGrafoDinamico panelGrafo) {
             this.panel = panel; this.panelGrafo = panelGrafo;
        }

        public void ponerIngredientes() throws InterruptedException {
            panelGrafo.setFlechaSolicitud("P-A", "R-M"); // Agente quiere la mesa
            agenteSemaforo.acquire(); // Espera que la mesa esté libre (fumador terminó)
            panelGrafo.setFlechaAsignacion("P-A", "R-M"); // Agente toma la mesa

            int faltante = random.nextInt(3);
            int ingr1 = (faltante + 1) % 3; int ingr2 = (faltante + 2) % 3;
            ingredientesMesa[ingr1] = true; ingredientesMesa[ingr2] = true;
            panel.actualizarEstado(ingredientesMesa, panel.fumadorFumando, nombresIngredientes[ingr1] + " y " + nombresIngredientes[ingr2]);

            // Agente libera la mesa INMEDIATAMENTE después de poner y antes de señalar
            panelGrafo.removerFlechas("P-A", "R-M");
            // agenteSemaforo.release(); <-- NO SE LIBERA AQUÍ, lo libera el fumador

            fumadorSemaforo[faltante].release(); // Avisa al fumador correspondiente
        }

        public void tomarIngredientes(int id, int faltante) throws InterruptedException {
            String fumadorId = "P" + id;
            panelGrafo.setFlechaSolicitud(fumadorId, "R-M"); // Fumador quiere ingredientes/mesa
            fumadorSemaforo[faltante].acquire(); // Espera su turno (señal del agente)
            panelGrafo.setFlechaAsignacion(fumadorId, "R-M"); // Fumador "toma" la mesa (lógicamente)

            int ingr1 = (faltante + 1) % 3; int ingr2 = (faltante + 2) % 3;
            ingredientesMesa[ingr1] = false; ingredientesMesa[ingr2] = false;
            panel.fumadorFumando[id] = true;
            panel.actualizarEstado(ingredientesMesa, panel.fumadorFumando, "Mesa vacía");
            // El fumador mantiene la "mesa" lógicamente ocupada mientras fuma
        }

        public void terminarFumar(int id) {
            panel.fumadorFumando[id] = false;
            panel.actualizarEstado(ingredientesMesa, panel.fumadorFumando, "Esperando");
            panelGrafo.removerFlechas("P" + id, "R-M"); // Fumador libera la mesa
            agenteSemaforo.release(); // Libera al agente para que pueda poner más ingredientes
        }
    }
    // (AgenteSemaforo y FumadorSemaforo sin cambios funcionales)
    private class AgenteSemaforo implements Runnable {
        private final MonitorMesaSemaforo monitor;
        public AgenteSemaforo(MonitorMesaSemaforo monitor) { this.monitor = monitor; }
        @Override
        public void run() {
            try { while (!Thread.currentThread().isInterrupted()) { monitor.ponerIngredientes(); Thread.sleep(random.nextInt(1000) + 100); }
            } catch (InterruptedException e) { System.out.println("Agente (Semaforo) detenido."); }
        }
    }
    private class FumadorSemaforo implements Runnable {
        private final int id; private final int faltante; private final MonitorMesaSemaforo monitor;
        public FumadorSemaforo(int id, int faltante, MonitorMesaSemaforo monitor) {
            this.id = id; this.faltante = faltante; this.monitor = monitor; }
        @Override
        public void run() {
            try { while (!Thread.currentThread().isInterrupted()) { monitor.tomarIngredientes(id, faltante); Thread.sleep(random.nextInt(3000) + 2000); /*Fuma*/ monitor.terminarFumar(id); }
            } catch (InterruptedException e) { System.out.println("Fumador (Semaforo) " + id + " detenido."); }
        }
    }
}