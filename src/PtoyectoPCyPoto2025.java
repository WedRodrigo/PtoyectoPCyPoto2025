import javax.swing.*;
import java.awt.*;
import java.awt.event.*; // <--- ¡NUEVO! (Necesario para el ActionListener)

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
    private JMenuItem itemEstacionSolar;
    private JMenuItem itemEstacionSolarMPJ;
    private JMenu menuGraficas;
    private JMenuItem itemAcordeon;
    private JMenuItem itemCarrusel;
    private JMenuItem itemScroll;

    private TanquePanel tanquePanel;
    private PanelGrafoDinamico panelGrafo; // Modificado: PanelDibujo -> PanelGrafoDinamico
    private CenaFilosofosPanel cenaFilosofosPanel;
    private BarberoDormilonPanel barberoDormilonPanel;
    private EscritorLectorPanel escritorLectorPanel;
    private FumadoresPanel fumadoresPanel;
    private EstacionSolarPanel estacionSolarPanel;
    private EstacionSolarPanelParalelo estacionSolarPanelParalelo;
    
    // --- INICIO DE CAMBIOS EN VARIABLES ---
    private GraficasPanel graficasPanel;
    private JScrollPane graficasScrollPane;
    private JCheckBox checkAutoScroll; // <--- ¡NUEVO!
    private JPanel panelInferior;      // <--- ¡NUEVO!
    // --- FIN DE CAMBIOS EN VARIABLES ---
    
    private Simulable simulacionActual; 

    private String tipoSincronizacion = "Monitores";
    private JPanel panelSimulacionActivo = null; // Panel para la simulación
    private JPanel panelSuperior;

    PtoyectoPCyPoto2025() {
        setSize(1500, 800);
        setTitle("Proyecto Programación Concurrente y Paralela Otoño 2025");
        getContentPane().setBackground(Color.BLACK);
    
        setLayout(new BorderLayout());
    
        // Panel superior para simulación y grafo
        panelSuperior = new JPanel(new GridLayout(1, 2));

        // 1. Inicializar el panel de grafo
        panelGrafo = new PanelGrafoDinamico();
        
        // 2. Inicializar un panel vacío para la simulación
        panelSimulacionActivo = new JPanel();
        panelSimulacionActivo.setBackground(new Color(30, 30, 30));
        panelSimulacionActivo.add(new JLabel("Seleccione un problema del menú"));
        
        panelSuperior.add(panelSimulacionActivo); // Panel izquierdo (simulación)
        panelSuperior.add(panelGrafo);            // Panel derecho (grafo)

        // 3. Inicializar el panel de gráficas
        graficasPanel = new GraficasPanel();
        graficasScrollPane = new JScrollPane(graficasPanel);
        graficasScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        // --- INICIO DE CÓDIGO NUEVO ---

        // 4. Crear el CheckBox para el auto-scroll
        checkAutoScroll = new JCheckBox("Auto-Scroll Gráfica", false);
        checkAutoScroll.setToolTipText("Marca esta casilla para que la gráfica se desplace automáticamente");
        
        // 5. Agregar el ActionListener al CheckBox
        checkAutoScroll.addActionListener(e -> {
            // Llama al método setAutoScroll de tu panel de gráficas
            graficasPanel.setAutoScroll(checkAutoScroll.isSelected());
        });

        // 6. Crear un panel inferior para agrupar la gráfica y el checkbox
        panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(graficasScrollPane, BorderLayout.CENTER); // La gráfica en el centro
        panelInferior.add(checkAutoScroll, BorderLayout.SOUTH);    // El checkbox abajo
        
        // --- FIN DE CÓDIGO NUEVO ---

        add(panelSuperior, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH); // <--- ¡CAMBIADO! (Antes era solo graficasScrollPane)
        
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
        itemEstacionSolar = new JMenuItem("Estación Solar");
        itemEstacionSolarMPJ = new JMenuItem("Estación Solar (MPJ)");
        menuGraficas = new JMenu("Gráficas");
        
        // Agregar acción a Estación Solar MPJ
        itemEstacionSolarMPJ.addActionListener(e -> mostrarEstacionSolarMPJ());
        itemAcordeon = new JMenuItem("Acordeón");
        itemCarrusel = new JMenuItem("Carrusel");
        itemScroll = new JMenuItem("Scroll");

        barraMenu.add(menuArchivo);
        barraMenu.add(menuSincronizacion);
        barraMenu.add(menuProblemas);
        barraMenu.add(menuGraficas);
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
        menuProblemas.add(itemEstacionSolar);
        menuProblemas.add(itemEstacionSolarMPJ);

        menuGraficas.add(itemAcordeon);
        menuGraficas.add(itemCarrusel);
        menuGraficas.add(itemScroll);

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
        itemEstacionSolar.addActionListener(e -> mostrarEstacionSolar());

        // Agregar acción a las gráficas
        itemAcordeon.addActionListener(e -> mostrarGraficaAcordeon());
        itemCarrusel.addActionListener(e -> mostrarGraficaCarrusel());
        itemScroll.addActionListener(e -> mostrarGraficaScroll());

        setJMenuBar(barraMenu);
        cambiarPanelSimulacion(new JPanel(), null); // Inicializa con un panel vacío
    }

    private void mostrarGraficaAcordeon() {
        graficasPanel.setPreferredSize(new Dimension(graficasPanel.getWidth(), 150));
        graficasScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        graficasPanel.revalidate();
    }

    private void mostrarGraficaCarrusel() {
        // --- CAMBIOS AQUÍ ---
        checkAutoScroll.setSelected(true);     // <--- ¡NUEVO! Actualiza el checkbox
        graficasPanel.setAutoScroll(true);     // <--- ¡NUEVO! Llama al método del panel
        graficasScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    private void mostrarGraficaScroll() {
        // --- CAMBIOS AQUÍ ---
        checkAutoScroll.setSelected(false);    // <--- ¡NUEVO! Actualiza el checkbox
        graficasPanel.setAutoScroll(false);    // <--- ¡NUEVO! Llama al método del panel
        graficasScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
        } else if (panelSimulacionActivo instanceof EstacionSolarPanel) {
            mostrarEstacionSolar();
        }
    }

    private void cambiarPanelSimulacion(JPanel nuevoPanel, Simulable nuevaSimulacion) {
        if (simulacionActual != null) {
            simulacionActual.detener();
        }

        panelSuperior.removeAll();
        panelSuperior.add(nuevoPanel);
        panelSuperior.add(panelGrafo);
        panelSuperior.revalidate();
        panelSuperior.repaint();

        simulacionActual = nuevaSimulacion;
    }

    // Métodos "mostrar" actualizados para inyectar el panelGrafo
    
    private void mostrarCenaFilosofos() {
        panelGrafo.inicializarGrafo("CenaFilosofos");
        cenaFilosofosPanel = new CenaFilosofosPanel(tipoSincronizacion, panelGrafo);
        cambiarPanelSimulacion(cenaFilosofosPanel, cenaFilosofosPanel);
    }
    
    private void mostrarBarberoDormilon() {
        panelGrafo.inicializarGrafo("BarberoDormilon");
        barberoDormilonPanel = new BarberoDormilonPanel(tipoSincronizacion, panelGrafo);
        cambiarPanelSimulacion(barberoDormilonPanel, barberoDormilonPanel);
    }
    
    private void mostrarEscritorLector() {
        panelGrafo.inicializarGrafo("EscritorLector");
        escritorLectorPanel = new EscritorLectorPanel(tipoSincronizacion, panelGrafo);
        cambiarPanelSimulacion(escritorLectorPanel, escritorLectorPanel);
    }

    private void mostrarFumadores() {
        panelGrafo.inicializarGrafo("Fumadores");
        fumadoresPanel = new FumadoresPanel(tipoSincronizacion, panelGrafo);
        cambiarPanelSimulacion(fumadoresPanel, fumadoresPanel);
    }
    
    private void mostrarProductorConsumidor() {
        panelGrafo.inicializarGrafo("ProductorConsumidor");
        tanquePanel = new TanquePanel(tipoSincronizacion, panelGrafo);
        cambiarPanelSimulacion(tanquePanel, tanquePanel);
    }
    
    private void mostrarEstacionSolar() {
        panelGrafo.inicializarGrafo("EstacionSolar");
        estacionSolarPanel = new EstacionSolarPanel(tipoSincronizacion, panelGrafo);
        cambiarPanelSimulacion(estacionSolarPanel, estacionSolarPanel);
    }
    
    private void mostrarEstacionSolarMPJ() {
        panelGrafo.inicializarGrafo("EstacionSolar");
        graficasPanel.resetSeries("Comparativa de Eficiencia (MPI)", 
                                 "Núcleo 1 (Mutex)", 
                                 "Núcleo 2 (Semáforos)", 
                                 "Núcleo 3 (Monitores)", 
                                 "Núcleo 4 (Var. Cond)", 
                                 "Núcleo 5 (Barreras)");
        estacionSolarPanelParalelo = new EstacionSolarPanelParalelo(panelGrafo, graficasPanel);
        cambiarPanelSimulacion(estacionSolarPanelParalelo, estacionSolarPanelParalelo);
    }



    public static void main(String[] args) {
        // Es una buena práctica asegurar que la UI se cree en el Hilo de Despacho de Eventos (EDT)
        SwingUtilities.invokeLater(() -> {
            PtoyectoPCyPoto2025 ventana = new PtoyectoPCyPoto2025();
            ventana.setVisible(true);
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        });
    }
}
