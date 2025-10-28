import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.atomic.AtomicInteger;

public class EscritorLectorPanel extends JPanel {
    // ... (Constantes sin cambios) ...
    private static final int ANCHO_PIZARRA = 400;
    private static final int ALTO_PIZARRA = 250;
    private static final int TAMANO_PERSONA = 50;
    
    private volatile int lectoresActivos = 0;
    private volatile boolean escritorActivo = false;
    private volatile int lectoresEnEspera = 0;
    private volatile int escritoresEnEspera = 0;
    private volatile String textoPizarra = "Esperando Escritor...";
    
    private final Random random = new Random();
    private final String tipoSincronizacion;
    private final PanelGrafoDinamico panelGrafo; // <-- Referencia al grafo
    
    private MonitorPizarra monitorPizarraMutex;
    private MonitorPizarraSemaforo monitorPizarraSemaforo;
    private MonitorPizarraCondicion monitorPizarraCondicion;
    private MonitorPizarraMonitores monitorPizarraMonitores;
    
    // Contadores atómicos para IDs únicos
    private final AtomicInteger lectorIdCounter = new AtomicInteger(1);
    private final AtomicInteger escritorIdCounter = new AtomicInteger(1);

    // Constructor modificado
    public EscritorLectorPanel(String tipoSincronizacion, PanelGrafoDinamico panelGrafo) {
        this.tipoSincronizacion = tipoSincronizacion;
        this.panelGrafo = panelGrafo; // <-- Guardar referencia
        setBackground(new Color(240, 240, 240));
        
        if ("Semáforo".equals(tipoSincronizacion)) {
            monitorPizarraSemaforo = new MonitorPizarraSemaforo(this);
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            monitorPizarraCondicion = new MonitorPizarraCondicion(this);
        } else if ("Monitores".equals(tipoSincronizacion)) {
            monitorPizarraMonitores = new MonitorPizarraMonitores(this);
        } else {
            monitorPizarraMutex = new MonitorPizarra(this);
        }
        
        iniciarSimulacion();
    }
    
    private void iniciarSimulacion() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        
        Runnable generadorEscritores = () -> {
            String id = "P-E" + escritorIdCounter.getAndIncrement();
            panelGrafo.crearNodoSiNoExiste(id, 500, 100 + (random.nextInt(5) * 40), "Proceso", "E" + (escritorIdCounter.get()-1));
            
            if ("Semáforo".equals(tipoSincronizacion)) {
                new Thread(new Escritor(monitorPizarraMutex, monitorPizarraSemaforo, monitorPizarraCondicion, monitorPizarraMonitores, this, id)).start();
            } else if ("Variable de Condición".equals(tipoSincronizacion)) {
                new Thread(new Escritor(monitorPizarraMutex, monitorPizarraSemaforo, monitorPizarraCondicion, monitorPizarraMonitores, this, id)).start();
            } else if ("Monitores".equals(tipoSincronizacion)) {
                new Thread(new Escritor(monitorPizarraMutex, monitorPizarraSemaforo, monitorPizarraCondicion, monitorPizarraMonitores, this, id)).start();
            } else {
                new Thread(new Escritor(monitorPizarraMutex, monitorPizarraSemaforo, monitorPizarraCondicion, monitorPizarraMonitores, this, id)).start();
            }
        };

        Runnable generadorLectores = () -> {
            String id = "P-L" + lectorIdCounter.getAndIncrement();
            panelGrafo.crearNodoSiNoExiste(id, 50, 100 + (random.nextInt(5) * 40), "Proceso", "L" + (lectorIdCounter.get()-1));
            
            if ("Semáforo".equals(tipoSincronizacion)) {
                new Thread(new Lector(monitorPizarraMutex, monitorPizarraSemaforo, monitorPizarraCondicion, monitorPizarraMonitores, this, id)).start();
            } else if ("Variable de Condición".equals(tipoSincronizacion)) {
                new Thread(new Lector(monitorPizarraMutex, monitorPizarraSemaforo, monitorPizarraCondicion, monitorPizarraMonitores, this, id)).start();
            } else if ("Monitores".equals(tipoSincronizacion)) {
                new Thread(new Lector(monitorPizarraMutex, monitorPizarraSemaforo, monitorPizarraCondicion, monitorPizarraMonitores, this, id)).start();
            } else {
                new Thread(new Lector(monitorPizarraMutex, null, null, null, this, id)).start();
            }
        };

        scheduler.scheduleAtFixedRate(generadorEscritores, 3, random.nextInt(4) + 3, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(generadorLectores, 1, random.nextInt(3) + 1, TimeUnit.SECONDS);
    }
    
    // ... (actualizarEstado, paintComponent, dibujarPersonaPalitos sin cambios) ...
    public void actualizarEstado(int lectoresA, boolean escritorA, int lectoresE, int escritoresE, String texto) {
        SwingUtilities.invokeLater(() -> {
            this.lectoresActivos = lectoresA;
            this.escritorActivo = escritorA;
            this.lectoresEnEspera = lectoresE;
            this.escritoresEnEspera = escritoresE;
            this.textoPizarra = texto;
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
        
        int pizarraX = centroX - ANCHO_PIZARRA / 2;
        int pizarraY = centroY - ALTO_PIZARRA / 2 - 50;
        
        g2d.setColor(new Color(139, 69, 19)); 
        g2d.fillRect(pizarraX - 10, pizarraY - 10, ANCHO_PIZARRA + 20, ALTO_PIZARRA + 20);
        
        g2d.setColor(new Color(30, 60, 30));
        g2d.fillRect(pizarraX, pizarraY, ANCHO_PIZARRA, ALTO_PIZARRA);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("PIZARRA (Recurso Compartido)", pizarraX + 70, pizarraY + 30);
        
        g2d.setFont(new Font("Courier New", Font.PLAIN, 16));
        g2d.drawString(textoPizarra, pizarraX + 20, pizarraY + 70);

        if (escritorActivo) {
            dibujarPersonaPalitos(g2d, pizarraX + ANCHO_PIZARRA/2 - TAMANO_PERSONA/2,
                                  pizarraY + ALTO_PIZARRA/2, Color.RED, "Escritor", true);
        } else {
            int inicioX = pizarraX + ANCHO_PIZARRA/2 - (lectoresActivos * (TAMANO_PERSONA + 10)) / 2;
            for (int i = 0; i < lectoresActivos; i++) {
                int x = inicioX + i * (TAMANO_PERSONA + 10);
                dibujarPersonaPalitos(g2d, x, pizarraY + ALTO_PIZARRA/2, Color.BLUE, "Lector", false);
            }
        }
        
        int colaX = 50;
        int colaY = centroY + ALTO_PIZARRA/2 + 30;
        
        g2d.setColor(Color.BLACK);
        g2d.drawString("Lectores en espera: " + lectoresEnEspera, colaX, colaY);
        for (int i = 0; i < lectoresEnEspera; i++) {
            dibujarPersonaPalitos(g2d, colaX + i * (TAMANO_PERSONA/2 + 10), colaY + 20, Color.BLUE, "", false);
        }
        
        g2d.drawString("Escritores en espera: " + escritoresEnEspera, colaX + 300, colaY);
        for (int i = 0; i < escritoresEnEspera; i++) {
            dibujarPersonaPalitos(g2d, colaX + 300 + i * (TAMANO_PERSONA/2 + 10), colaY + 20, Color.RED, "", true);
        }
    }
    
    private void dibujarPersonaPalitos(Graphics2D g, int x, int y, Color color, String etiqueta, boolean esEscritor) {
        int centroCuerpoX = x + TAMANO_PERSONA / 2;
        int centroCuerpoY = y;
        
        g.setColor(color);
        g.setStroke(new BasicStroke(2));

        g.drawOval(centroCuerpoX - 10, centroCuerpoY - 25, 20, 20);
        g.drawLine(centroCuerpoX, centroCuerpoY - 5, centroCuerpoX, centroCuerpoY + 20);
        g.drawLine(centroCuerpoX - 10, centroCuerpoY, centroCuerpoX + 10, centroCuerpoY);
        g.drawLine(centroCuerpoX, centroCuerpoY + 20, centroCuerpoX - 10, centroCuerpoY + 35);
        g.drawLine(centroCuerpoX, centroCuerpoY + 20, centroCuerpoX + 10, centroCuerpoY + 35);

        if (esEscritor) {
            g.setColor(Color.YELLOW.darker()); 
            g.drawLine(centroCuerpoX + 10, centroCuerpoY, centroCuerpoX + 25, centroCuerpoY - 15);
        } else {
            g.setColor(Color.CYAN.darker());
            g.drawRect(centroCuerpoX + 5, centroCuerpoY - 10, 15, 10);
        }
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int anchoTexto = fm.stringWidth(etiqueta);
        g.drawString(etiqueta, x + TAMANO_PERSONA/2 - anchoTexto/2, centroCuerpoY + 50);
    }
    
    // --- Clases Lector/Escritor (Modificadas para ID y panelGrafo) ---
    
    private class Lector implements Runnable {
        private final MonitorPizarra monitorMutex;
        private final MonitorPizarraSemaforo monitorSemaforo;
        private final MonitorPizarraCondicion monitorCondicion; 
        private final MonitorPizarraMonitores monitorMonitores; 
        private final EscritorLectorPanel panel;
        private final String id; // ID único, ej: "P-L1"

        public Lector(MonitorPizarra m, MonitorPizarraSemaforo s, MonitorPizarraCondicion c, MonitorPizarraMonitores mo, EscritorLectorPanel p, String id) {
            this.monitorMutex = m;
            this.monitorSemaforo = s;
            this.monitorCondicion = c;
            this.monitorMonitores = mo;
            this.panel = p;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                panel.panelGrafo.setFlechaSolicitud(id, "R1");
                
                if (panel.tipoSincronizacion.equals("Semáforo")) {
                    monitorSemaforo.iniciarLectura(id);
                    panel.panelGrafo.setFlechaAsignacion(id, "R1");
                    Thread.sleep(random.nextInt(1000) + 500);
                    monitorSemaforo.finalizarLectura(id);
                } else if (panel.tipoSincronizacion.equals("Variable de Condición")) {
                    monitorCondicion.iniciarLectura(id);
                    panel.panelGrafo.setFlechaAsignacion(id, "R1");
                    Thread.sleep(random.nextInt(1000) + 500);
                    monitorCondicion.finalizarLectura(id);
                } else if (panel.tipoSincronizacion.equals("Monitores")) {
                    monitorMonitores.iniciarLectura(id);
                    panel.panelGrafo.setFlechaAsignacion(id, "R1");
                    Thread.sleep(random.nextInt(1000) + 500);
                    monitorMonitores.finalizarLectura(id);
                } else {
                    monitorMutex.iniciarLectura(id);
                    panel.panelGrafo.setFlechaAsignacion(id, "R1");
                    Thread.sleep(random.nextInt(1000) + 500);
                    monitorMutex.finalizarLectura(id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                panel.panelGrafo.removerFlechas(id, "R1");
                panel.panelGrafo.removerNodo(id); // Lector se va
            }
        }
    }

    private class Escritor implements Runnable {
        private final MonitorPizarra monitorMutex;
        private final MonitorPizarraSemaforo monitorSemaforo;
        private final MonitorPizarraCondicion monitorCondicion; 
        private final MonitorPizarraMonitores monitorMonitores; 
        private final EscritorLectorPanel panel;
        private final String id; // ID único, ej: "P-E1"
        private int contadorEscritura = 0;

        public Escritor(MonitorPizarra m, MonitorPizarraSemaforo s, MonitorPizarraCondicion c, MonitorPizarraMonitores mo, EscritorLectorPanel p, String id) {
            this.monitorMutex = m;
            this.monitorSemaforo = s;
            this.monitorCondicion = c;
            this.monitorMonitores = mo;
            this.panel = p;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                contadorEscritura++;
                String nuevoTexto = "Escrito #" + id + " (" + System.currentTimeMillis() % 1000 + ")";
                
                panel.panelGrafo.setFlechaSolicitud(id, "R1");
                
                if (panel.tipoSincronizacion.equals("Semáforo")) {
                    monitorSemaforo.iniciarEscritura(id);
                    panel.panelGrafo.setFlechaAsignacion(id, "R1");
                    Thread.sleep(random.nextInt(1500) + 1000);
                    monitorSemaforo.finalizarEscritura(id, nuevoTexto);
                } else if (panel.tipoSincronizacion.equals("Variable de Condición")) {
                    monitorCondicion.iniciarEscritura(id);
                    panel.panelGrafo.setFlechaAsignacion(id, "R1");
                    Thread.sleep(random.nextInt(1500) + 1000);
                    monitorCondicion.finalizarEscritura(id, nuevoTexto);
                } else if (panel.tipoSincronizacion.equals("Monitores")) {
                    monitorMonitores.iniciarEscritura(id);
                    panel.panelGrafo.setFlechaAsignacion(id, "R1");
                    Thread.sleep(random.nextInt(1500) + 1000);
                    monitorMonitores.finalizarEscritura(id, nuevoTexto);
                } else {
                    monitorMutex.iniciarEscritura(id);
                    panel.panelGrafo.setFlechaAsignacion(id, "R1");
                    Thread.sleep(random.nextInt(1500) + 1000);
                    monitorMutex.finalizarEscritura(id, nuevoTexto);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                panel.panelGrafo.removerFlechas(id, "R1");
                panel.panelGrafo.removerNodo(id); // Escritor se va
            }
        }
    }
    
    // --- Monitores (actualizados para UI) ---
    // (Solo muestro Monitores y Mutex por brevedad, Semáforo y Condicion son similares)

    private class MonitorPizarraMonitores {
        private final ReentrantLock lock = new ReentrantLock(); 
        private final Condition canRead = lock.newCondition(); 
        private final Condition canWrite = lock.newCondition();
        
        private int readersActive = 0;
        private int readersWaiting = 0;
        private int writersActive = 0; 
        private int writersWaiting = 0;
        private final EscritorLectorPanel panel;

        public MonitorPizarraMonitores(EscritorLectorPanel panel) {
            this.panel = panel;
        }

        public void iniciarLectura(String id) throws InterruptedException {
            lock.lock();
            try {
                readersWaiting++;
                panel.actualizarEstado(readersActive, writersActive > 0, readersWaiting, writersWaiting, panel.textoPizarra);

                while (writersActive > 0 || writersWaiting > 0) { 
                    canRead.await();
                }
                
                readersWaiting--;
                readersActive++;
            } finally {
                lock.unlock();
            }
            panel.actualizarEstado(readersActive, writersActive > 0, readersWaiting, writersWaiting, panel.textoPizarra);
        }

        public void finalizarLectura(String id) {
            lock.lock();
            try {
                readersActive--;
                if (readersActive == 0) {
                    canWrite.signal(); 
                }
            } finally {
                lock.unlock();
            }
            panel.actualizarEstado(readersActive, writersActive > 0, readersWaiting, writersWaiting, panel.textoPizarra);
        }

        public void iniciarEscritura(String id) throws InterruptedException {
            lock.lock();
            try {
                writersWaiting++;
                panel.actualizarEstado(readersActive, writersActive > 0, readersWaiting, writersWaiting, panel.textoPizarra);

                while (readersActive > 0 || writersActive > 0) { 
                    canWrite.await();
                }

                writersWaiting--;
                writersActive = 1;
                panel.actualizarEstado(readersActive, true, readersWaiting, writersWaiting, "Limpiando pizarra...");
            } finally {
                lock.unlock();
            }
        }

        public void finalizarEscritura(String id, String nuevoTexto) {
            lock.lock();
            try {
                panel.textoPizarra = nuevoTexto;
                writersActive = 0;

                if (writersWaiting > 0) {
                    canWrite.signal();
                } else {
                    canRead.signalAll(); 
                }
            } finally {
                lock.unlock();
            }
            panel.actualizarEstado(readersActive, false, readersWaiting, writersWaiting, nuevoTexto);
        }
    }

    private class MonitorPizarraCondicion {
        // ... (Idéntico a MonitorPizarraMonitores) ...
        private final ReentrantLock lock = new ReentrantLock(); 
        private final Condition canRead = lock.newCondition(); 
        private final Condition canWrite = lock.newCondition();
        private int readersActive = 0;
        private int readersWaiting = 0;
        private int writersActive = 0; 
        private int writersWaiting = 0;
        private final EscritorLectorPanel panel;
        public MonitorPizarraCondicion(EscritorLectorPanel panel) { this.panel = panel; }
        public void iniciarLectura(String id) throws InterruptedException {
            lock.lock();
            try {
                readersWaiting++;
                panel.actualizarEstado(readersActive, writersActive > 0, readersWaiting, writersWaiting, panel.textoPizarra);
                while (writersActive > 0 || writersWaiting > 0) { canRead.await(); }
                readersWaiting--;
                readersActive++;
            } finally { lock.unlock(); }
            panel.actualizarEstado(readersActive, writersActive > 0, readersWaiting, writersWaiting, panel.textoPizarra);
        }
        public void finalizarLectura(String id) {
            lock.lock();
            try {
                readersActive--;
                if (readersActive == 0) { canWrite.signal(); }
            } finally { lock.unlock(); }
            panel.actualizarEstado(readersActive, writersActive > 0, readersWaiting, writersWaiting, panel.textoPizarra);
        }
        public void iniciarEscritura(String id) throws InterruptedException {
            lock.lock();
            try {
                writersWaiting++;
                panel.actualizarEstado(readersActive, writersActive > 0, readersWaiting, writersWaiting, panel.textoPizarra);
                while (readersActive > 0 || writersActive > 0) { canWrite.await(); }
                writersWaiting--;
                writersActive = 1;
                panel.actualizarEstado(readersActive, true, readersWaiting, writersWaiting, "Limpiando pizarra...");
            } finally { lock.unlock(); }
        }
        public void finalizarEscritura(String id, String nuevoTexto) {
            lock.lock();
            try {
                panel.textoPizarra = nuevoTexto;
                writersActive = 0;
                if (writersWaiting > 0) { canWrite.signal(); } else { canRead.signalAll(); }
            } finally { lock.unlock(); }
            panel.actualizarEstado(readersActive, false, readersWaiting, writersWaiting, nuevoTexto);
        }
    }

    private class MonitorPizarra { // Mutex
        private final ReentrantLock readCountLock = new ReentrantLock(); 
        private final ReentrantLock writeLock = new ReentrantLock(); 
        private int readersActive = 0;
        private int readersWaiting = 0;
        private int writersWaiting = 0;
        private final EscritorLectorPanel panel;

        public MonitorPizarra(EscritorLectorPanel panel) { this.panel = panel; }

        public void iniciarLectura(String id) throws InterruptedException {
            readCountLock.lock();
            try {
                readersWaiting++;
                panel.actualizarEstado(readersActive, writeLock.isLocked(), readersWaiting, writersWaiting, panel.textoPizarra);
                
                while (writeLock.isLocked()) {
                    readCountLock.unlock(); // Soltar lock temporalmente para no bloquear
                    Thread.sleep(100); 
                    readCountLock.lock();
                }

                if (readersActive == 0) {
                    writeLock.lock(); // Primer lector bloquea a escritores
                }
                readersActive++;
                readersWaiting--;
            } finally {
                readCountLock.unlock();
            }
            panel.actualizarEstado(readersActive, writeLock.isLocked(), readersWaiting, writersWaiting, panel.textoPizarra);
        }

        public void finalizarLectura(String id) {
            readCountLock.lock();
            try {
                readersActive--;
                if (readersActive == 0 && writeLock.isHeldByCurrentThread()) {
                    writeLock.unlock(); // Último lector libera
                }
            } finally {
                readCountLock.unlock();
            }
            panel.actualizarEstado(readersActive, writeLock.isLocked(), readersWaiting, writersWaiting, panel.textoPizarra);
        }

        public void iniciarEscritura(String id) throws InterruptedException {
            writersWaiting++;
            panel.actualizarEstado(readersActive, writeLock.isLocked(), readersWaiting, writersWaiting, panel.textoPizarra);
            writeLock.lock();
            writersWaiting--;
            panel.actualizarEstado(readersActive, true, readersWaiting, writersWaiting, "Limpiando pizarra...");
        }

        public void finalizarEscritura(String id, String nuevoTexto) {
            panel.textoPizarra = nuevoTexto;
            writeLock.unlock();
            panel.actualizarEstado(readersActive, false, readersWaiting, writersWaiting, nuevoTexto);
        }
    }

    private class MonitorPizarraSemaforo {
        private final Semaphore mutex = new Semaphore(1);
        private final Semaphore wrt = new Semaphore(1);
        private int readCount = 0;
        private int writerCount = 0;
        private int readersWaiting = 0;
        private int writersWaiting = 0;
        private final EscritorLectorPanel panel;

        public MonitorPizarraSemaforo(EscritorLectorPanel panel) { this.panel = panel; }

        public void iniciarLectura(String id) throws InterruptedException {
            readersWaiting++;
            panel.actualizarEstado(readCount, writerCount > 0, readersWaiting, writersWaiting, panel.textoPizarra);
            
            mutex.acquire();
            readCount++;
            if (readCount == 1) {
                wrt.acquire(); // Primer lector bloquea escritores
            }
            readersWaiting--;
            mutex.release();
            
            panel.actualizarEstado(readCount, writerCount > 0, readersWaiting, writersWaiting, panel.textoPizarra);
        }

        public void finalizarLectura(String id) throws InterruptedException {
            mutex.acquire();
            readCount--;
            if (readCount == 0) {
                wrt.release(); // Último lector libera escritores
            }
            mutex.release();
            panel.actualizarEstado(readCount, writerCount > 0, readersWaiting, writersWaiting, panel.textoPizarra);
        }

        public void iniciarEscritura(String id) throws InterruptedException {
            writersWaiting++;
            panel.actualizarEstado(readCount, writerCount > 0, readersWaiting, writersWaiting, panel.textoPizarra);
            wrt.acquire();
            writerCount++;
            writersWaiting--;
            panel.actualizarEstado(readCount, true, readersWaiting, writersWaiting, "Limpiando pizarra...");
        }

        public void finalizarEscritura(String id, String nuevoTexto) {
            panel.textoPizarra = nuevoTexto;
            writerCount--;
            wrt.release();
            panel.actualizarEstado(readCount, false, readersWaiting, writersWaiting, nuevoTexto);
        }
    }
}