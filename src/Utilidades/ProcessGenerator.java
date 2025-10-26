package Utilidades;

import EDD.Lista;
import Modelos.Process;

/**
 * Generador de procesos aleatorios para pruebas y demostraciones
 */
public class ProcessGenerator {

    private static final String[] PROCESS_NAMES = {
            "Chrome", "Firefox", "Word", "Excel", "Spotify",
            "Visual Studio", "Discord", "Zoom", "Teams", "Photoshop",
            "Steam", "Notepad", "Calculator", "WhatsApp", "Slack",
            "Outlook", "PowerPoint", "VLC", "Eclipse", "NetBeans"
    };

    /**
     * Genera una lista de procesos aleatorios
     * 
     * @param count          Cantidad de procesos a generar
     * @param maxArrivalTime Tiempo máximo de llegada
     * @param minBurstTime   Tiempo mínimo de ráfaga
     * @param maxBurstTime   Tiempo máximo de ráfaga
     * @param withPriority   Si se deben generar con prioridades
     * @return Lista de procesos generados
     */
    public static Lista<Process> generateRandomProcesses(
            int count,
            int maxArrivalTime,
            int minBurstTime,
            int maxBurstTime,
            boolean withPriority) {

        if (count <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (minBurstTime <= 0 || maxBurstTime < minBurstTime) {
            throw new IllegalArgumentException("Tiempos de ráfaga inválidos");
        }
        if (maxArrivalTime < 0) {
            throw new IllegalArgumentException("Tiempo de llegada inválido");
        }

        Lista<Process> processes = new Lista<>();

        for (int i = 1; i <= count; i++) {
            String name = getRandomProcessName();
            int arrivalTime = randomInt(0, maxArrivalTime);
            int burstTime = randomInt(minBurstTime, maxBurstTime);
            int priority = withPriority ? randomInt(1, 5) : 0;

            Process process = new Process(i, name, arrivalTime, burstTime, priority);
            processes.insertBegin(process);
        }

        return processes;
    }

    /**
     * Genera procesos con configuración por defecto
     * 
     * @param count Cantidad de procesos
     * @return Lista de procesos
     */
    public static Lista<Process> generateRandomProcesses(int count) {
        return generateRandomProcesses(count, 10, 1, 15, false);
    }

    /**
     * Genera un conjunto de procesos de ejemplo predefinidos
     * Útil para demostraciones consistentes
     */
    public static Lista<Process> generateExampleSet() {
        Lista<Process> processes = new Lista<>();

        processes.insertBegin(new Process(1, "Chrome", 0, 8, 2));
        processes.insertBegin(new Process(2, "Word", 1, 4, 1));
        processes.insertBegin(new Process(3, "Spotify", 2, 9, 3));
        processes.insertBegin(new Process(4, "Excel", 3, 5, 1));
        processes.insertBegin(new Process(5, "Discord", 4, 3, 2));

        return processes;
    }

    /**
     * Genera un escenario de carga ligera (procesos cortos)
     */
    public static Lista<Process> generateLightLoad(int count) {
        return generateRandomProcesses(count, 5, 1, 5, false);
    }

    /**
     * Genera un escenario de carga pesada (procesos largos)
     */
    public static Lista<Process> generateHeavyLoad(int count) {
        return generateRandomProcesses(count, 10, 10, 30, false);
    }

    /**
     * Genera un escenario mixto (variedad de procesos)
     */
    public static Lista<Process> generateMixedLoad(int count) {
        return generateRandomProcesses(count, 15, 1, 25, true);
    }

    /**
     * Genera procesos para probar el peor caso de un algoritmo
     * (Todos llegan al mismo tiempo con diferentes burst times)
     */
    public static Lista<Process> generateWorstCaseScenario(int count) {
        Lista<Process> processes = new Lista<>();

        for (int i = 1; i <= count; i++) {
            String name = getRandomProcessName();
            int arrivalTime = 0;
            int burstTime = i * 3;
            int priority = randomInt(1, 5);

            Process process = new Process(i, name, arrivalTime, burstTime, priority);
            processes.insertBegin(process);
        }

        return processes;
    }

    /**
     * Genera procesos para Round Robin (todos con mismo burst time)
     */
    public static Lista<Process> generateRoundRobinTest(int count, int burstTime) {
        Lista<Process> processes = new Lista<>();

        for (int i = 1; i <= count; i++) {
            String name = getRandomProcessName();
            int arrivalTime = i - 1; 

            Process process = new Process(i, name, arrivalTime, burstTime, 0);
            processes.insertBegin(process);
        }

        return processes;
    }

    /**
     * Genera procesos para probar SJF
     * (Variedad de burst times cortos y largos)
     */
    public static Lista<Process> generateSJFTest(int count) {
        Lista<Process> processes = new Lista<>();

        for (int i = 1; i <= count; i++) {
            String name = getRandomProcessName();
            int arrivalTime = randomInt(0, 5);
            int burstTime = (i % 2 == 0) ? randomInt(2, 5) : randomInt(10, 20);

            Process process = new Process(i, name, arrivalTime, burstTime, 0);
            processes.insertBegin(process);
        }

        return processes;
    }

    /**
     * Genera un nombre de proceso aleatorio
     */
    private static String getRandomProcessName() {
        int index = randomInt(0, PROCESS_NAMES.length - 1);
        return PROCESS_NAMES[index];
    }

    /**
     * Genera un número entero aleatorio en el rango [min, max]
     */
    private static int randomInt(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    /**
     * Imprime un resumen de los procesos generados
     */
    public static String printProcessSummary(Lista<Process> processes) {
        if (processes.isEmpty()) {
            return "No hay procesos";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("╔════════════════════════════════════════════╗\n");
        sb.append("║   PROCESOS GENERADOS                       ║\n");
        sb.append("╚════════════════════════════════════════════╝\n\n");

        sb.append("┌─────┬──────────────┬────────┬────────┬──────────┐\n");
        sb.append("│ PID │ Nombre       │ Llegada│ Ráfaga │ Prioridad│\n");
        sb.append("├─────┼──────────────┼────────┼────────┼──────────┤\n");

        for (int i = 0; i < processes.getSize(); i++) {
            Process p = processes.get(i);
            sb.append(String.format("│ %3d │ %-12s │   %4d │   %4d │     %4d │\n",
                    p.getPid(),
                    p.getName().length() > 12 ? p.getName().substring(0, 12) : p.getName(),
                    p.getArrivalTime(),
                    p.getBurstTime(),
                    p.getPriority()));
        }

        sb.append("└─────┴──────────────┴────────┴────────┴──────────┘\n");

        int totalBurst = 0;
        int maxArrival = 0;

        for (int i = 0; i < processes.getSize(); i++) {
            Process p = processes.get(i);
            totalBurst += p.getBurstTime();
            if (p.getArrivalTime() > maxArrival) {
                maxArrival = p.getArrivalTime();
            }
        }

        sb.append(String.format("\nTotal de procesos: %d\n", processes.getSize()));
        sb.append(String.format("Tiempo total de ráfaga: %d\n", totalBurst));
        sb.append(String.format("Tiempo promedio de ráfaga: %.2f\n",
                (double) totalBurst / processes.getSize()));
        sb.append(String.format("Ventana de llegadas: 0 - %d\n", maxArrival));

        return sb.toString();
    }
}