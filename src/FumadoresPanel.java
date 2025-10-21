import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock; 
import java.util.concurrent.Semaphore; 
import java.util.Random;
import java.util.concurrent.locks.Condition;

public class FumadoresPanel extends JPanel {
    private static final int TABACO = 0;
    private static final int PAPEL = 1;
    private static final int FOSFOROS = 2;
    private final String[] nombresIngredientes = {"Tabaco", "Papel", "Fósforos"};
    private final Random random = new Random();
    
    private volatile boolean[] ingredientesEnMesa = new boolean[3];
    private volatile boolean[] fumadorFumando = {false, false, false};
    private volatile String agentePuso = "Esperando";

    private  MonitorMesa monitorMesaMutex;
    private MonitorMesaSemaforo monitorMesaSemaforo; 
    private MonitorMesaCondicion monitorMesaCondicion; // Variable de Condición
    private MonitorMesaMonitores monitorMesaMonitores; // Monitores
    private final String tipoSincronizacion;
    
    public FumadoresPanel(String tipoSincronizacion) {
        this.tipoSincronizacion = tipoSincronizacion;
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(240, 240, 240)); 
        
        if ("Semáforo".equals(tipoSincronizacion)) {
            monitorMesaSemaforo = new MonitorMesaSemaforo(this);
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            monitorMesaCondicion = new MonitorMesaCondicion(this);
        } else if ("Monitores".equals(tipoSincronizacion)) {
            monitorMesaMonitores = new MonitorMesaMonitores(this);
        } else { // Mutex
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
        } else if ("Monitores".equals(tipoSincronizacion)) {
            AgenteMonitores agente = new AgenteMonitores(monitorMesaMonitores);
            new Thread(agente).start();
            
            FumadorMonitores fumador1 = new FumadorMonitores(0, TABACO, monitorMesaMonitores);
            new Thread(fumador1).start();
            
            FumadorMonitores fumador2 = new FumadorMonitores(1, PAPEL, monitorMesaMonitores);
            new Thread(fumador2).start();
            
            FumadorMonitores fumador3 = new FumadorMonitores(2, FOSFOROS, monitorMesaMonitores);
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
            g2d.drawString("F" + (i + 1), fumadorX - 5, fumadorY + 5);
            
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
    // CLASES DE SINCRONIZACIÓN MONITORES (ReentrantLock / Condition)
    // ====================================================================

    private class MonitorMesaMonitores {
        private final ReentrantLock lock = new ReentrantLock(); 
        private final Condition agenteCond = lock.newCondition(); 
        private final Condition[] fumadorCond = {lock.newCondition(), lock.newCondition(), lock.newCondition()}; 
        
        private boolean[] disponibles = new boolean[3];
        private final FumadoresPanel panel;

        public MonitorMesaMonitores(FumadoresPanel panel) {
            this.panel = panel;
        }

        public void ponerIngredientes() throws InterruptedException {
            lock.lock(); 
            try {
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
                
                fumadorCond[faltante].signal(); 
            } finally {
                lock.unlock();
            }
        }

        public void tomarIngredientes(int idFumador, int faltante) throws InterruptedException {
            lock.lock(); 
            try {
                int ingr1 = (faltante + 1) % 3;
                int ingr2 = (faltante + 2) % 3;
                
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
                agenteCond.signal(); 
            } finally {
                lock.unlock();
            }
        }
    }

    private class AgenteMonitores implements Runnable {
        private final MonitorMesaMonitores monitor;

        public AgenteMonitores(MonitorMesaMonitores monitor) {
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

    private class FumadorMonitores implements Runnable {
        private final int id;
        private final int ingredienteFaltante;
        private final MonitorMesaMonitores monitor;

        public FumadorMonitores(int id, int ingredienteFaltante, MonitorMesaMonitores monitor) {
            this.id = id;
            this.ingredienteFaltante = ingredienteFaltante;
            this.monitor = monitor;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    monitor.tomarIngredientes(id, ingredienteFaltante);
                    Thread.sleep(random.nextInt(3000) + 2000);
                    monitor.terminarFumar(id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN VARIABLE DE CONDICIÓN (ReentrantLock / Condition - Estructuralmente Separada)
    // ====================================================================

    private class MonitorMesaCondicion {
        private final ReentrantLock lock = new ReentrantLock(); 
        private final Condition agenteCond = lock.newCondition(); 
        private final Condition[] fumadorCond = {lock.newCondition(), lock.newCondition(), lock.newCondition()}; 
        
        private boolean[] disponibles = new boolean[3];
        private final FumadoresPanel panel;

        public MonitorMesaCondicion(FumadoresPanel panel) {
            this.panel = panel;
        }

        public void ponerIngredientes() throws InterruptedException {
            lock.lock(); 
            try {
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
                
                fumadorCond[faltante].signal(); 
            } finally {
                lock.unlock();
            }
        }

        public void tomarIngredientes(int idFumador, int faltante) throws InterruptedException {
            lock.lock(); 
            try {
                int ingr1 = (faltante + 1) % 3;
                int ingr2 = (faltante + 2) % 3;
                
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
                agenteCond.signal(); 
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
                    Thread.sleep(random.nextInt(3000) + 2000);
                    monitor.terminarFumar(id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN MUTEX Y SEMÁFORO (EXISTENTES)
    // ====================================================================

    private class MonitorMesa {
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
                        Thread.sleep(random.nextInt(3000) + 2000);
                        monitorMesa.terminarFumar(id);
                    } else {
                        Thread.sleep(100);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class MonitorMesaSemaforo {
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