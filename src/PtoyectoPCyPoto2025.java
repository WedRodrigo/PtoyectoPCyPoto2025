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
    private PanelDibujo panelDibujo;
    private CenaFilosofosPanel cenaFilosofosPanel;
    private BarberoDormilonPanel barberoDormilonPanel;
    private EscritorLectorPanel escritorLectorPanel;
    private FumadoresPanel fumadoresPanel;

    private String tipoSincronizacion = "Mutex"; // Por defecto

    PtoyectoPCyPoto2025() {
        setSize(1500, 800);
        setTitle("Proyecto Programación Concurrente y Paralela Otoño 2025");
        getContentPane().setBackground(Color.BLACK);
    
        // Configurar layout para dividir la ventana en dos partes
        setLayout(new GridLayout(1, 2));
    
        // Panel del tanque (izquierda)
        // Se inicializará en mostrarProductorConsumidor()
    
        // Panel de dibujo (derecha)
        panelDibujo = new PanelDibujo();
        add(panelDibujo);
        
        // Panel de la cena de los filósofos (no se agrega inicialmente)
        
        // Panel del barbero dormilón (no se agrega inicialmente)
        
        // Panel del problema de lectores-escritores (no se agrega inicialmente)
        
        // Panel de los fumadores (no se agrega inicialmente)
    
        // Configuración del menú
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
        // Aquí se agregarán los demás en el futuro

        // Agregar acción al menú de Cena de los Filósofos
        itemCenaFilosofos.addActionListener(e -> mostrarCenaFilosofos());
        
        // Agregar acción al menú de Barbero Dormilón
        itemBarberoDormilon.addActionListener(e -> mostrarBarberoDormilon());
        
        // Agregar acción al menú de Fumadores
        itemFumadores.addActionListener(e -> mostrarFumadores());

        // Agregar acción al menú de Lectores-Escritores
        itemLectoresEscritores.addActionListener(e -> mostrarEscritorLector());
        
        itemProductorConsumidor.addActionListener(e -> mostrarProductorConsumidor());

        setJMenuBar(barraMenu);
    }

    private void setSincronizacion(String tipo) {
        this.tipoSincronizacion = tipo;
        // Opcional: mostrar un mensaje o actualizar la UI para reflejar el cambio
        JOptionPane.showMessageDialog(this, "Sincronización cambiada a: " + tipo);
    }

    private void mostrarCenaFilosofos() {
        // Eliminar los paneles actuales y agregar los correspondientes
        getContentPane().removeAll();
        cenaFilosofosPanel = new CenaFilosofosPanel(tipoSincronizacion);
        add(cenaFilosofosPanel);
        add(panelDibujo);
        revalidate();
        repaint();
    }
    
    private void mostrarBarberoDormilon() {
        // Eliminar los paneles actuales y agregar los correspondientes
        getContentPane().removeAll();
        barberoDormilonPanel = new BarberoDormilonPanel(tipoSincronizacion);
        add(barberoDormilonPanel);
        add(panelDibujo);
        revalidate();
        repaint();
    }
    
    private void mostrarEscritorLector() {
        // Eliminar los paneles actuales y agregar los correspondientes
        getContentPane().removeAll();
        escritorLectorPanel = new EscritorLectorPanel(tipoSincronizacion);
        add(escritorLectorPanel);
        add(panelDibujo);
        revalidate();
        repaint();
    }

    private void mostrarFumadores() {
        // Eliminar los paneles actuales y agregar los correspondientes
        getContentPane().removeAll();
        fumadoresPanel = new FumadoresPanel(tipoSincronizacion);
        add(fumadoresPanel);
        add(panelDibujo);
        revalidate();
        repaint();
    }
    
    private void mostrarProductorConsumidor() {
        // Eliminar los paneles actuales y agregar los correspondientes
        getContentPane().removeAll();
        // Muestra el TanquePanel para Productor-Consumidor
        tanquePanel = new TanquePanel(tipoSincronizacion);
        add(tanquePanel); 
        add(panelDibujo);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        PtoyectoPCyPoto2025 ventana = new PtoyectoPCyPoto2025();
        ventana.setVisible(true);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}