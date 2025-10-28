// Archivo: Flecha.java
import java.awt.*;

// Flecha: conecta dos nodos
class Flecha {
    private Nodo desde, hasta;

    public Flecha(Nodo desde, Nodo hasta) {
        this.desde = desde;
        this.hasta = hasta;
    }

    public void dibujar(Graphics2D g) {
        Point inicio = desde.getCentro();
        Point finOriginal = hasta.getCentro();

        // Ajustar el punto final para que la flecha termine en el borde del nodo
        double dx = finOriginal.x - inicio.x;
        double dy = finOriginal.y - inicio.y;
        double distancia = Math.sqrt(dx * dx + dy * dy);
        double radioNodo = hasta.tamano / 2.0;

        // Solo ajustar si la distancia es mayor que el radio
        if (distancia > radioNodo) {
            double relacion = (distancia - radioNodo) / distancia;
            int finX = inicio.x + (int) (dx * relacion);
            int finY = inicio.y + (int) (dy * relacion);
            Point finAjustado = new Point(finX, finY);

            // Guardar el color y grosor originales
            Color colorOriginal = g.getColor();
            Stroke strokeOriginal = g.getStroke();

            // Dibujar la línea principal de la flecha
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawLine(inicio.x, inicio.y, finAjustado.x, finAjustado.y);

            // Dibujar la cabeza de la flecha en el punto final ajustado
            dibujarCabezaFlechaManual(g, inicio, finAjustado, 15);

            // Restaurar el color y grosor originales
            g.setColor(colorOriginal);
            g.setStroke(strokeOriginal);
        }
    }

    /**
     * Dibuja la cabeza de la flecha calculando manualmente las coordenadas
     * de sus puntos y rellenando el polígono resultante.
     */
    private void dibujarCabezaFlechaManual(Graphics2D g, Point desde, Point hasta, int tamanoFlecha) {
        double dx = hasta.x - desde.x;
        double dy = hasta.y - desde.y;
        // Calcular el ángulo de la línea principal
        double anguloLinea = Math.atan2(dy, dx);
        // Definir qué tan abierta será la punta de la flecha (en radianes)
        double anguloApertura = Math.PI / 10; // Puedes ajustar este valor (más pequeño = más puntiagudo)

        // Calcular las coordenadas del primer punto de la base de la cabeza
        int x1 = (int) (hasta.x - tamanoFlecha * Math.cos(anguloLinea - anguloApertura));
        int y1 = (int) (hasta.y - tamanoFlecha * Math.sin(anguloLinea - anguloApertura));

        // Calcular las coordenadas del segundo punto de la base de la cabeza
        int x2 = (int) (hasta.x - tamanoFlecha * Math.cos(anguloLinea + anguloApertura));
        int y2 = (int) (hasta.y - tamanoFlecha * Math.sin(anguloLinea + anguloApertura));

        // Crear el polígono triangular para la cabeza de la flecha
        Polygon cabeza = new Polygon();
        cabeza.addPoint(hasta.x, hasta.y); // La punta de la flecha (en el punto 'hasta')
        cabeza.addPoint(x1, y1);           // Un extremo de la base
        cabeza.addPoint(x2, y2);           // El otro extremo de la base

        // Asegurar que el color de relleno sea negro y rellenar el polígono
        g.setColor(Color.BLACK); // ¡Importante! Establecer el color de relleno
        g.fillPolygon(cabeza);   // Rellenar la cabeza triangular
    }

    // El método anterior con transformaciones (g2.create, translate, rotate) se elimina
    // ya que estamos usando el método manual que es más directo.
}