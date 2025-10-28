import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;

public class TanquePanel extends JPanel {
    private int nivelAgua = 0; // 0-100%
    
    // Mutex
    private final ReentrantLock mutex = new ReentrantLock();
    
    // Semáforos
    private final Semaphore mutexSemaforo;
    private final Semaphore empty;
    private final Semaphore full;
    
    // Monitores (ReentrantLock / Condition)
    private final ReentrantLock lockMonitores = new ReentrantLock(); 
    private final Condition notFullMonitores = lockMonitores.newCondition(); 
    private final Condition notEmptyMonitores = lockMonitores.newCondition(); 
    
    // Variable de Condición
    private final ReentrantLock lockCondicion = new ReentrantLock(); 
    private final Condition notFullCondicion = lockCondicion.newCondition(); 
    private final Condition notEmptyCondicion = lockCondicion.newCondition(); 
    
    private JLabel porcentajeLabel;
    private final String tipoSincronizacion;
    private final PanelGrafoDinamico panelGrafo; // <-- Referencia al grafo

    // Constructor modificado
    public TanquePanel(String tipoSincronizacion, PanelGrafoDinamico panelGrafo) {
        this.tipoSincronizacion = tipoSincronizacion;
        this.panelGrafo = panelGrafo; // <-- Guardar referencia
        
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        porcentajeLabel = new JLabel("0%");
        porcentajeLabel.setForeground(Color.WHITE);
        porcentajeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(porcentajeLabel, BorderLayout.NORTH);

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) { producir(); } 
                else if (e.getKeyCode() == KeyEvent.VK_DOWN) { consumir(); }
            }
        });

        if ("Semáforo".equals(tipoSincronizacion)) {
            mutexSemaforo = new Semaphore(1);
            empty = new Semaphore(10); 
            full = new Semaphore(0);
        } else {
            mutexSemaforo = null;
            empty = null;
            full = null;
        }

        iniciarProduccion();
        iniciarConsumo();
    }

    private void iniciarProduccion() {
        new Thread(() -> {
            while (true) {
                producir();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
    
    private void iniciarConsumo() {
        new Thread(() -> {
            while (true) {
                consumir(); 
                try {
                    Thread.sleep(1200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void producir() {
        if ("Semáforo".equals(tipoSincronizacion)) {
            producirSemaforo();
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            producirCondicion();
        } else if ("Monitores".equals(tipoSincronizacion)) {
            producirMonitores();
        } else {
            producirMutex();
        }
    }

    private void consumir() {
        if ("Semáforo".equals(tipoSincronizacion)) {
            consumirSemaforo();
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            consumirCondicion();
        } else if ("Monitores".equals(tipoSincronizacion)) {
            consumirMonitores();
        } else {
            consumirMutex();
        }
    }
    
    // --- Implementaciones con actualización de grafo ---

    private void producirMutex() {
        panelGrafo.setFlechaSolicitud("P1", "R1");
        mutex.lock();
        panelGrafo.setFlechaAsignacion("P1", "R1");
        try {
            if (nivelAgua < 100) {
                nivelAgua += 10;
                if (nivelAgua > 100) nivelAgua = 100;
                actualizarInterfaz();
            }
            Thread.sleep(50); // Simular tiempo en sección crítica
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.unlock();
            panelGrafo.removerFlechas("P1", "R1");
        }
    }

    private void consumirMutex() {
        panelGrafo.setFlechaSolicitud("P2", "R1");
        mutex.lock();
        panelGrafo.setFlechaAsignacion("P2", "R1");
        try {
            if (nivelAgua > 0) {
                nivelAgua -= 10;
                if (nivelAgua < 0) nivelAgua = 0;
                actualizarInterfaz();
            }
            Thread.sleep(50); // Simular tiempo en sección crítica
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutex.unlock();
            panelGrafo.removerFlechas("P2", "R1");
        }
    }

    private void producirSemaforo() {
        try {
            panelGrafo.setFlechaSolicitud("P1", "R1"); // Solicita "empty"
            empty.acquire();
            mutexSemaforo.acquire();
            panelGrafo.setFlechaAsignacion("P1", "R1"); // Obtiene "mutex"
            
            if (nivelAgua < 100) {
                nivelAgua += 10;
                actualizarInterfaz();
            }
            
            mutexSemaforo.release();
            full.release();
            panelGrafo.removerFlechas("P1", "R1"); // Libera "mutex"
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void consumirSemaforo() {
        try {
            panelGrafo.setFlechaSolicitud("P2", "R1"); // Solicita "full"
            full.acquire();
            mutexSemaforo.acquire();
            panelGrafo.setFlechaAsignacion("P2", "R1"); // Obtiene "mutex"
            
            if (nivelAgua > 0) {
                nivelAgua -= 10;
                actualizarInterfaz();
            }
            
            mutexSemaforo.release();
            empty.release();
            panelGrafo.removerFlechas("P2", "R1"); // Libera "mutex"
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void producirMonitores() {
        panelGrafo.setFlechaSolicitud("P1", "R1");
        lockMonitores.lock();
        try {
            while (nivelAgua == 100) {
                notFullMonitores.await(); 
            }
            panelGrafo.setFlechaAsignacion("P1", "R1");
            
            nivelAgua += 10;
            if (nivelAgua > 100) nivelAgua = 100;
            actualizarInterfaz();
            
            notEmptyMonitores.signal(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lockMonitores.unlock();
            panelGrafo.removerFlechas("P1", "R1");
        }
    }

    private void consumirMonitores() {
        panelGrafo.setFlechaSolicitud("P2", "R1");
        lockMonitores.lock();
        try {
            while (nivelAgua == 0) {
                notEmptyMonitores.await(); 
            }
            panelGrafo.setFlechaAsignacion("P2", "R1");
            
            nivelAgua -= 10;
            if (nivelAgua < 0) nivelAgua = 0;
            actualizarInterfaz();
            
            notFullMonitores.signal(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lockMonitores.unlock();
            panelGrafo.removerFlechas("P2", "R1");
        }
    }

    // (produciCondicion y consumirCondicion son idénticos a Monitores, solo cambian las variables)
    private void producirCondicion() {
        panelGrafo.setFlechaSolicitud("P1", "R1");
        lockCondicion.lock();
        try {
            while (nivelAgua == 100) {
                notFullCondicion.await(); 
            }
            panelGrafo.setFlechaAsignacion("P1", "R1");
            
            nivelAgua += 10;
            if (nivelAgua > 100) nivelAgua = 100;
            actualizarInterfaz();
            
            notEmptyCondicion.signal(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lockCondicion.unlock();
            panelGrafo.removerFlechas("P1", "R1");
        }
    }

    private void consumirCondicion() {
        panelGrafo.setFlechaSolicitud("P2", "R1");
        lockCondicion.lock();
        try {
            while (nivelAgua == 0) {
                notEmptyCondicion.await(); 
            }
            panelGrafo.setFlechaAsignacion("P2", "R1");
            
            nivelAgua -= 10;
            if (nivelAgua < 0) nivelAgua = 0;
            actualizarInterfaz();
            
            notFullCondicion.signal(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lockCondicion.unlock();
            panelGrafo.removerFlechas("P2", "R1");
        }
    }
    
    // --- Métodos de UI (sin cambios) ---
    
    private void actualizarInterfaz() { 
        SwingUtilities.invokeLater(() -> {
            porcentajeLabel.setText(nivelAgua + "%"); 
            repaint(); 
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        int margen = 50;
        int anchoTanque = getWidth() - 2 * margen;
        int altoTanque = getHeight() - 2 * margen;
        
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(margen, margen, anchoTanque, altoTanque);
        
        int altoAgua = (int)((altoTanque * nivelAgua) / 100.0);
        g2d.setColor(new Color(0, 0, 255, 128));
        g2d.fillRect(margen, margen + (altoTanque - altoAgua), anchoTanque, altoAgua);
        
        g2d.setColor(Color.WHITE);
        for (int i = 0; i <= 100; i += 20) {
            int y = margen + (int)((100 - i) * altoTanque / 100.0);
            g2d.drawString(i + "%", margen - 30, y);
            g2d.drawLine(margen - 5, y, margen, y);
        }
    }
}