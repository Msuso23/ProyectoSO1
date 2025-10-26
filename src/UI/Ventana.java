/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package UI;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;

import Algoritmos.*;
import Controladores.*;
import EDD.*;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;
import Modelos.ProcessType;
import Utilidades.*;

/**
 *
 * @author dacor
 */
public class Ventana extends javax.swing.JFrame {

    // ===== VARIABLES DE INSTANCIA =====
    private ChartPanel chartPanelGlobal;
    private ChartPanel chartPanelProcesses;
    private JFreeChart chartGlobal;
    private JFreeChart chartProcesses;
    private XYSeriesCollection datasetGlobal;
    private XYSeriesCollection datasetProcesses;
    private XYSeries seriesThroughput;
    private XYSeries seriesCPU;
    private XYSeries seriesAvgRT;
    private XYSeries seriesFairness;
    private Map<Integer, XYSeries> processSeriesMap;
    private Map<Integer, Color> processColorMap;
    private DefaultTableModel modeloTablaResultados;
    private Scheduler scheduler;
    private ProcessManager processManager;
    private CPU cpu;
    private IOManager ioManager;
    private Timer simulationTimer;
    private int globalClock = 0;
    private int simulationSpeed = 100;
    private boolean isPaused = false;
    private int nextPID = 1;
    private Lista<Process> allProcesses;
    private Lista<Process> terminatedProcesses;
    private Process lastExecutedProcess = null;
    private int eventCounter = 0;

    // ===== CONSTRUCTOR =====
    public Ventana() {
        initComponents(); // ← NO TOCAR, generado por NetBeans
        initializeCustomComponents(); // ← Nuestro método personalizado
    }

    // ===== MÉTODO DE INICIALIZACIÓN PERSONALIZADA =====
    private void initializeCustomComponents() {
        // 1. Inicializar estructuras de datos
        allProcesses = new Lista<>();
        terminatedProcesses = new Lista<>();
        processManager = new ProcessManager();
        cpu = new CPU();
        ioManager = new IOManager();
        processSeriesMap = new HashMap<>();
        processColorMap = new HashMap<>();

        // 2. Configurar ventana
        setTitle("Simulador de Planificación de CPU - Sistemas Operativos");
        setSize(1460, 855);
        setLocationRelativeTo(null);

        // 3. Configurar TABLA DE RESULTADOS
        initializeResultsTable();

        // 4. Configurar Spinners
        initializeSpinners();

        setupVelocidadSliderListener();

        initializeDetalleColasPanel();

        setupQuantumListener();

        initializeCharts();

        // 6. Configurar áreas de texto como no editables
        ActivosTextArea.setEditable(false);
        BloqueadosTextArea.setEditable(false);
        TerminadosTextArea.setEditable(false);
        SuspendidosTextArea.setEditable(false);

        // 7. Crear scheduler por defecto
        createScheduler();
    }

    private void initializeDetalleColasPanel() {
        // Configurar TextArea
        DetalleColasTextArea.setEditable(false);
        DetalleColasTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        DetalleColasTextArea.setBackground(new Color(250, 250, 250));
        DetalleColasTextArea.setText("Esperando inicio de simulación...");
    }

    // ===== INICIALIZAR LOG DE EVENTOS =====
    private void initializeEventLog() {
        // Configurar TextArea del log
        LogEventosTextArea.setEditable(false);
        LogEventosTextArea.setLineWrap(true);
        LogEventosTextArea.setWrapStyleWord(true);
        LogEventosTextArea.setFont(new Font("Consolas", Font.PLAIN, 10));
        LogEventosTextArea.setBackground(new Color(250, 250, 250));

        // Agregar mensaje inicial
        logEvent("SISTEMA", "Sistema de planificación inicializado", Color.BLUE);
        logEvent("INFO", "Algoritmo seleccionado: " + AlgorithmSelectorComboBox.getSelectedItem(), Color.BLUE);
    }

    // ===== MÉTODO PRINCIPAL PARA REGISTRAR EVENTOS =====
    private void logEvent(String category, String message, Color color) {
        eventCounter++;

        // Formato: [000] [HH:MM:SS] [CATEGORÍA] Mensaje
        String timestamp = String.format("%02d:%02d:%02d",
                globalClock / 3600,
                (globalClock % 3600) / 60,
                globalClock % 60);

        String logEntry = String.format("[%03d] [Ciclo %04d] [%s] %s\n",
                eventCounter,
                globalClock,
                category,
                message);

        LogEventosTextArea.append(logEntry);

        // Auto-scroll al final
        LogEventosTextArea.setCaretPosition(LogEventosTextArea.getDocument().getLength());

        // Limitar cantidad de líneas (mantener solo últimas 200)
        if (LogEventosTextArea.getLineCount() > 200) {
            try {
                int endOffset = LogEventosTextArea.getLineEndOffset(0);
                LogEventosTextArea.replaceRange("", 0, endOffset);
            } catch (Exception e) {
                // Ignorar errores
            }
        }
    }

    // ===== MÉTODOS AUXILIARES DE LOG =====
    private void logSchedulerDecision(String decision) {
        logEvent("SCHEDULER", decision, new Color(255, 152, 0));
    }

    private void logProcessStateChange(int pid, String fromState, String toState) {
        logEvent("ESTADO", String.format("P%d: %s → %s", pid, fromState, toState), new Color(76, 175, 80));
    }

    private void logCPUActivity(String activity) {
        logEvent("CPU", activity, new Color(33, 150, 243));
    }

    private void logIOActivity(String activity) {
        logEvent("I/O", activity, new Color(156, 39, 176));
    }

    private void logKernelMode(String operation) {
        logEvent("KERNEL", operation, new Color(255, 87, 34));
    }

    private void logError(String error) {
        logEvent("ERROR", error, new Color(244, 67, 54));
    }

    private void clearLog() {
        LogEventosTextArea.setText("");
        eventCounter = 0;
        logEvent("SISTEMA", "Log limpiado", Color.BLUE);
    }

    // ===== INICIALIZAR TABLA DE RESULTADOS =====
    private void initializeResultsTable() {
        String[] columnNames = {
                "PID", "Nombre", "AT", "BT", "CT", "TAT", "WT", "RT"
        };

        // Crear modelo NO EDITABLE
        modeloTablaResultados = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Todas las celdas NO editables
            }
        };

        // Aplicar modelo a la tabla
        ResultadosDeSimulacionTable.setModel(modeloTablaResultados);

        // Centrar contenido de todas las columnas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < ResultadosDeSimulacionTable.getColumnCount(); i++) {
            ResultadosDeSimulacionTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Ajustar anchos de columnas
        ResultadosDeSimulacionTable.getColumnModel().getColumn(0).setPreferredWidth(40); // PID
        ResultadosDeSimulacionTable.getColumnModel().getColumn(1).setPreferredWidth(80); // Nombre
        ResultadosDeSimulacionTable.getColumnModel().getColumn(2).setPreferredWidth(50); // AT
        ResultadosDeSimulacionTable.getColumnModel().getColumn(3).setPreferredWidth(50); // BT
        ResultadosDeSimulacionTable.getColumnModel().getColumn(4).setPreferredWidth(50); // CT
        ResultadosDeSimulacionTable.getColumnModel().getColumn(5).setPreferredWidth(50); // TAT
        ResultadosDeSimulacionTable.getColumnModel().getColumn(6).setPreferredWidth(50); // WT
        ResultadosDeSimulacionTable.getColumnModel().getColumn(7).setPreferredWidth(50); // RT

        // Permitir ordenar (click en headers)
        ResultadosDeSimulacionTable.setAutoCreateRowSorter(true);
    }

    // ===== INICIALIZAR SPINNERS =====
    private void initializeSpinners() {
        QuantumSpinner.setModel(new SpinnerNumberModel(4, 1, 20, 1));
        ProcesosEnMemoriaSpinner.setModel(new SpinnerNumberModel(10, 1, 20, 1));
        LlegadaCicloSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
        NroInstruccionesSpinner.setModel(new SpinnerNumberModel(10, 1, 100, 1));
        CicloDeIOSpinner.setModel(new SpinnerNumberModel(0, 0, 50, 1));
        DuracionDeIOSpinner.setModel(new SpinnerNumberModel(0, 0, 20, 1));
        TamañoEnMemoriaSpinner.setModel(new SpinnerNumberModel(1024, 512, 8192, 512));
        Nivel0Spinner.setModel(new SpinnerNumberModel(1, 1, 20, 1));
        Nivel1Spinner.setModel(new SpinnerNumberModel(2, 1, 20, 1));
        Nivel2Spinner.setModel(new SpinnerNumberModel(3, 1, 20, 1));
        Nivel3Spinner.setModel(new SpinnerNumberModel(4, 1, 20, 1));

        VelocidadSlider.setMinimum(1);
        VelocidadSlider.setMaximum(1000);
        VelocidadSlider.setValue(100);
        VelocidadSlider.setInverted(true); // Izquierda = rápido

        setFeedbackSpinnersEnabled(false); // Deshabilitar al inicio
    }

    private void setupVelocidadSliderListener() {
        VelocidadSlider.addChangeListener(e -> {
            int rawValue = VelocidadSlider.getValue();
            int speed = 1001 - rawValue; // Invertir escala

            // Actualizar el label con la velocidad actual
            VelocidadMsText.setText(speed + " ms");

            // Actualizar el timer si está corriendo
            if (simulationTimer != null && simulationTimer.isRunning()) {
                simulationTimer.setDelay(speed);
                simulationSpeed = speed;
            }
        });

        // Inicializar el label con el valor inicial del slider
        int initialSpeed = 1001 - VelocidadSlider.getValue();
        VelocidadMsText.setText(initialSpeed + " ms");
    }

    private void setupQuantumListener() {
        QuantumSpinner.addChangeListener(e -> {
            if (scheduler instanceof RoundRobinScheduler) {
                int newQuantum = (Integer) QuantumSpinner.getValue();
                ((RoundRobinScheduler) scheduler).setQuantum(newQuantum);
            }
        });
    }

    // ===== INICIALIZAR GRÁFICOS =====
    private void initializeCharts() {
        // 1. GRÁFICO DE MÉTRICAS DEL SISTEMA
        initializeSystemMetricsChart();

        // 2. GRÁFICO DE LÍNEA DE TIEMPO DE PROCESOS
        initializeProcessTimelineChart();
    }

    // ===== GRÁFICO 1: MÉTRICAS DE RENDIMIENTO DEL SISTEMA =====
    private void initializeSystemMetricsChart() {
        // Crear datasets (4 series)
        datasetGlobal = new XYSeriesCollection();

        seriesThroughput = new XYSeries("Throughput");
        seriesCPU = new XYSeries("CPU %");
        seriesAvgRT = new XYSeries("Avg RT");
        seriesFairness = new XYSeries("Fairness");

        datasetGlobal.addSeries(seriesThroughput);
        datasetGlobal.addSeries(seriesCPU);
        datasetGlobal.addSeries(seriesAvgRT);
        datasetGlobal.addSeries(seriesFairness);

        // Crear gráfico
        chartGlobal = ChartFactory.createXYLineChart(
                "Métricas de Rendimiento del Sistema",
                "Ciclo Global",
                "Valor Normalizado (0-1)",
                datasetGlobal,
                PlotOrientation.VERTICAL,
                true, // Mostrar leyenda
                true, // Tooltips
                false // URLs
        );

        // Personalizar apariencia
        customizeGlobalChart(chartGlobal);

        // Crear panel y agregarlo
        chartPanelGlobal = new ChartPanel(chartGlobal);
        chartPanelGlobal.setPreferredSize(new Dimension(480, 215));
        chartPanelGlobal.setMouseWheelEnabled(true);

        // Agregar al panel existente
        MetricasRendimientoSistemaPanel.setLayout(new BorderLayout());
        MetricasRendimientoSistemaPanel.add(chartPanelGlobal, BorderLayout.CENTER);
    }

    private void customizeGlobalChart(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setOutlineVisible(false); // Sin borde

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Colores de las series (sin cambios)
        renderer.setSeriesPaint(0, new Color(33, 150, 243)); // Azul - Throughput
        renderer.setSeriesPaint(1, new Color(76, 175, 80)); // Verde - CPU %
        renderer.setSeriesPaint(2, new Color(255, 152, 0)); // Naranja - Avg RT
        renderer.setSeriesPaint(3, new Color(244, 67, 54)); // Rojo - Fairness

        // Líneas más delgadas
        for (int i = 0; i < 4; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(1.5f));
            renderer.setSeriesShapesVisible(i, false);
        }

        plot.setRenderer(renderer);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));
        domainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 10));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0.0, 1.0);
        rangeAxis.setTickUnit(new NumberTickUnit(0.2));
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 10));

        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 11));

        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 9));
        chart.getLegend().setPadding(2, 2, 2, 2);
    }

    // ===== GRÁFICO 2: LÍNEA DE TIEMPO DE PROCESOS =====
    private void initializeProcessTimelineChart() {
        // Crear dataset vacío
        datasetProcesses = new XYSeriesCollection();
        processSeriesMap = new HashMap<>();
        processColorMap = new HashMap<>();

        // Crear gráfico
        chartProcesses = ChartFactory.createXYLineChart(
                "Línea de Tiempo de Ejecución de Procesos",
                "Ciclo Global",
                "Estado (1=Ejec, 0.5=Listo, 0=Bloq)",
                datasetProcesses,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        // Personalizar
        customizeProcessChart(chartProcesses);

        // Crear panel y agregarlo
        chartPanelProcesses = new ChartPanel(chartProcesses);
        chartPanelProcesses.setPreferredSize(new Dimension(480, 205));
        chartPanelProcesses.setMouseWheelEnabled(true);

        // Agregar al panel existente
        LineaTiempoEjecucionProcesosPane.setLayout(new BorderLayout());
        LineaTiempoEjecucionProcesosPane.add(chartPanelProcesses, BorderLayout.CENTER);
    }

    private void customizeProcessChart(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setOutlineVisible(false); // Sin borde

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Configuración para múltiples procesos
        for (int i = 0; i < 20; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(1.5f));
            renderer.setSeriesShapesVisible(i, false);
        }

        plot.setRenderer(renderer);

        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));
        domainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 10));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(-0.1, 1.1);
        rangeAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));
        rangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 10));

        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 11));

        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 8));
        chart.getLegend().setPadding(1, 1, 1, 1);
    }

    // ===== CREAR SCHEDULER =====
    private void createScheduler() {
        String algorithm = (String) AlgorithmSelectorComboBox.getSelectedItem();

        if (algorithm == null) {
            scheduler = new FCFSScheduler();
            return;
        }

        // Si hay simulación activa, migrar procesos
        boolean isMigrating = (simulationTimer != null && simulationTimer.isRunning());
        Lista<Process> processesToMigrate = null;
        Process currentProcess = null;

        if (isMigrating) {
            // Guardar proceso actual en CPU
            currentProcess = cpu.getCurrentProcess();

            // Recolectar todos los procesos READY del scheduler actual
            processesToMigrate = getReadyQueueFromScheduler();

            logEvent("SCHEDULER", "═══════════════════════════════════════", Color.ORANGE);
            logEvent("SCHEDULER", "CAMBIO DE ALGORITMO EN CALIENTE", new Color(255, 152, 0));
            logEvent("SCHEDULER", "Anterior: " + scheduler.getAlgorithmName(), Color.BLUE);
        }

        switch (algorithm) {
            case "FCFS":
                scheduler = new FCFSScheduler();
                setQuantumEnabled(false);
                setFeedbackSpinnersEnabled(false);
                break;

            case "SJF":
                scheduler = new SJFScheduler();
                setQuantumEnabled(false);
                setFeedbackSpinnersEnabled(false);
                break;

            case "SRTF":
                scheduler = new SRTFScheduler(true);
                setQuantumEnabled(false);
                setFeedbackSpinnersEnabled(false);
                break;

            case "RoundRobin":
                int quantum = (Integer) QuantumSpinner.getValue();
                scheduler = new RoundRobinScheduler(quantum);
                setQuantumEnabled(true);
                setFeedbackSpinnersEnabled(false);
                break;

            case "Priority":
                int agingThreshold = 10;
                scheduler = new PriorityScheduler(agingThreshold);
                setQuantumEnabled(false);
                setFeedbackSpinnersEnabled(false);
                break;

            case "HRRN":
                scheduler = new HRRNScheduler();
                setQuantumEnabled(false);
                setFeedbackSpinnersEnabled(false);
                break;

            case "Feedback":
                MultilevelFeedbackQueueScheduler mlfq = new MultilevelFeedbackQueueScheduler();
                mlfq.setQuantums(
                        (Integer) Nivel0Spinner.getValue(),
                        (Integer) Nivel1Spinner.getValue(),
                        (Integer) Nivel2Spinner.getValue(),
                        (Integer) Nivel3Spinner.getValue());
                scheduler = mlfq;
                setQuantumEnabled(false);
                setFeedbackSpinnersEnabled(true);
                break;

            default:
                scheduler = new FCFSScheduler();
                setQuantumEnabled(false);
                setFeedbackSpinnersEnabled(false);
        }

        // Migrar procesos al nuevo scheduler
        if (isMigrating && processesToMigrate != null) {
            logEvent("SCHEDULER", "Nuevo: " + scheduler.getAlgorithmName(), Color.BLUE);

            // Detectar si el nuevo scheduler es MLFQ
            boolean isTargetMLFQ = (scheduler instanceof MultilevelFeedbackQueueScheduler);

            // Si va a MLFQ, migrar también procesos bloqueados
            if (isTargetMLFQ) {
                MultilevelFeedbackQueueScheduler mlfq = (MultilevelFeedbackQueueScheduler) scheduler;

                // Migrar procesos bloqueados del IOManager al blockedQueue de MLFQ
                Queue<Process> blockedInIO = ioManager.getBlockedQueue();
                int blockedCount = blockedInIO.size();

                for (int i = 0; i < blockedCount; i++) {
                    Process p = blockedInIO.dequeue();
                    mlfq.getBlockedQueue().enqueue(p);
                    logEvent("MIGRACIÓN", "P" + p.getPid() + " bloqueado migrado a MLFQ", new Color(156, 39, 176));
                }

                logEvent("SCHEDULER",
                        "Migrando " + processesToMigrate.getSize() + " READY + " + blockedCount + " BLOCKED...",
                        Color.BLUE);
            } else {
                logEvent("SCHEDULER", "Migrando " + processesToMigrate.getSize() + " procesos...", Color.BLUE);
            }

            // Migrar procesos READY
            for (int i = 0; i < processesToMigrate.getSize(); i++) {
                Process p = processesToMigrate.get(i);
                if (p.getState() == ProcessState.READY) {

                    // Método especial para MLFQ
                    if (isTargetMLFQ) {
                        ((MultilevelFeedbackQueueScheduler) scheduler).addMigratedProcess(p);
                    } else {
                        scheduler.addProcess(p);
                    }

                    logEvent("MIGRACIÓN", "P" + p.getPid() + " migrado al nuevo scheduler", new Color(103, 58, 183));
                }
            }

            // Si había proceso en CPU, liberarlo y agregarlo a READY
            if (currentProcess != null && !currentProcess.isFinished()) {
                cpu.releaseProcess();
                currentProcess.setState(ProcessState.READY);

                // Método especial para MLFQ
                if (isTargetMLFQ) {
                    ((MultilevelFeedbackQueueScheduler) scheduler).addMigratedProcess(currentProcess);
                } else {
                    scheduler.addProcess(currentProcess);
                }

                lastExecutedProcess = null;
                logEvent("MIGRACIÓN", "P" + currentProcess.getPid() + " liberado de CPU y migrado",
                        new Color(103, 58, 183));
            }

            logEvent("SCHEDULER", "═══════════════════════════════════════", Color.ORANGE);
        }
    }

    // ===== HABILITAR/DESHABILITAR CONTROLES =====
    private void setQuantumEnabled(boolean enabled) {
        QuantumSpinner.setEnabled(enabled);
    }

    private void setFeedbackSpinnersEnabled(boolean enabled) {
        Nivel0Spinner.setEnabled(enabled);
        Nivel1Spinner.setEnabled(enabled);
        Nivel2Spinner.setEnabled(enabled);
        Nivel3Spinner.setEnabled(enabled);
    }

    // ===== MÉTODO PARA AGREGAR RESULTADOS A LA TABLA =====
    public void addResultToTable(Process p) {
        Object[] row = {
                p.getPid(),
                p.getName(),
                p.getArrivalTime(),
                p.getBurstTime(),
                p.getCompletionTime(),
                p.getTurnaroundTime(),
                p.getWaitingTime(),
                p.getResponseTime()
        };
        modeloTablaResultados.addRow(row);
    }

    // ===== MÉTODO PARA LIMPIAR LA TABLA =====
    public void clearResultsTable() {
        modeloTablaResultados.setRowCount(0);
    }

    private void iniciarSimulacion() {
        if (allProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe crear al menos un proceso antes de iniciar la simulación",
                    "Sin Procesos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (simulationTimer != null && simulationTimer.isRunning()) {
            JOptionPane.showMessageDialog(this,
                    "La simulación ya está en ejecución",
                    "Simulación Activa",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Reiniciar métricas
        clearResultsTable();
        terminatedProcesses = new Lista<>();
        globalClock = 0;

        // Establecer todos los procesos como NEW
        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            p.setState(ProcessState.NEW);
            p.setRemainingTime(p.getBurstTime());
            p.setProgramCounter(0);
        }

        // Crear scheduler según algoritmo seleccionado
        createScheduler();

        // Procesar procesos con AT=0 ANTES de iniciar timer
        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            if (p.getArrivalTime() == 0 && p.getState() == ProcessState.NEW) {
                p.setState(ProcessState.READY);
                scheduler.addProcess(p);
                System.out.println("🕐 Ciclo 0: P" + p.getPid() + " LLEGÓ (AT=" + p.getArrivalTime() + ")");
            }
        }

        // Configurar velocidad de simulación
        simulationSpeed = 1001 - VelocidadSlider.getValue();

        // Iniciar timer
        simulationTimer = new Timer(simulationSpeed, e -> ejecutarCiclo());
        simulationTimer.start();
        isPaused = false;

        JOptionPane.showMessageDialog(this,
                "Simulación iniciada con algoritmo: " + AlgorithmSelectorComboBox.getSelectedItem(),
                "Simulación Iniciada",
                JOptionPane.INFORMATION_MESSAGE);

        logEvent("SISTEMA", "═══════════════════════════════════════", Color.BLACK);
        logEvent("SISTEMA", "SIMULACIÓN INICIADA", Color.GREEN);
        logEvent("SISTEMA", "Algoritmo: " + AlgorithmSelectorComboBox.getSelectedItem(), Color.BLUE);
        logEvent("SISTEMA", "Total de procesos: " + allProcesses.getSize(), Color.BLUE);
        logEvent("SISTEMA", "═══════════════════════════════════════", Color.BLACK);

    }

    private void pausarReanudarSimulacion() {
        if (simulationTimer == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay ninguna simulación en ejecución",
                    "Advertencia",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (isPaused) {
            simulationTimer.start();
            isPaused = false;
            PausarButton.setText("⏸ Pausar");
        } else {
            simulationTimer.stop();
            isPaused = true;
            PausarButton.setText("▶ Reanudar");
        }
    }

    private void detenerSimulacion() {
        if (simulationTimer != null) {
            simulationTimer.stop();
            simulationTimer = null;
        }

        isPaused = false;
        PausarButton.setText("⏸ Pausar");

        logEvent("SISTEMA", "═══════════════════════════════════════", Color.BLACK);
        logEvent("SISTEMA", "SIMULACIÓN DETENIDA MANUALMENTE", Color.RED);
        logEvent("SISTEMA", String.format("Ciclos totales: %d", globalClock), Color.BLUE);
        logEvent("SISTEMA", String.format("Procesos terminados: %d/%d",
                terminatedProcesses.getSize(), allProcesses.getSize()), Color.BLUE);

        // ✅ NUEVO: Contar procesos en cada estado
        int ready = 0, blocked = 0, suspended = 0, running = 0, newState = 0;
        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            switch (p.getState()) {
                case READY:
                    ready++;
                    break;
                case BLOCKED:
                    blocked++;
                    break;
                case SUSPENDED:
                    suspended++;
                    break;
                case RUNNING:
                    running++;
                    break;
                case NEW:
                    newState++;
                    break;
            }
        }

        logEvent("SISTEMA", String.format("Estado al detener: READY=%d, RUNNING=%d, BLOCKED=%d, SUSPENDED=%d, NEW=%d",
                ready, running, blocked, suspended, newState), Color.BLUE);

        // Listar procesos que nunca ejecutaron
        if (newState > 0) {
            StringBuilder neverExecuted = new StringBuilder("Procesos que nunca ejecutaron: ");
            for (int i = 0; i < allProcesses.getSize(); i++) {
                Process p = allProcesses.get(i);
                if (p.getState() == ProcessState.NEW) {
                    neverExecuted.append(String.format("P%d (AT=%d) ", p.getPid(), p.getArrivalTime()));
                }
            }
            logEvent("ADVERTENCIA", neverExecuted.toString(), new Color(255, 152, 0));
        }

        logEvent("SISTEMA", "═══════════════════════════════════════", Color.BLACK);

        sincronizarTablaResultados();

        // Solo mostrar resultados si hay procesos terminados
        if (terminatedProcesses.getSize() > 0) {
            mostrarResultadosFinales();
        }
    }

    // Sincronizar tabla con procesos terminados
    private void sincronizarTablaResultados() {
        // Limpiar tabla actual
        clearResultsTable();

        // Agregar TODOS los procesos terminados a la tabla
        for (int i = 0; i < terminatedProcesses.getSize(); i++) {
            Process p = terminatedProcesses.get(i);
            addResultToTable(p);
        }

        logEvent("TABLA", String.format("Tabla actualizada: %d procesos agregados", terminatedProcesses.getSize()),
                Color.BLUE);
    }

    private void reiniciarSimulacion() {
        if (simulationTimer != null) {
            simulationTimer.stop();
            simulationTimer = null;
        }

        // Reiniciar todo
        globalClock = 0;
        isPaused = false;
        nextPID = 1;
        allProcesses = new Lista<>();
        terminatedProcesses = new Lista<>();
        clearResultsTable();

        // Limpiar áreas de texto
        ActivosTextArea.setText("");
        BloqueadosTextArea.setText("");
        TerminadosTextArea.setText("");
        SuspendidosTextArea.setText("");

        // Limpiar CPU display
        ProcesoResponseText.setText("--");
        PIDResponseText.setText("--");
        PCResponseText.setText("--");
        MARResponseText.setText("--");
        TotalResponseText.setText("--");
        CicloActualResponseText.setText("--");
        ModoResponseText.setText("--");

        PausarButton.setText("⏸ Pausar");

        clearCharts();

        clearLog();

        JOptionPane.showMessageDialog(this,
                "Simulación reiniciada",
                "Reiniciar",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ===== LIMPIAR GRÁFICOS =====
    private void clearCharts() {
        // Limpiar series del sistema
        if (seriesThroughput != null) {
            seriesThroughput.clear();
            seriesCPU.clear();
            seriesAvgRT.clear();
            seriesFairness.clear();
        }

        // Limpiar series de procesos
        if (processSeriesMap != null) {
            processSeriesMap.clear();
        }

        if (datasetProcesses != null) {
            datasetProcesses.removeAllSeries();
        }
    }

    // ===== CICLO DE SIMULACIÓN CON DEBUGGING =====

    private void ejecutarCiclo() {
        // Detectar si es MLFQ
        boolean isMLFQ = (scheduler instanceof MultilevelFeedbackQueueScheduler);

        // Crear una copia temporal para evitar problemas de concurrencia
        int currentSize = allProcesses.getSize();
        for (int i = 0; i < currentSize; i++) {
            Process p = allProcesses.get(i);

            if (p.getArrivalTime() == globalClock && p.getState() == ProcessState.NEW) {
                final Process arrivingProcess = p;
                showKernelModeForEvent("Proceso P" + p.getPid() + " llegando al sistema", () -> {
                    arrivingProcess.setState(ProcessState.READY);
                    scheduler.addProcess(arrivingProcess);

                    // Log de llegada
                    logEvent("LLEGADA", String.format("P%d llegó al sistema (AT=%d, BT=%d)",
                            arrivingProcess.getPid(), arrivingProcess.getArrivalTime(), arrivingProcess.getBurstTime()),
                            new Color(0, 150, 136));
                    logSchedulerDecision(String.format("P%d agregado a cola READY", arrivingProcess.getPid()));

                    System.out.println(
                            "🕐 Ciclo " + globalClock + ": P" + arrivingProcess.getPid() + " LLEGÓ (AT="
                                    + arrivingProcess.getArrivalTime() + ")");
                });
            }
        }

        // Incrementar ciclo
        globalClock++;

        checkMemoryPressure();

        boolean shouldLog = (globalClock % 50 == 0);

        if (shouldLog) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🕐 CICLO " + globalClock);
            System.out.println("=".repeat(60));
            System.out
                    .println("Progreso: " + terminatedProcesses.getSize() + "/" + allProcesses.getSize()
                            + " terminados");
        }

        // Si es MLFQ, procesar su lógica interna de ciclo
        if (isMLFQ) {
            ((MultilevelFeedbackQueueScheduler) scheduler).processSingleCycle(globalClock);
        }

        // Procesar I/O (solo si NO es MLFQ, porque MLFQ lo hace internamente)
        if (!isMLFQ) {
            int blockedCount = ioManager.getBlockedCount();
            Queue<Process> unblocked = ioManager.processIOCycle();

            while (!unblocked.isEmpty()) {
                Process p = unblocked.dequeue();

                final Process unblockedProcess = p;
                showKernelModeForEvent("Proceso P" + p.getPid() + " completó I/O", () -> {
                    unblockedProcess.setState(ProcessState.READY);
                    scheduler.addProcess(unblockedProcess);

                    // Log de I/O completado
                    logIOActivity(String.format("P%d completó operación I/O", unblockedProcess.getPid()));
                    logProcessStateChange(unblockedProcess.getPid(), "BLOCKED", "READY");
                    logSchedulerDecision(String.format("P%d devuelto a cola READY", unblockedProcess.getPid()));

                    if (shouldLog) {
                        System.out.println("  🔓 P" + unblockedProcess.getPid() + " desbloqueado de I/O → READY");
                    }
                });
            }

            if (shouldLog && blockedCount > 0) {
                System.out.println("  💾 I/O: " + blockedCount + " bloqueados");
            }
        }

        // Seleccionar proceso
        Process currentProcess = cpu.getCurrentProcess();

        if (currentProcess == null) {
            Process nextProcess = scheduler.selectNextProcess();

            if (nextProcess != null) {
                boolean isContextSwitch = (lastExecutedProcess != null && lastExecutedProcess != nextProcess);

                if (isContextSwitch) {
                    final Process selectedProcess = nextProcess;
                    showKernelModeForEvent(
                            "Context Switch: P" + lastExecutedProcess.getPid() + " → P" + selectedProcess.getPid(),
                            () -> {
                                cpu.assignProcess(selectedProcess);
                                selectedProcess.setState(ProcessState.RUNNING);

                                logKernelMode(String.format("Context Switch: P%d → P%d",
                                        lastExecutedProcess.getPid(), selectedProcess.getPid()));
                                logSchedulerDecision(String.format("CPU asignado a P%d (RT=%d)",
                                        selectedProcess.getPid(), selectedProcess.getRemainingTime()));
                                logProcessStateChange(selectedProcess.getPid(), "READY", "RUNNING");

                                if (shouldLog) {
                                    System.out.println("  🔄 Context switch: P" + lastExecutedProcess.getPid() + " → P"
                                            + selectedProcess.getPid());
                                }
                            });

                    executeProcessAfterKernel(nextProcess, shouldLog, isMLFQ);
                    return;
                }

                cpu.assignProcess(nextProcess);
                nextProcess.setState(ProcessState.RUNNING);
                lastExecutedProcess = nextProcess;

                logSchedulerDecision(String.format("Procesador selecciona P%d (RT=%d)",
                        nextProcess.getPid(), nextProcess.getRemainingTime()));
                logCPUActivity(String.format("P%d asignado a CPU", nextProcess.getPid()));
                logProcessStateChange(nextProcess.getPid(), "READY", "RUNNING");

                if (shouldLog) {
                    System.out.println("  ⚙️ P" + nextProcess.getPid() + " asignado a CPU (RT="
                            + nextProcess.getRemainingTime() + ")");
                }
            } else {
                cpu.tickIdle();
                lastExecutedProcess = null;

                if (globalClock % 10 == 0) {
                    logCPUActivity("CPU en estado IDLE - No hay procesos listos");
                }

                if (shouldLog) {
                    System.out.println("  💤 CPU IDLE - No hay procesos en READY");
                }

                updateUIWithMode("User");
            }
        }

        currentProcess = cpu.getCurrentProcess();

        if (currentProcess != null) {
            updateUIWithMode("User");

            boolean needsIO = !currentProcess.execute(globalClock);
            cpu.executeCycle();

            if (shouldLog) {
                System.out.println("  ⚙️ P" + currentProcess.getPid() + " ejecutando (RT="
                        + currentProcess.getRemainingTime() + ")");
            }

            handleProcessStateChange(currentProcess, needsIO, shouldLog, isMLFQ);
        }

        if (!isMLFQ) {
            for (int i = 0; i < allProcesses.getSize(); i++) {
                Process p = allProcesses.get(i);
                if (p.getState() == ProcessState.READY) {
                    p.incrementWaitingTime();
                }
            }
        }

        if (shouldLog) {
            System.out.println("\n📋 ESTADO DE TODOS LOS PROCESOS:");
            System.out.println("  " + "-".repeat(70));
            System.out.printf("  %-6s %-10s %-12s %-6s %-10s\n", "PID", "Nombre", "Estado", "RT", "I/O Rem");
            System.out.println("  " + "-".repeat(70));

            for (int i = 0; i < allProcesses.getSize(); i++) {
                Process p = allProcesses.get(i);
                System.out.printf("  %-6d %-10s %-12s %-6d %-10d\n",
                        p.getPid(), p.getName(), p.getState(), p.getRemainingTime(), p.getIoRemaining());
            }
            System.out.println("  " + "-".repeat(70));
        }

        checkSimulationEnd();
    }

    private void checkMemoryPressure() {
        int maxProcessesInMemory = (Integer) ProcesosEnMemoriaSpinner.getValue();

        int processesInMemory = 0;
        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            if (p.getState() == ProcessState.READY ||
                    p.getState() == ProcessState.RUNNING) {
                processesInMemory++;
            }
        }

        final int memoryCount = processesInMemory;

        int suspendThreshold = maxProcessesInMemory * 2;

        // SUSPENDER solo si hay sobrecarga EXTREMA
        if (processesInMemory > suspendThreshold) {
            Process toSuspend = findLowestPriorityReadyProcess();

            if (toSuspend != null) {
                showKernelModeForEvent("Suspendiendo P" + toSuspend.getPid() + " por falta de memoria", () -> {
                    ProcessState previousState = toSuspend.getState();
                    toSuspend.setState(ProcessState.SUSPENDED);

                    // Si estaba en CPU, liberarlo
                    if (previousState == ProcessState.RUNNING) {
                        cpu.releaseProcess();
                        lastExecutedProcess = null;
                    }

                    logKernelMode(String.format("P%d suspendido por presión de memoria (%d/%d en RAM)",
                            toSuspend.getPid(), memoryCount, maxProcessesInMemory));
                    logProcessStateChange(toSuspend.getPid(), previousState.toString(), "SUSPENDED");
                });
            }
        }

        else if (processesInMemory < maxProcessesInMemory / 2) {
            Process toResume = findSuspendedProcess();

            if (toResume != null) {
                showKernelModeForEvent("Reanudando P" + toResume.getPid() + " desde suspensión", () -> {
                    toResume.setState(ProcessState.READY);
                    scheduler.addProcess(toResume);

                    logKernelMode(String.format("P%d reanudado desde suspensión", toResume.getPid()));
                    logProcessStateChange(toResume.getPid(), "SUSPENDED", "READY");
                });
            }
        }
    }

    private Process findLowestPriorityReadyProcess() {
        Process lowest = null;
        int lowestPriority = -1;

        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);

            // Solo considerar procesos READY
            if (p.getState() == ProcessState.READY) {
                if (lowest == null || p.getPriority() > lowestPriority) {
                    lowest = p;
                    lowestPriority = p.getPriority();
                }
            }
        }

        return lowest;
    }

    private Process findSuspendedProcess() {
        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            if (p.getState() == ProcessState.SUSPENDED) {
                return p;
            }
        }
        return null;
    }

    private void showKernelModeForEvent(String eventDescription, Runnable action) {
        // Pausar simulación
        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        // Mostrar modo Kernel
        updateUIWithMode("Kernel");
        System.out.println("  🔧 Modo KERNEL: " + eventDescription);

        // EJECUTAR LA ACCIÓN INMEDIATAMENTE (cambiar estado del proceso)
        action.run();

        // Timer SOLO para mantener visible el modo Kernel
        Timer kernelTimer = new Timer(200, evt -> {
            // Volver a modo User
            updateUIWithMode("User");

            // Reanudar simulación
            if (!isPaused && simulationTimer != null) {
                simulationTimer.start();
            }
        });

        kernelTimer.setRepeats(false);
        kernelTimer.start();
    }

    // Ejecutar proceso después de context switch
    private void executeProcessAfterKernel(Process process, boolean shouldLog, boolean isMLFQ) {
        Timer executeTimer = new Timer(100, evt -> {
            updateUIWithMode("User");

            boolean needsIO = !process.execute(globalClock);
            cpu.executeCycle();
            lastExecutedProcess = process;

            handleProcessStateChange(process, needsIO, shouldLog, isMLFQ);

            if (!isMLFQ) {
                for (int i = 0; i < allProcesses.getSize(); i++) {
                    Process p = allProcesses.get(i);
                    if (p.getState() == ProcessState.READY) {
                        p.incrementWaitingTime();
                    }
                }
            }

            checkSimulationEnd();

            if (!isPaused && simulationTimer != null) {
                simulationTimer.start();
            }
        });

        executeTimer.setRepeats(false);
        executeTimer.start();
    }

    // Manejar cambios de estado según el algoritmo
    private void handleProcessStateChange(Process process, boolean needsIO, boolean shouldLog, boolean isMLFQ) {
        if (process.isFinished()) {
            showKernelModeForEvent("Proceso P" + process.getPid() + " terminando", () -> {
                process.setState(ProcessState.TERMINATED);
                process.setCompletionTime(globalClock);
                process.calculateMetrics(globalClock);
                terminatedProcesses.insertBegin(process);
                addResultToTable(process);
                cpu.releaseProcess();
                lastExecutedProcess = null;

                logKernelMode(String.format("P%d finalizó su ejecución", process.getPid()));
                logProcessStateChange(process.getPid(), "RUNNING", "TERMINATED");
                logEvent("MÉTRICAS", String.format("P%d - TAT=%d, WT=%d, RT=%d",
                        process.getPid(),
                        process.getTurnaroundTime(),
                        process.getWaitingTime(),
                        process.getResponseTime()), new Color(103, 58, 183));

                if (isMLFQ) {
                    ((MultilevelFeedbackQueueScheduler) scheduler).onProcessFinished(process);
                }

                if (shouldLog) {
                    System.out.println("   P" + process.getPid() + " TERMINADO en ciclo " + globalClock);
                }
            });
        } else if (needsIO) {
            showKernelModeForEvent("Proceso P" + process.getPid() + " bloqueándose por I/O", () -> {
                process.setState(ProcessState.BLOCKED);

                logKernelMode(String.format("P%d requiere operación I/O", process.getPid()));
                logProcessStateChange(process.getPid(), "RUNNING", "BLOCKED");
                logIOActivity(String.format("P%d bloqueado por I/O (duración: %d ciclos)",
                        process.getPid(), process.getIoRemaining()));

                if (isMLFQ) {
                    ((MultilevelFeedbackQueueScheduler) scheduler).onProcessBlocked(process);
                } else {
                    ioManager.blockProcess(process);
                }

                cpu.releaseProcess();
                lastExecutedProcess = null;

                if (shouldLog) {
                    System.out.println("  🚫 P" + process.getPid() + " BLOQUEADO por I/O (durará "
                            + process.getIoRemaining() + " ciclos)");
                }
            });
        }
    }

    // Método auxiliar para verificar fin de simulación
    private void checkSimulationEnd() {

        if (globalClock >= 10000) {
            System.out.println("\n⚠️ LÍMITE DE CICLOS ALCANZADO (10,000)");
            System.out
                    .println("   Procesos terminados: " + terminatedProcesses.getSize() + "/" + allProcesses.getSize());

            logEvent("SISTEMA", "═══════════════════════════════════════", Color.RED);
            logEvent("SISTEMA", "LÍMITE DE CICLOS ALCANZADO", Color.RED);
            logEvent("SISTEMA",
                    String.format("Terminados: %d/%d", terminatedProcesses.getSize(), allProcesses.getSize()),
                    Color.ORANGE);
            logEvent("SISTEMA", "═══════════════════════════════════════", Color.RED);

            detenerSimulacion();
        }

        // NUEVO: Solo mostrar aviso cada 100 ciclos si todos terminaron pero sigue
        // corriendo
        if (globalClock % 100 == 0) {
            int processosQueLlegaron = 0;
            for (int i = 0; i < allProcesses.getSize(); i++) {
                if (allProcesses.get(i).getState() != ProcessState.NEW) {
                    processosQueLlegaron++;
                }
            }

            if (processosQueLlegaron == allProcesses.getSize() &&
                    terminatedProcesses.getSize() >= allProcesses.getSize()) {
                logEvent("INFO",
                        String.format(
                                "Todos los procesos terminados (Ciclo %d). Esperando nuevos procesos o botón Detener.",
                                globalClock),
                        new Color(0, 150, 136));
            }
        }
    }

    // ===== ACTUALIZAR INTERFAZ =====

    private void updateUI() {
        // Delegar a updateUIWithMode con modo "User" por defecto
        Process current = cpu.getCurrentProcess();
        if (current != null) {
            updateUIWithMode("User");
        } else {
            updateUIWithMode("Idle");
        }
    }

    private void updateUIWithMode(String mode) {
        // Actualizar info de CPU
        Process current = cpu.getCurrentProcess();

        if (current != null) {

            ProcesoResponseText.setText(current.getName());
            PIDResponseText.setText(String.valueOf(current.getPid()));
            PCResponseText.setText(String.valueOf(current.getProgramCounter()));
            MARResponseText.setText(String.valueOf(current.getMemoryAddressRegister()));
            TotalResponseText.setText(current.getRemainingTime() + "/" + current.getBurstTime());
            CicloActualResponseText.setText(String.valueOf(globalClock));
        } else {

            ProcesoResponseText.setText("--");
            PIDResponseText.setText("--");
            PCResponseText.setText("--");
            MARResponseText.setText("--");
            TotalResponseText.setText("--");
            CicloActualResponseText.setText(String.valueOf(globalClock));
        }

        ModoResponseText.setText(mode);

        switch (mode) {
            case "User":
                ModoResponseText.setForeground(new Color(76, 175, 80)); // Verde
                break;
            case "Kernel":
                ModoResponseText.setForeground(new Color(255, 152, 0)); // Naranja
                break;
            default:
                ModoResponseText.setForeground(new Color(76, 175, 80)); // Verde por defecto
        }

        // Actualizar estados de procesos
        updateProcessStates();

        updateDetalleColasPanel();

        // Actualizar gráficos
        updateCharts();
    }

    private void updateProcessStates() {
        StringBuilder activos = new StringBuilder();
        StringBuilder bloqueados = new StringBuilder();
        StringBuilder terminados = new StringBuilder();
        StringBuilder suspendidos = new StringBuilder();

        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            String info = String.format("P%d (%s) - RT:%d\n",
                    p.getPid(), p.getName(), p.getRemainingTime());

            switch (p.getState()) {
                case READY:
                case RUNNING:
                    activos.append(info);
                    break;
                case BLOCKED:
                    bloqueados.append(info);
                    break;
                case TERMINATED:
                    terminados.append(info);
                    break;
                case SUSPENDED:
                    suspendidos.append(info);
                    break;
            }
        }

        ActivosTextArea.setText(activos.toString());
        BloqueadosTextArea.setText(bloqueados.toString());
        TerminadosTextArea.setText(terminados.toString());
        SuspendidosTextArea.setText(suspendidos.toString());
    }

    // ===== ACTUALIZAR DETALLE DE COLAS =====
    private void updateDetalleColasPanel() {
        StringBuilder detalle = new StringBuilder();

        // Encabezado
        detalle.append("═".repeat(40)).append("\n");
        detalle.append("  DETALLE DE COLAS - Ciclo ").append(globalClock).append("\n");
        detalle.append("═".repeat(40)).append("\n\n");

        // Detectar si es un algoritmo especial
        boolean isMLFQ = (scheduler instanceof MultilevelFeedbackQueueScheduler);
        boolean isRR = (scheduler instanceof RoundRobinScheduler);
        boolean isPriority = (scheduler instanceof PriorityScheduler);

        if (isMLFQ) {
            // MULTILEVEL FEEDBACK QUEUE
            MultilevelFeedbackQueueScheduler mlfq = (MultilevelFeedbackQueueScheduler) scheduler;

            detalle.append("┌─ COLA NIVEL 0 (Quantum: ").append(mlfq.getQuantums()[0]).append(") ─┐\n");
            appendQueueDetails(detalle, mlfq.getQueue0());

            detalle.append("\n┌─ COLA NIVEL 1 (Quantum: ").append(mlfq.getQuantums()[1]).append(") ─┐\n");
            appendQueueDetails(detalle, mlfq.getQueue1());

            detalle.append("\n┌─ COLA NIVEL 2 (Quantum: ").append(mlfq.getQuantums()[2]).append(") ─┐\n");
            appendQueueDetails(detalle, mlfq.getQueue2());

            detalle.append("\n┌─ COLA NIVEL 3 (Quantum: ").append(mlfq.getQuantums()[3]).append(") ─┐\n");
            appendQueueDetails(detalle, mlfq.getQueue3());

            detalle.append("\n┌─ COLA NEW (Esperando admisión) ─┐\n");
            appendQueueDetails(detalle, mlfq.getNewQueue());

            detalle.append("\n┌─ COLA BLOCKED (En I/O) ─────────┐\n");
            appendQueueDetails(detalle, mlfq.getBlockedQueue());

        } else {
            // ALGORITMOS ESTÁNDAR (FCFS, SJF, SRTF, RR, Priority, HRRN)

            detalle.append("─ COLA LISTOS ────────────────────────\n");
            Lista<Process> readyQueue = getReadyQueueFromScheduler();
            if (readyQueue != null) {
                appendListDetails(detalle, readyQueue);
            } else {
                detalle.append("  (No disponible para este algoritmo)\n");
            }

            detalle.append("\n─ COLA BLOQUEADOS (En I/O) ─────────\n");
            Queue<Process> blockedQueue = ioManager.getBlockedQueue();
            appendQueueDetails(detalle, blockedQueue);

            // Información adicional según algoritmo
            if (isRR) {
                RoundRobinScheduler rr = (RoundRobinScheduler) scheduler;
                detalle.append("\nQuantum: ").append(rr.getQuantum()).append(" ciclos\n");
            } else if (isPriority) {
                detalle.append("\nAging activado cada 10 ciclos\n");
            }
        }

        // Actualizar el TextArea
        DetalleColasTextArea.setText(detalle.toString());
    }

    // ===== MÉTODO AUXILIAR: Agregar detalles de una Queue =====
    private void appendQueueDetails(StringBuilder sb, Queue<Process> queue) {
        if (queue == null || queue.isEmpty()) {
            sb.append("  (Vacía)\n");
            return;
        }

        int count = 0;
        Queue<Process> temp = new Queue<>();

        while (!queue.isEmpty() && count < 10) { // Limitar a 10 procesos
            Process p = queue.dequeue();
            sb.append(String.format("  %d. P%-3d | RT:%-3d | Pri:%-2d | %s\n",
                    count + 1,
                    p.getPid(),
                    p.getRemainingTime(),
                    p.getPriority(),
                    p.getName()));
            temp.enqueue(p);
            count++;
        }

        // Restaurar cola
        while (!temp.isEmpty()) {
            queue.enqueue(temp.dequeue());
        }

        if (queue.size() > 10) {
            sb.append("  ... (").append(queue.size() - 10).append(" más)\n");
        }
    }

    // ===== MÉTODO AUXILIAR: Agregar detalles de una LinkedList =====
    private void appendListDetails(StringBuilder sb, Lista<Process> list) {
        if (list == null || list.isEmpty()) {
            sb.append("  (Vacía)\n");
            return;
        }

        int max = Math.min(list.getSize(), 10);
        for (int i = 0; i < max; i++) {
            Process p = list.get(i);
            sb.append(String.format("  %d. P%-3d | RT:%-3d | Pri:%-2d | %s\n",
                    i + 1,
                    p.getPid(),
                    p.getRemainingTime(),
                    p.getPriority(),
                    p.getName()));
        }

        if (list.getSize() > 10) {
            sb.append("  ... (").append(list.getSize() - 10).append(" más)\n");
        }
    }

    // ===== MÉTODO AUXILIAR: Obtener cola READY del scheduler =====
    private Lista<Process> getReadyQueueFromScheduler() {
        if (scheduler instanceof FCFSScheduler) {
            return ((FCFSScheduler) scheduler).getReadyQueue();
        } else if (scheduler instanceof SJFScheduler) {
            return ((SJFScheduler) scheduler).getReadyQueue();
        } else if (scheduler instanceof SRTFScheduler) {
            return ((SRTFScheduler) scheduler).getReadyQueue();
        } else if (scheduler instanceof RoundRobinScheduler) {
            return ((RoundRobinScheduler) scheduler).getReadyQueue();
        } else if (scheduler instanceof PriorityScheduler) {
            return ((PriorityScheduler) scheduler).getReadyQueue();
        } else if (scheduler instanceof HRRNScheduler) {
            return ((HRRNScheduler) scheduler).getReadyQueue();
        }
        return null;
    }

    // ===== ACTUALIZAR GRÁFICOS =====
    private void updateCharts() {
        // 1. ACTUALIZAR MÉTRICAS DEL SISTEMA
        updateSystemMetrics();

        // 2. ACTUALIZAR LÍNEA DE TIEMPO DE PROCESOS
        updateProcessTimeline();
    }

    private void updateSystemMetrics() {
        // Calcular métricas
        double throughput = terminatedProcesses.getSize() / Math.max((double) globalClock, 1.0);
        double cpuUtil = cpu.getUtilization();
        double avgRT = 0;
        double fairness = 0;

        // Calcular Avg RT solo si hay procesos activos
        if (allProcesses.getSize() > 0) {
            avgRT = Statistics.calculateAverageResponseTime(allProcesses);
            fairness = Statistics.calculateFairness(allProcesses);
        }

        // Normalizar valores (0-1)
        double normalizedThroughput = Math.min(throughput / 0.2, 1.0); // Max 0.2 procesos/ciclo = 1.0
        double normalizedCPU = cpuUtil / 100.0;
        double normalizedRT = Math.min(avgRT / 20.0, 1.0); // Max 20 ciclos = 1.0
        double normalizedFairness = fairness; // Ya está en 0-1

        // Agregar puntos al gráfico
        seriesThroughput.add(globalClock, normalizedThroughput);
        seriesCPU.add(globalClock, normalizedCPU);
        seriesAvgRT.add(globalClock, normalizedRT);
        seriesFairness.add(globalClock, normalizedFairness);

        // Limitar cantidad de puntos (opcional, para rendimiento)
        if (seriesThroughput.getItemCount() > 500) {
            seriesThroughput.remove(0);
            seriesCPU.remove(0);
            seriesAvgRT.remove(0);
            seriesFairness.remove(0);
        }
    }

    private void updateProcessTimeline() {
        // Actualizar línea de cada proceso
        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            int pid = p.getPid();

            // Crear serie si no existe
            if (!processSeriesMap.containsKey(pid)) {
                XYSeries series = new XYSeries("P" + pid);
                processSeriesMap.put(pid, series);
                datasetProcesses.addSeries(series);

                // Asignar color único
                Color color = generateColorForProcess(pid);
                processColorMap.put(pid, color);

                // Aplicar color al renderer
                int seriesIndex = datasetProcesses.getSeriesCount() - 1;
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chartProcesses.getXYPlot().getRenderer();
                renderer.setSeriesPaint(seriesIndex, color);
            }

            // Agregar punto según estado actual
            XYSeries series = processSeriesMap.get(pid);
            double stateValue = getProcessStateValue(p);
            series.add(globalClock, stateValue);

            // Limitar puntos por serie
            if (series.getItemCount() > 500) {
                series.remove(0);
            }
        }
    }

    // ===== CONVERTIR ESTADO A VALOR NUMÉRICO =====
    private double getProcessStateValue(Process p) {
        switch (p.getState()) {
            case RUNNING:
                return 1.0; // Ejecutando
            case READY:
                return 0.5; // Listo
            case BLOCKED:
                return 0.0; // Bloqueado
            case TERMINATED:
                return -0.1; // Terminado (fuera del gráfico)
            case NEW:
                return 0.3; // Nuevo (esperando)
            case SUSPENDED:
                return 0.2; // Suspendido
            default:
                return 0.3;
        }
    }

    // ===== GENERAR COLOR ÚNICO POR PROCESO =====
    private Color generateColorForProcess(int pid) {
        // Paleta de colores vibrantes
        Color[] palette = {
                new Color(33, 150, 243), // Azul
                new Color(76, 175, 80), // Verde
                new Color(255, 152, 0), // Naranja
                new Color(244, 67, 54), // Rojo
                new Color(156, 39, 176), // Púrpura
                new Color(0, 188, 212), // Cian
                new Color(255, 193, 7), // Amarillo
                new Color(121, 85, 72), // Marrón
                new Color(96, 125, 139), // Gris Azulado
                new Color(233, 30, 99), // Rosa
                new Color(139, 195, 74), // Lima
                new Color(255, 87, 34), // Naranja Profundo
                new Color(103, 58, 183), // Índigo
                new Color(0, 150, 136), // Verde Azulado
                new Color(255, 235, 59) // Amarillo Brillante
        };

        return palette[(pid - 1) % palette.length];
    }

    private void mostrarResultadosFinales() {
        if (terminatedProcesses.isEmpty()) {
            return;
        }

        // ✅ NUEVO: Asegurar que la tabla esté sincronizada
        sincronizarTablaResultados();

        // ✅ NUEVO: Contar procesos que ejecutaron (salieron de NEW)
        int processesExecuted = 0;
        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            if (p.getState() != ProcessState.NEW) {
                processesExecuted++;
            }
        }

        double avgWT = Statistics.calculateAverageWaitingTime(terminatedProcesses);
        double avgTAT = Statistics.calculateAverageTurnaroundTime(terminatedProcesses);
        double avgRT = Statistics.calculateAverageResponseTime(terminatedProcesses);
        double cpuUtil = cpu.getUtilization();

        String report = String.format(
                "===== SIMULACIÓN FINALIZADA =====\n\n" +
                        "Algoritmo: %s\n" +
                        "Procesos en el sistema: %d\n" +
                        "Procesos que ejecutaron: %d\n" +
                        "Procesos terminados: %d\n" +
                        "Ciclos totales: %d\n\n" +
                        "===== MÉTRICAS (solo procesos terminados) =====\n" +
                        "Tiempo de Espera Promedio: %.2f\n" +
                        "Tiempo de Retorno Promedio: %.2f\n" +
                        "Tiempo de Respuesta Promedio: %.2f\n" +
                        "Utilización de CPU: %.2f%%\n",
                AlgorithmSelectorComboBox.getSelectedItem(),
                allProcesses.getSize(),
                processesExecuted,
                terminatedProcesses.getSize(),
                globalClock,
                avgWT, avgTAT, avgRT, cpuUtil);

        JOptionPane.showMessageDialog(this,
                report,
                "Resultados Finales",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ===== MÉTODO 1: MOSTRAR LISTA DE PROCESOS =====
    private void mostrarListaProcesos() {
        if (allProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay procesos cargados en el sistema",
                    "Lista Vacía",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Crear tabla para mostrar procesos
        String[] columnNames = { "PID", "Nombre", "AT", "BT", "Prioridad", "Tipo", "I/O Cycle", "I/O Dur", "Memoria" };
        Object[][] data = new Object[allProcesses.getSize()][9];

        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);
            data[i][0] = p.getPid();
            data[i][1] = p.getName();
            data[i][2] = p.getArrivalTime();
            data[i][3] = p.getBurstTime();
            data[i][4] = p.getPriority();
            data[i][5] = p.getType();
            data[i][6] = p.getIoCycle();
            data[i][7] = p.getIoDuration();
            data[i][8] = p.getMemorySize() + " KB";
        }

        JTable table = new JTable(data, columnNames);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        JOptionPane.showMessageDialog(this,
                scrollPane,
                "Lista de Procesos Cargados (" + allProcesses.getSize() + " procesos)",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ===== MÉTODO 2: IMPORTAR PROCESOS DESDE ARCHIVO =====
    private void importarProcesos() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar archivo de procesos");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos CSV (*.csv)", "csv"));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();

            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader(selectedFile))) {

                String line;
                int importedCount = 0;
                int lineNumber = 0;

                // Saltar la primera línea (encabezados)
                br.readLine();
                lineNumber++;

                while ((line = br.readLine()) != null) {
                    lineNumber++;

                    // Ignorar líneas vacías
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        String[] values = line.split(",");

                        if (values.length < 8) {
                            System.err.println("⚠️ Línea " + lineNumber + " ignorada: faltan columnas");
                            continue;
                        }

                        // Leer datos del CSV
                        String name = values[0].trim();
                        int arrivalTime = Integer.parseInt(values[1].trim());
                        int burstTime = Integer.parseInt(values[2].trim());
                        int priority = Integer.parseInt(values[3].trim());
                        String tipoStr = values[4].trim();
                        int ioCycle = Integer.parseInt(values[5].trim());
                        int ioDuration = Integer.parseInt(values[6].trim());
                        int memorySize = Integer.parseInt(values[7].trim());

                        // Convertir tipo
                        ProcessType tipo = ProcessType.CPU_BOUND;
                        if (tipoStr.equalsIgnoreCase("IO_BOUND") || tipoStr.contains("I/O")) {
                            tipo = ProcessType.IO_BOUND;
                        } else if (tipoStr.equalsIgnoreCase("MIXED") || tipoStr.contains("Mixto")) {
                            tipo = ProcessType.MIXED;
                        }

                        // Crear proceso
                        int currentPID = nextPID;
                        nextPID++;

                        Process process = new Process(
                                currentPID,
                                name.isEmpty() ? "P" + currentPID : name,
                                arrivalTime,
                                burstTime,
                                priority,
                                tipo,
                                ioCycle,
                                ioDuration,
                                memorySize);

                        allProcesses.insertBegin(process);
                        importedCount++;

                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ Línea " + lineNumber + " ignorada: formato numérico inválido");
                    } catch (Exception e) {
                        System.err.println("⚠️ Línea " + lineNumber + " ignorada: " + e.getMessage());
                    }
                }

                JOptionPane.showMessageDialog(this,
                        importedCount + " procesos importados exitosamente desde:\n" + selectedFile.getName(),
                        "Importación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error al leer el archivo:\n" + e.getMessage(),
                        "Error de Importación",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ===== MÉTODO 3: DESCARGAR PROCESOS A ARCHIVO CSV =====
    private void descargarProcesos() {
        if (allProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay procesos para descargar",
                    "Lista Vacía",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar lista de procesos");
        fileChooser.setSelectedFile(new java.io.File("procesos_" + System.currentTimeMillis() + ".csv"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Archivos CSV (*.csv)", "csv"));

        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();

            // Asegurar extensión .csv
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new java.io.File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (java.io.PrintWriter pw = new java.io.PrintWriter(fileToSave)) {

                // Escribir encabezados
                pw.println("Nombre,ArrivalTime,BurstTime,Priority,Type,IOCycle,IODuration,MemorySize");

                // Escribir cada proceso
                for (int i = 0; i < allProcesses.getSize(); i++) {
                    Process p = allProcesses.get(i);
                    pw.printf("%s,%d,%d,%d,%s,%d,%d,%d\n",
                            p.getName(),
                            p.getArrivalTime(),
                            p.getBurstTime(),
                            p.getPriority(),
                            p.getType(),
                            p.getIoCycle(),
                            p.getIoDuration(),
                            p.getMemorySize());
                }

                JOptionPane.showMessageDialog(this,
                        allProcesses.getSize() + " procesos guardados exitosamente en:\n" + fileToSave.getName(),
                        "Descarga Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error al guardar el archivo:\n" + e.getMessage(),
                        "Error de Guardado",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        SimuladorCPUText = new javax.swing.JLabel();
        PlanificadorText = new javax.swing.JLabel();
        FeedbackText = new javax.swing.JLabel();
        VelocidadText = new javax.swing.JLabel();
        ProcesosEnMemoriaText = new javax.swing.JLabel();
        QuantumText = new javax.swing.JLabel();
        QuantumSpinner = new javax.swing.JSpinner();
        ProcesosEnMemoriaSpinner = new javax.swing.JSpinner();
        VelocidadSlider = new javax.swing.JSlider();
        Nivel0Text = new javax.swing.JLabel();
        Nivel0Spinner = new javax.swing.JSpinner();
        Nivel1Text = new javax.swing.JLabel();
        Nivel1Spinner = new javax.swing.JSpinner();
        Nivel2Text = new javax.swing.JLabel();
        Nivel2Spinner = new javax.swing.JSpinner();
        Nivel3Text = new javax.swing.JLabel();
        Nivel3Spinner = new javax.swing.JSpinner();
        AlgorithmSelectorComboBox = new javax.swing.JComboBox<>();
        CreacionDeProcesosText = new javax.swing.JLabel();
        AgregarQuinceProcesosAleatoriosButton = new javax.swing.JButton();
        NombreText = new javax.swing.JLabel();
        NombreTextField = new javax.swing.JTextField();
        LlegadaCicloSpinner = new javax.swing.JSpinner();
        LlegadaCicloText = new javax.swing.JLabel();
        NroInstruccionesText = new javax.swing.JLabel();
        NroInstruccionesSpinner = new javax.swing.JSpinner();
        CicloDeIOSpinner = new javax.swing.JSpinner();
        CicloDeIOText = new javax.swing.JLabel();
        TipoText = new javax.swing.JLabel();
        TipoComboBox = new javax.swing.JComboBox<>();
        DuracionDeIOSpinner = new javax.swing.JSpinner();
        DuracionDeIOText = new javax.swing.JLabel();
        TamañoEnMemoriaText = new javax.swing.JLabel();
        TamañoEnMemoriaSpinner = new javax.swing.JSpinner();
        CPUText = new javax.swing.JLabel();
        CPUPanel = new javax.swing.JPanel();
        ProcesoText = new javax.swing.JLabel();
        PIDText = new javax.swing.JLabel();
        PCText = new javax.swing.JLabel();
        MARText = new javax.swing.JLabel();
        TotalText = new javax.swing.JLabel();
        CicloActualText = new javax.swing.JLabel();
        ModoText = new javax.swing.JLabel();
        ProcesoResponseText = new javax.swing.JLabel();
        PIDResponseText = new javax.swing.JLabel();
        PCResponseText = new javax.swing.JLabel();
        MARResponseText = new javax.swing.JLabel();
        TotalResponseText = new javax.swing.JLabel();
        CicloActualResponseText = new javax.swing.JLabel();
        ModoResponseText = new javax.swing.JLabel();
        EstadosDeProcesosPanel = new javax.swing.JPanel();
        EstadosDeProcesosText = new javax.swing.JLabel();
        ActivosText = new javax.swing.JLabel();
        BloqueadosText = new javax.swing.JLabel();
        TerminadosTExt = new javax.swing.JLabel();
        ActivosScrollPane = new javax.swing.JScrollPane();
        ActivosTextArea = new javax.swing.JTextArea();
        BloqueadosScrollPane = new javax.swing.JScrollPane();
        BloqueadosTextArea = new javax.swing.JTextArea();
        TerminadosScrollPane = new javax.swing.JScrollPane();
        TerminadosTextArea = new javax.swing.JTextArea();
        SuspendidosText = new javax.swing.JLabel();
        SuspendidosScrollPane = new javax.swing.JScrollPane();
        SuspendidosTextArea = new javax.swing.JTextArea();
        MetricasRendimientoSistemaPanel = new javax.swing.JPanel();
        MetricasRendimientoSistemaText = new javax.swing.JLabel();
        LineaTiempoEjecucionProcesosText = new javax.swing.JLabel();
        LineaTiempoEjecucionProcesosPane = new javax.swing.JPanel();
        ResultadosSimulacionText = new javax.swing.JLabel();
        ResultadosDeSimulacionScrollPane = new javax.swing.JScrollPane();
        ResultadosDeSimulacionTable = new javax.swing.JTable();
        IniciarButton = new javax.swing.JButton();
        PausarButton = new javax.swing.JButton();
        DetenerButton = new javax.swing.JButton();
        ReiniciarButton = new javax.swing.JButton();
        CrearProcesoButton = new javax.swing.JButton();
        MostrarListaProcesosButton = new javax.swing.JButton();
        ImportarProcesosButton = new javax.swing.JButton();
        DescargarProcesosButton = new javax.swing.JButton();
        LogEventosText = new javax.swing.JLabel();
        LogEventosScrollPane = new javax.swing.JScrollPane();
        LogEventosTextArea = new javax.swing.JTextArea();
        LimpiarLogButton = new javax.swing.JButton();
        VelocidadMsText = new javax.swing.JLabel();
        DetalleColasText = new javax.swing.JLabel();
        DetalleColasScrollPane = new javax.swing.JScrollPane();
        DetalleColasTextArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        SimuladorCPUText.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        SimuladorCPUText.setText("Simulador de CPU");
        getContentPane().add(SimuladorCPUText);
        SimuladorCPUText.setBounds(6, 6, 160, 20);

        PlanificadorText.setText("Planificador:");
        getContentPane().add(PlanificadorText);
        PlanificadorText.setBounds(6, 35, 110, 16);

        FeedbackText.setText("Feedback:");
        getContentPane().add(FeedbackText);
        FeedbackText.setBounds(6, 69, 110, 16);

        VelocidadText.setText("Velocidad:");
        getContentPane().add(VelocidadText);
        VelocidadText.setBounds(6, 103, 110, 16);

        ProcesosEnMemoriaText.setText("Procesos en Memoria:");
        getContentPane().add(ProcesosEnMemoriaText);
        ProcesosEnMemoriaText.setBounds(6, 141, 130, 16);

        QuantumText.setText("Quantum:");
        getContentPane().add(QuantumText);
        QuantumText.setBounds(6, 170, 130, 16);
        getContentPane().add(QuantumSpinner);
        QuantumSpinner.setBounds(147, 167, 102, 22);
        getContentPane().add(ProcesosEnMemoriaSpinner);
        ProcesosEnMemoriaSpinner.setBounds(147, 138, 102, 22);
        getContentPane().add(VelocidadSlider);
        VelocidadSlider.setBounds(147, 102, 350, 20);

        Nivel0Text.setText("Nivel 0:");
        getContentPane().add(Nivel0Text);
        Nivel0Text.setBounds(147, 69, 50, 16);
        getContentPane().add(Nivel0Spinner);
        Nivel0Spinner.setBounds(192, 66, 44, 22);

        Nivel1Text.setText("Nivel 1:");
        getContentPane().add(Nivel1Text);
        Nivel1Text.setBounds(248, 69, 50, 16);
        getContentPane().add(Nivel1Spinner);
        Nivel1Spinner.setBounds(293, 66, 46, 22);

        Nivel2Text.setText("Nivel 2:");
        getContentPane().add(Nivel2Text);
        Nivel2Text.setBounds(351, 69, 50, 16);
        getContentPane().add(Nivel2Spinner);
        Nivel2Spinner.setBounds(396, 66, 41, 22);

        Nivel3Text.setText("Nivel 3:");
        getContentPane().add(Nivel3Text);
        Nivel3Text.setBounds(449, 69, 50, 16);
        getContentPane().add(Nivel3Spinner);
        Nivel3Spinner.setBounds(494, 66, 42, 22);

        AlgorithmSelectorComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[] { "FCFS", "HRRN", "Feedback", "RoundRobin", "SJF", "SRTF" }));
        AlgorithmSelectorComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AlgorithmSelectorComboBoxActionPerformed(evt);
            }
        });
        getContentPane().add(AlgorithmSelectorComboBox);
        AlgorithmSelectorComboBox.setBounds(147, 32, 403, 22);

        CreacionDeProcesosText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CreacionDeProcesosText.setText("Creación de Procesos:");
        getContentPane().add(CreacionDeProcesosText);
        CreacionDeProcesosText.setBounds(6, 229, 120, 16);

        AgregarQuinceProcesosAleatoriosButton.setText("Agregar 15 Procesos Aleatorios");
        AgregarQuinceProcesosAleatoriosButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AgregarQuinceProcesosAleatoriosButtonActionPerformed(evt);
            }
        });
        getContentPane().add(AgregarQuinceProcesosAleatoriosButton);
        AgregarQuinceProcesosAleatoriosButton.setBounds(6, 257, 243, 23);

        NombreText.setText("Nombre:");
        getContentPane().add(NombreText);
        NombreText.setBounds(6, 301, 120, 16);

        NombreTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NombreTextFieldActionPerformed(evt);
            }
        });
        getContentPane().add(NombreTextField);
        NombreTextField.setBounds(140, 298, 109, 22);
        getContentPane().add(LlegadaCicloSpinner);
        LlegadaCicloSpinner.setBounds(140, 338, 109, 22);

        LlegadaCicloText.setText("Llegada (Ciclo):");
        getContentPane().add(LlegadaCicloText);
        LlegadaCicloText.setBounds(6, 341, 120, 16);

        NroInstruccionesText.setText("Nro Intrucciones:");
        getContentPane().add(NroInstruccionesText);
        NroInstruccionesText.setBounds(6, 381, 120, 16);
        getContentPane().add(NroInstruccionesSpinner);
        NroInstruccionesSpinner.setBounds(140, 378, 109, 22);
        getContentPane().add(CicloDeIOSpinner);
        CicloDeIOSpinner.setBounds(140, 418, 109, 22);

        CicloDeIOText.setText("Ciclo de I/O:");
        getContentPane().add(CicloDeIOText);
        CicloDeIOText.setBounds(6, 421, 120, 16);

        TipoText.setText("Tipo:");
        getContentPane().add(TipoText);
        TipoText.setBounds(6, 461, 120, 16);

        TipoComboBox
                .setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "CPU Bound", "I/O Bound", "Mixto" }));
        TipoComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TipoComboBoxActionPerformed(evt);
            }
        });
        getContentPane().add(TipoComboBox);
        TipoComboBox.setBounds(140, 458, 109, 22);
        getContentPane().add(DuracionDeIOSpinner);
        DuracionDeIOSpinner.setBounds(140, 492, 109, 22);

        DuracionDeIOText.setText("Duración de I/O:");
        getContentPane().add(DuracionDeIOText);
        DuracionDeIOText.setBounds(6, 495, 130, 16);

        TamañoEnMemoriaText.setText("Tamaño En Memoria:");
        getContentPane().add(TamañoEnMemoriaText);
        TamañoEnMemoriaText.setBounds(6, 532, 130, 16);
        getContentPane().add(TamañoEnMemoriaSpinner);
        TamañoEnMemoriaSpinner.setBounds(140, 526, 109, 22);

        CPUText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        CPUText.setText("CPU:");
        getContentPane().add(CPUText);
        CPUText.setBounds(10, 610, 26, 16);

        CPUPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        ProcesoText.setText("Proceso:");

        PIDText.setText("PID:");

        PCText.setText("PC:");

        MARText.setText("MAR:");

        TotalText.setText("Total:");

        CicloActualText.setText("Ciclo Actual:");

        ModoText.setText("Modo:");

        ProcesoResponseText.setText("--");

        PIDResponseText.setText("--");

        PCResponseText.setText("--");

        MARResponseText.setText("--");

        TotalResponseText.setText("--");

        CicloActualResponseText.setText("--");

        ModoResponseText.setText("--");

        javax.swing.GroupLayout CPUPanelLayout = new javax.swing.GroupLayout(CPUPanel);
        CPUPanel.setLayout(CPUPanelLayout);
        CPUPanelLayout.setHorizontalGroup(
                CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(CPUPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(ProcesoText)
                                        .addComponent(PIDText)
                                        .addComponent(PCText)
                                        .addComponent(MARText)
                                        .addComponent(TotalText)
                                        .addComponent(ModoText)
                                        .addComponent(CicloActualText))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 79,
                                        Short.MAX_VALUE)
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(ProcesoResponseText, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(PIDResponseText, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(PCResponseText, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(MARResponseText, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(TotalResponseText, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(CicloActualResponseText,
                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(ModoResponseText, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.PREFERRED_SIZE, 80,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap()));
        CPUPanelLayout.setVerticalGroup(
                CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(CPUPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(ProcesoText)
                                        .addComponent(ProcesoResponseText))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(PIDText)
                                        .addComponent(PIDResponseText))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(PCText)
                                        .addComponent(PCResponseText))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(MARText)
                                        .addComponent(MARResponseText))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(TotalText)
                                        .addComponent(TotalResponseText))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(CicloActualResponseText)
                                        .addComponent(CicloActualText, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(CPUPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(ModoText)
                                        .addComponent(ModoResponseText))
                                .addContainerGap(16, Short.MAX_VALUE)));

        getContentPane().add(CPUPanel);
        CPUPanel.setBounds(10, 630, 240, 172);

        EstadosDeProcesosPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        EstadosDeProcesosText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        EstadosDeProcesosText.setText("Estados de Procesos:");

        ActivosText.setText("Listos:");

        BloqueadosText.setText("Bloqueados:");

        TerminadosTExt.setText("Terminados:");

        ActivosTextArea.setColumns(20);
        ActivosTextArea.setRows(5);
        ActivosScrollPane.setViewportView(ActivosTextArea);

        BloqueadosTextArea.setColumns(20);
        BloqueadosTextArea.setRows(5);
        BloqueadosScrollPane.setViewportView(BloqueadosTextArea);

        TerminadosTextArea.setColumns(20);
        TerminadosTextArea.setRows(5);
        TerminadosScrollPane.setViewportView(TerminadosTextArea);

        SuspendidosText.setText("Suspendidos:");

        SuspendidosTextArea.setColumns(20);
        SuspendidosTextArea.setRows(5);
        SuspendidosScrollPane.setViewportView(SuspendidosTextArea);

        javax.swing.GroupLayout EstadosDeProcesosPanelLayout = new javax.swing.GroupLayout(EstadosDeProcesosPanel);
        EstadosDeProcesosPanel.setLayout(EstadosDeProcesosPanelLayout);
        EstadosDeProcesosPanelLayout.setHorizontalGroup(
                EstadosDeProcesosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(EstadosDeProcesosPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(EstadosDeProcesosPanelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(ActivosScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 269,
                                                Short.MAX_VALUE)
                                        .addGroup(EstadosDeProcesosPanelLayout.createSequentialGroup()
                                                .addGroup(EstadosDeProcesosPanelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(EstadosDeProcesosText)
                                                        .addComponent(ActivosText)
                                                        .addComponent(BloqueadosText)
                                                        .addComponent(TerminadosTExt)
                                                        .addComponent(SuspendidosText))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(BloqueadosScrollPane)
                                        .addComponent(TerminadosScrollPane)
                                        .addComponent(SuspendidosScrollPane))
                                .addContainerGap()));
        EstadosDeProcesosPanelLayout.setVerticalGroup(
                EstadosDeProcesosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, EstadosDeProcesosPanelLayout
                                .createSequentialGroup()
                                .addContainerGap(12, Short.MAX_VALUE)
                                .addComponent(EstadosDeProcesosText)
                                .addGap(17, 17, 17)
                                .addComponent(ActivosText)
                                .addGap(5, 5, 5)
                                .addComponent(ActivosScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(BloqueadosText)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(BloqueadosScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(TerminadosTExt)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(TerminadosScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(SuspendidosText)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(SuspendidosScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));

        getContentPane().add(EstadosDeProcesosPanel);
        EstadosDeProcesosPanel.setBounds(267, 282, 283, 520);

        MetricasRendimientoSistemaPanel
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout MetricasRendimientoSistemaPanelLayout = new javax.swing.GroupLayout(
                MetricasRendimientoSistemaPanel);
        MetricasRendimientoSistemaPanel.setLayout(MetricasRendimientoSistemaPanelLayout);
        MetricasRendimientoSistemaPanelLayout.setHorizontalGroup(
                MetricasRendimientoSistemaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 478, Short.MAX_VALUE));
        MetricasRendimientoSistemaPanelLayout.setVerticalGroup(
                MetricasRendimientoSistemaPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 218, Short.MAX_VALUE));

        getContentPane().add(MetricasRendimientoSistemaPanel);
        MetricasRendimientoSistemaPanel.setBounds(568, 60, 480, 220);

        MetricasRendimientoSistemaText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        MetricasRendimientoSistemaText.setText("Métricas de Rendimiento del Sistema:");
        getContentPane().add(MetricasRendimientoSistemaText);
        MetricasRendimientoSistemaText.setBounds(568, 35, 480, 16);

        LineaTiempoEjecucionProcesosText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        LineaTiempoEjecucionProcesosText.setText("Línea de Tiempo de Ejecución de Procesos:");
        getContentPane().add(LineaTiempoEjecucionProcesosText);
        LineaTiempoEjecucionProcesosText.setBounds(568, 292, 480, 16);

        LineaTiempoEjecucionProcesosPane
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout LineaTiempoEjecucionProcesosPaneLayout = new javax.swing.GroupLayout(
                LineaTiempoEjecucionProcesosPane);
        LineaTiempoEjecucionProcesosPane.setLayout(LineaTiempoEjecucionProcesosPaneLayout);
        LineaTiempoEjecucionProcesosPaneLayout.setHorizontalGroup(
                LineaTiempoEjecucionProcesosPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 478, Short.MAX_VALUE));
        LineaTiempoEjecucionProcesosPaneLayout.setVerticalGroup(
                LineaTiempoEjecucionProcesosPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 218, Short.MAX_VALUE));

        getContentPane().add(LineaTiempoEjecucionProcesosPane);
        LineaTiempoEjecucionProcesosPane.setBounds(568, 314, 480, 220);

        ResultadosSimulacionText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        ResultadosSimulacionText.setText("Resulados de Simulación:");
        getContentPane().add(ResultadosSimulacionText);
        ResultadosSimulacionText.setBounds(568, 550, 480, 16);

        ResultadosDeSimulacionScrollPane
                .setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        ResultadosDeSimulacionTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null },
                        { null, null, null, null, null, null, null, null }
                },
                new String[] {
                        "PID", "Nombre", "AT", "BT", "CT", "TAT", "WT", "RT"
                }));
        ResultadosDeSimulacionScrollPane.setViewportView(ResultadosDeSimulacionTable);

        getContentPane().add(ResultadosDeSimulacionScrollPane);
        ResultadosDeSimulacionScrollPane.setBounds(568, 572, 480, 230);

        IniciarButton.setText("Iniciar");
        IniciarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IniciarButtonActionPerformed(evt);
            }
        });
        getContentPane().add(IniciarButton);
        IniciarButton.setBounds(267, 212, 92, 23);

        PausarButton.setText("Pausar");
        PausarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PausarButtonActionPerformed(evt);
            }
        });
        getContentPane().add(PausarButton);
        PausarButton.setBounds(371, 212, 84, 23);

        DetenerButton.setText("Detener");
        DetenerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DetenerButtonActionPerformed(evt);
            }
        });
        getContentPane().add(DetenerButton);
        DetenerButton.setBounds(467, 212, 83, 23);

        ReiniciarButton.setText("Reiniciar");
        ReiniciarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReiniciarButtonActionPerformed(evt);
            }
        });
        getContentPane().add(ReiniciarButton);
        ReiniciarButton.setBounds(267, 247, 283, 23);

        CrearProcesoButton.setText("Crear Proceso");
        CrearProcesoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CrearProcesoButtonActionPerformed(evt);
            }
        });
        getContentPane().add(CrearProcesoButton);
        CrearProcesoButton.setBounds(6, 558, 243, 23);

        MostrarListaProcesosButton.setText("Mostrar Lista de Procesos");
        MostrarListaProcesosButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MostrarListaProcesosButtonActionPerformed(evt);
            }
        });
        getContentPane().add(MostrarListaProcesosButton);
        MostrarListaProcesosButton.setBounds(267, 167, 283, 23);

        ImportarProcesosButton.setText("Importar Procesos");
        ImportarProcesosButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ImportarProcesosButtonActionPerformed(evt);
            }
        });
        getContentPane().add(ImportarProcesosButton);
        ImportarProcesosButton.setBounds(267, 138, 139, 23);

        DescargarProcesosButton.setText("Descargar Procesos");
        DescargarProcesosButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DescargarProcesosButtonActionPerformed(evt);
            }
        });
        getContentPane().add(DescargarProcesosButton);
        DescargarProcesosButton.setBounds(412, 138, 138, 23);

        LogEventosText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        LogEventosText.setText("Log de Eventos:");
        getContentPane().add(LogEventosText);
        LogEventosText.setBounds(1071, 35, 87, 16);

        LogEventosTextArea.setColumns(20);
        LogEventosTextArea.setRows(5);
        LogEventosScrollPane.setViewportView(LogEventosTextArea);

        getContentPane().add(LogEventosScrollPane);
        LogEventosScrollPane.setBounds(1066, 60, 370, 350);

        LimpiarLogButton.setText("Limpiar Log");
        LimpiarLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LimpiarLogButtonActionPerformed(evt);
            }
        });
        getContentPane().add(LimpiarLogButton);
        LimpiarLogButton.setBounds(1066, 416, 370, 23);

        VelocidadMsText.setText("50ms");
        getContentPane().add(VelocidadMsText);
        VelocidadMsText.setBounds(500, 100, 50, 16);

        DetalleColasText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        DetalleColasText.setText("Cola de Procesos:");
        getContentPane().add(DetalleColasText);
        DetalleColasText.setBounds(1066, 451, 95, 16);

        DetalleColasScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        DetalleColasTextArea.setColumns(20);
        DetalleColasTextArea.setRows(5);
        DetalleColasScrollPane.setViewportView(DetalleColasTextArea);

        getContentPane().add(DetalleColasScrollPane);
        DetalleColasScrollPane.setBounds(1066, 473, 370, 329);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void LimpiarLogButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_LimpiarLogButtonActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_LimpiarLogButtonActionPerformed

    private void MostrarListaProcesosButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_MostrarListaProcesosButtonActionPerformed
        mostrarListaProcesos();
    }// GEN-LAST:event_MostrarListaProcesosButtonActionPerformed

    private void DescargarProcesosButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_DescargarProcesosButtonActionPerformed
        descargarProcesos();
    }// GEN-LAST:event_DescargarProcesosButtonActionPerformed

    private void ImportarProcesosButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ImportarProcesosButtonActionPerformed
        importarProcesos();
    }// GEN-LAST:event_ImportarProcesosButtonActionPerformed

    private void CrearProcesoButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_CrearProcesoButtonActionPerformed
        crearProceso();
    }// GEN-LAST:event_CrearProcesoButtonActionPerformed

    private void ReiniciarButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_ReiniciarButtonActionPerformed
        reiniciarSimulacion();
    }// GEN-LAST:event_ReiniciarButtonActionPerformed

    private void AlgorithmSelectorComboBoxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_AlgorithmSelectorComboBoxActionPerformed
        createScheduler();
    }// GEN-LAST:event_AlgorithmSelectorComboBoxActionPerformed

    private void AgregarQuinceProcesosAleatoriosButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_AgregarQuinceProcesosAleatoriosButtonActionPerformed
        generarProcesosAleatorios();
    }// GEN-LAST:event_AgregarQuinceProcesosAleatoriosButtonActionPerformed

    private void NombreTextFieldActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_NombreTextFieldActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_NombreTextFieldActionPerformed

    private void TipoComboBoxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_TipoComboBoxActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_TipoComboBoxActionPerformed

    private void IniciarButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_IniciarButtonActionPerformed
        iniciarSimulacion();
    }// GEN-LAST:event_IniciarButtonActionPerformed

    private void PausarButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_PausarButtonActionPerformed
        pausarReanudarSimulacion();
    }// GEN-LAST:event_PausarButtonActionPerformed

    private void DetenerButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_DetenerButtonActionPerformed
        detenerSimulacion();
    }// GEN-LAST:event_DetenerButtonActionPerformed

    // ===== MÉTODO PARA GENERAR PROCESOS ALEATORIOS =====
    private void generarProcesosAleatorios() {
        Random rand = new Random();
        int count = 15;

        System.out.println("\n🔧 GENERANDO " + count + " PROCESOS ALEATORIOS...");

        boolean isRunning = (simulationTimer != null && simulationTimer.isRunning());
        int baseArrivalTime = isRunning ? globalClock + 1 : 0;

        for (int i = 0; i < count; i++) {
            int arrivalTime = baseArrivalTime + rand.nextInt(6); // Próximos 1-6 ciclos

            int burstTime = 5 + rand.nextInt(15);
            int ioCycle = rand.nextInt(10) + 5;
            int ioDuration = rand.nextInt(3) + 1;
            int priority = rand.nextInt(5);
            int memorySize = 1024 + rand.nextInt(3) * 512;

            ProcessType[] types = ProcessType.values();
            ProcessType tipo = types[rand.nextInt(types.length)];

            int currentPID = nextPID;
            nextPID++;

            Process process = new Process(
                    currentPID,
                    "P" + currentPID,
                    arrivalTime,
                    burstTime,
                    priority,
                    tipo,
                    ioCycle,
                    ioDuration,
                    memorySize);

            allProcesses.insertBegin(process);

            System.out.printf("  P%d: AT=%d, BT=%d, IO_Cycle=%d, IO_Dur=%d\n",
                    currentPID, arrivalTime, burstTime, ioCycle, ioDuration);
        }

        System.out.println("✅ " + count + " procesos generados correctamente\n");

        JOptionPane.showMessageDialog(this,
                count + " procesos aleatorios generados\n" +
                        (isRunning
                                ? "Arrival Times: Ciclo " + baseArrivalTime + " - " + (baseArrivalTime + 5)
                                        + " (llegarán automáticamente)\n"
                                : "Arrival Times: 0-5 ciclos (esperando inicio)\n")
                        +
                        "Burst Times: 5-19 ciclos\n" +
                        "I/O Cycle: 5-14 ciclos\n" +
                        "I/O Duration: 1-3 ciclos",
                "Procesos Generados",
                JOptionPane.INFORMATION_MESSAGE);

        if (isRunning) {
            logEvent("NUEVO",
                    String.format("%d procesos programados (AT: %d-%d)", count, baseArrivalTime, baseArrivalTime + 5),
                    new Color(0, 150, 136));
        }
    }

    // ===== MÉTODO PARA CREAR UN PROCESO INDIVIDUAL =====
    private void crearProceso() {
        try {
            int currentPID = nextPID;
            nextPID++;

            String name = NombreTextField.getText().trim();
            if (name.isEmpty()) {
                name = "P" + currentPID;
            }

            int arrivalTime = (Integer) LlegadaCicloSpinner.getValue();
            int burstTime = (Integer) NroInstruccionesSpinner.getValue();
            int ioCycle = (Integer) CicloDeIOSpinner.getValue();
            int ioDuration = (Integer) DuracionDeIOSpinner.getValue();
            int memorySize = (Integer) TamañoEnMemoriaSpinner.getValue();
            int priority = 3;

            String tipoStr = (String) TipoComboBox.getSelectedItem();
            ProcessType tipo = ProcessType.CPU_BOUND;

            if (tipoStr != null) {
                if (tipoStr.contains("I/O")) {
                    tipo = ProcessType.IO_BOUND;
                } else if (tipoStr.contains("Mixto")) {
                    tipo = ProcessType.MIXED;
                }
            }

            Process process = new Process(
                    currentPID,
                    name,
                    arrivalTime,
                    burstTime,
                    priority,
                    tipo,
                    ioCycle,
                    ioDuration,
                    memorySize);

            allProcesses.insertBegin(process);

            boolean isRunning = (simulationTimer != null && simulationTimer.isRunning());

            if (isRunning) {
                // Solo loguear que fue programado
                if (arrivalTime <= globalClock) {
                    logEvent("NUEVO",
                            String.format("P%d agregado (llegará en ciclo %d = AHORA)", process.getPid(), arrivalTime),
                            new Color(0, 150, 136));
                } else {
                    logEvent("NUEVO",
                            String.format("P%d programado para llegar en ciclo %d", process.getPid(), arrivalTime),
                            new Color(0, 150, 136));
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Proceso '" + name + "' creado exitosamente\n" +
                            "PID: " + process.getPid() + "\n" +
                            "Tipo: " + tipo + "\n" +
                            "Burst Time: " + burstTime + "\n" +
                            "Arrival Time: " + arrivalTime + "\n" +
                            (isRunning
                                    ? (arrivalTime <= globalClock ? "Estado: Llegará en el próximo ciclo"
                                            : "Estado: Programado para ciclo " + arrivalTime)
                                    : "Estado: Esperando inicio de simulación"),
                    "Proceso Creado",
                    JOptionPane.INFORMATION_MESSAGE);

            // Limpiar campos
            NombreTextField.setText("");
            LlegadaCicloSpinner.setValue(isRunning ? globalClock + 1 : 0);
            NroInstruccionesSpinner.setValue(10);
            CicloDeIOSpinner.setValue(0);
            DuracionDeIOSpinner.setValue(0);
            TamañoEnMemoriaSpinner.setValue(1024);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al crear proceso: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Ventana.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Ventana.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Ventana.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Ventana.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        // </editor-fold>
        // </editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Ventana().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane ActivosScrollPane;
    private javax.swing.JLabel ActivosText;
    private javax.swing.JTextArea ActivosTextArea;
    private javax.swing.JButton AgregarQuinceProcesosAleatoriosButton;
    private javax.swing.JComboBox<String> AlgorithmSelectorComboBox;
    private javax.swing.JScrollPane BloqueadosScrollPane;
    private javax.swing.JLabel BloqueadosText;
    private javax.swing.JTextArea BloqueadosTextArea;
    private javax.swing.JPanel CPUPanel;
    private javax.swing.JLabel CPUText;
    private javax.swing.JLabel CicloActualResponseText;
    private javax.swing.JLabel CicloActualText;
    private javax.swing.JSpinner CicloDeIOSpinner;
    private javax.swing.JLabel CicloDeIOText;
    private javax.swing.JLabel CreacionDeProcesosText;
    private javax.swing.JButton CrearProcesoButton;
    private javax.swing.JButton DescargarProcesosButton;
    private javax.swing.JScrollPane DetalleColasScrollPane;
    private javax.swing.JLabel DetalleColasText;
    private javax.swing.JTextArea DetalleColasTextArea;
    private javax.swing.JButton DetenerButton;
    private javax.swing.JSpinner DuracionDeIOSpinner;
    private javax.swing.JLabel DuracionDeIOText;
    private javax.swing.JPanel EstadosDeProcesosPanel;
    private javax.swing.JLabel EstadosDeProcesosText;
    private javax.swing.JLabel FeedbackText;
    private javax.swing.JButton ImportarProcesosButton;
    private javax.swing.JButton IniciarButton;
    private javax.swing.JButton LimpiarLogButton;
    private javax.swing.JPanel LineaTiempoEjecucionProcesosPane;
    private javax.swing.JLabel LineaTiempoEjecucionProcesosText;
    private javax.swing.JSpinner LlegadaCicloSpinner;
    private javax.swing.JLabel LlegadaCicloText;
    private javax.swing.JScrollPane LogEventosScrollPane;
    private javax.swing.JLabel LogEventosText;
    private javax.swing.JTextArea LogEventosTextArea;
    private javax.swing.JLabel MARResponseText;
    private javax.swing.JLabel MARText;
    private javax.swing.JPanel MetricasRendimientoSistemaPanel;
    private javax.swing.JLabel MetricasRendimientoSistemaText;
    private javax.swing.JLabel ModoResponseText;
    private javax.swing.JLabel ModoText;
    private javax.swing.JButton MostrarListaProcesosButton;
    private javax.swing.JSpinner Nivel0Spinner;
    private javax.swing.JLabel Nivel0Text;
    private javax.swing.JSpinner Nivel1Spinner;
    private javax.swing.JLabel Nivel1Text;
    private javax.swing.JSpinner Nivel2Spinner;
    private javax.swing.JLabel Nivel2Text;
    private javax.swing.JSpinner Nivel3Spinner;
    private javax.swing.JLabel Nivel3Text;
    private javax.swing.JLabel NombreText;
    private javax.swing.JTextField NombreTextField;
    private javax.swing.JSpinner NroInstruccionesSpinner;
    private javax.swing.JLabel NroInstruccionesText;
    private javax.swing.JLabel PCResponseText;
    private javax.swing.JLabel PCText;
    private javax.swing.JLabel PIDResponseText;
    private javax.swing.JLabel PIDText;
    private javax.swing.JButton PausarButton;
    private javax.swing.JLabel PlanificadorText;
    private javax.swing.JLabel ProcesoResponseText;
    private javax.swing.JLabel ProcesoText;
    private javax.swing.JSpinner ProcesosEnMemoriaSpinner;
    private javax.swing.JLabel ProcesosEnMemoriaText;
    private javax.swing.JSpinner QuantumSpinner;
    private javax.swing.JLabel QuantumText;
    private javax.swing.JButton ReiniciarButton;
    private javax.swing.JScrollPane ResultadosDeSimulacionScrollPane;
    private javax.swing.JTable ResultadosDeSimulacionTable;
    private javax.swing.JLabel ResultadosSimulacionText;
    private javax.swing.JLabel SimuladorCPUText;
    private javax.swing.JScrollPane SuspendidosScrollPane;
    private javax.swing.JLabel SuspendidosText;
    private javax.swing.JTextArea SuspendidosTextArea;
    private javax.swing.JSpinner TamañoEnMemoriaSpinner;
    private javax.swing.JLabel TamañoEnMemoriaText;
    private javax.swing.JScrollPane TerminadosScrollPane;
    private javax.swing.JLabel TerminadosTExt;
    private javax.swing.JTextArea TerminadosTextArea;
    private javax.swing.JComboBox<String> TipoComboBox;
    private javax.swing.JLabel TipoText;
    private javax.swing.JLabel TotalResponseText;
    private javax.swing.JLabel TotalText;
    private javax.swing.JLabel VelocidadMsText;
    private javax.swing.JSlider VelocidadSlider;
    private javax.swing.JLabel VelocidadText;
    // End of variables declaration//GEN-END:variables
}
