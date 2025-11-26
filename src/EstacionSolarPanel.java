import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

public class EstacionSolarPanel extends JPanel implements Simulable {
    private static final int NUM_BAHIAS = 3;
    private static final int NUM_DRONES = 8;
    private static final int ENERGIA_INICIAL = 100;
    private static final int ENERGIA_MINIMA = 20;
    private static final int ANCHO_PANEL = 400;
    private static final int ALTO_PANEL = 300;
    
    private volatile int bahiasDisponibles = NUM_BAHIAS;
    private volatile int energiaSolar = ENERGIA_INICIAL;
    private volatile boolean modoActivo = true;
    private volatile boolean[] dronEstado; // false = volando, true = cargando
    private volatile boolean[] dronCritico;
    private volatile int[] dronPosX, dronPosY;
    private volatile int[] dronBateria;
    
    private final EstacionSolarMonitor estacionMonitor;
    private final EstacionSolarSemaforo estacionSemaforo;
    private final EstacionSolarCondicion estacionCondicion;
    private final String tipoSincronizacion;
    private final PanelGrafoDinamico panelGrafo;
    private final Random random = new Random();
    private final List<Thread> hilos = new ArrayList<>();
    private volatile boolean simulacionActiva = true;
    
    // Colores para visualización
    private static final Color COLOR_DRON_NORMAL = new Color(100, 150, 255);
    private static final Color COLOR_DRON_CRITICO = new Color(255, 100, 100);
    private static final Color COLOR_DRON_CARGANDO = new Color(255, 200, 100);
    private static final Color COLOR_ESTACION = new Color(50, 200, 50);
    private static final Color COLOR_BAHIA_OCUPADA = new Color(255, 100, 100);
    private static final Color COLOR_BAHIA_LIBRE = new Color(200, 255, 200);
    
    public EstacionSolarPanel(String tipoSincronizacion, PanelGrafoDinamico panelGrafo) {
        this.tipoSincronizacion = tipoSincronizacion;
        this.panelGrafo = panelGrafo;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(ANCHO_PANEL, ALTO_PANEL));
        
        // Inicializar arrays
        dronEstado = new boolean[NUM_DRONES];
        dronCritico = new boolean[NUM_DRONES];
        dronPosX = new int[NUM_DRONES];
        dronPosY = new int[NUM_DRONES];
        dronBateria = new int[NUM_DRONES];
        
        // Inicializar posiciones y estados de drones
        for (int i = 0; i < NUM_DRONES; i++) {
            dronEstado[i] = false; // Todos empiezan volando
            dronCritico[i] = random.nextDouble() < 0.3; // 30% de drones críticos
            dronBateria[i] = random.nextInt(50) + 50; // Batería entre 50-100%
            
            // Posiciones aleatorias en el panel
            dronPosX[i] = random.nextInt(ANCHO_PANEL - 100) + 50;
            dronPosY[i] = random.nextInt(ALTO_PANEL - 100) + 50;
        }
        
        // Inicializar monitor según el tipo de sincronización
        if ("Semáforo".equals(tipoSincronizacion)) {
            this.estacionSemaforo = new EstacionSolarSemaforo(NUM_BAHIAS, this, panelGrafo);
            this.estacionMonitor = null;
            this.estacionCondicion = null;
        } else if ("Monitores".equals(tipoSincronizacion)) {
            this.estacionMonitor = new EstacionSolarMonitor(NUM_BAHIAS, this, panelGrafo);
            this.estacionSemaforo = null;
            this.estacionCondicion = null;
        } else if ("Variable de Condición".equals(tipoSincronizacion)) {
            this.estacionCondicion = new EstacionSolarCondicion(NUM_BAHIAS, this, panelGrafo);
            this.estacionMonitor = null;
            this.estacionSemaforo = null;
        } else {
            // Por defecto usamos Monitores
            this.estacionMonitor = new EstacionSolarMonitor(NUM_BAHIAS, this, panelGrafo);
            this.estacionSemaforo = null;
            this.estacionCondicion = null;
        }
        
        iniciarSimulacion();
    }
    
    private void iniciarSimulacion() {
        // Crear y arrancar hilos de drones
        for (int i = 0; i < NUM_DRONES; i++) {
            final int idDron = i;
            Thread dronThread = new Thread(() -> {
                dronTrabajo(idDron);
            }, "Dron-" + i);
            hilos.add(dronThread);
            dronThread.start();
        }
        
        // Crear y arrancar hilo del administrador energético
        Thread adminThread = new Thread(() -> {
            administradorEnergetico();
        }, "Administrador");
        hilos.add(adminThread);
        adminThread.start();
    }
    
    private void dronTrabajo(int idDron) {
        while (simulacionActiva) {
            try {
                // Fase 1: Trabajar (volar/inspeccionar)
                dronEstado[idDron] = false;
                dronPosX[idDron] = random.nextInt(ANCHO_PANEL - 100) + 50;
                dronPosY[idDron] = random.nextInt(ALTO_PANEL - 100) + 50;
                
                int tiempoTrabajo = random.nextInt(2000) + 1000; // 1-3 segundos
                Thread.sleep(tiempoTrabajo);
                
                // Reducir batería mientras trabaja
                dronBateria[idDron] -= random.nextInt(20) + 10;
                if (dronBateria[idDron] < 0) dronBateria[idDron] = 0;
                
                // Verificar si necesita recarga (crítico si batería < 30)
                dronCritico[idDron] = dronBateria[idDron] < 30;
                
                // Fase 2: Solicitar recarga
                panelGrafo.agregarNodo("Dron" + idDron, "Solicitando recarga");
                
                if ("Semáforo".equals(tipoSincronizacion)) {
                    estacionSemaforo.solicitarRecarga(idDron, dronCritico[idDron]);
                } else if ("Monitores".equals(tipoSincronizacion)) {
                    estacionMonitor.solicitarRecarga(idDron, dronCritico[idDron]);
                } else if ("Variable de Condición".equals(tipoSincronizacion)) {
                    estacionCondicion.solicitarRecarga(idDron, dronCritico[idDron]);
                }
                
                // Fase 3: Recargar
                dronEstado[idDron] = true;
                panelGrafo.agregarNodo("Dron" + idDron, "Recargando");
                
                int tiempoRecarga = random.nextInt(1500) + 1000; // 1-2.5 segundos
                Thread.sleep(tiempoRecarga);
                
                // Recargar batería
                dronBateria[idDron] = 100;
                dronCritico[idDron] = false;
                
                // Fase 4: Liberar bahía
                panelGrafo.agregarNodo("Dron" + idDron, "Liberando bahía");
                
                if ("Semáforo".equals(tipoSincronizacion)) {
                    estacionSemaforo.liberarBahia(idDron);
                } else if ("Monitores".equals(tipoSincronizacion)) {
                    estacionMonitor.liberarBahia(idDron);
                } else if ("Variable de Condición".equals(tipoSincronizacion)) {
                    estacionCondicion.liberarBahia(idDron);
                }
                
                panelGrafo.agregarNodo("Dron" + idDron, "Recarga completa");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void administradorEnergetico() {
        while (simulacionActiva) {
            try {
                // Simular variación de energía solar (ciclos día/nube)
                int cambioEnergia = random.nextInt(30) - 15; // -15 a +15
                energiaSolar += cambioEnergia;
                
                // Limitar energía entre 0 y 150
                if (energiaSolar < 0) energiaSolar = 0;
                if (energiaSolar > 150) energiaSolar = 150;
                
                // Actualizar modo activo
                boolean modoAnterior = modoActivo;
                modoActivo = energiaSolar > 0;
                
                // Actualizar en el monitor/semáforo
                if ("Semáforo".equals(tipoSincronizacion)) {
                    estacionSemaforo.actualizarEnergia(energiaSolar);
                } else if ("Monitores".equals(tipoSincronizacion)) {
                    estacionMonitor.actualizarEnergia(energiaSolar);
                } else if ("Variable de Condición".equals(tipoSincronizacion)) {
                    estacionCondicion.actualizarEnergia(energiaSolar);
                }
                
                // Si la energía se restableció, notificar
                if (!modoAnterior && modoActivo) {
                    panelGrafo.agregarNodo("Admin", "Energía restablecida: " + energiaSolar);
                } else if (modoAnterior && !modoActivo) {
                    panelGrafo.agregarNodo("Admin", "Sin energía solar");
                }
                
                Thread.sleep(1000); // Actualizar cada segundo
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Dibujar estación solar (centro)
        int centroX = getWidth() / 2;
        int centroY = getHeight() / 2;
        int estacionAncho = 120;
        int estacionAlto = 80;
        
        // Fondo de la estación
        g.setColor(COLOR_ESTACION);
        g.fillRect(centroX - estacionAncho/2, centroY - estacionAlto/2, estacionAncho, estacionAlto);
        g.setColor(Color.BLACK);
        g.drawRect(centroX - estacionAncho/2, centroY - estacionAlto/2, estacionAncho, estacionAlto);
        
        // Texto de la estación
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Estación Solar", centroX - 35, centroY - 25);
        g.drawString("Energía: " + energiaSolar, centroX - 30, centroY - 10);
        g.drawString("Bahías: " + bahiasDisponibles + "/" + NUM_BAHIAS, centroX - 30, centroY + 5);
        g.drawString("Modo: " + (modoActivo ? "Activo" : "Inactivo"), centroX - 25, centroY + 20);
        
        // Dibujar bahías
        int bahiaAncho = 30;
        int bahiaAlto = 20;
        int inicioX = centroX - (NUM_BAHIAS * bahiaAncho) / 2;
        int bahiaY = centroY + estacionAlto/2 + 10;
        
        for (int i = 0; i < NUM_BAHIAS; i++) {
            int bahiaX = inicioX + i * bahiaAncho;
            
            // Color según disponibilidad
            if (i < (NUM_BAHIAS - bahiasDisponibles)) {
                g.setColor(COLOR_BAHIA_OCUPADA);
            } else {
                g.setColor(COLOR_BAHIA_LIBRE);
            }
            
            g.fillRect(bahiaX, bahiaY, bahiaAncho - 2, bahiaAlto);
            g.setColor(Color.BLACK);
            g.drawRect(bahiaX, bahiaY, bahiaAncho - 2, bahiaAlto);
            g.drawString("B" + (i+1), bahiaX + 8, bahiaY + 14);
        }
        
        // Dibujar drones
        for (int i = 0; i < NUM_DRONES; i++) {
            int x = dronPosX[i];
            int y = dronPosY[i];
            
            // Color según estado
            if (dronEstado[i]) {
                g.setColor(COLOR_DRON_CARGANDO);
            } else if (dronCritico[i]) {
                g.setColor(COLOR_DRON_CRITICO);
            } else {
                g.setColor(COLOR_DRON_NORMAL);
            }
            
            // Cuerpo del dron
            g.fillOval(x, y, 20, 20);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, 20, 20);
            
            // Hélices
            g.drawLine(x + 10, y - 5, x + 10, y - 15);
            g.drawLine(x - 5, y + 10, x - 15, y + 10);
            g.drawLine(x + 25, y + 10, x + 35, y + 10);
            g.drawLine(x + 10, y + 25, x + 10, y + 35);
            
            // ID y batería
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.drawString("D" + i, x + 6, y + 14);
            g.drawString(dronBateria[i] + "%", x - 5, y + 45);
            
            // Indicador de crítico
            if (dronCritico[i] && !dronEstado[i]) {
                g.setColor(Color.RED);
                g.fillOval(x + 15, y - 10, 8, 8);
            }
        }
        
        // Leyenda
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.drawString("Leyenda:", 10, 20);
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        
        int leyendaY = 35;
        g.setColor(COLOR_DRON_NORMAL);
        g.fillOval(10, leyendaY - 5, 10, 10);
        g.setColor(Color.BLACK);
        g.drawString("Dron normal", 25, leyendaY + 5);
        
        leyendaY += 20;
        g.setColor(COLOR_DRON_CRITICO);
        g.fillOval(10, leyendaY - 5, 10, 10);
        g.setColor(Color.BLACK);
        g.drawString("Dron crítico", 25, leyendaY + 5);
        
        leyendaY += 20;
        g.setColor(COLOR_DRON_CARGANDO);
        g.fillOval(10, leyendaY - 5, 10, 10);
        g.setColor(Color.BLACK);
        g.drawString("Dron cargando", 25, leyendaY + 5);
    }
    
    // Métodos para actualizar el estado visual
    public void setBahiasDisponibles(int bahias) {
        this.bahiasDisponibles = bahias;
        repaint();
    }
    
    public void setEnergiaSolar(int energia) {
        this.energiaSolar = energia;
        repaint();
    }
    
    public void setModoActivo(boolean activo) {
        this.modoActivo = activo;
        repaint();
    }
    
    @Override
    public void detener() {
        simulacionActiva = false;
        for (Thread hilo : hilos) {
            hilo.interrupt();
        }
    }
    
    // Implementación con Monitores
    private static class EstacionSolarMonitor {
        private int bahias;
        private int energiaSolar;
        private boolean modoActivo;
        private final Queue<Integer> colaCriticos;
        private final Queue<Integer> colaNormales;
        private final EstacionSolarPanel panel;
        private final PanelGrafoDinamico panelGrafo;
        
        public EstacionSolarMonitor(int bahias, EstacionSolarPanel panel, PanelGrafoDinamico panelGrafo) {
            this.bahias = bahias;
            this.energiaSolar = 100;
            this.modoActivo = true;
            this.colaCriticos = new LinkedList<>();
            this.colaNormales = new LinkedList<>();
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }
        
        public synchronized void solicitarRecarga(int idDron, boolean critico) {
            panelGrafo.agregarNodo("Estacion", "Dron" + idDron + " solicita recarga (critico:" + critico + ")");
            
            // Agregar a la cola correspondiente
            if (critico) {
                colaCriticos.offer(idDron);
            } else {
                colaNormales.offer(idDron);
            }
            
            // Esperar hasta que se pueda cargar
            while (!modoActivo || bahias == 0 || 
                   (critico && !colaCriticos.peek().equals(idDron)) ||
                   (!critico && (!colaCriticos.isEmpty() || !colaNormales.peek().equals(idDron)))) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            // Asignar recursos
            bahias--;
            if (critico) {
                colaCriticos.poll();
            } else {
                colaNormales.poll();
            }
            
            panel.setBahiasDisponibles(bahias);
            panelGrafo.agregarNodo("Estacion", "Bahía asignada a Dron" + idDron);
            notifyAll();
        }
        
        public synchronized void liberarBahia(int idDron) {
            bahias++;
            panel.setBahiasDisponibles(bahias);
            panelGrafo.agregarNodo("Estacion", "Bahía liberada por Dron" + idDron);
            notifyAll();
        }
        
        public synchronized void actualizarEnergia(int nuevaEnergia) {
            energiaSolar = nuevaEnergia;
            modoActivo = energiaSolar > 0;
            panel.setEnergiaSolar(energiaSolar);
            panel.setModoActivo(modoActivo);
            
            if (modoActivo) {
                panelGrafo.agregarNodo("Estacion", "Energía actualizada: " + energiaSolar);
                notifyAll();
            }
        }
    }
    
    // Implementación con Semáforos
    private static class EstacionSolarSemaforo {
        private final Semaphore bahiasSemaforo;
        private final Semaphore mutex;
        private int bahias;
        private int energiaSolar;
        private boolean modoActivo;
        private final Queue<Integer> colaCriticos;
        private final Queue<Integer> colaNormales;
        private final EstacionSolarPanel panel;
        private final PanelGrafoDinamico panelGrafo;
        
        public EstacionSolarSemaforo(int numBahias, EstacionSolarPanel panel, PanelGrafoDinamico panelGrafo) {
            this.bahiasSemaforo = new Semaphore(numBahias);
            this.mutex = new Semaphore(1);
            this.bahias = numBahias;
            this.energiaSolar = 100;
            this.modoActivo = true;
            this.colaCriticos = new LinkedList<>();
            this.colaNormales = new LinkedList<>();
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }
        
        public void solicitarRecarga(int idDron, boolean critico) {
            panelGrafo.agregarNodo("Estacion", "Dron" + idDron + " solicita recarga (critico:" + critico + ")");
            
            try {
                mutex.acquire();
                
                // Agregar a la cola correspondiente
                if (critico) {
                    colaCriticos.offer(idDron);
                } else {
                    colaNormales.offer(idDron);
                }
                
                mutex.release();
                
                // Esperar hasta que se pueda cargar
                while (true) {
                    mutex.acquire();
                    
                    boolean puedeCargar = modoActivo && 
                        ((critico && colaCriticos.peek() != null && colaCriticos.peek().equals(idDron)) ||
                         (!critico && colaCriticos.isEmpty() && colaNormales.peek() != null && colaNormales.peek().equals(idDron)));
                    
                    if (puedeCargar && bahiasSemaforo.tryAcquire()) {
                        // Asignar recursos
                        bahias--;
                        if (critico) {
                            colaCriticos.poll();
                        } else {
                            colaNormales.poll();
                        }
                        
                        panel.setBahiasDisponibles(bahias);
                        panelGrafo.agregarNodo("Estacion", "Bahía asignada a Dron" + idDron);
                        mutex.release();
                        break;
                    }
                    
                    mutex.release();
                    Thread.sleep(100); // Esperar un poco antes de reintentar
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        public void liberarBahia(int idDron) {
            try {
                bahiasSemaforo.release();
                mutex.acquire();
                bahias++;
                panel.setBahiasDisponibles(bahias);
                panelGrafo.agregarNodo("Estacion", "Bahía liberada por Dron" + idDron);
                mutex.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        public void actualizarEnergia(int nuevaEnergia) {
            try {
                mutex.acquire();
                energiaSolar = nuevaEnergia;
                modoActivo = energiaSolar > 0;
                panel.setEnergiaSolar(energiaSolar);
                panel.setModoActivo(modoActivo);
                
                if (modoActivo) {
                    panelGrafo.agregarNodo("Estacion", "Energía actualizada: " + energiaSolar);
                }
                mutex.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // Implementación con Variables de Condición
    private static class EstacionSolarCondicion {
        private int bahias;
        private int energiaSolar;
        private boolean modoActivo;
        private final Queue<Integer> colaCriticos;
        private final Queue<Integer> colaNormales;
        private final ReentrantLock lock;
        private final Condition esperarBahia;
        private final Condition esperarEnergia;
        private final EstacionSolarPanel panel;
        private final PanelGrafoDinamico panelGrafo;
        
        public EstacionSolarCondicion(int numBahias, EstacionSolarPanel panel, PanelGrafoDinamico panelGrafo) {
            this.bahias = numBahias;
            this.energiaSolar = 100;
            this.modoActivo = true;
            this.colaCriticos = new LinkedList<>();
            this.colaNormales = new LinkedList<>();
            this.lock = new ReentrantLock();
            this.esperarBahia = lock.newCondition();
            this.esperarEnergia = lock.newCondition();
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }
        
        public void solicitarRecarga(int idDron, boolean critico) {
            panelGrafo.agregarNodo("Estacion", "Dron" + idDron + " solicita recarga (critico:" + critico + ")");
            
            lock.lock();
            try {
                // Agregar a la cola correspondiente
                if (critico) {
                    colaCriticos.offer(idDron);
                } else {
                    colaNormales.offer(idDron);
                }
                
                // Esperar hasta que se pueda cargar
                while (!modoActivo || bahias == 0 || 
                       (critico && !colaCriticos.peek().equals(idDron)) ||
                       (!critico && (!colaCriticos.isEmpty() || !colaNormales.peek().equals(idDron)))) {
                    esperarBahia.await();
                }
                
                // Asignar recursos
                bahias--;
                if (critico) {
                    colaCriticos.poll();
                } else {
                    colaNormales.poll();
                }
                
                panel.setBahiasDisponibles(bahias);
                panelGrafo.agregarNodo("Estacion", "Bahía asignada a Dron" + idDron);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }
        
        public void liberarBahia(int idDron) {
            lock.lock();
            try {
                bahias++;
                panel.setBahiasDisponibles(bahias);
                panelGrafo.agregarNodo("Estacion", "Bahía liberada por Dron" + idDron);
                esperarBahia.signalAll();
            } finally {
                lock.unlock();
            }
        }
        
        public void actualizarEnergia(int nuevaEnergia) {
            lock.lock();
            try {
                energiaSolar = nuevaEnergia;
                modoActivo = energiaSolar > 0;
                panel.setEnergiaSolar(energiaSolar);
                panel.setModoActivo(modoActivo);
                
                if (modoActivo) {
                    panelGrafo.agregarNodo("Estacion", "Energía actualizada: " + energiaSolar);
                    esperarBahia.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }
    }
}