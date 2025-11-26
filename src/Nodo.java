import java.awt.*;

// Nodo: puede ser proceso o recurso
class Nodo {
    int x, y;
    String tipoNodo; // "Proceso" o "Recurso"
    String etiqueta; // P1, P2, R1, R2
    String informacion; // Información adicional del nodo
    int tamano = 60;

    public Nodo(int x, int y, String tipoNodo, String etiqueta) {
        this.x = x;
        this.y = y;
        this.tipoNodo = tipoNodo;
        this.etiqueta = etiqueta;
        this.informacion = "";
    }

    public void setInformacion(String informacion) {
        this.informacion = informacion;
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
        
        // Dibujar información adicional si existe
        if (informacion != null && !informacion.isEmpty()) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            FontMetrics fmInfo = g.getFontMetrics();
            int anchoInfo = fmInfo.stringWidth(informacion);
            g.drawString(informacion, 
                        x + tamano/2 - anchoInfo/2,
                        y + tamano + 15);
        }
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