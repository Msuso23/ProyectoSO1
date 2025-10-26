package Algoritmos;

import EDD.Queue;
import EDD.Lista;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;

/**
 * Algoritmo Multilevel Feedback Queue (MLFQ)
 * - 4 niveles de prioridad (0 = mayor prioridad)
 * - Compatible con sistema de hilos concurrentes
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

    // ✅ MODIFICADO: CPU ahora es recibido externamente, no creado internamente
    private CPU cpu;
    private int currentTime;
    private int[] quantums = { 1, 2, 3, 4 };
    private int agingThreshold = 10;

    public MultilevelFeedbackQueueScheduler() {
        this.queue0 = new Queue<>();
        this.queue1 = new Queue<>();
        this.queue2 = new Queue<>();
        this.queue3 = new Queue<>();
        this.newQueue = new Queue<>();
        this.blockedQueue = new Queue<>();
        this.terminatedQueue = new Queue<>();
        // ❌ ELIMINAR: this.cpu = new CPU();
        this.cpu = null; // ✅ Se asignará después con setCPU()
        this.currentTime = 0;
    }

    // ✅ NUEVO: Método para asignar el CPU externo
    public void setCPU(CPU cpu) {
        this.cpu = cpu;
    }

    @Override
    public void addProcess(Process process) {
        if (process.getState() == ProcessState.READY) {
            process.setCurrentQueueLevel(0);
            process.resetQuantumUsedInCurrentLevel();
            queue0.enqueue(process);
            System.out
                    .println("  📥 [MLFQ] P" + process.getPid() + " agregado directamente a Queue0 (ya estaba READY)");
        } else {
            process.setState(ProcessState.NEW);
            newQueue.enqueue(process);
            System.out.println("  📥 [MLFQ] P" + process.getPid() + " agregado a newQueue (será admitido según AT)");
        }
    }

    @Override
    public Process selectNextProcess() {
        admitNewProcesses();

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

    // ✅ MODIFICADO: processSingleCycle ahora trabaja con el CPU externo
    public void processSingleCycle(int globalClock) {
        // ✅ NUEVO: Verificar que el CPU esté asignado
        if (cpu == null) {
            System.err.println("⚠️ [MLFQ] ERROR: CPU no asignado. Llamar setCPU() primero.");
            return;
        }

        this.currentTime = globalClock;

        admitNewProcesses();
        processBlockedQueue();

        if (currentTime % agingThreshold == 0 && currentTime > 0) {
            applyAging();
        }

        // ✅ MODIFICADO: Usar el CPU externo compartido
        synchronized (cpu) {
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

                    System.out.println("  ✅ [MLFQ] P" + current.getPid() + " TERMINADO (CT=" + currentTime + ")");
                    return;
                }

                if (!canContinue) {
                    current.setState(ProcessState.BLOCKED);
                    blockedQueue.enqueue(cpu.releaseProcess());

                    System.out.println("  🚫 [MLFQ] P" + current.getPid() + " bloqueado por I/O");
                    return;
                }

                if (quantumUsed >= quantum && !current.isFinished()) {
                    current.degradePriority();
                    current.setState(ProcessState.READY);
                    current.resetQuantumUsedInCurrentLevel();

                    Queue<Process> targetQueue = getQueueForLevel(current.getCurrentQueueLevel());
                    targetQueue.enqueue(cpu.releaseProcess());

                    System.out.println("  ⬇️ [MLFQ] P" + current.getPid() + " agotó quantum → degradado a nivel "
                            + current.getCurrentQueueLevel());
                }

            } else {
                // CPU está libre, intentar asignar proceso
                Process nextProcess = selectNextProcess();

                if (nextProcess != null) {
                    cpu.assignProcess(nextProcess);
                    nextProcess.setState(ProcessState.RUNNING);
                    nextProcess.resetQuantumUsedInCurrentLevel();

                    System.out.println("  ▶️ [MLFQ] P" + nextProcess.getPid() + " asignado a CPU desde Queue"
                            + nextProcess.getCurrentQueueLevel());
                } else {
                    cpu.tickIdle();
                    incrementWaitingTimes();
                }
            }
        }
    }

    public void addMigratedProcess(Process process) {
        process.setCurrentQueueLevel(0);
        process.resetQuantumUsedInCurrentLevel();
        queue0.enqueue(process);

        System.out.println("  📥 P" + process.getPid() + " migrado directamente a Queue0 (MLFQ)");
    }

    public int[] getQuantums() {
        return quantums;
    }

    public void onProcessFinished(Process process) {
        process.setState(ProcessState.TERMINATED);
        process.calculateMetrics(currentTime);
        terminatedQueue.enqueue(process);
    }

    public void onProcessBlocked(Process process) {
        process.setState(ProcessState.BLOCKED);
        blockedQueue.enqueue(process);
    }

    public void onQuantumExpired(Process process) {
        process.degradePriority();
        process.setState(ProcessState.READY);
        Queue<Process> targetQueue = getQueueForLevel(process.getCurrentQueueLevel());
        targetQueue.enqueue(process);
    }

    public int getQuantumForProcess(Process process) {
        int level = process.getCurrentQueueLevel();
        return quantums[level];
    }

    public boolean hasNewProcesses() {
        return !newQueue.isEmpty();
    }

    public int getBlockedCount() {
        return blockedQueue.size();
    }

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

    private void admitNewProcesses() {
        int size = newQueue.size();
        for (int i = 0; i < size; i++) {
            Process p = newQueue.dequeue();

            if (p.getArrivalTime() <= currentTime) {
                p.setState(ProcessState.READY);
                p.setCurrentQueueLevel(0);
                p.resetQuantumUsedInCurrentLevel();
                queue0.enqueue(p);

                System.out.println("  ✅ [MLFQ] P" + p.getPid() + " admitido a Queue0 (AT=" + p.getArrivalTime()
                        + ", Clock=" + currentTime + ")");
            } else {
                newQueue.enqueue(p);
            }
        }
    }

    private void processBlockedQueue() {
        int size = blockedQueue.size();
        for (int i = 0; i < size; i++) {
            Process p = blockedQueue.dequeue();

            if (p.processIO()) {
                p.setState(ProcessState.READY);
                Queue<Process> targetQueue = getQueueForLevel(p.getCurrentQueueLevel());
                targetQueue.enqueue(p);

                System.out.println("  🔓 [MLFQ] P" + p.getPid() + " completó I/O → Queue" + p.getCurrentQueueLevel());
            } else {
                blockedQueue.enqueue(p);
            }
        }
    }

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
        // ✅ MODIFICADO: Crear CPU temporal solo para executeSimulation()
        CPU tempCPU = new CPU();
        this.cpu = tempCPU;
        currentTime = 0;

        while (!newQueue.isEmpty() || !queue0.isEmpty() || !queue1.isEmpty() ||
                !queue2.isEmpty() || !queue3.isEmpty() || !blockedQueue.isEmpty() || tempCPU.isBusy()) {

            admitNewProcesses();
            processBlockedQueue();

            if (tempCPU.isIdle() && hasProcesses()) {
                Process nextProcess = selectNextProcess();
                if (nextProcess != null) {
                    tempCPU.assignProcess(nextProcess);
                }
            }

            if (tempCPU.isBusy()) {
                Process current = tempCPU.getCurrentProcess();
                int currentLevel = current.getCurrentQueueLevel();
                int quantum = quantums[currentLevel];

                for (int q = 0; q < quantum && current.getRemainingTime() > 0; q++) {
                    boolean canContinue = tempCPU.executeCycle(currentTime);
                    currentTime++;

                    incrementWaitingTimes();

                    if (currentTime % agingThreshold == 0) {
                        applyAging();
                    }

                    if (current.isFinished()) {
                        current.setState(ProcessState.TERMINATED);
                        current.calculateMetrics(currentTime);
                        terminatedQueue.enqueue(tempCPU.releaseProcess());
                        break;
                    }

                    if (!canContinue) {
                        current.setState(ProcessState.BLOCKED);
                        blockedQueue.enqueue(tempCPU.releaseProcess());
                        break;
                    }
                }

                if (tempCPU.isBusy() && !current.isFinished() && !current.isBlocked()) {
                    current.degradePriority();
                    current.setState(ProcessState.READY);
                    Queue<Process> targetQueue = getQueueForLevel(current.getCurrentQueueLevel());
                    targetQueue.enqueue(tempCPU.releaseProcess());
                }

            } else {
                tempCPU.tickIdle();
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
        if (cpu != null) {
            cpu.reset();
        }
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

    public void setAgingThreshold(int threshold) {
        this.agingThreshold = threshold;
    }

    public void setQuantums(int q0, int q1, int q2, int q3) {
        this.quantums = new int[] { q0, q1, q2, q3 };
    }
}