import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Import MPJ libraries
import mpi.*;

public class EstacionSolarMPJ extends JPanel implements Simulable {
    private static final int NUM_BAHIAS = 3;
    private static final int NUM_DRONES = 8;
    private static final int ENERGIA_INICIAL = 100;
    private static final int ENERGIA_MINIMA = 20;
    private static final int ANCHO_PANEL = 400;
    private static final int ALTO_PANEL = 300;
    
    // Variables compartidas entre procesos
    private volatile int bahiasDisponibles = NUM_BAHIAS;
    private volatile int energiaSolar = ENERGIA_INICIAL;
    private volatile boolean modoActivo = true;
    private volatile boolean[] dronEstado; // false = volando, true = cargando
    private volatile boolean[] dronCritico;
    private volatile int[] dronPosX, dronPosY;
    private volatile int[] dronBateria;
    
    // Control de simulación
    private final PanelGrafoDinamico panelGrafo;
    private final java.util.Random random = new java.util.Random();
    private volatile boolean simulacionActiva = true;
    
    // Estadísticas globales
    private final AtomicInteger totalRecargas = new AtomicInteger(0);
    private final AtomicInteger recargasCriticas = new AtomicInteger(0);
    private final AtomicInteger recargasNormales = new AtomicInteger(0);
    private final AtomicLong tiempoTotalMonitores = new AtomicLong(0);
    private final AtomicLong tiempoTotalSemaforos = new AtomicLong(0);
    private final AtomicLong tiempoTotalVariables = new AtomicLong(0);
    
    // Colores para visualización
    private static final Color COLOR_DRON_NORMAL = new Color(100, 150, 255);
    private static final Color COLOR_DRON_CRITICO = new Color(255, 100, 100);
    private static final Color COLOR_DRON_CARGANDO = new Color(255, 200, 100);
    private static final Color COLOR_ESTACION = new Color(50, 200, 50);
    private static final Color COLOR_BAHIA_OCUPADA = new Color(255, 100, 100);
    private static final Color COLOR_BAHIA_LIBRE = new Color(200, 255, 200);
    
    // Constantes para identificación de algoritmos
    private static final int ALGORITMO_MONITORES = 0;
    private static final int ALGORITMO_SEMAFOROS = 1;
    private static final int ALGORITMO_VARIABLES = 2;
    private static final int TAG_RESULTADO = 100;
    private static final int TAG_CONTROL = 200;
    
    public EstacionSolarMPJ(PanelGrafoDinamico panelGrafo) {
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
        
        // Inicializar MPI y comenzar simulación
        inicializarMPI();
    }
    
    private void inicializarMPI() {
        try {
            int rank = MPI.COMM_WORLD.Rank();
            int size = MPI.COMM_WORLD.Size();
            
            if (rank == 0) {
                // Proceso maestro - GUI y coordinación
                panelGrafo.agregarNodo("MPJ", "Maestro iniciado (Rank 0)");
                ejecutarMaestro();
            } else if (rank <= 3) {
                // Procesos trabajadores - ejecutan algoritmos
                panelGrafo.agregarNodo("MPJ", "Trabajador iniciado (Rank " + rank + ")");
                ejecutarTrabajador(rank);
            } else {
                // Procesos adicionales - no utilizados
                if (rank >= 5) {
                    // Procesos no utilizados
                    // System.out.println("Rank " + rank + " - Proceso no utilizado");
                    return;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error en MPI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void ejecutarMaestro() {
        // Hilo para recibir resultados de los trabajadores
        Thread receptorThread = new Thread(this::recibirResultadosTrabajadores, "Receptor-Maestro");
        receptorThread.start();
        
        // Hilo para administrar energía
        Thread adminThread = new Thread(this::administradorEnergetico, "Administrador-Energía");
        adminThread.start();
        
        // Hilo para actualizar visualización
        Thread visualThread = new Thread(this::actualizarVisualizacion, "Actualizador-Visual");
        visualThread.start();
        
        try {
            receptorThread.join();
            adminThread.join();
            visualThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void ejecutarTrabajador(int rank) {
        int algoritmo = rank - 1; // Rank 1 = Monitores, Rank 2 = Semáforos, Rank 3 = Variables
        String nombreAlgoritmo = obtenerNombreAlgoritmo(algoritmo);
        
        System.out.println("Trabajador " + rank + " ejecutando: " + nombreAlgoritmo);
        panelGrafo.agregarNodo("MPJ", "Algoritmo " + nombreAlgoritmo + " iniciado (Rank " + rank + ")");
        
        int recargasCompletadas = 0;
        long tiempoInicio = System.currentTimeMillis();
        
        while (simulacionActiva && recargasCompletadas < 20) {
            try {
                int idDron = random.nextInt(NUM_DRONES);
                boolean critico = actualizarEstadoDron(idDron);
                
                if (critico || random.nextDouble() < 0.4) { // 40% de probabilidad de solicitar recarga
                    // Enviar solicitud al maestro
                    enviarResultado(algoritmo, "SOLICITUD", idDron, critico ? 1 : 0);
                    
                    // Simular tiempo de espera según algoritmo
                    long tiempoEspera = simularTiempoEspera(algoritmo, critico);
                    Thread.sleep(tiempoEspera);
                    
                    // Simular recarga
                    dronEstado[idDron] = true;
                    enviarResultado(algoritmo, "RECARGANDO", idDron, (int)tiempoEspera);
                    
                    Thread.sleep(random.nextInt(1000) + 500);
                    
                    // Completar recarga
                    dronBateria[idDron] = 100;
                    dronCritico[idDron] = false;
                    
                    recargasCompletadas++;
                    totalRecargas.incrementAndGet();
                    if (critico) recargasCriticas.incrementAndGet();
                    else recargasNormales.incrementAndGet();
                    
                    enviarResultado(algoritmo, "COMPLETADO", idDron, (int)tiempoEspera);
                    dronEstado[idDron] = false;
                }
                
                Thread.sleep(random.nextInt(1000) + 500);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
        actualizarTiempoAlgoritmo(algoritmo, tiempoTotal);
        
        enviarResultado(algoritmo, "FINALIZADO", recargasCompletadas, (int)tiempoTotal);
        System.out.println(nombreAlgoritmo + " finalizado: " + recargasCompletadas + " recargas en " + tiempoTotal + "ms");
    }
    
    private void recibirResultadosTrabajadores() {
        try {
            while (simulacionActiva) {
                // Recibir mensajes de los trabajadores
                Status status = MPI.COMM_WORLD.Probe(MPI.ANY_SOURCE, TAG_RESULTADO);
                if (status != null) {
                    int source = status.source;
                    
                    // Recibir datos
                    int[] datos = new int[4]; // [algoritmo, tipoEvento, idDron/valor, valorExtra]
                    MPI.COMM_WORLD.Recv(datos, 0, datos.length, MPI.INT, source, TAG_RESULTADO);
                    
                    procesarResultadoRecibido(datos);
                }
                
                Thread.sleep(10); // Pequeña pausa
            }
        } catch (Exception e) {
            System.err.println("Error recibiendo resultados: " + e.getMessage());
        }
    }
    
    private void procesarResultadoRecibido(int[] datos) {
        int algoritmo = datos[0];
        int tipoEvento = datos[1];
        int idDron = datos[2];
        int valor = datos[3];
        
        String nombreAlgoritmo = obtenerNombreAlgoritmo(algoritmo);
        String evento = obtenerTipoEvento(tipoEvento);
        
        // Actualizar gráfico
        String etiqueta = nombreAlgoritmo + "_" + evento;
        String detalles = "Dron" + idDron + " - " + evento + " (" + valor + "ms)";
        
        panelGrafo.agregarNodo(etiqueta, detalles);
        
        // Actualizar estadísticas
        if ("COMPLETADO".equals(evento)) {
            repaint();
        }
    }
    
    private void enviarResultado(int algoritmo, String tipoEvento, int idDron, int valor) {
        try {
            int tipoEventoNum = obtenerNumeroEvento(tipoEvento);
            int[] datos = {algoritmo, tipoEventoNum, idDron, valor};
            MPI.COMM_WORLD.Send(datos, 0, datos.length, MPI.INT, 0, TAG_RESULTADO);
        } catch (Exception e) {
            System.err.println("Error enviando resultado: " + e.getMessage());
        }
    }
    
    private boolean actualizarEstadoDron(int idDron) {
        // Reducir batería
        dronBateria[idDron] -= random.nextInt(15) + 5;
        if (dronBateria[idDron] < 0) dronBateria[idDron] = 0;
        
        // Actualizar estado crítico
        dronCritico[idDron] = dronBateria[idDron] < 30;
        
        return dronCritico[idDron];
    }
    
    private long simularTiempoEspera(int algoritmo, boolean critico) {
        // Simular tiempos de espera realistas para cada algoritmo
        long tiempoBase = 100 + random.nextInt(200);
        
        switch (algoritmo) {
            case ALGORITMO_MONITORES:
                tiempoBase = 150 + random.nextInt(250);
                break;
            case ALGORITMO_SEMAFOROS:
                tiempoBase = 100 + random.nextInt(200);
                break;
            case ALGORITMO_VARIABLES:
                tiempoBase = 200 + random.nextInt(300);
                break;
        }
        
        if (critico) {
            tiempoBase = tiempoBase / 2; // Drones críticos tienen prioridad
        }
        
        return tiempoBase;
    }
    
    private void actualizarTiempoAlgoritmo(int algoritmo, long tiempoTotal) {
        switch (algoritmo) {
            case ALGORITMO_MONITORES:
                tiempoTotalMonitores.addAndGet(tiempoTotal);
                break;
            case ALGORITMO_SEMAFOROS:
                tiempoTotalSemaforos.addAndGet(tiempoTotal);
                break;
            case ALGORITMO_VARIABLES:
                tiempoTotalVariables.addAndGet(tiempoTotal);
                break;
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
    
    private void actualizarVisualizacion() {
        while (simulacionActiva) {
            try {
                // Actualizar posiciones de drones
                for (int i = 0; i < NUM_DRONES; i++) {
                    if (!dronEstado[i]) { // Solo mover si están volando
                        dronPosX[i] += random.nextInt(20) - 10;
                        dronPosY[i] += random.nextInt(20) - 10;
                        
                        // Mantener dentro de límites
                        dronPosX[i] = Math.max(10, Math.min(ANCHO_PANEL - 30, dronPosX[i]));
                        dronPosY[i] = Math.max(10, Math.min(ALTO_PANEL - 30, dronPosY[i]));
                    }
                }
                
                repaint();
                Thread.sleep(200); // Actualizar 5 veces por segundo
                
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
        g.drawString("Estación Solar (MPJ)", centroX - 50, centroY - 25);
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
        
        // Dibujar estadísticas de MPJ
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.drawString("Estadísticas MPJ:", 10, 20);
        g.drawString("Total recargas: " + totalRecargas.get(), 10, 35);
        g.drawString("Críticas: " + recargasCriticas.get(), 10, 50);
        g.drawString("Normales: " + recargasNormales.get(), 10, 65);
        
        // Tiempos de ejecución
        g.drawString("Tiempos (ms):", 10, 85);
        g.drawString("Monitores: " + tiempoTotalMonitores.get(), 10, 100);
        g.drawString("Semáforos: " + tiempoTotalSemaforos.get(), 10, 115);
        g.drawString("Variables: " + tiempoTotalVariables.get(), 10, 130);
        
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
        panelGrafo.agregarNodo("MPJ", "Simulación MPJ finalizada");
        System.out.println("Simulación MPJ detenida");
        System.out.println("Total recargas: " + totalRecargas.get());
        System.out.println("Críticas: " + recargasCriticas.get() + ", Normales: " + recargasNormales.get());
        System.out.println("Tiempos - Monitores: " + tiempoTotalMonitores.get() + "ms, " +
                          "Semáforos: " + tiempoTotalSemaforos.get() + "ms, " +
                          "Variables: " + tiempoTotalVariables.get() + "ms");
    }
    
    // Métodos auxiliares
    private String obtenerNombreAlgoritmo(int algoritmo) {
        switch (algoritmo) {
            case ALGORITMO_MONITORES:
                return "Monitores";
            case ALGORITMO_SEMAFOROS:
                return "Semáforos";
            case ALGORITMO_VARIABLES:
                return "Variables de Condición";
            default:
                return "Desconocido";
        }
    }
    
    private String obtenerTipoEvento(int tipoEvento) {
        switch (tipoEvento) {
            case 0: return "SOLICITUD";
            case 1: return "RECARGANDO";
            case 2: return "COMPLETADO";
            case 3: return "FINALIZADO";
            default: return "DESCONOCIDO";
        }
    }
    
    private int obtenerNumeroEvento(String tipoEvento) {
        switch (tipoEvento) {
            case "SOLICITUD": return 0;
            case "RECARGANDO": return 1;
            case "COMPLETADO": return 2;
            case "FINALIZADO": return 3;
            default: return -1;
        }
    }
    
    // Método main para ejecutar con MPJ
    public static void main(String[] args) {
        try {
            // Inicializar MPI
            MPI.Init(args);
            
            int rank = MPI.COMM_WORLD.Rank();
            
            if (rank == 0) {
                // Solo el proceso 0 crea la GUI
                SwingUtilities.invokeLater(() -> {
                    try {
                        JFrame frame = new JFrame("Estación Solar - Ejecución Paralela con MPJ");
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        frame.setSize(800, 600);
                        
                        PanelGrafoDinamico panelGrafo = new PanelGrafoDinamico();
                        EstacionSolarMPJ panelMPJ = new EstacionSolarMPJ(panelGrafo);
                        
                        frame.setLayout(new BorderLayout());
                        frame.add(panelMPJ, BorderLayout.CENTER);
                        frame.add(panelGrafo, BorderLayout.EAST);
                        
                        frame.setVisible(true);
                        
                        // Agregar listener para cerrar adecuadamente
                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(java.awt.event.WindowEvent e) {
                                panelMPJ.detener();
                                MPI.Finalize();
                                System.exit(0);
                            }
                        });
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                // Los otros procesos esperan a que el proceso 0 inicie la simulación
                System.out.println("Proceso " + rank + " listo para ejecutar");
                
                // Mantener el proceso activo hasta que se finalice MPI
                while (true) {
                    Thread.sleep(1000);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error en ejecución MPJ: " + e.getMessage());
            e.printStackTrace();
        }
    }
}