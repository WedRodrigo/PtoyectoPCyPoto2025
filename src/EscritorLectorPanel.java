import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class EscritorLectorPanel extends JPanel implements Simulable {
    private final JTextArea textArea;
    private final String tipoSincronizacion;
    private final PanelGrafoDinamico panelGrafo;
    private final List<Thread> hilos = new ArrayList<>();

    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore db = new Semaphore(1);
    private int readCount = 0;

    public EscritorLectorPanel(String tipoSincronizacion, PanelGrafoDinamico panelGrafo) {
        this.tipoSincronizacion = tipoSincronizacion;
        this.panelGrafo = panelGrafo;
        setLayout(new BorderLayout());
        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        iniciarSimulacion();
    }

    private void iniciarSimulacion() {
        for (int i = 0; i < 3; i++) {
            Thread escritor = new Thread(new Escritor(i));
            escritor.start();
            hilos.add(escritor);
        }
        for (int i = 0; i < 5; i++) {
            Thread lector = new Thread(new Lector(i));
            lector.start();
            hilos.add(lector);
        }
    }

    @Override
    public void detener() {
        for (Thread hilo : hilos) {
            hilo.interrupt();
        }
        hilos.clear();
    }

    private void escribir(String texto) {
        SwingUtilities.invokeLater(() -> textArea.append(texto + "\n"));
    }

    class Escritor implements Runnable {
        private final int id;

        public Escritor(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    db.acquire();
                    escribir("Escritor " + id + " está escribiendo.");
                    Thread.sleep((long) (Math.random() * 2000));
                    db.release();
                    Thread.sleep((long) (Math.random() * 2000));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class Lector implements Runnable {
        private final int id;

        public Lector(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    mutex.acquire();
                    readCount++;
                    if (readCount == 1) {
                        db.acquire();
                    }
                    mutex.release();

                    escribir("Lector " + id + " está leyendo.");
                    Thread.sleep((long) (Math.random() * 1000));

                    mutex.acquire();
                    readCount--;
                    if (readCount == 0) {
                        db.release();
                    }
                    mutex.release();
                    Thread.sleep((long) (Math.random() * 1000));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}