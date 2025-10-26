package Algoritmos;

import EDD.Lista;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;
import Controladores.IOManager;

/**
 * Algoritmo SRTF (Shortest Remaining Time First)
 */
public class SRTFScheduler implements Scheduler {
    private Lista<Process> readyQueue;
    private Queue<Process> blockedQueue;
    private CPU cpu;
    private Lista<Process> allProcesses;
    private Lista<Process> completedProcesses;
    private IOManager ioManager;
    private boolean preemptive = true; 

    public SRTFScheduler() {
        this.readyQueue = new Lista<>();
        this.blockedQueue = new Queue<>();
        this.cpu = new CPU();
        this.allProcesses = new Lista<>();
        this.completedProcesses = new Lista<>();
        this.ioManager = new IOManager();
    }

    /**
     * Constructor con opción de preempción
     * 
     * @param preemptive true para SRTF expulsivo, false para no expulsivo
     *                  
     */
    public SRTFScheduler(boolean preemptive) {
        this();
        this.preemptive = preemptive;
    }

    @Override
    public void addProcess(Process process) {
        process.setState(ProcessState.READY);
        readyQueue.insertBegin(process);

        allProcesses.insertBegin(process);
    }

    @Override
    public Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }

        int shortestIndex = 0;
        int shortestRemaining = readyQueue.get(0).getRemainingTime();

        for (int i = 1; i < readyQueue.getSize(); i++) {
            int remaining = readyQueue.get(i).getRemainingTime();

            if (remaining < shortestRemaining) {
                shortestRemaining = remaining;
                shortestIndex = i;
            } else if (remaining == shortestRemaining) {
                if (readyQueue.get(i).getArrivalTime() < readyQueue.get(shortestIndex).getArrivalTime()) {
                    shortestIndex = i;
                }
            }
        }

        return readyQueue.remove(shortestIndex);
    }

    @Override
    public Lista<Process> executeSimulation() {
        int currentTime = 0;
        int processIndex = 0;

        sortProcessesByArrivalTime();

        while (completedProcesses.getSize() < allProcesses.getSize() || !blockedQueue.isEmpty() || cpu.isBusy()) {
            while (processIndex < allProcesses.getSize() &&
                    allProcesses.get(processIndex).getArrivalTime() <= currentTime) {
                Process p = allProcesses.get(processIndex);
                p.setState(ProcessState.READY);
                readyQueue.insertBegin(p);
                processIndex++;
            }

            Queue<Process> readyFromIO = ioManager.processIOCycle();
            while (!readyFromIO.isEmpty()) {
                Process p = readyFromIO.dequeue();
                p.setState(ProcessState.READY);
                readyQueue.insertBegin(p);
            }

            if (preemptive && !cpu.isIdle() && !readyQueue.isEmpty()) {
                Process currentProcess = cpu.getCurrentProcess();
                int currentRemaining = currentProcess.getRemainingTime();

                for (int i = 0; i < readyQueue.getSize(); i++) {
                    if (readyQueue.get(i).getRemainingTime() < currentRemaining) {
                        currentProcess.setState(ProcessState.READY);
                        readyQueue.insertBegin(cpu.releaseProcess());
                        break;
                    }
                }
            }

            if (cpu.isIdle() && !readyQueue.isEmpty()) {
                Process nextProcess = selectNextProcess();
                cpu.assignProcess(nextProcess);
            }

            if (!cpu.isIdle()) {
                Process currentProcess = cpu.getCurrentProcess();
                boolean canContinue = cpu.executeCycle(currentTime);

                if (currentProcess.isFinished()) {
                    currentProcess.setState(ProcessState.TERMINATED);
                    currentProcess.calculateMetrics(currentTime);
                    completedProcesses.insertBegin(cpu.releaseProcess());
                }
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
     * Ordena los procesos por tiempo de llegada (Bubble Sort)
     */
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
        return preemptive ? "SRTF (Shortest Remaining Time First) - Preemptive" : "SRTF (Non-Preemptive)";
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
     * Establece si el algoritmo es expulsivo
     */
    public void setPreemptive(boolean preemptive) {
        this.preemptive = preemptive;
    }

    /**
     * Verifica si el algoritmo es expulsivo
     */
    public boolean isPreemptive() {
        return preemptive;
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

    /**
     * Obtiene la cola de listos
     */
    public Lista<Process> getReadyQueue() {
        return readyQueue;
    }
}