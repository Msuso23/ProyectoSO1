package Algoritmos;

import EDD.Lista;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;
import Controladores.IOManager;

/**
 * Algoritmo Shortest Job First (SJF) - No Expulsivo con soporte I/O
 * Selecciona el proceso con el menor tiempo de ráfaga
 */
public class SJFScheduler implements Scheduler {
    private Lista<Process> readyQueue;
    private Queue<Process> blockedQueue;
    private CPU cpu;
    private Lista<Process> allProcesses;
    private Lista<Process> completedProcesses;
    private IOManager ioManager;

    public SJFScheduler() {
        this.readyQueue = new Lista<>();
        this.blockedQueue = new Queue<>();
        this.cpu = new CPU();
        this.allProcesses = new Lista<>();
        this.completedProcesses = new Lista<>();
        this.ioManager = new IOManager();
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

        // Buscar el proceso con menor burst time
        int shortestIndex = 0;
        int shortestBurst = readyQueue.get(0).getBurstTime();

        for (int i = 1; i < readyQueue.getSize(); i++) {
            if (readyQueue.get(i).getBurstTime() < shortestBurst) {
                shortestBurst = readyQueue.get(i).getBurstTime();
                shortestIndex = i;
            }
        }

        return readyQueue.remove(shortestIndex);
    }

    @Override
    public Lista<Process> executeSimulation() {
        int currentTime = 0;
        int processIndex = 0;

        // Ordenar procesos por tiempo de llegada
        sortProcessesByArrivalTime();

        while (completedProcesses.getSize() < allProcesses.getSize() || !blockedQueue.isEmpty() || cpu.isBusy()) {
            // 1. Agregar procesos que han llegado
            while (processIndex < allProcesses.getSize() &&
                    allProcesses.get(processIndex).getArrivalTime() <= currentTime) {
                Process p = allProcesses.get(processIndex);
                p.setState(ProcessState.READY);
                readyQueue.insertBegin(p);
                processIndex++;
            }

            // 2. Procesar operaciones I/O de procesos bloqueados
            Queue<Process> readyFromIO = ioManager.processIOCycle();
            while (!readyFromIO.isEmpty()) {
                Process p = readyFromIO.dequeue();
                p.setState(ProcessState.READY);
                readyQueue.insertBegin(p);
            }

            // 3. Si la CPU está ociosa, asignar proceso con menor burst time
            if (cpu.isIdle() && !readyQueue.isEmpty()) {
                Process nextProcess = selectNextProcess();
                cpu.assignProcess(nextProcess);
            }

            // 4. Ejecutar ciclo
            if (!cpu.isIdle()) {
                Process currentProcess = cpu.getCurrentProcess();
                boolean canContinue = cpu.executeCycle(currentTime);

                // Verificar si el proceso terminó
                if (currentProcess.isFinished()) {
                    currentProcess.setState(ProcessState.TERMINATED);
                    currentProcess.calculateMetrics(currentTime);
                    completedProcesses.insertBegin(cpu.releaseProcess());
                }
                // Verificar si necesita operación I/O
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
        return "Shortest Job First (SJF)";
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

    /**
     * Obtiene el gestor de I/O
     */
    public IOManager getIOManager() {
        return ioManager;
    }

    /**
     * Obtiene la cola de bloqueados
     */
    public Queue<Process> getBlockedQueue() {
        return blockedQueue;
    }
}