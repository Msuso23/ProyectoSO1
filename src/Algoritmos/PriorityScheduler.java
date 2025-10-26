package Algoritmos;

import EDD.Lista;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;
import Controladores.IOManager;

/**
 * Algoritmo de Planificación por Prioridad con Aging y soporte I/O
 * Menor número = Mayor prioridad
 * Aging: Previene inanición incrementando prioridad con el tiempo
 */
public class PriorityScheduler implements Scheduler {
    private Lista<Process> readyQueue;
    private Queue<Process> blockedQueue;
    private CPU cpu;
    private Lista<Process> allProcesses;
    private Lista<Process> completedProcesses;
    private IOManager ioManager;

    // Configuración de Aging
    private int agingThreshold = 10; // Tiempo de espera para mejorar prioridad
    private boolean agingEnabled = true;

    public PriorityScheduler() {
        this.readyQueue = new Lista<>();
        this.blockedQueue = new Queue<>();
        this.cpu = new CPU();
        this.allProcesses = new Lista<>();
        this.completedProcesses = new Lista<>();
        this.ioManager = new IOManager();
    }

    /**
     * Constructor con configuración de aging
     */
    public PriorityScheduler(int agingThreshold) {
        this();
        this.agingThreshold = agingThreshold;
    }

    @Override
    public void addProcess(Process process) {
        // ✅ CAMBIO: Agregar DIRECTAMENTE a readyQueue
        process.setState(ProcessState.READY);
        readyQueue.insertBegin(process);

        // También agregar a allProcesses
        allProcesses.insertBegin(process);
    }

    public Lista<Process> getReadyQueue() {
        return readyQueue;
    }

    @Override
    public Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }

        // Buscar proceso con mayor prioridad (menor número)
        int highestPriorityIndex = 0;
        int highestPriority = readyQueue.get(0).getPriority();

        for (int i = 1; i < readyQueue.getSize(); i++) {
            if (readyQueue.get(i).getPriority() < highestPriority) {
                highestPriority = readyQueue.get(i).getPriority();
                highestPriorityIndex = i;
            }
        }

        return readyQueue.remove(highestPriorityIndex);
    }

    @Override
    public Lista<Process> executeSimulation() {
        int currentTime = 0;
        int processIndex = 0;

        sortProcessesByArrivalTime();

        while (completedProcesses.getSize() < allProcesses.getSize() || !blockedQueue.isEmpty() || cpu.isBusy()) {

            // 1. Admitir procesos que han llegado
            while (processIndex < allProcesses.getSize() &&
                    allProcesses.get(processIndex).getArrivalTime() <= currentTime) {
                Process p = allProcesses.get(processIndex);
                p.setState(ProcessState.READY);
                readyQueue.insertBegin(p);
                processIndex++;
            }

            // 2. Procesar operaciones I/O de procesos bloqueados
            processBlockedQueue();

            // 3. Aplicar aging (si está habilitado)
            if (agingEnabled && currentTime % agingThreshold == 0 && currentTime > 0) {
                applyAging();
            }

            // 4. Incrementar tiempo de espera de procesos en ready
            incrementWaitingTimes();

            // 5. Asignar proceso si CPU está libre
            if (cpu.isIdle() && !readyQueue.isEmpty()) {
                Process nextProcess = selectNextProcess();
                cpu.assignProcess(nextProcess);
            }

            // 6. Ejecutar ciclo de CPU
            if (!cpu.isIdle()) {
                Process currentProcess = cpu.getCurrentProcess();
                boolean canContinue = cpu.executeCycle(currentTime);

                // Verificar si terminó
                if (currentProcess.isFinished()) {
                    currentProcess.setState(ProcessState.TERMINATED);
                    currentProcess.calculateMetrics(currentTime);
                    completedProcesses.insertBegin(cpu.releaseProcess());
                }
                // Verificar si necesita I/O
                else if (!canContinue) {
                    currentProcess.setState(ProcessState.BLOCKED);
                    ioManager.blockProcess(cpu.releaseProcess());
                }
            } else {
                cpu.tickIdle();
            }

            currentTime++;
            ioManager.setCurrentTime(currentTime);
        }

        return completedProcesses;
    }

    /**
     * Procesa la cola de bloqueados (I/O)
     */
    private void processBlockedQueue() {
        Queue<Process> readyFromIO = ioManager.processIOCycle();

        while (!readyFromIO.isEmpty()) {
            Process p = readyFromIO.dequeue();
            p.setState(ProcessState.READY);
            readyQueue.insertBegin(p);
        }
    }

    /**
     * Aplica aging a procesos que han esperado mucho tiempo
     * Mejora la prioridad para prevenir inanición
     */
    private void applyAging() {
        for (int i = 0; i < readyQueue.getSize(); i++) {
            Process p = readyQueue.get(i);

            // Si ha esperado más del umbral, mejorar prioridad
            if (p.getTimeInCurrentQueue() >= agingThreshold && p.getPriority() > 0) {
                int oldPriority = p.getPriority();
                p.improvePriority(); // Disminuye el número = aumenta prioridad

                // Si la prioridad base es mayor que 0, decrementar
                if (oldPriority > 0) {
                    p.setPriority(oldPriority - 1);
                }
            }
        }
    }

    /**
     * Incrementa el tiempo de espera de procesos en ready
     */
    private void incrementWaitingTimes() {
        for (int i = 0; i < readyQueue.getSize(); i++) {
            Process p = readyQueue.get(i);
            p.incrementWaitingTime();
        }
    }

    private void sortProcessesByArrivalTime() {
        for (int i = 0; i < allProcesses.getSize() - 1; i++) {
            for (int j = 0; j < allProcesses.getSize() - i - 1; j++) {
                if (allProcesses.get(j).getArrivalTime() > allProcesses.get(j + 1).getArrivalTime()) {
                    Process temp = allProcesses.get(j);
                    allProcesses.remove(j);
                    allProcesses.insertBegin(temp);
                }
            }
        }
    }

    @Override
    public String getAlgorithmName() {
        return "Priority Scheduling" + (agingEnabled ? " (with Aging)" : "");
    }

    @Override
    public void reset() {
        readyQueue.vaciar();
        blockedQueue.clear();
        cpu.reset();
        completedProcesses.vaciar();
        ioManager.reset();
    }

    @Override
    public boolean hasProcesses() {
        return !readyQueue.isEmpty();
    }

    // Getters y Setters

    /**
     * Habilita o deshabilita el aging
     */
    public void setAgingEnabled(boolean enabled) {
        this.agingEnabled = enabled;
    }

    /**
     * Establece el umbral de aging
     */
    public void setAgingThreshold(int threshold) {
        this.agingThreshold = threshold;
    }

    /**
     * Obtiene el umbral de aging actual
     */
    public int getAgingThreshold() {
        return agingThreshold;
    }

    /**
     * Verifica si el aging está habilitado
     */
    public boolean isAgingEnabled() {
        return agingEnabled;
    }

    /**
     * Obtiene la cola de bloqueados
     */
    public Queue<Process> getBlockedQueue() {
        return blockedQueue;
    }

    /**
     * Obtiene el gestor de I/O
     */
    public IOManager getIOManager() {
        return ioManager;
    }
}