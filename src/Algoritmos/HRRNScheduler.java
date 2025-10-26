package Algoritmos;

import EDD.Lista;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;
import Controladores.IOManager;

/**
 * Algoritmo HRRN (Highest Response Ratio Next)
 * Selecciona el proceso con mayor ratio de respuesta
 * Response Ratio = (Tiempo de Espera + Burst Time) / Burst Time
 */
public class HRRNScheduler implements Scheduler {
    private Lista<Process> readyQueue;
    private Queue<Process> blockedQueue;
    private CPU cpu;
    private Lista<Process> allProcesses;
    private Lista<Process> completedProcesses;
    private IOManager ioManager;
    private int currentTime = 0;

    public HRRNScheduler() {
        this.readyQueue = new Lista<>();
        this.blockedQueue = new Queue<>();
        this.cpu = new CPU();
        this.allProcesses = new Lista<>();
        this.completedProcesses = new Lista<>();
        this.ioManager = new IOManager();
    }

    @Override
    public void addProcess(Process process) {
        process.setState(ProcessState.READY);
        readyQueue.insertBegin(process);

        allProcesses.insertBegin(process);

        // ✅ NUEVO: Log de debugging
        System.out.println("  📥 [HRRN] Proceso agregado: P" + process.getPid() +
                " (Estado=" + process.getState() +
                ", RT=" + process.getRemainingTime() +
                ", WT=" + process.getWaitingTime() + ")");
        System.out.println("  📋 [HRRN] Cola READY ahora tiene " + readyQueue.getSize() + " procesos");
    }

    @Override
    public Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }

        // ✅ MODIFICADO: Usar el tiempo de espera real de cada proceso
        Process selectedProcess = null;
        double highestRatio = -1;

        for (int i = 0; i < readyQueue.getSize(); i++) {
            Process p = readyQueue.get(i);

            // ✅ Calcular ratio usando el tiempo de espera acumulado del proceso
            int waitingTime = p.getWaitingTime();
            int burstTime = p.getRemainingTime();

            // Response Ratio = (Waiting Time + Burst Time) / Burst Time
            double ratio = (waitingTime + burstTime) / (double) burstTime;

            System.out.println("  [HRRN] P" + p.getPid() + " - WT=" + waitingTime +
                    ", BT=" + burstTime + ", Ratio=" + String.format("%.2f", ratio));

            if (ratio > highestRatio) {
                highestRatio = ratio;
                selectedProcess = p;
            }
        }

        if (selectedProcess != null) {
            // Remover de la cola
            for (int i = 0; i < readyQueue.getSize(); i++) {
                if (readyQueue.get(i).getPid() == selectedProcess.getPid()) {
                    readyQueue.remove(i);
                    break;
                }
            }

            System.out.println("  ✅ [HRRN] Seleccionado: P" + selectedProcess.getPid() +
                    " (Ratio=" + String.format("%.2f", highestRatio) + ")");
        }

        return selectedProcess;
    }

    /**
     * Calcula el Response Ratio de un proceso
     * 
     * @param process Proceso a evaluar
     * @return Response Ratio (mayor es mejor)
     */
    private double calculateResponseRatio(Process process) {
        int waitingTime = currentTime - process.getArrivalTime() -
                (process.getBurstTime() - process.getRemainingTime());

        if (process.getCurrentCycle() == 0) {
            waitingTime = process.getWaitingTime();
        }

        int remainingTime = process.getRemainingTime();

        if (remainingTime == 0) {
            remainingTime = 1;
        }

        double responseRatio = 1.0 + ((double) waitingTime / remainingTime);

        return responseRatio;
    }

    @Override
    public Lista<Process> executeSimulation() {
        currentTime = 0;
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

            incrementWaitingTimes();

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
                } else if (!canContinue) {
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
     * Incrementa el tiempo de espera de procesos en ready
     */
    private void incrementWaitingTimes() {
        for (int i = 0; i < readyQueue.getSize(); i++) {
            Process p = readyQueue.get(i);
            p.incrementWaitingTime();
        }
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
        return "HRRN (Highest Response Ratio Next)";
    }

    @Override
    public void reset() {
        readyQueue.vaciar();
        blockedQueue.clear();
        cpu.reset();
        completedProcesses.vaciar();
        ioManager.reset();
        currentTime = 0;
    }

    @Override
    public boolean hasProcesses() {
        return !readyQueue.isEmpty();
    }

    // Getters y Setters

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

    /**
     * @param process Proceso a evaluar
     * @return Response Ratio actual
     */
    public double getProcessResponseRatio(Process process) {
        return calculateResponseRatio(process);
    }

    /**
     * Obtiene el tiempo actual de la simulación
     */
    public int getCurrentTime() {
        return currentTime;
    }
}
