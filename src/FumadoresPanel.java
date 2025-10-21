import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock; 
import java.util.concurrent.Semaphore; 
import java.util.Random;
import java.util.concurrent.locks.Condition; // Nuevo

public class FumadoresPanel extends JPanel {
    // Definición de recursos
    private static final int TABACO = 0;
    private static final int PAPEL = 1;
    private static final int FOSFOROS = 2;
    private final String[] nombresIngredientes = {"Tabaco", "Papel", "Fósforos"};
    private final Random random = new Random();
    
    // Estado Volátil para la Interfaz (Compartido)
    private volatile boolean[] ingredientesEnMesa = new boolean[3]; // T, P, F
    private volatile boolean[] fumadorFumando = {false, false, false}; // F1, F2, F3
    private volatile String agentePuso = "Esperando";

    private final MonitorMesa monitorMesaMutex;
    private MonitorMesaSemaforo monitorMesaSemaforo; 
    private MonitorMesaCondicion monitorMesaCondicion; // Nuevo
    private final String tipoSincronizacion;
    
    public FumadoresPanel(String tipoSincronizacion) {
        this.tipoSincronizacion = tipoSincronizacion;
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(240, 240, 240)); 
        
        if ("Semáforo".equals(tipoSincronizacion)) {
            monitorMesaMutex = null;
            monitorMesaCondicion = null;
            monitorMesaSemaforo = new MonitorMesaSemaforo(this);
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            monitorMesaMutex = null;
            monitorMesaSemaforo = null;
            monitorMesaCondicion = new MonitorMesaCondicion(this);
        } else { // Mutex
            monitorMesaSemaforo = null;
            monitorMesaCondicion = null;
            monitorMesaMutex = new MonitorMesa(this);
        }
        iniciarSimulacion();
    }

    private void iniciarSimulacion() {
        if ("Semáforo".equals(tipoSincronizacion)) {
            AgenteSemaforo agente = new AgenteSemaforo(monitorMesaSemaforo);
            new Thread(agente).start();
            
            FumadorSemaforo fumador1 = new FumadorSemaforo(0, TABACO, monitorMesaSemaforo);
            new Thread(fumador1).start();
            
            FumadorSemaforo fumador2 = new FumadorSemaforo(1, PAPEL, monitorMesaSemaforo);
            new Thread(fumador2).start();
            
            FumadorSemaforo fumador3 = new FumadorSemaforo(2, FOSFOROS, monitorMesaSemaforo);
            new Thread(fumador3).start();
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            AgenteCondicion agente = new AgenteCondicion(monitorMesaCondicion);
            new Thread(agente).start();
            
            FumadorCondicion fumador1 = new FumadorCondicion(0, TABACO, monitorMesaCondicion);
            new Thread(fumador1).start();
            
            FumadorCondicion fumador2 = new FumadorCondicion(1, PAPEL, monitorMesaCondicion);
            new Thread(fumador2).start();
            
            FumadorCondicion fumador3 = new FumadorCondicion(2, FOSFOROS, monitorMesaCondicion);
            new Thread(fumador3).start();
        } else { // Mutex
            Agente agente = new Agente(monitorMesaMutex);
            new Thread(agente).start();
            
            Fumador fumador1 = new Fumador(0, TABACO, monitorMesaMutex);
            new Thread(fumador1).start();
            
            Fumador fumador2 = new Fumador(1, PAPEL, monitorMesaMutex);
            new Thread(fumador2).start();
            
            Fumador fumador3 = new Fumador(2, FOSFOROS, monitorMesaMutex);
            new Thread(fumador3).start();
        }
    }
    
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

        // --- Dibujar Mesa del Agente (Recurso Compartido) ---
        int anchoMesa = 400;
        int altoMesa = 200;
        int mesaX = centroX - anchoMesa/2;
        int mesaY = centroY - altoMesa/2 - 50;
        
        // Estilo de mesa realista
        g2d.setColor(new Color(150, 90, 45)); // Madera oscura
        g2d.fillRoundRect(mesaX, mesaY, anchoMesa, altoMesa, 30, 30);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(mesaX, mesaY, anchoMesa, altoMesa, 30, 30);
        
        // Área central para ingredientes
        int areaIngredientesX = mesaX + 20;
        int areaIngredientesY = mesaY + 20;
        int areaIngredientesW = anchoMesa - 40;
        int areaIngredientesH = altoMesa - 40;
        g2d.setColor(new Color(230, 230, 230)); // Mantel claro
        g2d.fillRect(areaIngredientesX, areaIngredientesY, areaIngredientesW, areaIngredientesH);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(areaIngredientesX, areaIngredientesY, areaIngredientesW, areaIngredientesH);

        g2d.drawString("Puestos: " + agentePuso, areaIngredientesX + 10, areaIngredientesY + 20);
        
        // Dibujar ingredientes en la mesa (Separación corregida)
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
        

        // --- Dibujar Fumadores ---
        int radio = 250;
        
        // ÁNGULOS CORREGIDOS: F1 (270°), F2 (135°), F3 (45°)
        int[] angulos = {240, 120, 60}; 
        
        for (int i = 0; i < 3; i++) {
            double angulo = Math.toRadians(angulos[i]);
            int fumadorX = (int) (centroX + Math.cos(angulo) * radio);
            int fumadorY = (int) (centroY + Math.sin(angulo) * radio);

            String ingredienteFaltante = nombresIngredientes[i];
            
            // Dibujar fumador (cuerpo y cabeza)
            Color colorFumador = fumadorFumando[i] ? Color.GREEN.darker() : new Color(0, 100, 200); 
            g2d.setColor(colorFumador);
            g2d.fillOval(fumadorX - 25, fumadorY - 25, 50, 50);
            g2d.setColor(Color.WHITE);
            // Uso de fuente constante para etiquetas de fumadores
            g2d.setFont(new Font("Arial", Font.BOLD, 12)); 
            g2d.drawString("F" + (i + 1), fumadorX - 5, fumadorY + 5);
            
            // POSICIÓN DEL TEXTO DEL FUMADOR 
            int offsetTextoX = 0; 
            int offsetTextoY = 0; 
            
            // F1 (Inferior Centro: 270 grados)
            if (i == 0) { 
                offsetTextoX = 100; 
                offsetTextoY = 0; // Posición encima del círculo
            } 
            // F2 (Arriba Izquierda: 135 grados)
            else if (i == 1) { 
                offsetTextoX = -120; 
                offsetTextoY = -55;
            } 
            // F3 (Arriba Derecha: 45 grados)
            else { 
                offsetTextoX = 60; 
                offsetTextoY = -55;
            }

            // Información (Uso de fuente constante)
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 14)); // Fuente constante para el estado
            g2d.drawString("FALTANTE: " + ingredienteFaltante, fumadorX + offsetTextoX, fumadorY + offsetTextoY);
            g2d.drawString(fumadorFumando[i] ? "FUMANDO" : "ESPERANDO MESA", fumadorX + offsetTextoX, fumadorY + offsetTextoY + 20);

            // Dibujar cigarrillo si está fumando
            if (fumadorFumando[i]) {
                dibujarCigarrillo(g2d, fumadorX, fumadorY);
            }
        }
    }
    
    private void dibujarIngrediente(Graphics2D g2d, int id, int x, int y) {
        // Fuente constante
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
        
        // Filtro (Naranja claro)
        g2d.setColor(new Color(255, 190, 120));
        g2d.fillRect(cx - 5, cy, 5, 10);
        
        // Cuerpo (Blanco)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(cx, cy, 35, 10);
        
        // Punta encendida (Rojo/Naranja)
        g2d.setColor(Color.ORANGE.darker());
        g2d.fillOval(cx + 35, cy, 10, 10);
        
        // Humo (Fuente constante)
        g2d.setColor(new Color(150, 150, 150, 150));
        g2d.setFont(new Font("Arial", Font.ITALIC, 20));
        g2d.drawString("~", cx + 50, cy - 5);
        g2d.drawString("~~", cx + 65, cy - 15);
    }

    // ====================================================================
    // CLASES DE SINCRONIZACIÓN (Variable de Condición - Monitor)
    // ====================================================================

    private class MonitorMesaCondicion {
        private final ReentrantLock lock = new ReentrantLock(); 
        private final Condition agenteCond = lock.newCondition(); // Agente espera a que la mesa esté vacía
        private final Condition[] fumadorCond = {lock.newCondition(), lock.newCondition(), lock.newCondition()}; // Un Condition para cada fumador
        
        private boolean[] disponibles = new boolean[3];
        private final FumadoresPanel panel;

        public MonitorMesaCondicion(FumadoresPanel panel) {
            this.panel = panel;
        }

        public void ponerIngredientes() throws InterruptedException {
            lock.lock(); 
            try {
                // Espera a que la mesa esté vacía 
                while (disponibles[TABACO] || disponibles[PAPEL] || disponibles[FOSFOROS]) {
                    agenteCond.await();
                }
                
                int faltante = random.nextInt(3); 
                int ingr1 = (faltante + 1) % 3;
                int ingr2 = (faltante + 2) % 3;
                
                disponibles[ingr1] = true;
                disponibles[ingr2] = true;
                
                panel.actualizarEstado(disponibles, panel.fumadorFumando, 
                                      nombresIngredientes[ingr1] + " y " + nombresIngredientes[ingr2]);
                
                fumadorCond[faltante].signal(); // Despierta al fumador que necesita esos ingredientes
            } finally {
                lock.unlock();
            }
        }

        public void tomarIngredientes(int idFumador, int faltante) throws InterruptedException {
            lock.lock(); 
            try {
                int ingr1 = (faltante + 1) % 3;
                int ingr2 = (faltante + 2) % 3;
                
                // Espera hasta que sus dos ingredientes estén en la mesa
                while (!disponibles[ingr1] || !disponibles[ingr2]) {
                    fumadorCond[faltante].await();
                }

                disponibles[ingr1] = false;
                disponibles[ingr2] = false;
                
                panel.fumadorFumando[idFumador] = true; 
                panel.actualizarEstado(disponibles, panel.fumadorFumando, "Mesa vacía");
                
            } finally {
                lock.unlock(); 
            }
        }
        
        public void terminarFumar(int idFumador) {
            lock.lock();
            try {
                panel.fumadorFumando[idFumador] = false;
                panel.actualizarEstado(disponibles, panel.fumadorFumando, "Esperando");
                agenteCond.signal(); // Despierta al agente para que ponga nuevos ingredientes
            } finally {
                lock.unlock();
            }
        }
    }

    private class AgenteCondicion implements Runnable {
        private final MonitorMesaCondicion monitor;

        public AgenteCondicion(MonitorMesaCondicion monitor) {
            this.monitor = monitor;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    monitor.ponerIngredientes();
                    Thread.sleep(random.nextInt(2000) + 1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class FumadorCondicion implements Runnable {
        private final int id;
        private final int ingredienteFaltante;
        private final MonitorMesaCondicion monitor;

        public FumadorCondicion(int id, int ingredienteFaltante, MonitorMesaCondicion monitor) {
            this.id = id;
            this.ingredienteFaltante = ingredienteFaltante;
            this.monitor = monitor;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    monitor.tomarIngredientes(id, ingredienteFaltante);
                    // Simula fumar
                    Thread.sleep(random.nextInt(3000) + 2000);
                    monitor.terminarFumar(id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN (Mutex - Existente)
    // ====================================================================

    private class MonitorMesa {
        // ... (Se mantiene igual) ...
        private final ReentrantLock mutex = new ReentrantLock(); 
        private boolean[] disponibles = new boolean[3]; 
        private final FumadoresPanel panel;

        public MonitorMesa(FumadoresPanel panel) {
            this.panel = panel;
        }

        public void ponerIngredientes() throws InterruptedException {
            mutex.lock(); 
            try {
                while (disponibles[TABACO] || disponibles[PAPEL] || disponibles[FOSFOROS]) {
                    mutex.unlock(); 
                    Thread.sleep(100); 
                    mutex.lock();
                }
                
                int faltante = random.nextInt(3); 
                int ingr1 = (faltante + 1) % 3;
                int ingr2 = (faltante + 2) % 3;
                
                disponibles[ingr1] = true;
                disponibles[ingr2] = true;
                
                panel.actualizarEstado(disponibles, panel.fumadorFumando, 
                                      nombresIngredientes[ingr1] + " y " + nombresIngredientes[ingr2]);

            } finally {
                if (mutex.isHeldByCurrentThread()) {
                    mutex.unlock();
                }
            }
        }

        public boolean tomarIngredientes(int idFumador, int faltante) throws InterruptedException {
            int ingr1 = (faltante + 1) % 3;
            int ingr2 = (faltante + 2) % 3;

            mutex.lock(); 
            try {
                if (disponibles[ingr1] && disponibles[ingr2]) {
                    disponibles[ingr1] = false;
                    disponibles[ingr2] = false;
                    
                    panel.fumadorFumando[idFumador] = true; 
                    panel.actualizarEstado(disponibles, panel.fumadorFumando, "Mesa vacía");
                    return true;
                }
                return false;
            } finally {
                mutex.unlock(); 
            }
        }
        
        public void terminarFumar(int idFumador) {
            mutex.lock();
            try {
                panel.fumadorFumando[idFumador] = false;
                panel.actualizarEstado(disponibles, panel.fumadorFumando, panel.agentePuso);
            } finally {
                mutex.unlock();
            }
        }
    }

    private class Agente implements Runnable {
        // ... (Se mantiene igual) ...
        private final MonitorMesa monitorMesa;

        public Agente(MonitorMesa monitorMesa) {
            this.monitorMesa = monitorMesa;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    monitorMesa.ponerIngredientes();
                    Thread.sleep(random.nextInt(2000) + 1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class Fumador implements Runnable {
        // ... (Se mantiene igual) ...
        private final int id;
        private final int ingredienteFaltante;
        private final MonitorMesa monitorMesa;

        public Fumador(int id, int ingredienteFaltante, MonitorMesa monitorMesa) {
            this.id = id;
            this.ingredienteFaltante = ingredienteFaltante;
            this.monitorMesa = monitorMesa;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (monitorMesa.tomarIngredientes(id, ingredienteFaltante)) {
                        // Simula fumar
                        Thread.sleep(random.nextInt(3000) + 2000);
                        monitorMesa.terminarFumar(id);
                    } else {
                        Thread.sleep(100); // Espera si no pudo tomar
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ====================================================================
    // CLASES DE SINCRONIZACIÓN (Semáforos - Existente)
    // ====================================================================

    private class MonitorMesaSemaforo {
        // ... (Se mantiene igual) ...
        private final Semaphore agenteSemaforo = new Semaphore(1);
        private final Semaphore[] fumadorSemaforo = {new Semaphore(0), new Semaphore(0), new Semaphore(0)};
        private final boolean[] ingredientesMesa = new boolean[3];
        private final FumadoresPanel panel;

        public MonitorMesaSemaforo(FumadoresPanel panel) {
            this.panel = panel;
        }

        public void ponerIngredientes() throws InterruptedException {
            agenteSemaforo.acquire();
            int faltante = random.nextInt(3);
            int ingr1 = (faltante + 1) % 3;
            int ingr2 = (faltante + 2) % 3;

            ingredientesMesa[ingr1] = true;
            ingredientesMesa[ingr2] = true;
            panel.actualizarEstado(ingredientesMesa, panel.fumadorFumando, nombresIngredientes[ingr1] + " y " + nombresIngredientes[ingr2]);
            
            fumadorSemaforo[faltante].release();
        }

        public void tomarIngredientes(int id, int faltante) throws InterruptedException {
            fumadorSemaforo[faltante].acquire();
            
            int ingr1 = (faltante + 1) % 3;
            int ingr2 = (faltante + 2) % 3;

            ingredientesMesa[ingr1] = false;
            ingredientesMesa[ingr2] = false;
            
            panel.fumadorFumando[id] = true;
            panel.actualizarEstado(ingredientesMesa, panel.fumadorFumando, "Mesa vacía");
        }

        public void terminarFumar(int id) {
            panel.fumadorFumando[id] = false;
            panel.actualizarEstado(ingredientesMesa, panel.fumadorFumando, "Esperando");
            agenteSemaforo.release();
        }
    }

    private class AgenteSemaforo implements Runnable {
        // ... (Se mantiene igual) ...
        private final MonitorMesaSemaforo monitor;

        public AgenteSemaforo(MonitorMesaSemaforo monitor) {
            this.monitor = monitor;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    monitor.ponerIngredientes();
                    Thread.sleep(random.nextInt(1000) + 100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class FumadorSemaforo implements Runnable {
        // ... (Se mantiene igual) ...
        private final int id;
        private final int faltante;
        private final MonitorMesaSemaforo monitor;

        public FumadorSemaforo(int id, int faltante, MonitorMesaSemaforo monitor) {
            this.id = id;
            this.faltante = faltante;
            this.monitor = monitor;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    monitor.tomarIngredientes(id, faltante);
                    Thread.sleep(random.nextInt(3000) + 2000);
                    monitor.terminarFumar(id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}