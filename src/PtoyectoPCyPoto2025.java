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

    private String tipoSincronizacion = "Monitores"; 

    PtoyectoPCyPoto2025() {
        setSize(1500, 800);
        setTitle("Proyecto Programación Concurrente y Paralela Otoño 2025");
        getContentPane().setBackground(Color.BLACK);
    
        setLayout(new GridLayout(1, 2));
    
        panelDibujo = new PanelDibujo();
        add(panelDibujo);
        
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
    }

    private void mostrarCenaFilosofos() {
        getContentPane().removeAll();
        cenaFilosofosPanel = new CenaFilosofosPanel(tipoSincronizacion);
        add(cenaFilosofosPanel);
        add(panelDibujo);
        revalidate();
        repaint();
    }
    
    private void mostrarBarberoDormilon() {
        getContentPane().removeAll();
        barberoDormilonPanel = new BarberoDormilonPanel(tipoSincronizacion);
        add(barberoDormilonPanel);
        add(panelDibujo);
        revalidate();
        repaint();
    }
    
    private void mostrarEscritorLector() {
        getContentPane().removeAll();
        escritorLectorPanel = new EscritorLectorPanel(tipoSincronizacion);
        add(escritorLectorPanel);
        add(panelDibujo);
        revalidate();
        repaint();
    }

    private void mostrarFumadores() {
        getContentPane().removeAll();
        fumadoresPanel = new FumadoresPanel(tipoSincronizacion);
        add(fumadoresPanel);
        add(panelDibujo);
        revalidate();
        repaint();
    }
    
    private void mostrarProductorConsumidor() {
        getContentPane().removeAll();
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