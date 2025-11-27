import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class GestorEstacion {
    private int bahias; // B
    private int maxBahias;
    private volatile boolean haySol = true; // E > 0

    // Para estadísticas
    private int recargasCompletadas = 0;

    // --- Sincronización Mutex/Condición (Lo que pide el PDF) ---
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition esperarEnergia = lock.newCondition();
    private final Condition esperarBahia = lock.newCondition();
    private final Queue<Integer> colaCriticos = new LinkedList<>();
    private final Queue<Integer> colaNormales = new LinkedList<>();

    // --- Sincronización Semáforos ---
    private final Semaphore semBahias;
    private final Semaphore semMutex = new Semaphore(1);
    private final Semaphore semCriticos = new Semaphore(0);
    private final Semaphore semNormales = new Semaphore(0);

    private final String tipoAlgoritmo;

    public GestorEstacion(int bahias, String tipoAlgoritmo) {
        this.maxBahias = bahias;
        this.bahias = bahias;
        this.tipoAlgoritmo = tipoAlgoritmo;
        this.semBahias = new Semaphore(bahias);
    }

    public void solicitarRecarga(int idDron, boolean critico) throws InterruptedException {
        if (tipoAlgoritmo.equals("Mutex") || tipoAlgoritmo.equals("Monitores")) {
            usarMonitor(idDron, critico);
        } else {
            usarSemaforo(idDron, critico);
        }
    }

    public void liberarBahia() {
        if (tipoAlgoritmo.equals("Mutex") || tipoAlgoritmo.equals("Monitores")) {
            liberarMonitor();
        } else {
            liberarSemaforo();
        }
    }

    // --- Lógica del PDF (Adaptada a Java real) ---
    private void usarMonitor(int id, boolean critico) throws InterruptedException {
        lock.lock();
        try {
            if (critico) colaCriticos.add(id);
            else colaNormales.add(id);

            // while (!modoActivo || bahias == 0 || condiciones de prioridad...)
            while (!haySol || bahias == 0 ||
                   (critico && colaCriticos.peek() != id) ||
                   (!critico && (!colaCriticos.isEmpty() || colaNormales.peek() != id))) 
            {
                esperarBahia.await();
            }

            bahias--;
            if (critico) colaCriticos.poll();
            else colaNormales.poll();
        } finally {
            lock.unlock();
        }
    }

    private void liberarMonitor() {
        lock.lock();
        try {
            bahias++;
            recargasCompletadas++;
            esperarBahia.signalAll(); // Avisar a todos para que reevalúen condiciones
        } finally {
            lock.unlock();
        }
    }

    // Lógica Alternativa con Semáforos (para comparar rendimiento)
    private void usarSemaforo(int id, boolean critico) throws InterruptedException {
        // Implementación simplificada para benchmarking
        semBahias.acquire();
        semMutex.acquire();
        // Sección crítica simulada
        semMutex.release();
    }

    private void liberarSemaforo() {
        semBahias.release();
        recargasCompletadas++;
    }

    // Simulación del Sol (Administrador Energético)
    public void setEnergiaSolar(boolean activo) {
        lock.lock();
        try {
            this.haySol = activo;
            if (haySol) esperarEnergia.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public int getRecargas() { return recargasCompletadas; }
}
