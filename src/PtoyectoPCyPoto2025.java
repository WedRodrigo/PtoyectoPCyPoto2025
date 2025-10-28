import javax.swing.*;
import java.awt.*;

public class PtoyectoPCyPoto2025 extends JFrame {
    private JMenu menuArchivo;
    private JMenuBar barraMenu;
    private JMenuItem itemAbrir;
    private JMenuItem itemGuardar;
    private JMenuItem itemNuevo;
    private JMenuItem itemCerrar;
    private JMenu menuSincronizacion;
    private JMenuItem itemMutex;
    private JMenuItem itemSemaforo;
    private JMenuItem itemVariableCondicion;
    private JMenuItem itemMonitores;
    private JMenuItem itemBarreras;
    private JMenu menuProblemas;
    private JMenuItem itemProductorConsumidor;
    private JMenuItem itemCenaFilosofos;
    private JMenuItem itemBarberoDormilon;
    private JMenuItem itemFumadores;
    private JMenuItem itemLectoresEscritores;

    private TanquePanel tanquePanel;
    private PanelGrafoDinamico panelGrafo; // Modificado: PanelDibujo -> PanelGrafoDinamico
    private CenaFilosofosPanel cenaFilosofosPanel;
    private BarberoDormilonPanel barberoDormilonPanel;
    private EscritorLectorPanel escritorLectorPanel;
    private FumadoresPanel fumadoresPanel;

    private String tipoSincronizacion = "Monitores";
    private JPanel panelSimulacionActivo = null; // Panel para la simulación

    PtoyectoPCyPoto2025() {
        setSize(1500, 800);
        setTitle("Proyecto Programación Concurrente y Paralela Otoño 2025");
        getContentPane().setBackground(Color.BLACK);
    
        setLayout(new GridLayout(1, 2));
    
        // 1. Inicializar el panel de grafo
        panelGrafo = new PanelGrafoDinamico();
        
        // 2. Inicializar un panel vacío para la simulación
        panelSimulacionActivo = new JPanel();
        panelSimulacionActivo.setBackground(new Color(30, 30, 30));
        panelSimulacionActivo.add(new JLabel("Seleccione un problema del menú"));
        
        add(panelSimulacionActivo); // Panel izquierdo (simulación)
        add(panelGrafo);           // Panel derecho (grafo)
        
        // ... (Configuración idéntica de la barra de menú) ...
        barraMenu = new JMenuBar();
        menuArchivo = new JMenu("Archivo");
        itemNuevo = new JMenuItem("Nuevo");
        itemAbrir = new JMenuItem("Abrir");
        itemGuardar = new JMenuItem("Guardar");
        itemCerrar = new JMenuItem("Cerrar");
        menuSincronizacion = new JMenu("Sincronización");
        itemMutex = new JMenuItem("Mutex");
        itemSemaforo = new JMenuItem("Semáforos");
        itemVariableCondicion = new JMenuItem("Variable de Condición");
        itemMonitores = new JMenuItem("Monitores");
        itemBarreras = new JMenuItem("Barreras");
        menuProblemas = new JMenu("Problemas");
        itemProductorConsumidor = new JMenuItem("Productor - Consumidor");
        itemCenaFilosofos = new JMenuItem("Cena de los Filósofos");
        itemBarberoDormilon = new JMenuItem("Barbero Dormilón");
        itemFumadores = new JMenuItem("Fumadores");
        itemLectoresEscritores = new JMenuItem("Lectores - Escritores");

        barraMenu.add(menuArchivo);
        barraMenu.add(menuSincronizacion);
        barraMenu.add(menuProblemas);
        menuArchivo.add(itemNuevo);
        menuArchivo.add(itemAbrir);
        menuArchivo.add(itemGuardar);
        menuArchivo.add(itemCerrar);
        menuSincronizacion.add(itemMutex);
        menuSincronizacion.add(itemSemaforo);
        menuSincronizacion.add(itemVariableCondicion);
        menuSincronizacion.add(itemMonitores);
        menuSincronizacion.add(itemBarreras);
        menuProblemas.add(itemProductorConsumidor);
        menuProblemas.add(itemCenaFilosofos);
        menuProblemas.add(itemBarberoDormilon);
        menuProblemas.add(itemFumadores);
        menuProblemas.add(itemLectoresEscritores);

        // Agregar acción a los menú de Sincronización
        itemMutex.addActionListener(e -> setSincronizacion("Mutex"));
        itemSemaforo.addActionListener(e -> setSincronizacion("Semáforo"));
        itemVariableCondicion.addActionListener(e -> setSincronizacion("Variable de Condición"));
        itemMonitores.addActionListener(e -> setSincronizacion("Monitores")); 

        // Agregar acción a los problemas
        itemCenaFilosofos.addActionListener(e -> mostrarCenaFilosofos());
        itemBarberoDormilon.addActionListener(e -> mostrarBarberoDormilon());
        itemFumadores.addActionListener(e -> mostrarFumadores());
        itemLectoresEscritores.addActionListener(e -> mostrarEscritorLector());
        itemProductorConsumidor.addActionListener(e -> mostrarProductorConsumidor());

        setJMenuBar(barraMenu);
    }

    private void setSincronizacion(String tipo) {
        this.tipoSincronizacion = tipo;
        JOptionPane.showMessageDialog(this, "Sincronización cambiada a: " + tipo);
        
        // Reiniciar la simulación actual con la nueva sincronización
        if (panelSimulacionActivo instanceof CenaFilosofosPanel) {
            mostrarCenaFilosofos();
        } else if (panelSimulacionActivo instanceof BarberoDormilonPanel) {
            mostrarBarberoDormilon();
        } else if (panelSimulacionActivo instanceof EscritorLectorPanel) {
            mostrarEscritorLector();
        } else if (panelSimulacionActivo instanceof FumadoresPanel) {
            mostrarFumadores();
        } else if (panelSimulacionActivo instanceof TanquePanel) {
            mostrarProductorConsumidor();
        }
    }

    // Métodos "mostrar" actualizados para inyectar el panelGrafo
    
    private void mostrarCenaFilosofos() {
        getContentPane().removeAll();
        panelGrafo.inicializarGrafo("CenaFilosofos"); // Prepara el grafo
        cenaFilosofosPanel = new CenaFilosofosPanel(tipoSincronizacion, panelGrafo);
        panelSimulacionActivo = cenaFilosofosPanel;
        add(panelSimulacionActivo);
        add(panelGrafo);
        revalidate();
        repaint();
    }
    
    private void mostrarBarberoDormilon() {
        getContentPane().removeAll();
        panelGrafo.inicializarGrafo("BarberoDormilon"); // Prepara el grafo
        barberoDormilonPanel = new BarberoDormilonPanel(tipoSincronizacion, panelGrafo);
        panelSimulacionActivo = barberoDormilonPanel;
        add(panelSimulacionActivo);
        add(panelGrafo);
        revalidate();
        repaint();
    }
    
    private void mostrarEscritorLector() {
        getContentPane().removeAll();
        panelGrafo.inicializarGrafo("EscritorLector"); // Prepara el grafo
        escritorLectorPanel = new EscritorLectorPanel(tipoSincronizacion, panelGrafo);
        panelSimulacionActivo = escritorLectorPanel;
        add(panelSimulacionActivo);
        add(panelGrafo);
        revalidate();
        repaint();
    }

    private void mostrarFumadores() {
        getContentPane().removeAll();
        panelGrafo.inicializarGrafo("Fumadores"); // Prepara el grafo
        fumadoresPanel = new FumadoresPanel(tipoSincronizacion, panelGrafo);
        panelSimulacionActivo = fumadoresPanel;
        add(panelSimulacionActivo);
        add(panelGrafo);
        revalidate();
        repaint();
    }
    
    private void mostrarProductorConsumidor() {
        getContentPane().removeAll();
        panelGrafo.inicializarGrafo("ProductorConsumidor"); // Prepara el grafo
        tanquePanel = new TanquePanel(tipoSincronizacion, panelGrafo);
        panelSimulacionActivo = tanquePanel;
        add(panelSimulacionActivo); 
        add(panelGrafo);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        PtoyectoPCyPoto2025 ventana = new PtoyectoPCyPoto2025();
        ventana.setVisible(true);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}