package Algoritmos;

import EDD.Lista;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;
import Controladores.IOManager;

/**
 * Algoritmo First-Come, First-Served (FCFS) con soporte I/O
 * Los procesos se ejecutan en el orden en que llegan
 */
public class FCFSScheduler implements Scheduler {
    private Queue<Process> readyQueue;
    private Queue<Process> blockedQueue;
    private CPU cpu;
    private Lista<Process> allProcesses;
    private Lista<Process> completedProcesses;
    private IOManager ioManager;

    public FCFSScheduler() {
        this.readyQueue = new Queue<>();
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
        readyQueue.enqueue(process);

        // También agregar a allProcesses para estadísticas
        allProcesses.insertBegin(process);
    }

    @Override
    public Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        return readyQueue.dequeue();
    }

    @Override
    public Lista<Process> executeSimulation() {
        int currentTime = 0;
        int processIndex = 0;

        // Ordenar procesos por tiempo de llegada (bubble sort simple)
        sortProcessesByArrivalTime();

        while (completedProcesses.getSize() < allProcesses.getSize() || !blockedQueue.isEmpty() || cpu.isBusy()) {
            // 1. Agregar procesos que han llegado a la cola de listos
            while (processIndex < allProcesses.getSize() &&
                    allProcesses.get(processIndex).getArrivalTime() <= currentTime) {
                Process p = allProcesses.get(processIndex);
                p.setState(ProcessState.READY);
                readyQueue.enqueue(p);
                processIndex++;
            }

            // 2. Procesar operaciones I/O de procesos bloqueados
            Queue<Process> readyFromIO = ioManager.processIOCycle();
            while (!readyFromIO.isEmpty()) {
                Process p = readyFromIO.dequeue();
                p.setState(ProcessState.READY);
                readyQueue.enqueue(p);
            }

            // 3. Si la CPU está ociosa, asignar un nuevo proceso
            if (cpu.isIdle() && !readyQueue.isEmpty()) {
                Process nextProcess = selectNextProcess();
                cpu.assignProcess(nextProcess);
            }

            // 4. Ejecutar ciclo de CPU
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
                // CPU ociosa, avanzar tiempo
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
                    // Intercambiar
                    Process temp = allProcesses.get(j);
                    allProcesses.remove(j);
                    allProcesses.insertBegin(temp);
                }
            }
        }
    }

    @Override
    public String getAlgorithmName() {
        return "First-Come, First-Served (FCFS)";
    }

    @Override
    public void reset() {
        readyQueue.clear();
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
    /**
     * Obtiene la cola de bloqueados
     */
    public Queue<Process> getBlockedQueue() {
        return blockedQueue;
    }

    /**
     * Obtiene la cola de listos (convertida a LinkedList para compatibilidad)
     */
    public Lista<Process> getReadyQueue() {
        // Convertir Queue a LinkedList para visualización
        Lista<Process> list = new Lista<>();

        // Crear una copia temporal de la queue
        Queue<Process> temp = new Queue<>();

        // Transferir elementos de readyQueue a list y temp
        while (!readyQueue.isEmpty()) {
            Process p = readyQueue.dequeue();
            list.insertBegin(p);
            temp.enqueue(p);
        }

        // Restaurar readyQueue desde temp
        while (!temp.isEmpty()) {
            readyQueue.enqueue(temp.dequeue());
        }

        return list;
    }
}