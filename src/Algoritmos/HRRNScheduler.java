package Algoritmos;

import EDD.Lista;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;
import Controladores.IOManager;

/**
 * Algoritmo HRRN (Highest Response Ratio Next) con soporte I/O
 * Selecciona el proceso con mayor ratio de respuesta
 * Response Ratio = (Tiempo de Espera + Burst Time) / Burst Time
 * 
 * Ventajas:
 * - Favorece procesos cortos (como SJF)
 * - Previene inanición (procesos largos eventualmente tienen alto RR)
 * - No requiere aging explícito
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
        // ✅ CAMBIO: Agregar DIRECTAMENTE a readyQueue
        process.setState(ProcessState.READY);
        readyQueue.insertBegin(process);

        // También agregar a allProcesses
        allProcesses.insertBegin(process);
    }

    @Override
    public Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }

        // Calcular Response Ratio para cada proceso
        int bestIndex = 0;
        double highestRR = calculateResponseRatio(readyQueue.get(0));

        for (int i = 1; i < readyQueue.getSize(); i++) {
            double rr = calculateResponseRatio(readyQueue.get(i));

            // Seleccionar el proceso con mayor Response Ratio
            if (rr > highestRR) {
                highestRR = rr;
                bestIndex = i;
            }
            // En caso de empate, usar FCFS (menor arrival time)
            else if (rr == highestRR) {
                if (readyQueue.get(i).getArrivalTime() < readyQueue.get(bestIndex).getArrivalTime()) {
                    bestIndex = i;
                }
            }
        }

        return readyQueue.remove(bestIndex);
    }

    /**
     * Calcula el Response Ratio de un proceso
     * RR = (Tiempo de Espera + Burst Time Restante) / Burst Time Restante
     * 
     * Nota: Cuanto más espera, mayor es el RR
     * Cuanto menor es el burst time, mayor es el RR
     * 
     * @param process Proceso a evaluar
     * @return Response Ratio (mayor es mejor)
     */
    private double calculateResponseRatio(Process process) {
        // Tiempo de espera = tiempo actual - tiempo de llegada - tiempo ejecutado
        int waitingTime = currentTime - process.getArrivalTime() -
                (process.getBurstTime() - process.getRemainingTime());

        // Si el proceso nunca se ha ejecutado, usar getWaitingTime() directamente
        if (process.getCurrentCycle() == 0) {
            waitingTime = process.getWaitingTime();
        }

        int remainingTime = process.getRemainingTime();

        // Evitar división por cero
        if (remainingTime == 0) {
            remainingTime = 1;
        }

        // RR = (WT + BT) / BT = 1 + (WT / BT)
        double responseRatio = 1.0 + ((double) waitingTime / remainingTime);

        return responseRatio;
    }

    @Override
    public Lista<Process> executeSimulation() {
        currentTime = 0;
        int processIndex = 0;

        // Ordenar procesos por tiempo de llegada
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
            Queue<Process> readyFromIO = ioManager.processIOCycle();
            while (!readyFromIO.isEmpty()) {
                Process p = readyFromIO.dequeue();
                p.setState(ProcessState.READY);
                readyQueue.insertBegin(p);
            }

            // 3. Incrementar tiempo de espera de procesos en ready
            incrementWaitingTimes();

            // 4. Asignar proceso si CPU está libre (seleccionar por HRRN)
            if (cpu.isIdle() && !readyQueue.isEmpty()) {
                Process nextProcess = selectNextProcess();
                cpu.assignProcess(nextProcess);
            }

            // 5. Ejecutar ciclo de CPU
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
     * Calcula y retorna el Response Ratio de un proceso (para
     * debugging/visualización)
     * 
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
