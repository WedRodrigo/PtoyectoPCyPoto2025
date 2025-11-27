import mpi.*;
import javax.swing.*;
import java.util.Random;
import java.awt.*;

public class EstacionSolarMPJ {

    // Tags para mensajes
    static final int TAG_DATA = 1;
    static final int TAG_EVENT = 2; // eventos de dron: [idDron, accion]

    // Referencias a GUI (Solo usadas por Rank 0)
    private GraficasPanel graficasPanel;
    private PanelGrafoDinamico panelGrafo;

    public EstacionSolarMPJ(GraficasPanel graficas, PanelGrafoDinamico grafo) {
        this.graficasPanel = graficas;
        this.panelGrafo = grafo;
    }

    public void iniciar(String[] args) {
        int me = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (me == 0) {
            System.out.println("Maestro (Rank 0): Escuchando resultados...");
            panelGrafo.inicializarGrafo("EstacionSolar");
            correrMaestro();
        } else {
            String algoritmo = "";
            switch (me) {
                case 1: algoritmo = "Mutex"; break;
                case 2: algoritmo = "Semaforos"; break;
                case 3: algoritmo = "Monitores"; break;
                case 4: algoritmo = "LockCondicion"; break;
                case 5: algoritmo = "Barreras"; break;
                default: algoritmo = "General"; break;
            }
            System.out.println("Nucleo " + me + ": Probando algoritmo " + algoritmo);
            correrTrabajador(me, algoritmo);
        }
    }

    private void correrMaestro() {
        double[] bufferData = new double[2]; // [Rank, Rendimiento]
        int[] niveles = new int[]{0,0,0,0,0};
        while (true) {
            try {
                Status st = MPI.COMM_WORLD.Probe(MPI.ANY_SOURCE, MPI.ANY_TAG);
                if (st == null) continue;
                if (st.tag == TAG_DATA) {
                    MPI.COMM_WORLD.Recv(bufferData, 0, 2, MPI.DOUBLE, st.source, TAG_DATA);
                    int rankOrigen = (int) bufferData[0];
                    double tiempo = bufferData[1];
                    final String serie = getSeriePorRank(rankOrigen);
                    int idx = Math.max(0, Math.min(4, rankOrigen - 1));
                    int nivel = ++niveles[idx];
                    SwingUtilities.invokeLater(() -> graficasPanel.addPoint(serie, nivel, tiempo));
                    panelGrafo.setFlechaSolicitud("Nucleo" + rankOrigen, "Estacion");
                } else if (st.tag == TAG_EVENT) {
                    int[] ev = new int[2]; // [idDron, accion]
                    MPI.COMM_WORLD.Recv(ev, 0, 2, MPI.INT, st.source, TAG_EVENT);
                    int idDron = ev[0];
                    int accion = ev[1]; // 0=solicitud,1=asignacion/completado
                    if (accion == 0) {
                        panelGrafo.setFlechaSolicitud("D" + idDron, "Estacion");
                    } else {
                        panelGrafo.setFlechaAsignacion("D" + idDron, "Estacion");
                    }
                }
            } catch (MPIException e) {
                break;
            }
        }
    }

    private void correrTrabajador(int rank, String algoritmo) {
        int numDrones = 20;
        int bahias = 3;
        GestorEstacion estacion = new GestorEstacion(bahias, algoritmo);
        Random rand = new Random();

        double[] bufferEnvio = new double[2];

        while (true) {
            long inicio = System.nanoTime();

            try {
                for (int i = 0; i < numDrones; i++) {
                    boolean critico = rand.nextBoolean();
                    // Evento: solicitud de dron
                    int[] evSolicitud = new int[]{i, 0};
                    MPI.COMM_WORLD.Send(evSolicitud, 0, 2, MPI.INT, 0, TAG_EVENT);
                    estacion.solicitarRecarga(i, critico);
                    Thread.sleep(rand.nextInt(10) + 1);
                    estacion.liberarBahia();
                    // Evento: asignación/completado
                    int[] evAsignacion = new int[]{i, 1};
                    MPI.COMM_WORLD.Send(evAsignacion, 0, 2, MPI.INT, 0, TAG_EVENT);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long fin = System.nanoTime();
            double tiempoMs = (fin - inicio) / 1_000_000.0;

            bufferEnvio[0] = (double) rank;
            bufferEnvio[1] = tiempoMs;

            try {
                MPI.COMM_WORLD.Send(bufferEnvio, 0, 2, MPI.DOUBLE, 0, TAG_DATA);
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getNombreAlgoritmo(int rank) {
        switch (rank) {
            case 1: return "Algoritmo: Mutex (PDF)";
            case 2: return "Algoritmo: Semáforos";
            case 3: return "Algoritmo: Monitores";
            case 4: return "Algoritmo: Lock/Cond";
            default: return "Nucleo " + rank;
        }
    }

    private String getSeriePorRank(int rank) {
        switch (rank) {
            case 1: return "Núcleo 1 (Mutex)";
            case 2: return "Núcleo 2 (Semáforos)";
            case 3: return "Núcleo 3 (Monitores)";
            case 4: return "Núcleo 4 (Var. Cond)";
            default: return "Núcleo 5 (Barreras)";
        }
    }

    public static void main(String[] args) {
        try {
            MPI.Init(args);
            int rank = MPI.COMM_WORLD.Rank();

            if (rank == 0) {
                SwingUtilities.invokeLater(() -> {
                    JFrame frame = new JFrame("Estación Solar (MPJ Benchmark)");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setSize(1000, 700);

                    GraficasPanel graficas = new GraficasPanel();
                    graficas.resetSeries("Comparativa de Eficiencia (MPI)",
                            "Núcleo 1 (Mutex)",
                            "Núcleo 2 (Semáforos)",
                            "Núcleo 3 (Monitores)",
                            "Núcleo 4 (Var. Cond)",
                            "Núcleo 5 (Barreras)");
                    PanelGrafoDinamico grafo = new PanelGrafoDinamico();
                    // Modo inicial según args: "drones" o "algoritmos" (por defecto)
                    if (args != null && args.length > 0 && "drones".equalsIgnoreCase(args[0])) {
                        grafo.inicializarGrafo("EstacionSolarDrones");
                    } else {
                        grafo.inicializarGrafo("EstacionSolar");
                    }

                    EstacionSolarPanel dronesPanel = new EstacionSolarPanel("Monitores", grafo);
                    dronesPanel.setPreferredSize(new Dimension(1000, 300));

                    JPanel top = new JPanel(new GridLayout(1, 2));
                    top.add(graficas);
                    top.add(grafo);

                    // Controles para alternar representación del grafo
                    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    JButton btnAlgoritmos = new JButton("Algoritmos");
                    JButton btnDrones = new JButton("Drones");
                    controls.add(btnAlgoritmos);
                    controls.add(btnDrones);

                    btnAlgoritmos.addActionListener(e -> grafo.inicializarGrafo("EstacionSolar"));
                    btnDrones.addActionListener(e -> grafo.inicializarGrafo("EstacionSolarDrones"));

                    frame.setLayout(new BorderLayout());
                    frame.add(controls, BorderLayout.NORTH);
                    frame.add(top, BorderLayout.CENTER);
                    frame.add(dronesPanel, BorderLayout.SOUTH);
                    frame.setVisible(true);

                    new Thread(() -> {
                        try {
                            EstacionSolarMPJ estacion = new EstacionSolarMPJ(graficas, grafo);
                            estacion.iniciar(new String[]{});
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame,
                                "Error iniciando MPJ: " + ex.getMessage());
                        }
                    }, "Starter-MPJ").start();

                    frame.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            try { MPI.Finalize(); } catch (Exception ignored) {}
                            System.exit(0);
                        }
                    });
                });
            } else {
                EstacionSolarMPJ estacion = new EstacionSolarMPJ(null, null);
                estacion.iniciar(new String[]{});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
