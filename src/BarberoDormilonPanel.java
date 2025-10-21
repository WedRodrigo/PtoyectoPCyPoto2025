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
    
    private volatile boolean barberoDormido = true;
    private volatile int clientesEnEspera = 0;
    private volatile boolean clienteSiendoAtendido = false;
    private volatile int clienteActualId = -1;
    
    private final BarberiaCondicion barberiaCondicion; // Variable de Condición
    private final BarberiaMonitores barberiaMonitores; // Monitores
    private final BarberiaSemaforo barberiaSemaforo;
    private final String tipoSincronizacion;
    private final Random random = new Random();
    
    public BarberoDormilonPanel(String tipoSincronizacion) {
        this.tipoSincronizacion = tipoSincronizacion;
        setBackground(Color.WHITE);
        
        if ("Semáforo".equals(tipoSincronizacion)) {
            this.barberiaSemaforo = new BarberiaSemaforo(NUM_SILLAS_ESPERA, this);
            this.barberiaCondicion = null;
            this.barberiaMonitores = null;
        } else if ("Monitores".equals(tipoSincronizacion)) {
            this.barberiaMonitores = new BarberiaMonitores(NUM_SILLAS_ESPERA, this);
            this.barberiaCondicion = null;
            this.barberiaSemaforo = null;
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            this.barberiaCondicion = new BarberiaCondicion(NUM_SILLAS_ESPERA, this);
            this.barberiaMonitores = null;
            this.barberiaSemaforo = null;
        } else { // Mutex
            this.barberiaCondicion = new BarberiaCondicion(NUM_SILLAS_ESPERA, this); // Reusa la implementación Condicion para Mutex
            this.barberiaMonitores = null;
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
        } else if ("Monitores".equals(tipoSincronizacion)) {
            BarberoMonitores barbero = new BarberoMonitores(barberiaMonitores, this);
            new Thread(barbero).start();
            
            new Thread(() -> {
                int clienteId = 1;
                while (true) {
                    try {
                        Thread.sleep(random.nextInt(2000) + 1000); 
                        ClienteMonitores cliente = new ClienteMonitores(clienteId++, barberiaMonitores, this);
                        new Thread(cliente).start();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }).start();
        } else { // Mutex o Variable de Condición
            BarberoCondicion barbero = new BarberoCondicion(barberiaCondicion, this);
            new Thread(barbero).start();
            
            new Thread(() -> {
                int clienteId = 1;
                while (true) {
                    try {
                        Thread.sleep(random.nextInt(2000) + 1000); 
                        ClienteCondicion cliente = new ClienteCondicion(clienteId++, barberiaCondicion, this);
                        new Thread(cliente).start();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }).start();
        }
    }
    
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
    // CLASES DE SINCRONIZACIÓN MONITORES (ReentrantLock / Condition)
    // ====================================================================
    
    private class BarberiaMonitores {
        private final ReentrantLock mutex = new ReentrantLock(); 
        private final Condition barberoListo = mutex.newCondition(); 
        
        private final int numSillas;
        private int clientesEnEspera = 0;
        private final BarberoDormilonPanel panel;

        public BarberiaMonitores(int numSillas, BarberoDormilonPanel panel) {
            this.numSillas = numSillas;
            this.panel = panel;
        }

        public int cortarPelo() throws InterruptedException {
            mutex.lock(); 
            try {
                while (clientesEnEspera == 0) {
                    panel.actualizarEstado(true, true, 0, false, -1);
                    barberoListo.await(); 
                }
                
                clientesEnEspera--;
                int clienteId = -1; 
                panel.actualizarEstado(true, false, clientesEnEspera, true, clienteId); 
                
                return clienteId;
            } finally {
                mutex.unlock(); 
            }
        }

        public boolean entrar(int id) throws InterruptedException {
            mutex.lock(); 
            try {
                if (clientesEnEspera < numSillas) {
                    clientesEnEspera++;
                    panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
                    
                    if (clientesEnEspera == 1) {
                        barberoListo.signal(); 
                    }
                    return true;
                } else {
                    return false;
                }
            } finally {
                mutex.unlock(); 
            }
        }
        
        public void terminarCorte() {
             mutex.lock();
             try {
                panel.actualizarEstado(false, false, clientesEnEspera, false, -1); 
             } finally {
                 mutex.unlock();
             }
        }
    }

    private class BarberoMonitores implements Runnable {
        private final BarberiaMonitores barberia;
        private final BarberoDormilonPanel panel;

        public BarberoMonitores(BarberiaMonitores barberia, BarberoDormilonPanel panel) {
            this.barberia = barberia;
            this.panel = panel;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    int clienteId = barberia.cortarPelo();
                    
                    panel.actualizarEstado(false, false, barberia.clientesEnEspera, true, clienteId);
                    Thread.sleep(random.nextInt(1000) + 2000);
                    
                    barberia.terminarCorte();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class ClienteMonitores implements Runnable {
        private final int id;
        private final BarberiaMonitores barberia;

        public ClienteMonitores(int id, BarberiaMonitores barberia, BarberoDormilonPanel panel) {
            this.id = id;
            this.barberia = barberia;
        }

        @Override
        public void run() {
            try {
                barberia.entrar(id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN VARIABLE DE CONDICIÓN (ReentrantLock / Condition - Estructuralmente Separada)
    // ====================================================================

    private class BarberiaCondicion {
        private final ReentrantLock mutex = new ReentrantLock(); 
        private final Condition barberoListo = mutex.newCondition(); 
        
        private final int numSillas;
        private int clientesEnEspera = 0;
        private final BarberoDormilonPanel panel;

        public BarberiaCondicion(int numSillas, BarberoDormilonPanel panel) {
            this.numSillas = numSillas;
            this.panel = panel;
        }

        public int cortarPelo() throws InterruptedException {
            mutex.lock(); 
            try {
                while (clientesEnEspera == 0) {
                    panel.actualizarEstado(true, true, 0, false, -1);
                    barberoListo.await(); 
                }
                
                clientesEnEspera--;
                int clienteId = -1; 
                panel.actualizarEstado(true, false, clientesEnEspera, true, clienteId); 
                
                return clienteId;
            } finally {
                mutex.unlock(); 
            }
        }

        public boolean entrar(int id) throws InterruptedException {
            mutex.lock(); 
            try {
                if (clientesEnEspera < numSillas) {
                    clientesEnEspera++;
                    panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
                    
                    if (clientesEnEspera == 1) {
                        barberoListo.signal(); 
                    }
                    return true;
                } else {
                    return false;
                }
            } finally {
                mutex.unlock(); 
            }
        }
        
        public void terminarCorte() {
             mutex.lock();
             try {
                panel.actualizarEstado(false, false, clientesEnEspera, false, -1); 
             } finally {
                 mutex.unlock();
             }
        }
    }

    private class BarberoCondicion implements Runnable {
        private final BarberiaCondicion barberia;
        private final BarberoDormilonPanel panel;

        public BarberoCondicion(BarberiaCondicion barberia, BarberoDormilonPanel panel) {
            this.barberia = barberia;
            this.panel = panel;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    int clienteId = barberia.cortarPelo();
                    
                    panel.actualizarEstado(false, false, barberia.clientesEnEspera, true, clienteId);
                    Thread.sleep(random.nextInt(1000) + 2000);
                    
                    barberia.terminarCorte();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class ClienteCondicion implements Runnable {
        private final int id;
        private final BarberiaCondicion barberia;

        public ClienteCondicion(int id, BarberiaCondicion barberia, BarberoDormilonPanel panel) {
            this.id = id;
            this.barberia = barberia;
        }

        @Override
        public void run() {
            try {
                barberia.entrar(id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // ====================================================================
    // CLASES DE SINCRONIZACIÓN MUTEX Y SEMÁFORO (EXISTENTES)
    // ====================================================================
    
    private class Barberia { // Usada por Mutex - ahora en desuso o ruta a Condicion
        private final ReentrantLock mutex = new ReentrantLock(); 
        private final Condition barberoListo = mutex.newCondition(); 
        private final Condition clienteAtendido = mutex.newCondition(); 
        
        private final int numSillas;
        private int clientesEnEspera = 0;
        private final BarberoDormilonPanel panel;

        public Barberia(int numSillas, BarberoDormilonPanel panel) {
            this.numSillas = numSillas;
            this.panel = panel;
        }

        public int cortarPelo() throws InterruptedException {
            mutex.lock(); 
            try {
                while (clientesEnEspera == 0) {
                    panel.actualizarEstado(true, true, 0, false, -1);
                    barberoListo.await(); 
                }
                
                clientesEnEspera--;
                int clienteId = -1; 
                panel.actualizarEstado(true, false, clientesEnEspera, true, clienteId); 
                
                return clienteId;
            } finally {
                mutex.unlock(); 
            }
        }

        public boolean entrar(int id) throws InterruptedException {
            mutex.lock(); 
            try {
                if (clientesEnEspera < numSillas) {
                    clientesEnEspera++;
                    panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
                    
                    if (clientesEnEspera == 1) {
                        barberoListo.signal(); 
                    }

                    return true;
                } else {
                    return false;
                }
            } finally {
                mutex.unlock(); 
            }
        }
        
        public void terminarCorte() {
             mutex.lock();
             try {
                panel.actualizarEstado(false, false, clientesEnEspera, false, -1); 
             } finally {
                 mutex.unlock();
             }
        }
    }

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
                barberia.entrar(id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
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
                    barberia.cortarPelo(); 
                    
                    panel.actualizarEstado(false, false, barberia.clientesEnEspera, true, -1);
                    Thread.sleep(random.nextInt(1000) + 2000); 
                    
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
                barberia.entrar(id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}