package Utilidades;

import EDD.Lista;
import Modelos.Process;
import Modelos.CPU;

/**
 * Clase de utilidad para cálculos estadísticos mejorada
 */
public class Statistics {

    /**
     * Calcula el tiempo promedio de espera
     */
    public static double calculateAverageWaitingTime(Lista<Process> processes) {
        if (processes.isEmpty()) {
            return 0.0;
        }

        double total = 0;
        for (int i = 0; i < processes.getSize(); i++) {
            total += processes.get(i).getWaitingTime();
        }

        return total / processes.getSize();
    }

    /**
     * Calcula el tiempo promedio de retorno
     */
    public static double calculateAverageTurnaroundTime(Lista<Process> processes) {
        if (processes.isEmpty()) {
            return 0.0;
        }

        double total = 0;
        for (int i = 0; i < processes.getSize(); i++) {
            total += processes.get(i).getTurnaroundTime();
        }

        return total / processes.getSize();
    }

    /**
     * Calcula el tiempo promedio de respuesta
     */
    public static double calculateAverageResponseTime(Lista<Process> processes) {
        if (processes.isEmpty()) {
            return 0.0;
        }

        double total = 0;
        int count = 0;
        for (int i = 0; i < processes.getSize(); i++) {
            int rt = processes.get(i).getResponseTime();
            if (rt >= 0) {
                total += rt;
                count++;
            }
        }

        return count > 0 ? total / count : 0.0;
    }

    /**
     * Calcula la utilización de CPU (método original)
     */
    public static double calculateCPUUtilization(Lista<Process> processes) {
        if (processes.isEmpty()) {
            return 0.0;
        }

        int totalBurstTime = 0;
        int lastCompletionTime = 0;

        for (int i = 0; i < processes.getSize(); i++) {
            Process p = processes.get(i);
            totalBurstTime += p.getBurstTime();
            if (p.getCompletionTime() > lastCompletionTime) {
                lastCompletionTime = p.getCompletionTime();
            }
        }

        if (lastCompletionTime == 0) {
            return 0.0;
        }

        return (totalBurstTime * 100.0) / lastCompletionTime;
    }

    /**
     * Calcula la utilización del CPU desde objeto CPU
     */
    public static double calculateCPUUtilization(CPU cpu) {
        return cpu.getUtilization();
    }

    /**
     * Calcula la utilización del CPU con tiempos
     */
    public static double calculateCPUUtilization(int totalExecutionTime, int totalIdleTime) {
        if (totalExecutionTime + totalIdleTime == 0) {
            return 0.0;
        }
        return (double) totalExecutionTime / (totalExecutionTime + totalIdleTime) * 100.0;
    }

    /**
     * Calcula el throughput (procesos por unidad de tiempo)
     */
    public static double calculateThroughput(Lista<Process> processes) {
        if (processes.isEmpty()) {
            return 0.0;
        }

        int lastCompletionTime = 0;
        for (int i = 0; i < processes.getSize(); i++) {
            if (processes.get(i).getCompletionTime() > lastCompletionTime) {
                lastCompletionTime = processes.get(i).getCompletionTime();
            }
        }

        if (lastCompletionTime == 0) {
            return 0.0;
        }

        return (double) processes.getSize() / lastCompletionTime;
    }

    /**
     * Calcula el Throughput con tiempo total especificado
     */
    public static double calculateThroughput(Lista<Process> processes, int totalTime) {
        if (totalTime == 0 || processes.isEmpty()) {
            return 0.0;
        }
        return (double) processes.getSize() / totalTime;
    }

    /**
     * Calcula el índice de equidad (Fairness) usando coeficiente de Jain
     * Resultado: 0 = muy inequitativo, 1 = perfectamente justo
     */
    public static double calculateFairness(Lista<Process> processes) {
        if (processes.isEmpty()) {
            return 1.0;
        }

        double sumCpuTime = 0.0;
        double sumSquaredCpuTime = 0.0;

        for (int i = 0; i < processes.getSize(); i++) {
            Process p = processes.get(i);
            double cpuTime = p.getBurstTime();
            sumCpuTime += cpuTime;
            sumSquaredCpuTime += cpuTime * cpuTime;
        }

        int n = processes.getSize();

        if (sumSquaredCpuTime == 0) {
            return 1.0;
        }

        // Índice de Jain
        double fairness = (sumCpuTime * sumCpuTime) / (n * sumSquaredCpuTime);

        return fairness;
    }

    /**
     * Calcula equidad basada en tiempos de espera
     */
    public static double calculateWaitTimeFairness(Lista<Process> processes) {
        if (processes.isEmpty()) {
            return 1.0;
        }

        double sumWaitTime = 0.0;
        double sumSquaredWaitTime = 0.0;

        for (int i = 0; i < processes.getSize(); i++) {
            Process p = processes.get(i);
            double waitTime = p.getWaitingTime();
            sumWaitTime += waitTime;
            sumSquaredWaitTime += waitTime * waitTime;
        }

        int n = processes.getSize();

        if (sumSquaredWaitTime == 0) {
            return 1.0;
        }

        double fairness = (sumWaitTime * sumWaitTime) / (n * sumSquaredWaitTime);
        return fairness;
    }

    /**
     * Calcula el total de cambios de contexto
     */
    public static int calculateTotalContextSwitches(Lista<Process> processes) {
        int total = 0;
        for (int i = 0; i < processes.getSize(); i++) {
            total += processes.get(i).getContextSwitches();
        }
        return total;
    }

    /**
     * Calcula el total de operaciones I/O
     */
    public static int calculateTotalIOOperations(Lista<Process> processes) {
        int total = 0;
        for (int i = 0; i < processes.getSize(); i++) {
            total += processes.get(i).getIoOperations();
        }
        return total;
    }

    /**
     * Genera un reporte completo de estadísticas
     */
    public static String generateReport(Lista<Process> processes, CPU cpu, int totalTime) {
        StringBuilder report = new StringBuilder();
        report.append("========== ESTADÍSTICAS DE LA SIMULACIÓN ==========\n\n");

        // Métricas de tiempo
        report.append("MÉTRICAS DE TIEMPO:\n");
        report.append(String.format("  Tiempo Promedio de Espera (WT): %.2f unidades\n",
                calculateAverageWaitingTime(processes)));
        report.append(String.format("  Tiempo Promedio de Retorno (TAT): %.2f unidades\n",
                calculateAverageTurnaroundTime(processes)));
        report.append(String.format("  Tiempo Promedio de Respuesta (RT): %.2f unidades\n",
                calculateAverageResponseTime(processes)));

        // Métricas del sistema
        report.append("\nMÉTRICAS DEL SISTEMA:\n");
        report.append(String.format("  Utilización del CPU: %.2f%%\n",
                calculateCPUUtilization(cpu)));
        report.append(String.format("  Throughput: %.4f procesos/unidad\n",
                calculateThroughput(processes, totalTime)));
        report.append(String.format("  Índice de Equidad (Fairness): %.4f\n",
                calculateFairness(processes)));

        // Métricas operacionales
        report.append("\nMÉTRICAS OPERACIONALES:\n");
        report.append(String.format("  Total de Procesos: %d\n", processes.getSize()));
        report.append(String.format("  Tiempo Total de Simulación: %d unidades\n", totalTime));
        report.append(String.format("  Cambios de Contexto: %d\n",
                calculateTotalContextSwitches(processes)));
        report.append(String.format("  Operaciones I/O: %d\n",
                calculateTotalIOOperations(processes)));

        report.append("\n===================================================\n");

        return report.toString();
    }
}