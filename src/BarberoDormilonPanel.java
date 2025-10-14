import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class BarberoDormilonPanel extends JPanel {
    private static final int ANCHO_BARBERIA = 400;
    private static final int ALTO_BARBERIA = 300;
    private static final int ANCHO_SILLA = 60;
    private static final int ALTO_SILLA = 80;
    private static final int ANCHO_PERSONA = 40;
    private static final int ALTO_PERSONA = 60;
    private static final int NUM_SILLAS_ESPERA = 4;
    
    // Estados compartidos y protegidos por el Mutex de la Barberia
    private volatile boolean barberoDormido = true;
    private volatile int clientesEnEspera = 0;
    private volatile boolean clienteSiendoAtendido = false;
    private volatile int clienteActualId = -1;
    
    private final Barberia barberiaMutex;
    private final BarberiaSemaforo barberiaSemaforo;
    private final String tipoSincronizacion;
    private final Random random = new Random();
    
    public BarberoDormilonPanel(String tipoSincronizacion) {
        this.tipoSincronizacion = tipoSincronizacion;
        setBackground(Color.WHITE);
        
        if ("Semáforo".equals(tipoSincronizacion)) {
            this.barberiaSemaforo = new BarberiaSemaforo(NUM_SILLAS_ESPERA, this);
            this.barberiaMutex = null;
        } else {
            this.barberiaMutex = new Barberia(NUM_SILLAS_ESPERA, this);
            this.barberiaSemaforo = null;
        }
        
        iniciarSimulacion();
    }
    
    private void iniciarSimulacion() {
        if ("Semáforo".equals(tipoSincronizacion)) {
            BarberoSemaforo barbero = new BarberoSemaforo(barberiaSemaforo, this);
            new Thread(barbero).start();
            
            new Thread(() -> {
                int clienteId = 1;
                while (true) {
                    try {
                        Thread.sleep(random.nextInt(2000) + 1000); 
                        ClienteSemaforo cliente = new ClienteSemaforo(clienteId++, barberiaSemaforo, this);
                        new Thread(cliente).start();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }).start();
        } else {
            Barbero barbero = new Barbero(barberiaMutex, this);
            new Thread(barbero).start();
            
            new Thread(() -> {
                int clienteId = 1;
                while (true) {
                    try {
                        Thread.sleep(random.nextInt(2000) + 1000); 
                        Cliente cliente = new Cliente(clienteId++, barberiaMutex, this);
                        new Thread(cliente).start();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }).start();
        }
    }
    
    // Método para que los hilos actualicen el estado y la interfaz
    public void actualizarEstado(boolean esBarbero, boolean dormido, int clientesQuedan, boolean atendiendo, int clienteId) {
        SwingUtilities.invokeLater(() -> {
            if (esBarbero) {
                this.barberoDormido = dormido;
            }
            this.clientesEnEspera = clientesQuedan;
            this.clienteSiendoAtendido = atendiendo;
            this.clienteActualId = clienteId;
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
        
        // Dibujar Barbería
        g2d.setColor(new Color(240, 240, 240));
        g2d.fillRect(centroX - ANCHO_BARBERIA/2, centroY - ALTO_BARBERIA/2, ANCHO_BARBERIA, ALTO_BARBERIA);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(centroX - ANCHO_BARBERIA/2, centroY - ALTO_BARBERIA/2, ANCHO_BARBERIA, ALTO_BARBERIA);
        g2d.drawString("Barbería - Sillas Libres: " + (NUM_SILLAS_ESPERA - clientesEnEspera), centroX - ANCHO_BARBERIA/2 + 10, centroY - ALTO_BARBERIA/2 + 20);

        // --- Silla del Barbero ---
        int sillaBarberoX = centroX - ANCHO_BARBERIA/4;
        int sillaBarberoY = centroY - ALTO_SILLA/2;
        dibujarSilla(g2d, sillaBarberoX, sillaBarberoY, Color.RED);
        
        // Dibujar Barbero
        dibujarPersona(g2d, sillaBarberoX + ANCHO_SILLA/2 - ANCHO_PERSONA/2, 
                      sillaBarberoY - ALTO_PERSONA/2, Color.BLUE, "Barbero", barberoDormido);
        
        // --- Silla del Cliente ---
        int sillaClienteX = centroX + ANCHO_BARBERIA/8;
        int sillaClienteY = centroY - ALTO_SILLA/2;
        dibujarSilla(g2d, sillaClienteX, sillaClienteY, Color.GREEN);
        
        // Dibujar Cliente siendo atendido
        if (clienteSiendoAtendido) {
            dibujarPersona(g2d, sillaClienteX + ANCHO_SILLA/2 - ANCHO_PERSONA/2, 
                          sillaClienteY - ALTO_PERSONA/2, Color.ORANGE, "C" + clienteActualId, false);
        }
        
        // --- Sillas de Espera ---
        int espacioEntreSillas = ANCHO_SILLA + 20;
        int inicioSillasX = centroX - (NUM_SILLAS_ESPERA * espacioEntreSillas) / 2;
        int sillasY = centroY + ALTO_BARBERIA/4;
        
        for (int i = 0; i < NUM_SILLAS_ESPERA; i++) {
            int sillaX = inicioSillasX + i * espacioEntreSillas;
            dibujarSilla(g2d, sillaX, sillasY, Color.LIGHT_GRAY);
            
            // Dibujar Clientes en Espera
            if (i < clientesEnEspera) {
                dibujarPersona(g2d, sillaX + ANCHO_SILLA/2 - ANCHO_PERSONA/2,
                              sillasY - ALTO_PERSONA/2, Color.ORANGE, "C", false);
            }
        }
    }
    
    private void dibujarSilla(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        // Asiento
        g.fillRect(x, y + ALTO_SILLA/2, ANCHO_SILLA, ALTO_SILLA/4);
        // Respaldo
        g.fillRect(x + ANCHO_SILLA/8, y, ANCHO_SILLA/4, ALTO_SILLA/2);
    }
    
    private void dibujarPersona(Graphics2D g, int x, int y, Color color, String etiqueta, boolean dormido) {
        g.setColor(color);
        // Cuerpo
        g.fillRect(x + ANCHO_PERSONA/4, y + ALTO_PERSONA/3, ANCHO_PERSONA/2, ALTO_PERSONA/2);
        // Cabeza
        g.fillOval(x, y, ANCHO_PERSONA, ALTO_PERSONA/3);
        
        if (dormido) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString("Z", x + ANCHO_PERSONA - 5, y - 5);
            g.drawString("Z", x + ANCHO_PERSONA, y - 15);
        }
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int anchoTexto = fm.stringWidth(etiqueta);
        g.drawString(etiqueta, x + ANCHO_PERSONA/2 - anchoTexto/2, y + ALTO_PERSONA + 15);
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN CON MUTEX (ReentrantLock)
    // ====================================================================

    /**
     * Monitor: Gestiona el estado de la barbería y sincroniza los hilos.
     */
    private class Barberia {
        private final ReentrantLock mutex = new ReentrantLock(); // El Mutex
        private final Condition barberoListo = mutex.newCondition(); // Barbero espera aquí a un cliente
        private final Condition clienteAtendido = mutex.newCondition(); // Cliente espera aquí a ser atendido
        
        private final int numSillas;
        private int clientesEnEspera = 0;
        private final BarberoDormilonPanel panel;

        public Barberia(int numSillas, BarberoDormilonPanel panel) {
            this.numSillas = numSillas;
            this.panel = panel;
        }

        // Método llamado por los hilos Barbero
        public int cortarPelo() throws InterruptedException {
            mutex.lock(); // Mutex: Entrada a la sección crítica
            try {
                // Si no hay clientes, el Barbero se duerme
                while (clientesEnEspera == 0) {
                    panel.actualizarEstado(true, true, 0, false, -1);
                    barberoListo.await(); // Barbero se duerme y libera el Mutex
                }
                
                // Un cliente pasa de la silla de espera a la de corte
                clientesEnEspera--;
                int clienteId = -1; // No es necesario rastrear el ID aquí, pero en una versión completa se haría.
                
                // Actualizar estado antes de la señal
                panel.actualizarEstado(true, false, clientesEnEspera, true, clienteId); 
                
                return clienteId;
            } finally {
                mutex.unlock(); // Mutex: Salida de la sección crítica
            }
        }

        // Método llamado por los hilos Cliente
        public boolean entrar(int id) throws InterruptedException {
            mutex.lock(); // Mutex: Entrada a la sección crítica
            try {
                // 1. Revisar si hay sillas de espera
                if (clientesEnEspera < numSillas) {
                    clientesEnEspera++;
                    panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
                    
                    // 2. Despertar al Barbero si está dormido
                    if (clientesEnEspera == 1) {
                        barberoListo.signal(); // Despierta al Barbero (si estaba dormido)
                        panel.actualizarEstado(true, false, clientesEnEspera, false, -1); // Barbero se despierta
                    }

                    // 3. El cliente espera a ser atendido (en el monitor)
                    // (En la simulación visual, el cliente pasa de la cola a la silla de corte
                    // de forma inmediata tras despertar al barbero o tomar turno)
                    return true;
                } else {
                    // La barbería está llena, el cliente se va
                    return false;
                }
            } finally {
                mutex.unlock(); // Mutex: Salida de la sección crítica
            }
        }
        
        // Método para simular el fin del servicio y liberar los Mutexes
        public void terminarCorte() {
             mutex.lock();
             try {
                panel.actualizarEstado(false, false, clientesEnEspera, false, -1); // El cliente se va
                // El barbero debe volver a cortar o a dormir, el ciclo continúa en el hilo Barbero.
             } finally {
                 mutex.unlock();
             }
        }
    }

    /**
     * Hilo Barbero: Simula el proceso del barbero.
     */
    private class Barbero implements Runnable {
        private final Barberia barberia;
        private final BarberoDormilonPanel panel;

        public Barbero(Barberia barberia, BarberoDormilonPanel panel) {
            this.barberia = barberia;
            this.panel = panel;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    int clienteId = barberia.cortarPelo();
                    
                    // Simular tiempo de corte
                    panel.actualizarEstado(false, false, barberia.clientesEnEspera, true, clienteId);
                    Thread.sleep(random.nextInt(1000) + 2000);
                    
                    barberia.terminarCorte();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class Cliente implements Runnable {
        private final int id;
        private final Barberia barberia;

        public Cliente(int id, Barberia barberia, BarberoDormilonPanel panel) {
            this.id = id;
            this.barberia = barberia;
        }

        @Override
        public void run() {
            try {
                if (!barberia.entrar(id)) {
                    // Cliente se fue porque no había sillas
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN CON SEMÁFOROS
    // ====================================================================

    private class BarberiaSemaforo {
        private final Semaphore mutex = new Semaphore(1);
        private final Semaphore clientes = new Semaphore(0);
        private final Semaphore barbero = new Semaphore(0);
        private final Semaphore sillasAcceso = new Semaphore(1);

        private int clientesEnEspera = 0;
        private final int numSillas;
        private final BarberoDormilonPanel panel;

        public BarberiaSemaforo(int numSillas, BarberoDormilonPanel panel) {
            this.numSillas = numSillas;
            this.panel = panel;
        }

        public void cortarPelo() throws InterruptedException {
            clientes.acquire();
            sillasAcceso.acquire();
            
            clientesEnEspera--;
            panel.actualizarEstado(true, false, clientesEnEspera, true, -1);
            
            barbero.release();
            sillasAcceso.release();
        }

        public boolean entrar(int id) throws InterruptedException {
            sillasAcceso.acquire();
            if (clientesEnEspera < numSillas) {
                clientesEnEspera++;
                panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
                
                clientes.release();
                sillasAcceso.release();
                
                barbero.acquire();
                return true;
            } else {
                sillasAcceso.release();
                return false;
            }
        }
        
        public void terminarCorte() {
            panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
        }
    }

    private class BarberoSemaforo implements Runnable {
        private final BarberiaSemaforo barberia;
        private final BarberoDormilonPanel panel;

        public BarberoSemaforo(BarberiaSemaforo barberia, BarberoDormilonPanel panel) {
            this.barberia = barberia;
            this.panel = panel;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // El barbero intenta cortar pelo (si no hay, espera/duerme)
                    barberia.cortarPelo(); 
                    
                    // Simular tiempo de corte (Sección Crítica Lógica)
                    panel.actualizarEstado(false, false, barberia.clientesEnEspera, true, -1);
                    Thread.sleep(random.nextInt(1000) + 2000); // Cortando 2-5 segundos
                    
                    // Terminar el corte
                    barberia.terminarCorte();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class ClienteSemaforo implements Runnable {
        private final int id;
        private final BarberiaSemaforo barberia;

        public ClienteSemaforo(int id, BarberiaSemaforo barberia, BarberoDormilonPanel panel) {
            this.id = id;
            this.barberia = barberia;
        }

        @Override
        public void run() {
            try {
                if (!barberia.entrar(id)) {
                    // Cliente se fue
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}