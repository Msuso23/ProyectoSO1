package Algoritmos;

import EDD.Lista;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;
import Controladores.IOManager;

/**
 * Algoritmo Round Robin (RR) con soporte I/O
 * Cada proceso recibe un quantum de tiempo
 */
public class RoundRobinScheduler implements Scheduler {
    private Queue<Process> readyQueue;
    private Queue<Process> blockedQueue;
    private CPU cpu;
    private Lista<Process> allProcesses;
    private Lista<Process> completedProcesses;
    private IOManager ioManager;
    private int quantum; // Tiempo de quantum
    private int currentQuantum; // Quantum restante del proceso actual

    public RoundRobinScheduler(int quantum) {
        this.readyQueue = new Queue<>();
        this.blockedQueue = new Queue<>();
        this.cpu = new CPU();
        this.allProcesses = new Lista<>();
        this.completedProcesses = new Lista<>();
        this.ioManager = new IOManager();
        this.quantum = quantum;
        this.currentQuantum = quantum;
    }

    @Override
    public void addProcess(Process process) {
        // ✅ CAMBIO: Agregar DIRECTAMENTE a readyQueue
        process.setState(ProcessState.READY);
        readyQueue.enqueue(process);

        // También agregar a allProcesses
        allProcesses.insertBegin(process);
    }

    @Override
    public Process selectNextProcess() {
        if (readyQueue.isEmpty()) {
            return null;
        }
        currentQuantum = quantum; // Reiniciar quantum
        return readyQueue.dequeue();
    }

    @Override
    public Lista<Process> executeSimulation() {
        int currentTime = 0;
        int processIndex = 0;

        sortProcessesByArrivalTime();

        while (completedProcesses.getSize() < allProcesses.getSize() || !blockedQueue.isEmpty() || cpu.isBusy()) {
            // 1. Agregar procesos que han llegado
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
                // Los procesos que retornan de I/O se les reinicia el quantum
                readyQueue.enqueue(p);
            }

            // 3. Asignar proceso si CPU ociosa
            if (cpu.isIdle() && !readyQueue.isEmpty()) {
                Process nextProcess = selectNextProcess();
                cpu.assignProcess(nextProcess);
            }

            // 4. Ejecutar ciclo
            if (!cpu.isIdle()) {
                Process currentProcess = cpu.getCurrentProcess();
                boolean canContinue = cpu.executeCycle(currentTime);
                currentQuantum--;

                // Verificar si el proceso terminó
                if (currentProcess.isFinished()) {
                    currentProcess.setState(ProcessState.TERMINATED);
                    currentProcess.calculateMetrics(currentTime);
                    completedProcesses.insertBegin(cpu.releaseProcess());
                    currentQuantum = quantum; // Reiniciar para siguiente proceso
                }
                // Verificar si necesita operación I/O
                else if (!canContinue) {
                    currentProcess.setState(ProcessState.BLOCKED);
                    ioManager.blockProcess(cpu.releaseProcess());
                    currentQuantum = quantum; // Reiniciar para siguiente proceso
                }
                // Verificar si se agotó el quantum
                else if (currentQuantum <= 0) {
                    // Quantum agotado, devolver a cola
                    currentProcess.setState(ProcessState.READY);
                    readyQueue.enqueue(cpu.releaseProcess());
                    currentQuantum = quantum; // Reiniciar
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
        return "Round Robin (RR) - Quantum: " + quantum;
    }

    @Override
    public void reset() {
        readyQueue.clear();
        blockedQueue.clear();
        cpu.reset();
        completedProcesses.vaciar();
        ioManager.reset();
        currentQuantum = quantum;
    }

    @Override
    public boolean hasProcesses() {
        return !readyQueue.isEmpty();
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    public int getQuantum() {
        return quantum;
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