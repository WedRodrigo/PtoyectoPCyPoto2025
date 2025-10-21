import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition; // Nuevo

public class TanquePanel extends JPanel {
    private int nivelAgua = 0; // 0-100%
    private final ReentrantLock mutex = new ReentrantLock();
    
    // Semáforos
    private final Semaphore mutexSemaforo;
    private final Semaphore empty;
    private final Semaphore full;
    
    // Variables de Condición
    private final ReentrantLock lockCondicion = new ReentrantLock(); // Lock para el monitor
    private final Condition notFull = lockCondicion.newCondition(); // Productor espera aquí
    private final Condition notEmpty = lockCondicion.newCondition(); // Consumidor espera aquí
    
    private JLabel porcentajeLabel;
    private final String tipoSincronizacion;

    public TanquePanel(String tipoSincronizacion) {
        this.tipoSincronizacion = tipoSincronizacion;
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
            empty = new Semaphore(10); // 10 "espacios" de 10% cada uno
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
                    e.printStackTrace();
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
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void producir() {
        if ("Semáforo".equals(tipoSincronizacion)) {
            producirSemaforo();
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            producirCondicion();
        } else {
            producirMutex();
        }
    }

    private void consumir() {
        if ("Semáforo".equals(tipoSincronizacion)) {
            consumirSemaforo();
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            consumirCondicion();
        } else {
            consumirMutex();
        }
    }

    private void producirMutex() {
        mutex.lock();
        try {
            if (nivelAgua < 100) {
                nivelAgua += 10;
                if (nivelAgua > 100) nivelAgua = 100;
                actualizarInterfaz();
            }
        } finally {
            mutex.unlock();
        }
    }

    private void consumirMutex() {
        mutex.lock();
        try {
            if (nivelAgua > 0) {
                nivelAgua -= 10;
                if (nivelAgua < 0) nivelAgua = 0;
                actualizarInterfaz();
            }
        } finally {
            mutex.unlock();
        }
    }
    
    // ====================================================================
    // IMPLEMENTACIÓN CON VARIABLE DE CONDICIÓN
    // ====================================================================

    private void producirCondicion() {
        lockCondicion.lock();
        try {
            // El productor espera (await) MIENTRAS el tanque esté lleno
            while (nivelAgua == 100) {
                notFull.await(); 
            }
            
            nivelAgua += 10;
            if (nivelAgua > 100) nivelAgua = 100;
            actualizarInterfaz();
            
            // Si estaba vacío y se llenó, despierta a un consumidor
            notEmpty.signal(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lockCondicion.unlock();
        }
    }

    private void consumirCondicion() {
        lockCondicion.lock();
        try {
            // El consumidor espera (await) MIENTRAS el tanque esté vacío
            while (nivelAgua == 0) {
                notEmpty.await(); 
            }
            
            nivelAgua -= 10;
            if (nivelAgua < 0) nivelAgua = 0;
            actualizarInterfaz();
            
            // Si estaba lleno y se vació, despierta a un productor
            notFull.signal(); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lockCondicion.unlock();
        }
    }
    
    // ====================================================================
    // IMPLEMENTACIÓN CON SEMÁFORO (EXISTENTE)
    // ====================================================================

    private void producirSemaforo() {
        try {
            empty.acquire();
            mutexSemaforo.acquire();
            if (nivelAgua < 100) {
                nivelAgua += 10;
                actualizarInterfaz();
            }
            mutexSemaforo.release();
            full.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void consumirSemaforo() {
        try {
            full.acquire();
            mutexSemaforo.acquire();
            if (nivelAgua > 0) {
                nivelAgua -= 10;
                actualizarInterfaz();
            }
            mutexSemaforo.release();
            empty.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ====================================================================

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