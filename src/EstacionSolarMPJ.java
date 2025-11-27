import mpi.*;
import javax.swing.*;
import java.util.Random;
import java.awt.*;

public class EstacionSolarMPJ {

    // Tags para mensajes
    static final int TAG_DATA = 1;

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
                default: algoritmo = "General"; break;
            }
            System.out.println("Nucleo " + me + ": Probando algoritmo " + algoritmo);
            correrTrabajador(me, algoritmo);
        }
    }

    private void correrMaestro() {
        double[] buffer = new double[2]; // [Rank, Rendimiento]
        int[] niveles = new int[]{0,0,0,0,0};
        while (true) {
            try {
                MPI.COMM_WORLD.Recv(buffer, 0, 2, MPI.DOUBLE, MPI.ANY_SOURCE, TAG_DATA);

                int rankOrigen = (int) buffer[0];
                double tiempo = buffer[1]; // Tiempo por recarga (ms)

                String nombreAlgoritmo = getNombreAlgoritmo(rankOrigen);
                final String serie = getSeriePorRank(rankOrigen);
                int idx = Math.max(0, Math.min(4, rankOrigen - 1));
                int nivel = ++niveles[idx];

                SwingUtilities.invokeLater(() -> {
                    // X = Nivel Construido, Y = Tiempo (ms)
                    graficasPanel.addPoint(serie, nivel, tiempo);
                });
                panelGrafo.setFlechaSolicitud("Nucleo" + rankOrigen, "Estacion");
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
                    estacion.solicitarRecarga(i, critico);
                    Thread.sleep(rand.nextInt(10) + 1);
                    estacion.liberarBahia();
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
                    grafo.inicializarGrafo("EstacionSolar");

                    JPanel top = new JPanel(new GridLayout(1, 2));
                    top.add(graficas);
                    top.add(grafo);

                    EstacionSolarPanel dronesPanel = new EstacionSolarPanel("Monitores", grafo);
                    dronesPanel.setPreferredSize(new Dimension(1000, 300));

                    frame.setLayout(new BorderLayout());
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
