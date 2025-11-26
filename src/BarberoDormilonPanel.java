import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.List;
import java.util.ArrayList;

public class BarberoDormilonPanel extends JPanel implements Simulable {
    // ... (Constantes sin cambios) ...
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
    private int proximoClienteId = 1; // Contador para IDs de clientes
    
    private final BarberiaCondicion barberiaCondicion;
    private final BarberiaMonitores barberiaMonitores;
    private final BarberiaSemaforo barberiaSemaforo;
    private final String tipoSincronizacion;
    private final PanelGrafoDinamico panelGrafo; // <-- Referencia al grafo
    private final Random random = new Random();
    private final List<Thread> hilos = new ArrayList<>();
    
    // Constructor modificado
    public BarberoDormilonPanel(String tipoSincronizacion, PanelGrafoDinamico panelGrafo) {
        this.tipoSincronizacion = tipoSincronizacion;
        this.panelGrafo = panelGrafo; // <-- Guardar referencia
        setBackground(Color.WHITE);
        
        // El resto de la lógica del constructor es igual
        if ("Semáforo".equals(tipoSincronizacion)) {
            this.barberiaSemaforo = new BarberiaSemaforo(NUM_SILLAS_ESPERA, this, panelGrafo);
            this.barberiaCondicion = null;
            this.barberiaMonitores = null;
        } else if ("Monitores".equals(tipoSincronizacion)) {
            this.barberiaMonitores = new BarberiaMonitores(NUM_SILLAS_ESPERA, this, panelGrafo);
            this.barberiaCondicion = null;
            this.barberiaSemaforo = null;
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            this.barberiaCondicion = new BarberiaCondicion(NUM_SILLAS_ESPERA, this, panelGrafo);
            this.barberiaMonitores = null;
            this.barberiaSemaforo = null;
        } else { // Mutex
            // Reusamos Condicion para Mutex, pasando el grafo
            this.barberiaCondicion = new BarberiaCondicion(NUM_SILLAS_ESPERA, this, panelGrafo);
            this.barberiaMonitores = null;
            this.barberiaSemaforo = null;
        }
        
        iniciarSimulacion();
    }
    
    @Override
    public void detener() {
        for (Thread hilo : hilos) {
            hilo.interrupt();
        }
        hilos.clear();
    }
    
    private void iniciarSimulacion() {
        if ("Semáforo".equals(tipoSincronizacion)) {
            BarberoSemaforo barbero = new BarberoSemaforo(barberiaSemaforo, this);
            Thread hiloBarbero = new Thread(barbero);
            hiloBarbero.start();
            hilos.add(hiloBarbero);
            
            Thread generadorClientes = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(random.nextInt(2000) + 1000); 
                        int clienteId = proximoClienteId++;
                        // Crear nodo de cliente en el grafo
                        panelGrafo.crearNodoSiNoExiste("P-C" + clienteId, 50, 50 + (clienteId * 30) % 400, "Proceso", "C" + clienteId);
                        ClienteSemaforo cliente = new ClienteSemaforo(clienteId, barberiaSemaforo, this);
                        Thread hiloCliente = new Thread(cliente);
                        hiloCliente.start();
                        hilos.add(hiloCliente);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            generadorClientes.start();
            hilos.add(generadorClientes);
        } else if ("Monitores".equals(tipoSincronizacion)) {
            BarberoMonitores barbero = new BarberoMonitores(barberiaMonitores, this);
            Thread hiloBarbero = new Thread(barbero);
            hiloBarbero.start();
            hilos.add(hiloBarbero);
            
            Thread generadorClientes = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(random.nextInt(2000) + 1000); 
                        int clienteId = proximoClienteId++;
                        panelGrafo.crearNodoSiNoExiste("P-C" + clienteId, 50, 50 + (clienteId * 30) % 400, "Proceso", "C" + clienteId);
                        ClienteMonitores cliente = new ClienteMonitores(clienteId, barberiaMonitores, this);
                        Thread hiloCliente = new Thread(cliente);
                        hiloCliente.start();
                        hilos.add(hiloCliente);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            generadorClientes.start();
            hilos.add(generadorClientes);
        } else { // Mutex o Variable de Condición
            BarberoCondicion barbero = new BarberoCondicion(barberiaCondicion, this);
            Thread hiloBarbero = new Thread(barbero);
            hiloBarbero.start();
            hilos.add(hiloBarbero);
            
            Thread generadorClientes = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(random.nextInt(2000) + 1000); 
                        int clienteId = proximoClienteId++;
                        panelGrafo.crearNodoSiNoExiste("P-C" + clienteId, 50, 50 + (clienteId * 30) % 400, "Proceso", "C" + clienteId);
                        ClienteCondicion cliente = new ClienteCondicion(clienteId, barberiaCondicion, this);
                        Thread hiloCliente = new Thread(cliente);
                        hiloCliente.start();
                        hilos.add(hiloCliente);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            generadorClientes.start();
            hilos.add(generadorClientes);
        }
    }
    
    // Método clave para actualizar el grafo
    public void actualizarEstado(boolean esBarbero, boolean dormido, int clientesQuedan, boolean atendiendo, int clienteId) {
        SwingUtilities.invokeLater(() -> {
            if (esBarbero) {
                this.barberoDormido = dormido;
            }
            this.clientesEnEspera = clientesQuedan;
            this.clienteSiendoAtendido = atendiendo;
            
            // Lógica de actualización del grafo
            if (atendiendo) {
                // El clienteId que está siendo atendido
                this.clienteActualId = clienteId; 
                panelGrafo.setFlechaAsignacion("P-B", "R-Silla");
                panelGrafo.setFlechaAsignacion("P-C" + clienteId, "R-Silla");
                // Ya no está en espera
                panelGrafo.removerFlechas("P-C" + clienteId, "R-Espera");
            } else {
                // Cliente se fue, barbero está libre
                if (this.clienteActualId != -1) {
                    panelGrafo.removerFlechas("P-B", "R-Silla");
                    panelGrafo.removerFlechas("P-C" + this.clienteActualId, "R-Silla");
                    // El nodo del cliente se va
                    panelGrafo.removerNodo("P-C" + this.clienteActualId);
                    this.clienteActualId = -1;
                }
            }
            
            repaint();
        });
    }
    
    // ... (paintComponent, dibujarSilla, dibujarPersona sin cambios) ...
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
    
    // --- Clases de Sincronización (Modificadas para pasar el ID del cliente) ---
    
    private class BarberiaMonitores {
        private final ReentrantLock mutex = new ReentrantLock(); 
        private final Condition barberoListo = mutex.newCondition(); 
        private int clientesEnEspera = 0;
        private final int numSillas;
        private final BarberoDormilonPanel panel;
        private final PanelGrafoDinamico panelGrafo;
        private int clienteAtendidoId = -1; // ID del cliente a atender

        public BarberiaMonitores(int numSillas, BarberoDormilonPanel panel, PanelGrafoDinamico panelGrafo) {
            this.numSillas = numSillas;
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }

        public int cortarPelo() throws InterruptedException {
            mutex.lock(); 
            try {
                while (clientesEnEspera == 0) {
                    panel.actualizarEstado(true, true, 0, false, -1);
                    barberoListo.await(); 
                }
                
                clientesEnEspera--;
                // (Se asume que el cliente que despertó es el que se atiende)
                // Para un monitor real, necesitaríamos una cola (Queue) de IDs
                int clienteId = clienteAtendidoId; // Tomar el ID guardado
                panel.actualizarEstado(true, false, clientesEnEspera, true, clienteId); 
                return clienteId;
            } finally {
                mutex.unlock(); 
            }
        }

        public boolean entrar(int id) throws InterruptedException {
            panelGrafo.setFlechaSolicitud("P-C" + id, "R-Espera");
            mutex.lock(); 
            try {
                if (clientesEnEspera < numSillas) {
                    clientesEnEspera++;
                    panelGrafo.setFlechaAsignacion("P-C" + id, "R-Espera");
                    panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
                    
                    if (clientesEnEspera == 1) {
                        clienteAtendidoId = id; // Guardar ID del primer cliente
                        barberoListo.signal(); 
                    }
                    return true;
                } else {
                    panelGrafo.removerFlechas("P-C" + id, "R-Espera");
                    panelGrafo.removerNodo("P-C" + id); // Cliente se va
                    return false;
                }
            } finally {
                mutex.unlock(); 
            }
        }
        
        public void terminarCorte(int clienteId) {
             mutex.lock();
             try {
                // El panel se encarga de remover nodos en actualizarEstado
                panel.actualizarEstado(false, false, clientesEnEspera, false, clienteId); 
             } finally {
                 mutex.unlock();
             }
        }
    }

    private class BarberoMonitores implements Runnable {
        // ... (constructor) ...
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
                    
                    // Ya actualizado por el monitor, pero re-afirmamos
                    panel.actualizarEstado(false, false, barberia.clientesEnEspera, true, clienteId);
                    Thread.sleep(random.nextInt(1000) + 2000);
                    
                    barberia.terminarCorte(clienteId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class ClienteMonitores implements Runnable {
        // ... (constructor y run) ...
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
    
    // (Las clases BarberiaCondicion, BarberoCondicion, ClienteCondicion son idénticas a las de Monitores)
    
    private class BarberiaCondicion {
        private final ReentrantLock mutex = new ReentrantLock(); 
        private final Condition barberoListo = mutex.newCondition(); 
        private int clientesEnEspera = 0;
        private final int numSillas;
        private final BarberoDormilonPanel panel;
        private final PanelGrafoDinamico panelGrafo;
        private int clienteAtendidoId = -1;

        public BarberiaCondicion(int numSillas, BarberoDormilonPanel panel, PanelGrafoDinamico panelGrafo) {
            this.numSillas = numSillas;
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }

        public int cortarPelo() throws InterruptedException {
            mutex.lock(); 
            try {
                while (clientesEnEspera == 0) {
                    panel.actualizarEstado(true, true, 0, false, -1);
                    barberoListo.await(); 
                }
                
                clientesEnEspera--;
                int clienteId = clienteAtendidoId;
                panel.actualizarEstado(true, false, clientesEnEspera, true, clienteId); 
                return clienteId;
            } finally {
                mutex.unlock(); 
            }
        }

        public boolean entrar(int id) throws InterruptedException {
            panelGrafo.setFlechaSolicitud("P-C" + id, "R-Espera");
            mutex.lock(); 
            try {
                if (clientesEnEspera < numSillas) {
                    clientesEnEspera++;
                    panelGrafo.setFlechaAsignacion("P-C" + id, "R-Espera");
                    panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
                    
                    if (clientesEnEspera == 1) {
                        clienteAtendidoId = id;
                        barberoListo.signal(); 
                    }
                    return true;
                } else {
                    panelGrafo.removerFlechas("P-C" + id, "R-Espera");
                    panelGrafo.removerNodo("P-C" + id);
                    return false;
                }
            } finally {
                mutex.unlock(); 
            }
        }
        
        public void terminarCorte(int clienteId) {
             mutex.lock();
             try {
                panel.actualizarEstado(false, false, clientesEnEspera, false, clienteId); 
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
                    barberia.terminarCorte(clienteId);
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
    
    // --- Clases de Semáforo (Modificadas) ---
    
    private class BarberiaSemaforo {
        private final Semaphore mutex = new Semaphore(1);
        private final Semaphore clientes = new Semaphore(0);
        private final Semaphore barbero = new Semaphore(0);
        private final Semaphore sillasAcceso = new Semaphore(1);
        private int clientesEnEspera = 0;
        private final int numSillas;
        private final BarberoDormilonPanel panel;
        private final PanelGrafoDinamico panelGrafo;
        private int clienteAtendidoId = -1; // ID del cliente a atender
        private int siguienteClienteId = -1; // ID del siguiente en la cola

        public BarberiaSemaforo(int numSillas, BarberoDormilonPanel panel, PanelGrafoDinamico panelGrafo) {
            this.numSillas = numSillas;
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }

        public int cortarPelo() throws InterruptedException {
            clientes.acquire(); // Espera a un cliente
            sillasAcceso.acquire();
            
            clientesEnEspera--;
            clienteAtendidoId = siguienteClienteId; // Atiende al siguiente
            
            panel.actualizarEstado(true, false, clientesEnEspera, true, clienteAtendidoId);
            
            barbero.release(); // Libera al cliente de la silla de espera
            sillasAcceso.release();
            return clienteAtendidoId;
        }

        public boolean entrar(int id) throws InterruptedException {
            panelGrafo.setFlechaSolicitud("P-C" + id, "R-Espera");
            sillasAcceso.acquire();
            if (clientesEnEspera < numSillas) {
                clientesEnEspera++;
                panelGrafo.setFlechaAsignacion("P-C" + id, "R-Espera");
                panel.actualizarEstado(false, false, clientesEnEspera, false, -1);
                
                siguienteClienteId = id; // El último en llegar es el "siguiente" (simplificado)
                clientes.release(); // Despierta al barbero si duerme
                sillasAcceso.release();
                
                panelGrafo.setFlechaSolicitud("P-C" + id, "R-Silla");
                barbero.acquire(); // Espera a que el barbero lo llame
                panelGrafo.setFlechaAsignacion("P-C" + id, "R-Silla");
                panelGrafo.removerFlechas("P-C" + id, "R-Espera"); // Deja la silla de espera
                return true;
            } else {
                sillasAcceso.release();
                panelGrafo.removerFlechas("P-C" + id, "R-Espera");
                panelGrafo.removerNodo("P-C" + id); // Cliente se va
                return false;
            }
        }
        
        public void terminarCorte(int clienteId) {
            panel.actualizarEstado(false, false, clientesEnEspera, false, clienteId);
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
                    int clienteId = barberia.cortarPelo(); 
                    
                    panel.actualizarEstado(false, false, barberia.clientesEnEspera, true, clienteId);
                    Thread.sleep(random.nextInt(1000) + 2000); 
                    
                    barberia.terminarCorte(clienteId);
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
    
    // Las clases MUTEX (Barberia, Barbero, Cliente) no se usarán,
    // ya que estamos usando la implementación de Condicion para "Mutex"
}