import javax.swing.*;
import java.awt.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class GraficasPanel extends JPanel {
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private ChartPanel chartPanel;
    private volatile boolean autoScroll = false;
    private static final int PANEL_HEIGHT = 200;
    private Thread generadorPorDefecto;
    private JLabel statusLabel;
    private int[] contadoresPuntos = new int[5]; // Contadores para cada serie

    public GraficasPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, PANEL_HEIGHT));

        dataset = new XYSeriesCollection(new XYSeries("Datos Aleatorios"));
        chart = ChartFactory.createXYLineChart(
                "Comparativa de Eficiencia (MPI)",
                "Nivel Construido",
                "Tiempo (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Configurar el renderer para líneas tipo escalera
        XYPlot plot = chart.getXYPlot();
        XYStepRenderer stepRenderer = new XYStepRenderer();
        plot.setRenderer(stepRenderer);

        // Configurar el eje Y con auto-rango para que los datos siempre se vean
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRange(true);

        // Configurar el eje X con formato de miles
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setNumberFormatOverride(new java.text.DecimalFormat("#,##0"));

        // Fondo blanco con malla gris tenue
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, PANEL_HEIGHT));
        add(chartPanel, BorderLayout.CENTER);
        
        statusLabel = new JLabel("<html>Estado:<br/>Esperando datos...</html>");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(statusLabel, BorderLayout.SOUTH);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        chartPanel = new ChartPanel(chart);
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
        add(chartPanel, BorderLayout.CENTER);

        // Rango inicial del eje X (Nivel Construido)
        chart.getXYPlot().getDomainAxis().setRange(0, 1);

        iniciarGeneradorPorDefecto();
    }

    private void iniciarGeneradorPorDefecto() {
        detenerGeneradorPorDefecto();
        generadorPorDefecto = new Thread(() -> {
            try {
                int x = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    final int currentX = x++;
                    final double currentY = 50 + 40 * Math.sin(Math.toRadians(currentX * 5));
                    SwingUtilities.invokeLater(() -> {
                        dataset.getSeries(0).add(currentX, currentY);
                        ajustarRangosYTamano(currentX);
                    });
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignored) {}
        }, "Grafica-Generador-Default");
        generadorPorDefecto.setDaemon(true);
        generadorPorDefecto.start();
    }

    private void detenerGeneradorPorDefecto() {
        if (generadorPorDefecto != null && generadorPorDefecto.isAlive()) {
            generadorPorDefecto.interrupt();
        }
    }

    private void ajustarRangosYTamano(double currentX) {
        if (autoScroll) {
            double upper = currentX <= 0 ? 1 : currentX;
            double lower = Math.max(0, upper - 50);
            chart.getXYPlot().getDomainAxis().setRange(lower, upper);
            if (getParent() != null) {
                setPreferredSize(new Dimension(getParent().getWidth(), PANEL_HEIGHT));
            }
        } else {
            double upper = currentX <= 0 ? 1 : currentX;
            chart.getXYPlot().getDomainAxis().setRange(0, upper);
            int newWidth = Math.max(getPreferredSize().width, (int) currentX);
            setPreferredSize(new Dimension(newWidth, PANEL_HEIGHT));
        }
        revalidate();
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        double maxX = obtenerMaxX();
        if (autoScroll) {
            if (getParent() != null) {
                setPreferredSize(new Dimension(getParent().getWidth(), PANEL_HEIGHT));
            }
            double upper = maxX <= 0 ? 1 : maxX;
            double lower = Math.max(0, upper - 50);
            chart.getXYPlot().getDomainAxis().setRange(lower, upper);
        } else {
            int newWidth = Math.max(getPreferredSize().width, (int) maxX);
            setPreferredSize(new Dimension(newWidth, PANEL_HEIGHT));
            double upper = maxX <= 0 ? 1 : maxX;
            chart.getXYPlot().getDomainAxis().setRange(0, upper);
        }
        revalidate();
    }

    private double obtenerMaxX() {
        double maxX = 0;
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            maxX = Math.max(maxX, dataset.getSeries(i).getMaxX());
        }
        return maxX;
    }

    public synchronized void resetSeries(String titulo, String... nombres) {
        detenerGeneradorPorDefecto();
        dataset.removeAllSeries();
        
        // Crear las 5 series para los algoritmos de sincronización
        XYSeries mutexSeries = new XYSeries("Núcleo 1 (Mutex)", false); // setAutoSort(false)
        XYSeries semaforoSeries = new XYSeries("Núcleo 2 (Semáforos)", false);
        XYSeries monitorSeries = new XYSeries("Núcleo 3 (Monitores)", false);
        XYSeries condicionSeries = new XYSeries("Núcleo 4 (Var. Cond)", false);
        XYSeries barreraSeries = new XYSeries("Núcleo 5 (Barreras)", false);
        
        dataset.addSeries(mutexSeries);
        dataset.addSeries(semaforoSeries);
        dataset.addSeries(monitorSeries);
        dataset.addSeries(condicionSeries);
        dataset.addSeries(barreraSeries);
        
        // Configurar colores para cada línea
        XYPlot plot = chart.getXYPlot();
        XYStepRenderer renderer = (XYStepRenderer) plot.getRenderer();
        
        // Colores específicos para cada algoritmo
        renderer.setSeriesPaint(0, Color.RED);      // Mutex
        renderer.setSeriesPaint(1, Color.BLUE);     // Semáforos
        renderer.setSeriesPaint(2, Color.GREEN);    // Monitores
        renderer.setSeriesPaint(3, Color.YELLOW);   // Variables de Condición
        renderer.setSeriesPaint(4, Color.MAGENTA);  // Barreras
        
        chart.setTitle(titulo);
        chart.getXYPlot().setDataset(dataset);
        chart.getXYPlot().getDomainAxis().setRange(0, 1);
        
        // Habilitar auto-rango en Y para que se ajuste a los datos
        NumberAxis rangeAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        rangeAxis.setAutoRange(true);
        
        setPreferredSize(new Dimension(getPreferredSize().width, PANEL_HEIGHT));
        revalidate();
        
        // Reiniciar contadores
        for (int i = 0; i < contadoresPuntos.length; i++) {
            contadoresPuntos[i] = 0;
        }
        
        // Actualizar estado
        statusLabel.setText("Estado: Gráfico reiniciado - Esperando datos...");
    }

    private XYSeries buscarSerie(String nombre) {
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            XYSeries s = dataset.getSeries(i);
            if (String.valueOf(s.getKey()).equals(nombre)) return s;
        }
        return null;
    }
    
    private int getSerieIndex(String nombre) {
        String[] nombresSeries = {"Núcleo 1 (Mutex)", "Núcleo 2 (Semáforos)", "Núcleo 3 (Monitores)", "Núcleo 4 (Var. Cond)", "Núcleo 5 (Barreras)"};
        for (int i = 0; i < nombresSeries.length; i++) {
            if (nombresSeries[i].equals(nombre)) return i;
        }
        return -1;
    }

    public void addPoint(String serie, double x, double y) {
        SwingUtilities.invokeLater(() -> {
            XYSeries s = buscarSerie(serie);
            if (s != null) {
                s.add(x, y);
                ajustarRangosYTamano(x);
                
                // Incrementar contador correspondiente
                int index = getSerieIndex(serie);
                if (index >= 0 && index < contadoresPuntos.length) {
                    contadoresPuntos[index]++;
                }
                
                String[] nombresSeries = {"Mutex", "Semáforos", "Monitores", "Var. Cond", "Barreras"};
                StringBuilder resumen = new StringBuilder("<html>Puntos por serie:<br/>");
                for (int i = 0; i < contadoresPuntos.length; i++) {
                    resumen.append(nombresSeries[i]).append(": ").append(contadoresPuntos[i]).append("<br/>");
                }
                resumen.append("</html>");
                statusLabel.setText(resumen.toString());
            } else {
                statusLabel.setText("<html>Estado:<br/>ERROR - Serie no encontrada: " + serie + "</html>");
            }
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < dataset.getSeriesCount(); i++) {
                dataset.getSeries(i).clear();
            }
            chart.getXYPlot().getDomainAxis().setRange(0, 1);
            revalidate();
            
            // Reiniciar contadores
            for (int i = 0; i < contadoresPuntos.length; i++) {
                contadoresPuntos[i] = 0;
            }
            statusLabel.setText("Estado: Datos limpiados");
        });
    }
}
