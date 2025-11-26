# Guía de Ejecución Paralela - Estación Solar

## Objetivo
Esta guía describe los pasos para ejecutar la simulación de la Estación Solar con ejecución paralela MPJ, asegurando que:
- La interfaz gráfica muestre el **grafo de asignación de recursos con nodos y aristas activas**
- Se visualicen **5 series de algoritmos en JFreeChart**
- Los contadores de "Total recargas", "Críticas" y "Normales" se actualicen en tiempo real
- La UI mantenga la paridad visual con la interfaz original

## Requisitos Previos
- MPJ Express configurado correctamente
- JFreeChart en el classpath
- Archivo `mpj.conf` en `src/` con:
  ```
  NP=5
  mainclass=EstacionSolarMPJ
  cp=build/classes:libs/*
  ```

## Estructura de Ejecución

### Flujo de Ranks MPJ
```
Rank 0: UI Principal (EstacionSolarPanelParalelo)
  ├── Panel central: Estación (bloque verde con energía, bahías, modo)
  ├── Panel izquierdo: Estadísticas (Total recargas, Críticas, Normales)
  ├── Panel derecho: Grafo de Asignación de Recursos
  ├── Panel inferior: Gráficas JFreeChart (5 series)
  └── Barra inferior: Botones "Mostrar Información" y "Detener Simulación"

Ranks 1-5: Algoritmos Paralelos
  ├── Rank 1: Monitores
  ├── Rank 2: Semáforos
  ├── Rank 3: Condición
  ├── Rank 4: Mutex
  └── Rank 5: Barreras
```

### Pipeline de Eventos
1. **Generación**: Cada algoritmo (Ranks 1-5) genera eventos `ResultadoAlgoritmo`
2. **Cola Concurrente**: Eventos se encolan en estructura thread-safe
3. **Procesamiento EDT**: Rank 0 procesa lotes con `SwingUtilities.invokeLater`
4. **Renderizado**: Actualiza simultáneamente PanelGrafoDinamico y GraficasPanel

### Formato de Evento ResultadoAlgoritmo
```json
{
  "algoritmo": "Monitores|Semáforos|Condición|Mutex|Barreras",
  "timestamp": 1732632000000,
  "nodoId": "D0-D7",
  "energia": 0-100,
  "tipoRecarga": "Crítica|Normal",
  "aristas": ["D3->B1", "D3->B2"],
  "serieValor": 0.87
}
```

## Pasos de Ejecución

### 1. Preparar el Proyecto
```bash
# Compilar
javac -cp "libs/mpj.jar:libs/jfreechart-*.jar" -d build/classes src/**/*.java

# Verificar mpj.conf
cat src/mpj.conf
```

### 2. Iniciar Simulación
```bash
# Ejecutar con MPJ
mpjrun.sh -np 5 -cp "build/classes:libs/*" EstacionSolarMPJ
```

### 3. Verificar Visualización
**Criterios de Aceptación**:
- ✅ Panel derecho muestra **nodos y aristas activas** en el grafo
- ✅ Panel inferior muestra **5 curvas** (una por algoritmo) en JFreeChart
- ✅ Contadores se actualizan: "Total recargas", "Críticas", "Normales"
- ✅ Título de ventana: "Test Estación Solar - Ejecución Paralela MPJ"
- ✅ Al detener: se imprime resumen y **UI permanece visible**

## Solución de Problemas

### Grafo Vacío
**Síntoma**: Panel derecho sin nodos ni aristas
**Causas comunes**:
- Suscriptores no registrados en PanelGrafoDinamico
- Layout incorrecto del panel derecho
- Falta de repintado después de actualizar datos

**Solución**:
```java
// Verificar suscripción en EstacionSolarPanelParalelo
panelGrafo.suscribir(eventProcessor);

// Asegurar repintado en EDT
SwingUtilities.invokeLater(() -> {
    panelGrafo.actualizarGrafo(eventos);
    panelGrafo.repaint();
});
```

### UI No Coincide con Original
**Síntoma**: Layout diferente, colores incorrectos, elementos faltantes
**Verificar**:
- Uso de `EstacionSolarPanelParalelo` en `EstacionSolarMPJ`
- BorderLayout con paneles en posiciones correctas:
  - `BorderLayout.CENTER`: Panel estación y estadísticas
  - `BorderLayout.EAST`: Panel grafo
  - `BorderLayout.SOUTH`: Panel gráficas
  - `BorderLayout.SOUTH` (segundo panel): Barra de controles

### Gráficas sin Datos
**Síntoma**: JFreeChart vacío o con series incompletas
**Verificar**:
- Todos los ranks 1-5 están enviando eventos
- Formato correcto de `serieValor` en eventos
- Suscripción de GraficasPanel al EventProcessor

## Comandos de Control

### Detener Simulación
```java
// En barra inferior
JButton btnDetener = new JButton("Detener Simulación");
btnDetener.addActionListener(e -> {
    // Enviar comando de parada a todos los ranks
    enviarComandoDetener();
    // Imprimir resumen
    imprimirResumen();
    // UI permanece visible
});
```

### Mostrar Información
```java
JButton btnInfo = new JButton("Mostrar Información");
btnInfo.addActionListener(e -> {
    JOptionPane.showMessageDialog(frame, 
        "Estadísticas:\n" +
        "Total recargas: " + totalRecargas + "\n" +
        "Críticas: " + criticas + "\n" +
        "Normales: " + normales);
});
```

## Validación Final
Antes de considerar completa la tarea, verificar:
1. **Interfaz**: Misma disposición que imagen original (nodos D0-D7, bloque verde, estadísticas)
2. **Funcionalidad**: Todos los algoritmos contribuyen datos visibles
3. **Estabilidad**: No hay excepciones de concurrencia
4. ** Rendimiento**: Actualizaciones fluidas sin bloqueos de UI