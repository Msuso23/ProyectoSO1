package Algoritmos;

import EDD.Queue;
import EDD.Lista;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;

/**
 * Algoritmo Multilevel Feedback Queue (MLFQ)
 * - 4 niveles de prioridad (0 = mayor prioridad)
 * - Quantum variable por nivel: Q0=2, Q1=4, Q2=8, Q3=16
 * - Aging: después de cierto tiempo en cola, sube de prioridad
 * - Degradación: al agotar quantum, baja de prioridad
 */
public class MultilevelFeedbackQueueScheduler implements Scheduler {

    // 4 colas de prioridad
    private Queue<Process> queue0; // Prioridad más alta, quantum=2
    private Queue<Process> queue1; // quantum=4
    private Queue<Process> queue2; // quantum=8
    private Queue<Process> queue3; // Prioridad más baja, quantum=16

    private Queue<Process> newQueue; // Procesos nuevos
    private Queue<Process> blockedQueue; // Procesos bloqueados por I/O
    private Queue<Process> terminatedQueue; // Procesos terminados

    private CPU cpu;
    private int currentTime;
    private int[] quantums = { 2, 4, 8, 16 }; // Quantum por nivel
    private int agingThreshold = 10; // Tiempo para aging

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
        newQueue.enqueue(process); // ✅ CORRECTO
    }

    @Override
    public Process selectNextProcess() {
        // Seleccionar de colas por prioridad
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

    // ===== NUEVOS MÉTODOS PARA COMPATIBILIDAD CON VENTANA =====

    /**
     * Procesa UN SOLO CICLO (compatible con Ventana.ejecutarCiclo())
     * Este método se llama desde Ventana en lugar de executeSimulation()
     */
    public void processSingleCycle(int globalClock) {
        this.currentTime = globalClock;

        // 1. Admitir procesos nuevos
        admitNewProcesses();

        // 2. Procesar cola de bloqueados (I/O)
        processBlockedQueue();

        // 3. Aplicar aging periódicamente
        if (currentTime % agingThreshold == 0 && currentTime > 0) {
            applyAging();
        }

        // 4. Si CPU está ocupado, procesar proceso actual
        if (cpu.isBusy()) {
            Process current = cpu.getCurrentProcess();
            int currentLevel = current.getCurrentQueueLevel();
            int quantum = quantums[currentLevel];

            // ✅ NUEVO: Obtener quantum usado en este nivel
            int quantumUsed = current.getQuantumUsedInCurrentLevel();

            // Ejecutar un ciclo
            boolean canContinue = current.execute(currentTime);
            cpu.executeCycle();

            // ✅ NUEVO: Incrementar quantum usado
            quantumUsed++;
            current.setQuantumUsedInCurrentLevel(quantumUsed);

            // Incrementar waiting time de otros procesos
            incrementWaitingTimes();

            // ✅ VERIFICAR: ¿El proceso terminó?
            if (current.isFinished()) {
                current.setState(ProcessState.TERMINATED);
                current.setCompletionTime(currentTime);
                current.calculateMetrics(currentTime);
                terminatedQueue.enqueue(cpu.releaseProcess());
                return;
            }

            // ✅ VERIFICAR: ¿Necesita I/O?
            if (!canContinue) {
                current.setState(ProcessState.BLOCKED);
                blockedQueue.enqueue(cpu.releaseProcess());
                return;
            }

            // ✅ NUEVO: VERIFICAR SI AGOTÓ EL QUANTUM
            if (quantumUsed >= quantum && !current.isFinished()) {
                // ✅ DEGRADAR PRIORIDAD
                current.degradePriority();
                current.setState(ProcessState.READY);
                current.resetQuantumUsedInCurrentLevel(); // ✅ Resetear contador

                // ✅ Agregar a la cola del nuevo nivel
                Queue<Process> targetQueue = getQueueForLevel(current.getCurrentQueueLevel());
                targetQueue.enqueue(cpu.releaseProcess());

                System.out.println("  ⬇️ P" + current.getPid() + " agotó quantum → degradado a nivel "
                        + current.getCurrentQueueLevel());
            }

        } else {
            // 5. Si CPU está libre, seleccionar siguiente proceso
            Process nextProcess = selectNextProcess();

            if (nextProcess != null) {
                cpu.assignProcess(nextProcess);
                nextProcess.setState(ProcessState.RUNNING);
                nextProcess.resetQuantumUsedInCurrentLevel(); // ✅ Resetear quantum al iniciar
            } else {
                // CPU idle
                cpu.tickIdle();
                incrementWaitingTimes();
            }
        }
    }

    public int[] getQuantums() {
        return quantums;
    }

    /**
     * Maneja la finalización de un proceso en el quantum actual
     * (Llamado desde Ventana cuando un proceso termina)
     */
    public void onProcessFinished(Process process) {
        process.setState(ProcessState.TERMINATED);
        process.calculateMetrics(currentTime);
        terminatedQueue.enqueue(process);
    }

    /**
     * Maneja el bloqueo de un proceso por I/O
     * (Llamado desde Ventana cuando un proceso necesita I/O)
     */
    public void onProcessBlocked(Process process) {
        process.setState(ProcessState.BLOCKED);
        blockedQueue.enqueue(process);
    }

    /**
     * Maneja la degradación de un proceso cuando agota su quantum
     * (Llamado desde Ventana cuando un proceso agota quantum pero no termina)
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
                p.setCurrentQueueLevel(0); // Empiezan en máxima prioridad
                queue0.enqueue(p);
            } else {
                newQueue.enqueue(p); // Todavía no llega
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
                // Terminó el I/O, regresa a cola ready (misma prioridad)
                p.setState(ProcessState.READY);
                Queue<Process> targetQueue = getQueueForLevel(p.getCurrentQueueLevel());
                targetQueue.enqueue(p);
            } else {
                // Aún bloqueado
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
                p.improvePriority(); // Sube de nivel
                Queue<Process> targetQueue = getQueueForLevel(p.getCurrentQueueLevel());
                targetQueue.enqueue(p);
            } else {
                queue.enqueue(p); // Permanece
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

            // 1. Admitir procesos nuevos
            admitNewProcesses();

            // 2. Procesar cola de bloqueados (I/O)
            processBlockedQueue();

            // 3. Si CPU está libre, seleccionar siguiente proceso
            if (cpu.isIdle() && hasProcesses()) {
                Process nextProcess = selectNextProcess();
                if (nextProcess != null) {
                    cpu.assignProcess(nextProcess);
                }
            }

            // 4. Ejecutar un ciclo del CPU
            if (cpu.isBusy()) {
                Process current = cpu.getCurrentProcess();
                int currentLevel = current.getCurrentQueueLevel();
                int quantum = quantums[currentLevel];

                // Ejecutar por el quantum del nivel actual
                for (int q = 0; q < quantum && current.getRemainingTime() > 0; q++) {
                    boolean canContinue = cpu.executeCycle(currentTime);
                    currentTime++;

                    // Incrementar waiting time de otros procesos
                    incrementWaitingTimes();

                    // Aplicar aging periódicamente
                    if (currentTime % agingThreshold == 0) {
                        applyAging();
                    }

                    // Si el proceso terminó
                    if (current.isFinished()) {
                        current.setState(ProcessState.TERMINATED);
                        current.calculateMetrics(currentTime);
                        terminatedQueue.enqueue(cpu.releaseProcess());
                        break;
                    }

                    // Si necesita I/O
                    if (!canContinue) {
                        current.setState(ProcessState.BLOCKED);
                        blockedQueue.enqueue(cpu.releaseProcess());
                        break;
                    }
                }

                // Si agotó el quantum y no terminó ni bloqueó, degradar
                if (cpu.isBusy() && !current.isFinished() && !current.isBlocked()) {
                    current.degradePriority();
                    current.setState(ProcessState.READY);
                    Queue<Process> targetQueue = getQueueForLevel(current.getCurrentQueueLevel());
                    targetQueue.enqueue(cpu.releaseProcess());
                }

            } else {
                // CPU idle
                cpu.tickIdle();
                currentTime++;
                incrementWaitingTimes();
            }
        }

        // Convertir Queue a LinkedList para compatibilidad
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

    // Getters para acceso externo
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
