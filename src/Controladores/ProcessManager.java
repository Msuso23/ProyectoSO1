package Controladores;

import EDD.List;
import Modelos.Process;
import Modelos.ProcessState;

/**
 * Controlador para gestionar procesos
 * Maneja la creación, validación y almacenamiento de procesos
 */
public class ProcessManager {
    private List<Process> processes;
    private int nextPid;

    public ProcessManager() {
        this.processes = new List<>();
        this.nextPid = 1;
    }

    /**
     * Crea un nuevo proceso con los parámetros dados
     */
    public Process createProcess(String name, int arrivalTime, int burstTime, int priority) {
        // Validaciones
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del proceso no puede estar vacío");
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("El tiempo de llegada no puede ser negativo");
        }
        if (burstTime <= 0) {
            throw new IllegalArgumentException("El tiempo de ráfaga debe ser mayor a 0");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("La prioridad no puede ser negativa");
        }

        Process process = new Process(nextPid++, name, arrivalTime, burstTime, priority);
        processes.insertBegin(process);
        return process;
    }

    /**
     * Crea un proceso sin prioridad
     */
    public Process createProcess(String name, int arrivalTime, int burstTime) {
        return createProcess(name, arrivalTime, burstTime, 0);
    }

    /**
     * Elimina un proceso por su PID
     */
    public boolean removeProcess(int pid) {
        for (int i = 0; i < processes.getSize(); i++) {
            if (processes.get(i).getPid() == pid) {
                processes.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Busca un proceso por PID
     */
    public Process findProcessById(int pid) {
        for (int i = 0; i < processes.getSize(); i++) {
            Process p = processes.get(i);
            if (p.getPid() == pid) {
                return p;
            }
        }
        return null;
    }

    /**
     * Obtiene todos los procesos
     */
    public List<Process> getAllProcesses() {
        return processes;
    }

    /**
     * Obtiene procesos por estado
     */
    public List<Process> getProcessesByState(ProcessState state) {
        List<Process> filtered = new List<>();
        for (int i = 0; i < processes.getSize(); i++) {
            Process p = processes.get(i);
            if (p.getState() == state) {
                filtered.insertBegin(p);
            }
        }
        return filtered;
    }

    /**
     * Limpia todos los procesos
     */
    public void clearAll() {
        processes.vaciar();
        nextPid = 1;
    }

    /**
     * Retorna la cantidad de procesos
     */
    public int getProcessCount() {
        return processes.getSize();
    }

    /**
     * Valida que haya procesos para simular
     */
    public boolean hasProcesses() {
        return !processes.isEmpty();
    }

    /**
     * Crea una copia independiente de los procesos para simulación
     * Esto evita modificar los procesos originales
     */
    public List<Process> getProcessesCopy() {
        List<Process> copy = new List<>();
        for (int i = 0; i < processes.getSize(); i++) {
            Process original = processes.get(i);
            Process duplicate = new Process(
                    original.getPid(),
                    original.getName(),
                    original.getArrivalTime(),
                    original.getBurstTime(),
                    original.getPriority());
            copy.insertBegin(duplicate);
        }
        return copy;
    }

    /**
     * Genera un resumen de los procesos
     */
    public String getProcessesSummary() {
        if (processes.isEmpty()) {
            return "No hay procesos registrados";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Procesos Registrados ===\n");
        sb.append(String.format("Total: %d procesos\n\n", processes.getSize()));

        for (int i = 0; i < processes.getSize(); i++) {
            Process p = processes.get(i);
            sb.append(String.format("P%d - %s | AT: %d | BT: %d | Pri: %d\n",
                    p.getPid(), p.getName(), p.getArrivalTime(),
                    p.getBurstTime(), p.getPriority()));
        }

        return sb.toString();
    }
}