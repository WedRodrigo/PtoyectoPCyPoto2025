import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition; // Nuevo

public class CenaFilosofosPanel extends JPanel {
    private static final int NUM_FILOSOFOS = 5;
    private static final int RADIO_MESA = 200;
    private static final int RADIO_PLATO = 60;
    private static final int RADIO_CENTRO = 80;
    
    private static final int RADIO_TENEDOR_POSICION = 150; 
    
    private Tenedor[] tenedores;
    private String[] estadoFilosofos = new String[NUM_FILOSOFOS];
    private boolean[] tenedorEnUso = new boolean[NUM_FILOSOFOS];
    private final String tipoSincronizacion;
    
    // Para la implementación con Variable de Condición
    private MonitorMesa monitorMesa;

    public CenaFilosofosPanel(String tipoSincronizacion) {
        this.tipoSincronizacion = tipoSincronizacion;
        setBackground(Color.WHITE);
        
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            estadoFilosofos[i] = "Pensando";
        }
        
        // Inicializar Monitor de Condición si es necesario
        if ("Variable de Condición".equals(tipoSincronizacion)) {
            monitorMesa = new MonitorMesa(this);
        }
        
        iniciarSimulacion();
    }
    
    private void iniciarSimulacion() {
        tenedores = new Tenedor[NUM_FILOSOFOS];
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            tenedores[i] = new Tenedor(i);
        }
        
        if ("Semáforo".equals(tipoSincronizacion)) {
            Semaphore sala = new Semaphore(NUM_FILOSOFOS - 1);
            for (int i = 0; i < NUM_FILOSOFOS; i++) {
                Tenedor tenedorIzquierdo = tenedores[i];
                Tenedor tenedorDerecho = tenedores[(i + 1) % NUM_FILOSOFOS];
                FilosofoSemaforo f = new FilosofoSemaforo(i, tenedorIzquierdo, tenedorDerecho, this, sala);
                new Thread(f).start();
            }
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            for (int i = 0; i < NUM_FILOSOFOS; i++) {
                FilosofoCondicion f = new FilosofoCondicion(i, monitorMesa, this);
                new Thread(f).start();
            }
        } else {
            for (int i = 0; i < NUM_FILOSOFOS; i++) {
                Tenedor tenedorIzquierdo = tenedores[i];
                Tenedor tenedorDerecho = tenedores[(i + 1) % NUM_FILOSOFOS];
                Filosofo f = new Filosofo(i, tenedorIzquierdo, tenedorDerecho, this);
                new Thread(f).start();
            }
        }
    }
    
    public void actualizarEstado(int id, String estado) {
        SwingUtilities.invokeLater(() -> {
            estadoFilosofos[id] = estado;
            repaint();
        });
    }

    public void setTenedorEnUso(int tenedorId, boolean enUso) {
        SwingUtilities.invokeLater(() -> {
            tenedorEnUso[tenedorId] = enUso;
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
        
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillOval(centroX - RADIO_CENTRO, centroY - RADIO_CENTRO, RADIO_CENTRO * 2, RADIO_CENTRO * 2);
        
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            double angulo = Math.toRadians(i * 360.0 / NUM_FILOSOFOS - 90);
            int platoX = (int) (centroX + Math.cos(angulo) * RADIO_MESA);
            int platoY = (int) (centroY + Math.sin(angulo) * RADIO_MESA);
            
            g2d.setColor(Color.WHITE);
            g2d.fillOval(platoX - RADIO_PLATO, platoY - RADIO_PLATO, RADIO_PLATO * 2, RADIO_PLATO * 2);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(platoX - RADIO_PLATO, platoY - RADIO_PLATO, RADIO_PLATO * 2, RADIO_PLATO * 2);
            g2d.drawOval(platoX - RADIO_PLATO + 10, platoY - RADIO_PLATO + 10, RADIO_PLATO * 2 - 20, RADIO_PLATO * 2 - 20);
            
            String etiqueta = "P" + i;
            FontMetrics fm = g2d.getFontMetrics();
            int anchoTexto = fm.stringWidth(etiqueta);
            
            Color colorEstado;
            String estado = estadoFilosofos[i];
            
            switch (estado) {
                case "Comiendo":
                    colorEstado = Color.GREEN;
                    break;
                case "Hambriento":
                    colorEstado = Color.ORANGE;
                    break;
                case "Pensando":
                default:
                    colorEstado = Color.BLUE;
                    break;
            }
            
            g2d.setColor(colorEstado);
            g2d.fillOval(platoX - 15, platoY - 15, 30, 30);
            g2d.setColor(Color.BLACK);
            g2d.drawString(etiqueta, platoX - anchoTexto / 2, platoY + 20);
            
            g2d.setColor(Color.BLACK);
            g2d.drawString(estado, platoX - fm.stringWidth(estado) / 2, platoY + RADIO_PLATO + 20);
            
            double anguloTenedor = Math.toRadians((i + 0.5) * 360.0 / NUM_FILOSOFOS - 90);
            
            int tenedorX = (int) (centroX + Math.cos(anguloTenedor) * RADIO_TENEDOR_POSICION);
            int tenedorY = (int) (centroY + Math.sin(anguloTenedor) * RADIO_TENEDOR_POSICION);
            
            g2d.setStroke(new BasicStroke(2));
            double anguloRotacion = anguloTenedor + Math.PI/2;
            
            AffineTransform transformOriginal = g2d.getTransform();
            
            g2d.translate(tenedorX, tenedorY);
            g2d.rotate(anguloRotacion);
            
            g2d.setColor(tenedorEnUso[i] ? Color.RED : Color.GRAY);
            
            g2d.fillRect(-5, -30, 10, 60);
            
            g2d.setColor(tenedorEnUso[i] ? Color.DARK_GRAY : new Color(60, 60, 60));
            g2d.fillRect(-10, 25, 20, 5);
            for (int j = 0; j < 4; j++) {
                g2d.fillRect(-8 + j * 5, 15, 3, 15);
            }
            
            g2d.setTransform(transformOriginal);
        }
    }

    private static class Tenedor {
        public final ReentrantLock mutex; 
        public final Semaphore semaforo;
        private final int id;

        public Tenedor(int id) {
            this.id = id;
            this.mutex = new ReentrantLock();
            this.semaforo = new Semaphore(1);
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN CON VARIABLE DE CONDICIÓN (Monitor)
    // ====================================================================
    
    enum Estado { PENSANDO, HAMBRIENTO, COMIENDO };
    
    private class MonitorMesa {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition[] condition = new Condition[NUM_FILOSOFOS]; // Una condición por filósofo
        private final Estado[] estado = new Estado[NUM_FILOSOFOS];
        
        public MonitorMesa(CenaFilosofosPanel panel) {
            for (int i = 0; i < NUM_FILOSOFOS; i++) {
                condition[i] = lock.newCondition();
                estado[i] = Estado.PENSANDO;
            }
        }
        
        private int izquierda(int i) { return i; } 
        private int derecha(int i) { return (i + 1) % NUM_FILOSOFOS; } 
        private int vecinoIzquierdo(int i) { return (i + NUM_FILOSOFOS - 1) % NUM_FILOSOFOS; }
        private int vecinoDerecho(int i) { return (i + 1) % NUM_FILOSOFOS; }
        
        /**
         * Verifica si el filósofo i puede comer y lo pone a comer si es posible.
         */
        private void test(int i, CenaFilosofosPanel panel) {
            // Un filósofo hambriento puede comer si ninguno de sus vecinos está comiendo
            if (estado[i] == Estado.HAMBRIENTO &&
                estado[vecinoIzquierdo(i)] != Estado.COMIENDO &&
                estado[vecinoDerecho(i)] != Estado.COMIENDO) {
                
                estado[i] = Estado.COMIENDO;
                // Actualización de estado en la interfaz
                panel.actualizarEstado(i, "Comiendo");
                panel.setTenedorEnUso(izquierda(i), true);
                panel.setTenedorEnUso(derecha(i), true);
                // Despierta al filósofo que estaba esperando
                condition[i].signal(); 
            }
        }
        
        public void tomarTenedores(int i, CenaFilosofosPanel panel) throws InterruptedException {
            lock.lock();
            try {
                estado[i] = Estado.HAMBRIENTO;
                panel.actualizarEstado(i, "Hambriento");
                test(i, panel);
                // Espera MIENTRAS no pueda comer
                while (estado[i] != Estado.COMIENDO) {
                    condition[i].await();
                }
            } finally {
                lock.unlock();
            }
        }
        
        public void dejarTenedores(int i, CenaFilosofosPanel panel) {
            lock.lock();
            try {
                estado[i] = Estado.PENSANDO;
                panel.actualizarEstado(i, "Pensando");
                panel.setTenedorEnUso(izquierda(i), false);
                panel.setTenedorEnUso(derecha(i), false);

                // Revisa si los vecinos pueden empezar a comer ahora
                test(vecinoIzquierdo(i), panel);
                test(vecinoDerecho(i), panel);
            } finally {
                lock.unlock();
            }
        }
    }
    
    private static class FilosofoCondicion implements Runnable {
        private final int id;
        private final MonitorMesa monitor;
        private final CenaFilosofosPanel panel;
        private final Random random = new Random();

        public FilosofoCondicion(int id, MonitorMesa monitor, CenaFilosofosPanel panel) {
            this.id = id;
            this.monitor = monitor;
            this.panel = panel;
        }

        @Override
        public void run() {
            while (true) {
                pensar();
                try {
                    monitor.tomarTenedores(id, panel);
                    comer();
                    monitor.dejarTenedores(id, panel);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void pensar() {
            try {
                panel.actualizarEstado(id, "Pensando");
                Thread.sleep(random.nextInt(3000) + 1000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void comer() {
            try {
                // El estado "Comiendo" ya se actualiza dentro del monitor.
                Thread.sleep(random.nextInt(3000) + 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN CON MUTEX (EXISTENTE)
    // ====================================================================

    private static class Filosofo implements Runnable {
        // ... (Se mantiene igual) ...
        private final int id;
        private final Tenedor tenedorIzquierdo;
        private final Tenedor tenedorDerecho;
        private final CenaFilosofosPanel panel;
        private final Random random = new Random();

        public Filosofo(int id, Tenedor izq, Tenedor der, CenaFilosofosPanel panel) {
            this.id = id;
            this.tenedorIzquierdo = izq;
            this.tenedorDerecho = der;
            this.panel = panel;
        }

        @Override
        public void run() {
            while (true) {
                pensar();
                comer();
            }
        }

        private void pensar() {
            try {
                panel.actualizarEstado(id, "Pensando");
                Thread.sleep(random.nextInt(3000) + 1000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void comer() {
            panel.actualizarEstado(id, "Hambriento");

            Tenedor primerTenedor;
            Tenedor segundoTenedor;
            
            // Lógica de Prevención de Deadlock por jerarquía de recursos
            if (id == NUM_FILOSOFOS - 1) { 
                primerTenedor = tenedorDerecho;
                segundoTenedor = tenedorIzquierdo;
            } else { 
                primerTenedor = tenedorIzquierdo;
                segundoTenedor = tenedorDerecho;
            }

            primerTenedor.mutex.lock(); 
            panel.setTenedorEnUso(primerTenedor.id, true);
            
            try {
                if (segundoTenedor.mutex.tryLock()) { 
                    panel.setTenedorEnUso(segundoTenedor.id, true);
                    try {
                        panel.actualizarEstado(id, "Comiendo");
                        Thread.sleep(random.nextInt(3000) + 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        segundoTenedor.mutex.unlock();
                        panel.setTenedorEnUso(segundoTenedor.id, false);
                    }
                }
            } finally {
                primerTenedor.mutex.unlock();
                panel.setTenedorEnUso(primerTenedor.id, false);
            }
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN CON SEMÁFORO (EXISTENTE)
    // ====================================================================
    
    private static class FilosofoSemaforo implements Runnable {
        // ... (Se mantiene igual) ...
        private final int id;
        private final Tenedor tenedorIzquierdo;
        private final Tenedor tenedorDerecho;
        private final CenaFilosofosPanel panel;
        private final Random random = new Random();
        private final Semaphore sala;

        public FilosofoSemaforo(int id, Tenedor izq, Tenedor der, CenaFilosofosPanel panel, Semaphore sala) {
            this.id = id;
            this.tenedorIzquierdo = izq;
            this.tenedorDerecho = der;
            this.panel = panel;
            this.sala = sala;
        }

        @Override
        public void run() {
            while (true) {
                pensar();
                comer();
            }
        }

        private void pensar() {
            try {
                panel.actualizarEstado(id, "Pensando");
                Thread.sleep(random.nextInt(3000) + 1000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void comer() {
            try {
                panel.actualizarEstado(id, "Hambriento");
                sala.acquire();

                tenedorIzquierdo.semaforo.acquire();
                panel.setTenedorEnUso(tenedorIzquierdo.id, true);

                tenedorDerecho.semaforo.acquire();
                panel.setTenedorEnUso(tenedorDerecho.id, true);

                panel.actualizarEstado(id, "Comiendo");
                Thread.sleep(random.nextInt(3000) + 1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                tenedorDerecho.semaforo.release();
                panel.setTenedorEnUso(tenedorDerecho.id, false);
                
                tenedorIzquierdo.semaforo.release();
                panel.setTenedorEnUso(tenedorIzquierdo.id, false);
                
                sala.release();
            }
        }
    }
}