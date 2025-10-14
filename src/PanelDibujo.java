import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

// Nodo: puede ser proceso o recurso
class Nodo {
    int x, y;
    String tipoNodo; // "Proceso" o "Recurso"
    String etiqueta; // P1, P2, R1, R2
    int tamano = 60;

    public Nodo(int x, int y, String tipoNodo, String etiqueta) {
        this.x = x;
        this.y = y;
        this.tipoNodo = tipoNodo;
        this.etiqueta = etiqueta;
    }

    public void dibujar(Graphics2D g) {
        g.setColor(tipoNodo.equals("Proceso") ? Color.BLUE : Color.RED);
        if (tipoNodo.equals("Proceso")) {
            g.fillOval(x, y, tamano, tamano);
        } else {
            g.fillRect(x, y, tamano, tamano);
        }
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int anchoTexto = fm.stringWidth(etiqueta);
        int altoTexto = fm.getHeight();
        g.drawString(etiqueta, 
                    x + tamano/2 - anchoTexto/2,
                    y + tamano/2 + altoTexto/4);
    }

    public boolean contiene(int mx, int my) {
        if (tipoNodo.equals("Proceso")) {
            double dx = (mx - (x + tamano / 2.0));
            double dy = (my - (y + tamano / 2.0));
            return dx * dx + dy * dy <= (tamano / 2.0) * (tamano / 2.0);
        } else {
            return new Rectangle(x, y, tamano, tamano).contains(mx, my);
        }
    }

    public Point getCentro() {
        return new Point(x + tamano / 2, y + tamano / 2);
    }
}

// Flecha: conecta dos nodos
class Flecha {
    private Nodo desde, hasta;

    public Flecha(Nodo desde, Nodo hasta) {
        this.desde = desde;
        this.hasta = hasta;
    }

    public void dibujar(Graphics2D g) {
        Point inicio = desde.getCentro();
        Point fin = hasta.getCentro();

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawLine(inicio.x, inicio.y, fin.x, fin.y);

        dibujarCabezaFlecha(g, inicio, fin, 15);
    }

    private void dibujarCabezaFlecha(Graphics2D g, Point desde, Point hasta, int tamanoFlecha) {
        Graphics2D g2 = (Graphics2D) g.create();

        double dx = hasta.x - desde.x;
        double dy = hasta.y - desde.y;
        double angulo = Math.atan2(dy, dx);

        Polygon cabeza = new Polygon();
        cabeza.addPoint(0, 0);
        cabeza.addPoint(-tamanoFlecha, -tamanoFlecha / 2);
        cabeza.addPoint(-tamanoFlecha, tamanoFlecha / 2);

        g2.translate(hasta.x, hasta.y);
        g2.rotate(angulo);
        g2.fill(cabeza);
        g2.dispose();
    }
}

// Panel de dibujo
public class PanelDibujo extends JPanel {
    private List<Nodo> nodos = new ArrayList<>();
    private List<Flecha> flechas = new ArrayList<>();
    private Nodo nodoSeleccionado = null;
    private Point finLineaTemporal = null;
    private Point ultimoClick;

    private Nodo nodoArrastrado = null;
    private int desfaseX, desfaseY;

    // Contadores de procesos y recursos
    private int contadorProcesos = 0;
    private int contadorRecursos = 0;

    public PanelDibujo() {
        setBackground(Color.WHITE);

        // Menú emergente al dar clic derecho
        JPopupMenu menuEmergente = new JPopupMenu();
        JMenuItem itemAgregarProceso = new JMenuItem("Agregar Proceso");
        JMenuItem itemAgregarRecurso = new JMenuItem("Agregar Recurso");
        JMenuItem itemSalir = new JMenuItem("Salir");

        menuEmergente.add(itemAgregarProceso);
        menuEmergente.add(itemAgregarRecurso);
        menuEmergente.addSeparator();
        menuEmergente.add(itemSalir);

        itemAgregarProceso.addActionListener(e -> {
            contadorProcesos++;
            nodos.add(new Nodo(ultimoClick.x, ultimoClick.y, "Proceso", "P" + contadorProcesos));
            repaint();
        });
        itemAgregarRecurso.addActionListener(e -> {
            contadorRecursos++;
            nodos.add(new Nodo(ultimoClick.x, ultimoClick.y, "Recurso", "R" + contadorRecursos));
            repaint();
        });
        itemSalir.addActionListener(e -> {
            System.exit(0);
        });

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                ultimoClick = e.getPoint();

                if (SwingUtilities.isRightMouseButton(e)) {
                    menuEmergente.show(PanelDibujo.this, e.getX(), e.getY());
                    return;
                }

                for (Nodo n : nodos) {
                    if (n.contiene(e.getX(), e.getY())) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            // Arrastrar nodo
                            nodoArrastrado = n;
                            desfaseX = e.getX() - n.x;
                            desfaseY = e.getY() - n.y;
                        }
                        // Conexión
                        nodoSeleccionado = n;
                        finLineaTemporal = e.getPoint();
                        return;
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (nodoArrastrado != null) {
                    nodoArrastrado = null;
                } else if (nodoSeleccionado != null) {
                    for (Nodo n : nodos) {
                        if (n != nodoSeleccionado && n.contiene(e.getX(), e.getY())) {
                            flechas.add(new Flecha(nodoSeleccionado, n));
                            break;
                        }
                    }
                    nodoSeleccionado = null;
                    finLineaTemporal = null;
                }
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (nodoArrastrado != null) {
                    nodoArrastrado.x = e.getX() - desfaseX;
                    nodoArrastrado.y = e.getY() - desfaseY;
                    repaint();
                } else if (nodoSeleccionado != null) {
                    finLineaTemporal = e.getPoint();
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dibujar flechas
        for (Flecha f : flechas) {
            f.dibujar(g2d);
        }

        // Dibujar línea temporal
        if (nodoSeleccionado != null && finLineaTemporal != null) {
            Point inicio = nodoSeleccionado.getCentro();
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            g2d.drawLine(inicio.x, inicio.y, finLineaTemporal.x, finLineaTemporal.y);
        }

        // Dibujar nodos
        for (Nodo n : nodos) {
            n.dibujar(g2d);
        }
    }
}