package Algoritmos;

import EDD.Queue;
import EDD.Lista;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;

/**
 * Algoritmo Multilevel Feedback Queue (MLFQ)
 * - 4 niveles de prioridad (0 = mayor prioridad)
 */
public class MultilevelFeedbackQueueScheduler implements Scheduler {

    // 4 colas de prioridad
    private Queue<Process> queue0;
    private Queue<Process> queue1;
    private Queue<Process> queue2;
    private Queue<Process> queue3;

    private Queue<Process> newQueue;
    private Queue<Process> blockedQueue;
    private Queue<Process> terminatedQueue;

    private CPU cpu;
    private int currentTime;
    private int[] quantums = { 2, 4, 8, 16 };
    private int agingThreshold = 10;

    public MultilevelFeedbackQueueScheduler() {
        this.queue0 = new Queue<>();
        this.queue1 = new Queue<>();
        this.queue2 = new Queue<>();
        this.queue3 = new Queue<>();
        this.newQueue = new Queue<>();
        this.blockedQueue = new Queue<>();
        this.terminatedQueue = new Queue<>();
        this.cpu = new CPU();
        this.currentTime = 0;
    }

    @Override
    public void addProcess(Process process) {
        process.setState(ProcessState.NEW);
        newQueue.enqueue(process);
    }

    @Override
    public Process selectNextProcess() {
        if (!queue0.isEmpty())
            return queue0.dequeue();
        if (!queue1.isEmpty())
            return queue1.dequeue();
        if (!queue2.isEmpty())
            return queue2.dequeue();
        if (!queue3.isEmpty())
            return queue3.dequeue();
        return null;
    }

    public void processSingleCycle(int globalClock) {
        this.currentTime = globalClock;

        admitNewProcesses();

        processBlockedQueue();

        if (currentTime % agingThreshold == 0 && currentTime > 0) {
            applyAging();
        }

        if (cpu.isBusy()) {
            Process current = cpu.getCurrentProcess();
            int currentLevel = current.getCurrentQueueLevel();
            int quantum = quantums[currentLevel];

            int quantumUsed = current.getQuantumUsedInCurrentLevel();

            boolean canContinue = current.execute(currentTime);
            cpu.executeCycle();

            quantumUsed++;
            current.setQuantumUsedInCurrentLevel(quantumUsed);

            incrementWaitingTimes();

            if (current.isFinished()) {
                current.setState(ProcessState.TERMINATED);
                current.setCompletionTime(currentTime);
                current.calculateMetrics(currentTime);
                terminatedQueue.enqueue(cpu.releaseProcess());
                return;
            }

            if (!canContinue) {
                current.setState(ProcessState.BLOCKED);
                blockedQueue.enqueue(cpu.releaseProcess());
                return;
            }

            if (quantumUsed >= quantum && !current.isFinished()) {
                current.degradePriority();
                current.setState(ProcessState.READY);
                current.resetQuantumUsedInCurrentLevel();

                Queue<Process> targetQueue = getQueueForLevel(current.getCurrentQueueLevel());
                targetQueue.enqueue(cpu.releaseProcess());

                System.out.println("  ⬇️ P" + current.getPid() + " agotó quantum → degradado a nivel "
                        + current.getCurrentQueueLevel());
            }

        } else {
            Process nextProcess = selectNextProcess();

            if (nextProcess != null) {
                cpu.assignProcess(nextProcess);
                nextProcess.setState(ProcessState.RUNNING);
                nextProcess.resetQuantumUsedInCurrentLevel();
            } else {
                cpu.tickIdle();
                incrementWaitingTimes();
            }
        }
    }

    public void addMigratedProcess(Process process) {
        // No cambiar estado ni arrival time
        // Agregar directamente a queue0 (máxima prioridad)
        process.setCurrentQueueLevel(0);
        process.resetQuantumUsedInCurrentLevel();
        queue0.enqueue(process);

        System.out.println("  📥 P" + process.getPid() + " migrado directamente a Queue0 (MLFQ)");
    }

    public int[] getQuantums() {
        return quantums;
    }

    /**
     * Maneja la finalización de un proceso en el quantum actual
     */
    public void onProcessFinished(Process process) {
        process.setState(ProcessState.TERMINATED);
        process.calculateMetrics(currentTime);
        terminatedQueue.enqueue(process);
    }

    /**
     * Maneja el bloqueo de un proceso por I/O
     */
    public void onProcessBlocked(Process process) {
        process.setState(ProcessState.BLOCKED);
        blockedQueue.enqueue(process);
    }

    /**
     * Maneja la degradación de un proceso cuando agota su quantum
     */
    public void onQuantumExpired(Process process) {
        process.degradePriority();
        process.setState(ProcessState.READY);
        Queue<Process> targetQueue = getQueueForLevel(process.getCurrentQueueLevel());
        targetQueue.enqueue(process);
    }

    /**
     * Obtiene el quantum del nivel actual de un proceso
     */
    public int getQuantumForProcess(Process process) {
        int level = process.getCurrentQueueLevel();
        return quantums[level];
    }

    /**
     * Verifica si hay procesos en la cola NEW esperando ser admitidos
     */
    public boolean hasNewProcesses() {
        return !newQueue.isEmpty();
    }

    /**
     * Obtiene el número de procesos bloqueados
     */
    public int getBlockedCount() {
        return blockedQueue.size();
    }

    /**
     * Obtiene la cola correspondiente al nivel
     */
    private Queue<Process> getQueueForLevel(int level) {
        switch (level) {
            case 0:
                return queue0;
            case 1:
                return queue1;
            case 2:
                return queue2;
            case 3:
                return queue3;
            default:
                return queue3;
        }
    }

    /**
     * Admite procesos nuevos que ya llegaron
     */
    private void admitNewProcesses() {
        int size = newQueue.size();
        for (int i = 0; i < size; i++) {
            Process p = newQueue.dequeue();
            if (p.getArrivalTime() <= currentTime) {
                p.setState(ProcessState.READY);
                p.setCurrentQueueLevel(0);
                queue0.enqueue(p);
            } else {
                newQueue.enqueue(p);
            }
        }
    }

    /**
     * Procesa las operaciones I/O de procesos bloqueados
     */
    private void processBlockedQueue() {
        int size = blockedQueue.size();
        for (int i = 0; i < size; i++) {
            Process p = blockedQueue.dequeue();

            if (p.processIO()) {
                p.setState(ProcessState.READY);
                Queue<Process> targetQueue = getQueueForLevel(p.getCurrentQueueLevel());
                targetQueue.enqueue(p);
            } else {
                blockedQueue.enqueue(p);
            }
        }
    }

    /**
     * Aplica aging a procesos que llevan mucho tiempo esperando
     */
    private void applyAging() {
        applyAgingToQueue(queue1);
        applyAgingToQueue(queue2);
        applyAgingToQueue(queue3);
    }

    private void applyAgingToQueue(Queue<Process> queue) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            Process p = queue.dequeue();

            if (p.getTimeInCurrentQueue() >= agingThreshold) {
                p.improvePriority();
                Queue<Process> targetQueue = getQueueForLevel(p.getCurrentQueueLevel());
                targetQueue.enqueue(p);
            } else {
                queue.enqueue(p);
            }
        }
    }

    /**
     * Incrementa tiempo de espera de procesos en colas ready
     */
    private void incrementWaitingTimes() {
        incrementWaitingInQueue(queue0);
        incrementWaitingInQueue(queue1);
        incrementWaitingInQueue(queue2);
        incrementWaitingInQueue(queue3);
    }

    private void incrementWaitingInQueue(Queue<Process> queue) {
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            Process p = queue.dequeue();
            p.incrementWaitingTime();
            queue.enqueue(p);
        }
    }

    @Override
    public Lista<Process> executeSimulation() {
        currentTime = 0;

        while (!newQueue.isEmpty() || !queue0.isEmpty() || !queue1.isEmpty() ||
                !queue2.isEmpty() || !queue3.isEmpty() || !blockedQueue.isEmpty() || cpu.isBusy()) {

            admitNewProcesses();

            processBlockedQueue();

            if (cpu.isIdle() && hasProcesses()) {
                Process nextProcess = selectNextProcess();
                if (nextProcess != null) {
                    cpu.assignProcess(nextProcess);
                }
            }

            if (cpu.isBusy()) {
                Process current = cpu.getCurrentProcess();
                int currentLevel = current.getCurrentQueueLevel();
                int quantum = quantums[currentLevel];

                for (int q = 0; q < quantum && current.getRemainingTime() > 0; q++) {
                    boolean canContinue = cpu.executeCycle(currentTime);
                    currentTime++;

                    incrementWaitingTimes();

                    if (currentTime % agingThreshold == 0) {
                        applyAging();
                    }

                    if (current.isFinished()) {
                        current.setState(ProcessState.TERMINATED);
                        current.calculateMetrics(currentTime);
                        terminatedQueue.enqueue(cpu.releaseProcess());
                        break;
                    }

                    if (!canContinue) {
                        current.setState(ProcessState.BLOCKED);
                        blockedQueue.enqueue(cpu.releaseProcess());
                        break;
                    }
                }

                if (cpu.isBusy() && !current.isFinished() && !current.isBlocked()) {
                    current.degradePriority();
                    current.setState(ProcessState.READY);
                    Queue<Process> targetQueue = getQueueForLevel(current.getCurrentQueueLevel());
                    targetQueue.enqueue(cpu.releaseProcess());
                }

            } else {
                cpu.tickIdle();
                currentTime++;
                incrementWaitingTimes();
            }
        }

        Lista<Process> result = new Lista<>();
        while (!terminatedQueue.isEmpty()) {
            result.insertBegin(terminatedQueue.dequeue());
        }
        return result;
    }

    @Override
    public boolean hasProcesses() {
        return !queue0.isEmpty() || !queue1.isEmpty() ||
                !queue2.isEmpty() || !queue3.isEmpty();
    }

    @Override
    public void reset() {
        queue0.clear();
        queue1.clear();
        queue2.clear();
        queue3.clear();
        newQueue.clear();
        blockedQueue.clear();
        terminatedQueue.clear();
        cpu.reset();
        currentTime = 0;
    }

    @Override
    public String getAlgorithmName() {
        return "Multilevel Feedback Queue (MLFQ)";
    }

    public Queue<Process> getQueue0() {
        return queue0;
    }

    public Queue<Process> getQueue1() {
        return queue1;
    }

    public Queue<Process> getQueue2() {
        return queue2;
    }

    public Queue<Process> getQueue3() {
        return queue3;
    }

    public Queue<Process> getNewQueue() {
        return newQueue;
    }

    public Queue<Process> getBlockedQueue() {
        return blockedQueue;
    }

    public Queue<Process> getTerminatedQueue() {
        return terminatedQueue;
    }

    public CPU getCpu() {
        return cpu;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    /**
     * Establece el umbral de aging
     */
    public void setAgingThreshold(int threshold) {
        this.agingThreshold = threshold;
    }

    /**
     * Establece los quantums personalizados
     */
    public void setQuantums(int q0, int q1, int q2, int q3) {
        this.quantums = new int[] { q0, q1, q2, q3 };
    }
}
