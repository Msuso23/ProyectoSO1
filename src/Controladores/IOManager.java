package Controladores;

import EDD.Queue;
import Modelos.Process;
import Modelos.ProcessState;

/**
 * Gestor de operaciones de Entrada/Salida
 * Maneja procesos bloqueados esperando I/O
 */
public class IOManager {

    private Queue<Process> blockedQueue; // Cola de procesos bloqueados
    private int totalIOOperations; // Total de operaciones I/O
    private int currentTime; // Tiempo actual del sistema

    public IOManager() {
        this.blockedQueue = new Queue<>();
        this.totalIOOperations = 0;
        this.currentTime = 0;
    }

    /**
     * Bloquea un proceso por operación I/O
     */
    public void blockProcess(Process process) {
        process.setState(ProcessState.BLOCKED);
        blockedQueue.enqueue(process);
        totalIOOperations++;
    }

    /**
     * Procesa un ciclo de todas las operaciones I/O
     * 
     * @return Queue con procesos que terminaron su I/O
     */
    public Queue<Process> processIOCycle() {
        Queue<Process> readyProcesses = new Queue<>();
        int size = blockedQueue.size();

        for (int i = 0; i < size; i++) {
            Process process = blockedQueue.dequeue();

            // ✅ VERIFICAR: Procesar un ciclo de I/O
            boolean ioComplete = process.processIO();

            if (ioComplete) {
                // El proceso completó su I/O, regresa a Ready
                process.setState(ProcessState.READY);
                readyProcesses.enqueue(process);
                totalIOOperations++;
            } else {
                // Aún bloqueado, regresa a la cola
                blockedQueue.enqueue(process);
            }
        }

        return readyProcesses;
    }

    /**
     * Verifica si hay procesos bloqueados
     */
    public boolean hasBlockedProcesses() {
        return !blockedQueue.isEmpty();
    }

    /**
     * Obtiene el número de procesos bloqueados
     */
    public int getBlockedCount() {
        return blockedQueue.size();
    }

    /**
     * Obtiene la cola de bloqueados
     */
    public Queue<Process> getBlockedQueue() {
        return blockedQueue;
    }

    /**
     * Obtiene el total de operaciones I/O realizadas
     */
    public int getTotalIOOperations() {
        return totalIOOperations;
    }

    /**
     * Actualiza el tiempo actual
     */
    public void setCurrentTime(int time) {
        this.currentTime = time;
    }

    /**
     * Reinicia el gestor
     */
    public void reset() {
        blockedQueue.clear();
        totalIOOperations = 0;
        currentTime = 0;
    }

    /**
     * Obtiene estadísticas de I/O
     */
    public String getStatistics() {
        return String.format(
                "I/O Stats: Total Operations=%d, Currently Blocked=%d",
                totalIOOperations, blockedQueue.size());
    }
}
