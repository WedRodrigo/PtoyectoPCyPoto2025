import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class EstacionSolarPanelParalelo extends JPanel implements Simulable {
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
    
    // Sistemas de sincronización paralelos
    private final EstacionSolarMonitor estacionMonitor;
    private final EstacionSolarSemaforo estacionSemaforo;
    private final EstacionSolarCondicion estacionCondicion;
    private final EstacionSolarMutex estacionMutex;
    private final EstacionSolarBarrera estacionBarrera;
    private final PanelGrafoDinamico panelGrafo;
    private final Random random = new Random();
    private final List<Thread> hilos = new ArrayList<>();
    private volatile boolean simulacionActiva = true;
    
    // Control de ejecución paralela
    private final ExecutorService executorParalelo;
    private final ConcurrentLinkedQueue<ResultadoAlgoritmo> resultadosQueue;
    private final AtomicInteger totalRecargas;
    private final AtomicInteger recargasCriticas;
    private final AtomicInteger recargasNormales;
    
    // Colores para visualización
    private static final Color COLOR_DRON_NORMAL = new Color(100, 150, 255);
    private static final Color COLOR_DRON_CRITICO = new Color(255, 100, 100);
    private static final Color COLOR_DRON_CARGANDO = new Color(255, 200, 100);
    private static final Color COLOR_ESTACION = new Color(50, 200, 50);
    private static final Color COLOR_BAHIA_OCUPADA = new Color(255, 100, 100);
    private static final Color COLOR_BAHIA_LIBRE = new Color(200, 255, 200);
    
    // Referencia al panel de gráficas
    private GraficasPanel graficasPanel;
    
    // JLabel para mostrar contador de resultados (temporal para debug)
    private JLabel contadorLabel;
    private AtomicInteger contadorResultados;
    
    // Clase para almacenar resultados
    private static class ResultadoAlgoritmo {
        final String algoritmo;
        final String tipoEvento;
        final int idDron;
        final long timestamp;
        final int valor;
        
        ResultadoAlgoritmo(String algoritmo, String tipoEvento, int idDron, int valor) {
            this.algoritmo = algoritmo;
            this.tipoEvento = tipoEvento;
            this.idDron = idDron;
            this.timestamp = System.currentTimeMillis();
            this.valor = valor;
        }
    }
    
    public EstacionSolarPanelParalelo(PanelGrafoDinamico panelGrafo, GraficasPanel graficasPanel) {
        this.panelGrafo = panelGrafo;
        this.graficasPanel = graficasPanel;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(ANCHO_PANEL, ALTO_PANEL));
        
        // Inicializar JLabel para contador (temporal para debug)
        contadorLabel = new JLabel("Resultados procesados: 0");
        contadorLabel.setForeground(Color.BLACK);
        contadorLabel.setBounds(10, 10, 200, 20);
        add(contadorLabel);
        
        // Inicializar sistemas de sincronización
        this.estacionMonitor = new EstacionSolarMonitor(NUM_BAHIAS, this, panelGrafo);
        this.estacionSemaforo = new EstacionSolarSemaforo(NUM_BAHIAS, this, panelGrafo);
        this.estacionCondicion = new EstacionSolarCondicion(NUM_BAHIAS, this, panelGrafo);
        this.estacionMutex = new EstacionSolarMutex(NUM_BAHIAS, this, panelGrafo);
        this.estacionBarrera = new EstacionSolarBarrera(NUM_BAHIAS, this, panelGrafo);
        
        // Inicializar control de paralelismo
        this.executorParalelo = Executors.newFixedThreadPool(6); // 5 algoritmos + 1 procesador
        this.resultadosQueue = new ConcurrentLinkedQueue<>();
        this.totalRecargas = new AtomicInteger(0);
        this.recargasCriticas = new AtomicInteger(0);
        this.recargasNormales = new AtomicInteger(0);
        this.contadorResultados = new AtomicInteger(0);
        
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
        
        iniciarSimulacionParalela();
    }
    
    private void iniciarSimulacionParalela() {
        // Iniciar los 5 algoritmos en paralelo
        executorParalelo.submit(() -> ejecutarAlgoritmoConMonitor("Monitores"));
        executorParalelo.submit(() -> ejecutarAlgoritmoConSemaforo("Semáforos"));
        executorParalelo.submit(() -> ejecutarAlgoritmoConCondicion("Variables de Condición"));
        executorParalelo.submit(() -> ejecutarAlgoritmoConMutex("Mutex"));
        executorParalelo.submit(() -> ejecutarAlgoritmoConBarrera("Barrera"));
        
        // Iniciar procesador de resultados
        executorParalelo.submit(this::procesarResultados);
        
        // Iniciar administrador energético
        Thread adminThread = new Thread(this::administradorEnergetico, "Administrador");
        hilos.add(adminThread);
        adminThread.start();
    }
    
    private void ejecutarAlgoritmoConMonitor(String nombreAlgoritmo) {
        panelGrafo.agregarNodo("PARALELO", "Algoritmo " + nombreAlgoritmo + " iniciado");
        
        int recargasCompletadas = 0;
        
        while (simulacionActiva && recargasCompletadas < 15) {
            try {
                int idDron = random.nextInt(NUM_DRONES);
                boolean critico = actualizarEstadoDron(idDron);
                
                if (critico || random.nextDouble() < 0.4) {
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "SOLICITUD", idDron, critico ? 1 : 0));
                    
                    // Usar monitor para sincronización
                    estacionMonitor.solicitarRecarga(idDron, critico);
                    
                    // Simular recarga
                    dronEstado[idDron] = true;
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "RECARGANDO", idDron, 0));
                    
                    Thread.sleep(random.nextInt(1000) + 500);
                    
                    // Completar recarga
                    dronBateria[idDron] = 100;
                    dronCritico[idDron] = false;
                    estacionMonitor.liberarBahia(idDron);
                    
                    recargasCompletadas++;
                    totalRecargas.incrementAndGet();
                    if (critico) recargasCriticas.incrementAndGet();
                    else recargasNormales.incrementAndGet();
                    
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "COMPLETADO", idDron, 0));
                    dronEstado[idDron] = false;
                }
                
                Thread.sleep(random.nextInt(1000) + 500);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "FINALIZADO", 0, recargasCompletadas));
        // // System.out.println(nombreAlgoritmo + " finalizado: " + recargasCompletadas + " recargas");
    }
    
    private void ejecutarAlgoritmoConSemaforo(String nombreAlgoritmo) {
        // // System.out.println("Iniciando algoritmo: " + nombreAlgoritmo);
        panelGrafo.agregarNodo("PARALELO", "Algoritmo " + nombreAlgoritmo + " iniciado");
        
        int recargasCompletadas = 0;
        
        while (simulacionActiva && recargasCompletadas < 15) {
            try {
                int idDron = random.nextInt(NUM_DRONES);
                boolean critico = actualizarEstadoDron(idDron);
                
                if (critico || random.nextDouble() < 0.4) {
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "SOLICITUD", idDron, critico ? 1 : 0));
                    
                    // Usar semáforo para sincronización
                    estacionSemaforo.solicitarRecarga(idDron, critico);
                    
                    // Simular recarga
                    dronEstado[idDron] = true;
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "RECARGANDO", idDron, 0));
                    
                    Thread.sleep(random.nextInt(1000) + 500);
                    
                    // Completar recarga
                    dronBateria[idDron] = 100;
                    dronCritico[idDron] = false;
                    estacionSemaforo.liberarBahia(idDron);
                    
                    recargasCompletadas++;
                    totalRecargas.incrementAndGet();
                    if (critico) recargasCriticas.incrementAndGet();
                    else recargasNormales.incrementAndGet();
                    
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "COMPLETADO", idDron, 0));
                    dronEstado[idDron] = false;
                }
                
                Thread.sleep(random.nextInt(1000) + 500);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "FINALIZADO", 0, recargasCompletadas));
        System.out.println(nombreAlgoritmo + " finalizado: " + recargasCompletadas + " recargas");
    }
    
    private void ejecutarAlgoritmoConCondicion(String nombreAlgoritmo) {
        System.out.println("Iniciando algoritmo: " + nombreAlgoritmo);
        panelGrafo.agregarNodo("PARALELO", "Algoritmo " + nombreAlgoritmo + " iniciado");
        
        int recargasCompletadas = 0;
        
        while (simulacionActiva && recargasCompletadas < 15) {
            try {
                int idDron = random.nextInt(NUM_DRONES);
                boolean critico = actualizarEstadoDron(idDron);
                
                if (critico || random.nextDouble() < 0.4) {
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "SOLICITUD", idDron, critico ? 1 : 0));
                    
                    // Usar variables de condición para sincronización
                    estacionCondicion.solicitarRecarga(idDron, critico);
                    
                    // Simular recarga
                    dronEstado[idDron] = true;
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "RECARGANDO", idDron, 0));
                    
                    Thread.sleep(random.nextInt(1000) + 500);
                    
                    // Completar recarga
                    dronBateria[idDron] = 100;
                    dronCritico[idDron] = false;
                    estacionCondicion.liberarBahia(idDron);
                    
                    recargasCompletadas++;
                    totalRecargas.incrementAndGet();
                    if (critico) recargasCriticas.incrementAndGet();
                    else recargasNormales.incrementAndGet();
                    
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "COMPLETADO", idDron, 0));
                    dronEstado[idDron] = false;
                }
                
                Thread.sleep(random.nextInt(1000) + 500);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "FINALIZADO", 0, recargasCompletadas));
        System.out.println(nombreAlgoritmo + " finalizado: " + recargasCompletadas + " recargas");
    }
    
    private boolean actualizarEstadoDron(int idDron) {
        // Reducir batería
        dronBateria[idDron] -= random.nextInt(15) + 5;
        if (dronBateria[idDron] < 0) dronBateria[idDron] = 0;
        
        // Actualizar estado crítico
        dronCritico[idDron] = dronBateria[idDron] < 30;
        
        return dronCritico[idDron];
    }
    
    private void procesarResultados() {
        while (simulacionActiva) {
            try {
                ResultadoAlgoritmo resultado = resultadosQueue.poll();
                if (resultado != null) {
                    contadorResultados.incrementAndGet();
                    // Actualizar contador en EDT
                    SwingUtilities.invokeLater(() -> {
                        contadorLabel.setText("Resultados procesados: " + contadorResultados.get());
                    });
                    // Procesar resultado y actualizar gráficos
                    String etiqueta = resultado.algoritmo + "_" + resultado.tipoEvento;
                    String detalles = "Dron" + resultado.idDron + " - " + resultado.tipoEvento;
                    
                    panelGrafo.agregarNodo(etiqueta, detalles);
                    
                    // Mapear nombres de algoritmos a series del gráfico
                    String nombreSerie = mapearNombreSerie(resultado.algoritmo);
                    
                    // Mapear tipos de evento a valores numéricos
                    int valorEvento = mapearValorEvento(resultado.tipoEvento);
                    
                    // Agregar punto al gráfico si tenemos una serie válida
                    if (nombreSerie != null && graficasPanel != null) {
                        final String serieFinal = nombreSerie;
                        final long timestampFinal = resultado.timestamp;
                        final int valorFinal = valorEvento;
                        
                        // Debug: mostrar información del punto a agregar
                        final String debugInfo = "Agregando punto - Serie: " + serieFinal + ", Tiempo: " + timestampFinal + ", Valor: " + valorFinal;
                        
                        // Asegurar que se ejecute en el EDT para actualizar la GUI
                        SwingUtilities.invokeLater(() -> {
                            contadorLabel.setText("Resultados: " + contadorResultados.get() + " - " + debugInfo);
                            graficasPanel.addPoint(serieFinal, timestampFinal, valorFinal);
                        });
                    }
                    
                    // Actualizar estadísticas en tiempo real
                    if ("COMPLETADO".equals(resultado.tipoEvento)) {
                        SwingUtilities.invokeLater(() -> {
                            repaint(); // Redibujar panel con nuevas estadísticas
                        });
                    }
                }
                
                Thread.sleep(50); // Pequeña pausa para no sobrecargar CPU
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    // Métodos de mapeo para conectar con GraficasPanel
    private String mapearNombreSerie(String nombreAlgoritmo) {
        switch (nombreAlgoritmo) {
            case "Monitores":
                return "Núcleo 3 (Monitores)";
            case "Semáforos":
                return "Núcleo 2 (Semáforos)";
            case "Variables de Condición":
                return "Núcleo 4 (Var. Cond)";
            case "Mutex":
                return "Núcleo 1 (Mutex)";
            case "Barrera":
                return "Núcleo 5 (Barreras)";
            default:
                return null;
        }
    }
    
    private int mapearValorEvento(String tipoEvento) {
        switch (tipoEvento) {
            case "SOLICITUD":
                return 1;
            case "RECARGANDO":
                return 2;
            case "COMPLETADO":
                return 3;
            case "FINALIZADO":
                return 4;
            default:
                return 0;
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
                
                // Actualizar en todos los sistemas de sincronización
                estacionMonitor.actualizarEnergia(energiaSolar);
                estacionSemaforo.actualizarEnergia(energiaSolar);
                estacionCondicion.actualizarEnergia(energiaSolar);
                estacionMutex.actualizarEnergia(energiaSolar);
                estacionBarrera.actualizarEnergia(energiaSolar);
                
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
        g.drawString("Estación Solar (Paralelo)", centroX - 50, centroY - 25);
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
            g.drawString("B" + (i + 1), bahiaX + 8, bahiaY + 14);
        }
        
        // Dibujar estadísticas de paralelismo
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("Estadísticas Paralelas:", 10, 20);
        g.drawString("Total recargas: " + totalRecargas.get(), 10, 35);
        g.drawString("Críticas: " + recargasCriticas.get(), 10, 50);
        g.drawString("Normales: " + recargasNormales.get(), 10, 65);
        
        // Dibujar drones
        for (int i = 0; i < NUM_DRONES; i++) {
            // Color según estado
            if (dronEstado[i]) {
                g.setColor(COLOR_DRON_CARGANDO);
            } else if (dronCritico[i]) {
                g.setColor(COLOR_DRON_CRITICO);
            } else {
                g.setColor(COLOR_DRON_NORMAL);
            }
            
            g.fillOval(dronPosX[i], dronPosY[i], 20, 20);
            g.setColor(Color.BLACK);
            g.drawOval(dronPosX[i], dronPosY[i], 20, 20);
            
            // Etiqueta del dron
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("D" + i, dronPosX[i] + 6, dronPosY[i] + 13);
            
            // Barra de batería
            g.setColor(Color.GRAY);
            g.fillRect(dronPosX[i] - 5, dronPosY[i] + 25, 30, 5);
            
            if (dronBateria[i] > 50) {
                g.setColor(Color.GREEN);
            } else if (dronBateria[i] > 20) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.RED);
            }
            
            g.fillRect(dronPosX[i] - 5, dronPosY[i] + 25, (int)(30 * dronBateria[i] / 100.0), 5);
            g.setColor(Color.BLACK);
            g.drawRect(dronPosX[i] - 5, dronPosY[i] + 25, 30, 5);
            
            // Porcentaje de batería
            g.drawString(dronBateria[i] + "%", dronPosX[i] - 5, dronPosY[i] + 45);
        }
    }
    
    @Override
    public void detener() {
        simulacionActiva = false;
        
        // Detener executor de paralelismo
        executorParalelo.shutdown();
        try {
            if (!executorParalelo.awaitTermination(5, TimeUnit.SECONDS)) {
                executorParalelo.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorParalelo.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Detener hilos tradicionales
        for (Thread hilo : hilos) {
            hilo.interrupt();
        }
        
        panelGrafo.agregarNodo("PARALELO", "Simulación paralela finalizada");
        // System.out.println("Simulación paralela detenida");
        // System.out.println("Total recargas: " + totalRecargas.get());
        // System.out.println("Críticas: " + recargasCriticas.get() + ", Normales: " + recargasNormales.get());
    }
    
    // Clases de sincronización (mismo código que antes, pero adaptadas para paralelismo)
    private static class EstacionSolarMonitor {
        private int bahias;
        private int energiaSolar;
        private boolean modoActivo;
        private final Queue<Integer> colaCriticos;
        private final Queue<Integer> colaNormales;
        private final EstacionSolarPanelParalelo panel;
        private final PanelGrafoDinamico panelGrafo;
        
        public EstacionSolarMonitor(int numBahias, EstacionSolarPanelParalelo panel, PanelGrafoDinamico panelGrafo) {
            this.bahias = numBahias;
            this.energiaSolar = 100;
            this.modoActivo = true;
            this.colaCriticos = new LinkedList<>();
            this.colaNormales = new LinkedList<>();
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }
        
        public synchronized void solicitarRecarga(int idDron, boolean critico) throws InterruptedException {
            panelGrafo.agregarNodo("Monitores", "Dron" + idDron + " solicita recarga (critico:" + critico + ")");
            
            // Agregar a la cola correspondiente
            if (critico) {
                colaCriticos.offer(idDron);
            } else {
                colaNormales.offer(idDron);
            }
            
            // Esperar hasta que se pueda cargar
            while (true) {
                boolean puedeCargar = modoActivo && bahias > 0 &&
                    ((critico && !colaCriticos.isEmpty() && colaCriticos.peek().equals(idDron)) ||
                     (!critico && colaCriticos.isEmpty() && !colaNormales.isEmpty() && colaNormales.peek().equals(idDron)));
                
                if (puedeCargar) {
                    // Asignar recursos
                    bahias--;
                    if (critico) {
                        colaCriticos.poll();
                    } else {
                        colaNormales.poll();
                    }
                    
                    panel.setBahiasDisponibles(bahias);
                    panelGrafo.agregarNodo("Monitores", "Bahía asignada a Dron" + idDron);
                    break;
                }
                
                wait();
            }
        }
        
        public synchronized void liberarBahia(int idDron) {
            bahias++;
            panel.setBahiasDisponibles(bahias);
            panelGrafo.agregarNodo("Monitores", "Bahía liberada por Dron" + idDron);
            notifyAll();
        }
        
        public synchronized void actualizarEnergia(int nuevaEnergia) {
            energiaSolar = nuevaEnergia;
            modoActivo = energiaSolar > 0;
            panel.setEnergiaSolar(energiaSolar);
            panel.setModoActivo(modoActivo);
            
            if (modoActivo) {
                panelGrafo.agregarNodo("Monitores", "Energía actualizada: " + energiaSolar);
                notifyAll();
            }
        }
    }
    
    private static class EstacionSolarSemaforo {
        private final Semaphore bahiasSemaforo;
        private final Semaphore mutex;
        private int bahias;
        private int energiaSolar;
        private boolean modoActivo;
        private final Queue<Integer> colaCriticos;
        private final Queue<Integer> colaNormales;
        private final EstacionSolarPanelParalelo panel;
        private final PanelGrafoDinamico panelGrafo;
        
        public EstacionSolarSemaforo(int numBahias, EstacionSolarPanelParalelo panel, PanelGrafoDinamico panelGrafo) {
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
        
        public void solicitarRecarga(int idDron, boolean critico) throws InterruptedException {
            panelGrafo.agregarNodo("Semáforos", "Dron" + idDron + " solicita recarga (critico:" + critico + ")");
            
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
                    ((critico && !colaCriticos.isEmpty() && colaCriticos.peek().equals(idDron)) ||
                     (!critico && colaCriticos.isEmpty() && !colaNormales.isEmpty() && colaNormales.peek().equals(idDron)));
                
                if (puedeCargar && bahiasSemaforo.tryAcquire()) {
                    // Asignar recursos
                    bahias--;
                    if (critico) {
                        colaCriticos.poll();
                    } else {
                        colaNormales.poll();
                    }
                    
                    panel.setBahiasDisponibles(bahias);
                    panelGrafo.agregarNodo("Semáforos", "Bahía asignada a Dron" + idDron);
                    mutex.release();
                    break;
                }
                
                mutex.release();
                Thread.sleep(100); // Esperar antes de reintentar
            }
        }
        
        public void liberarBahia(int idDron) throws InterruptedException {
            bahiasSemaforo.release();
            mutex.acquire();
            bahias++;
            panel.setBahiasDisponibles(bahias);
            panelGrafo.agregarNodo("Semáforos", "Bahía liberada por Dron" + idDron);
            mutex.release();
        }
        
        public void actualizarEnergia(int nuevaEnergia) throws InterruptedException {
            mutex.acquire();
            energiaSolar = nuevaEnergia;
            modoActivo = energiaSolar > 0;
            panel.setEnergiaSolar(energiaSolar);
            panel.setModoActivo(modoActivo);
            mutex.release();
        }
    }
    
    private static class EstacionSolarCondicion {
        private final ReentrantLock lock;
        private final Condition bahiaDisponible;
        private final Condition energiaDisponible;
        private int bahias;
        private int energiaSolar;
        private boolean modoActivo;
        private final Queue<Integer> colaCriticos;
        private final Queue<Integer> colaNormales;
        private final EstacionSolarPanelParalelo panel;
        private final PanelGrafoDinamico panelGrafo;
        
        public EstacionSolarCondicion(int numBahias, EstacionSolarPanelParalelo panel, PanelGrafoDinamico panelGrafo) {
            this.lock = new ReentrantLock();
            this.bahiaDisponible = lock.newCondition();
            this.energiaDisponible = lock.newCondition();
            this.bahias = numBahias;
            this.energiaSolar = 100;
            this.modoActivo = true;
            this.colaCriticos = new LinkedList<>();
            this.colaNormales = new LinkedList<>();
            this.panel = panel;
            this.panelGrafo = panelGrafo;
        }
        
        public void solicitarRecarga(int idDron, boolean critico) throws InterruptedException {
            lock.lock();
            try {
                panelGrafo.agregarNodo("Variables", "Dron" + idDron + " solicita recarga (critico:" + critico + ")");
                
                // Agregar a la cola correspondiente
                if (critico) {
                    colaCriticos.offer(idDron);
                } else {
                    colaNormales.offer(idDron);
                }
                
                // Esperar hasta que se pueda cargar
                while (true) {
                    boolean puedeCargar = modoActivo && bahias > 0 &&
                        ((critico && !colaCriticos.isEmpty() && colaCriticos.peek().equals(idDron)) ||
                         (!critico && colaCriticos.isEmpty() && !colaNormales.isEmpty() && colaNormales.peek().equals(idDron)));
                    
                    if (puedeCargar) {
                        // Asignar recursos
                        bahias--;
                        if (critico) {
                            colaCriticos.poll();
                        } else {
                            colaNormales.poll();
                        }
                        
                        panel.setBahiasDisponibles(bahias);
                        panelGrafo.agregarNodo("Variables", "Bahía asignada a Dron" + idDron);
                        break;
                    }
                    
                    bahiaDisponible.await();
                }
            } finally {
                lock.unlock();
            }
        }
        
        public void liberarBahia(int idDron) throws InterruptedException {
            lock.lock();
            try {
                bahias++;
                panel.setBahiasDisponibles(bahias);
                panelGrafo.agregarNodo("Variables", "Bahía liberada por Dron" + idDron);
                bahiaDisponible.signal();
            } finally {
                lock.unlock();
            }
        }
        
        public void actualizarEnergia(int nuevaEnergia) throws InterruptedException {
            lock.lock();
            try {
                energiaSolar = nuevaEnergia;
                modoActivo = energiaSolar > 0;
                panel.setEnergiaSolar(energiaSolar);
                panel.setModoActivo(modoActivo);
                
                if (modoActivo) {
                    panelGrafo.agregarNodo("Variables", "Energía actualizada: " + energiaSolar);
                    energiaDisponible.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }
    }
    
    // Métodos setter para sincronización
    public synchronized void setBahiasDisponibles(int bahias) {
        this.bahiasDisponibles = bahias;
    }
    
    public synchronized void setEnergiaSolar(int energia) {
        this.energiaSolar = energia;
    }
    
    public synchronized void setModoActivo(boolean activo) {
        this.modoActivo = activo;
    }
    
    private void ejecutarAlgoritmoConMutex(String nombreAlgoritmo) {
        System.out.println("Iniciando algoritmo: " + nombreAlgoritmo);
        panelGrafo.agregarNodo("PARALELO", "Algoritmo " + nombreAlgoritmo + " iniciado");
        
        int recargasCompletadas = 0;
        
        while (simulacionActiva && recargasCompletadas < 15) {
            try {
                int idDron = random.nextInt(NUM_DRONES);
                boolean critico = actualizarEstadoDron(idDron);
                
                if (critico || random.nextDouble() < 0.4) {
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "SOLICITUD", idDron, critico ? 1 : 0));
                    
                    // Usar mutex para sincronización
                    estacionMutex.solicitarRecarga(idDron, critico);
                    
                    // Simular recarga
                    dronEstado[idDron] = true;
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "RECARGANDO", idDron, 0));
                    
                    Thread.sleep(random.nextInt(1000) + 500);
                    
                    // Completar recarga
                    dronBateria[idDron] = 100;
                    dronCritico[idDron] = false;
                    estacionMutex.liberarBahia(idDron);
                    
                    recargasCompletadas++;
                    totalRecargas.incrementAndGet();
                    if (critico) recargasCriticas.incrementAndGet();
                    else recargasNormales.incrementAndGet();
                    
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "COMPLETADO", idDron, 0));
                    dronEstado[idDron] = false;
                }
                
                Thread.sleep(random.nextInt(1000) + 500);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "FINALIZADO", 0, recargasCompletadas));
        System.out.println(nombreAlgoritmo + " finalizado: " + recargasCompletadas + " recargas");
    }
    
    private void ejecutarAlgoritmoConBarrera(String nombreAlgoritmo) {
        System.out.println("Iniciando algoritmo: " + nombreAlgoritmo);
        panelGrafo.agregarNodo("PARALELO", "Algoritmo " + nombreAlgoritmo + " iniciado");
        
        int recargasCompletadas = 0;
        
        while (simulacionActiva && recargasCompletadas < 15) {
            try {
                int idDron = random.nextInt(NUM_DRONES);
                boolean critico = actualizarEstadoDron(idDron);
                
                if (critico || random.nextDouble() < 0.4) {
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "SOLICITUD", idDron, critico ? 1 : 0));
                    
                    // Usar barrera para sincronización
                    estacionBarrera.solicitarRecarga(idDron, critico);
                    
                    // Simular recarga
                    dronEstado[idDron] = true;
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "RECARGANDO", idDron, 0));
                    
                    Thread.sleep(random.nextInt(1000) + 500);
                    
                    // Completar recarga
                    dronBateria[idDron] = 100;
                    dronCritico[idDron] = false;
                    estacionBarrera.liberarBahia(idDron);
                    
                    recargasCompletadas++;
                    totalRecargas.incrementAndGet();
                    if (critico) recargasCriticas.incrementAndGet();
                    else recargasNormales.incrementAndGet();
                    
                    resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "COMPLETADO", idDron, 0));
                    dronEstado[idDron] = false;
                }
                
                Thread.sleep(random.nextInt(1000) + 500);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        resultadosQueue.offer(new ResultadoAlgoritmo(nombreAlgoritmo, "FINALIZADO", 0, recargasCompletadas));
        System.out.println(nombreAlgoritmo + " finalizado: " + recargasCompletadas + " recargas");
    }
    
    // Clase para sincronización con Mutex (ReentrantLock)
    private static class EstacionSolarMutex {
        private int bahias;
        private int energiaSolar;
        private boolean modoActivo;
        private final Queue<Integer> colaCriticos;
        private final Queue<Integer> colaNormales;
        private final EstacionSolarPanelParalelo panel;
        private final PanelGrafoDinamico panelGrafo;
        private final ReentrantLock lock;
        
        public EstacionSolarMutex(int numBahias, EstacionSolarPanelParalelo panel, PanelGrafoDinamico panelGrafo) {
            this.bahias = numBahias;
            this.energiaSolar = 100;
            this.modoActivo = true;
            this.colaCriticos = new LinkedList<>();
            this.colaNormales = new LinkedList<>();
            this.panel = panel;
            this.panelGrafo = panelGrafo;
            this.lock = new ReentrantLock(true); // Fair lock
        }
        
        public void solicitarRecarga(int idDron, boolean critico) throws InterruptedException {
            panelGrafo.agregarNodo("Mutex", "Dron" + idDron + " solicita recarga (critico:" + critico + ")");
            
            lock.lock();
            try {
                // Agregar a la cola correspondiente
                if (critico) {
                    colaCriticos.offer(idDron);
                } else {
                    colaNormales.offer(idDron);
                }
                
                // Esperar hasta que se pueda cargar
                while (true) {
                    boolean puedeCargar = modoActivo && bahias > 0 &&
                        ((critico && !colaCriticos.isEmpty() && colaCriticos.peek().equals(idDron)) ||
                         (!critico && colaCriticos.isEmpty() && !colaNormales.isEmpty() && colaNormales.peek().equals(idDron)));
                    
                    if (puedeCargar) {
                        // Asignar bahía
                        bahias--;
                        panel.setBahiasDisponibles(bahias);
                        
                        // Remover de la cola
                        if (critico) {
                            colaCriticos.poll();
                        } else {
                            colaNormales.poll();
                        }
                        
                        panelGrafo.agregarNodo("Mutex", "Dron" + idDron + " asignado a bahía");
                        break;
                    }
                    
                    // Esperar un poco antes de reintentar
                    Thread.sleep(100);
                }
            } finally {
                lock.unlock();
            }
        }
        
        public void liberarBahia(int idDron) {
            lock.lock();
            try {
                bahias++;
                panel.setBahiasDisponibles(bahias);
                panelGrafo.agregarNodo("Mutex", "Dron" + idDron + " liberó bahía");
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
                    panelGrafo.agregarNodo("Mutex", "Energía actualizada: " + energiaSolar);
                }
            } finally {
                lock.unlock();
            }
        }
    }
    
    // Clase para sincronización con Barrera (CyclicBarrier)
    private static class EstacionSolarBarrera {
        private int bahias;
        private int energiaSolar;
        private boolean modoActivo;
        private final EstacionSolarPanelParalelo panel;
        private final PanelGrafoDinamico panelGrafo;
        private final CyclicBarrier barreraRecarga;
        private final Queue<Integer> dronesEsperando;
        private final ReentrantLock lock;
        
        public EstacionSolarBarrera(int numBahias, EstacionSolarPanelParalelo panel, PanelGrafoDinamico panelGrafo) {
            this.bahias = numBahias;
            this.energiaSolar = 100;
            this.modoActivo = true;
            this.panel = panel;
            this.panelGrafo = panelGrafo;
            this.barreraRecarga = new CyclicBarrier(3, () -> {
                // Acción cuando se alcanza la barrera
                panelGrafo.agregarNodo("Barrera", "Grupo formado - procesando recargas");
            });
            this.dronesEsperando = new LinkedList<>();
            this.lock = new ReentrantLock();
        }
        
        public void solicitarRecarga(int idDron, boolean critico) throws InterruptedException {
            panelGrafo.agregarNodo("Barrera", "Dron" + idDron + " solicita recarga (critico:" + critico + ")");
            
            lock.lock();
            try {
                dronesEsperando.offer(idDron);
                
                // Esperar a que haya suficientes drones para formar un grupo
                while (dronesEsperando.size() < 3 || bahias < 3) {
                    if (!modoActivo) {
                        Thread.sleep(200);
                        continue;
                    }
                    Thread.sleep(100);
                }
                
                // Intentar pasar la barrera
                try {
                    panelGrafo.agregarNodo("Barrera", "Dron" + idDron + " esperando en barrera");
                    barreraRecarga.await();
                    
                    // Asignar bahías al grupo
                    if (bahias >= 3) {
                        bahias -= 3;
                        panel.setBahiasDisponibles(bahias);
                        
                        // Remover drones del grupo
                        for (int i = 0; i < 3 && !dronesEsperando.isEmpty(); i++) {
                            dronesEsperando.poll();
                        }
                        
                        panelGrafo.agregarNodo("Barrera", "Dron" + idDron + " asignado a bahía (grupo)");
                    }
                } catch (BrokenBarrierException e) {
                    panelGrafo.agregarNodo("Barrera", "Barrera rota - reintentando");
                    Thread.sleep(500);
                }
            } finally {
                lock.unlock();
            }
        }
        
        public void liberarBahia(int idDron) {
            lock.lock();
            try {
                bahias++;
                panel.setBahiasDisponibles(bahias);
                panelGrafo.agregarNodo("Barrera", "Dron" + idDron + " liberó bahía");
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
                    panelGrafo.agregarNodo("Barrera", "Energía actualizada: " + energiaSolar);
                }
            } finally {
                lock.unlock();
            }
        }
    }
}