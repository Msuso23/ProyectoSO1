package Controladores;

import javax.swing.SwingUtilities;

import Algoritmos.MultilevelFeedbackQueueScheduler;
import Algoritmos.Scheduler;
import EDD.Lista;
import EDD.Queue;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;

public class CPUSchedulerThread extends Thread {
    private final Scheduler scheduler;
    private final CPU cpu;
    private final Lista<Process> allProcesses;
    private final Semaphore cpuSemaphore;
    private volatile boolean running;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
    private int globalClock;
    private final Object clockLock = new Object();
    private SchedulerListener listener;
    private int simulationSpeed = 100;

    public interface SchedulerListener {
        void onProcessSelected(Process process);

        void onContextSwitch(Process oldProcess, Process newProcess);

        void onCycleCompleted(int clock);

        void onProcessFinished(Process process);

        void onProcessBlocked(Process process);
    }

    // ✅ MODIFICADO: Constructor con reloj inicial
    public CPUSchedulerThread(Scheduler scheduler, CPU cpu, Lista<Process> allProcesses, Semaphore cpuSemaphore,
            int initialClock) {
        super("CPUSchedulerThread");
        this.scheduler = scheduler;
        this.cpu = cpu;
        this.allProcesses = allProcesses;
        this.cpuSemaphore = cpuSemaphore;
        this.running = true;
        this.globalClock = initialClock; // ✅ NUEVO: Iniciar con reloj proporcionado
        setDaemon(true);
    }

    // ✅ NUEVO: Constructor alternativo (sin reloj inicial, comienza en 0)
    public CPUSchedulerThread(Scheduler scheduler, CPU cpu, Lista<Process> allProcesses, Semaphore cpuSemaphore) {
        this(scheduler, cpu, allProcesses, cpuSemaphore, 0); // Delega al otro constructor
    }

    public void setListener(SchedulerListener listener) {
        this.listener = listener;
    }

    public void setSimulationSpeed(int speed) {
        this.simulationSpeed = speed;
    }

    public int getGlobalClock() {
        synchronized (clockLock) {
            return globalClock;
        }
    }

    public void pauseScheduler() {
        paused = true;
        System.out.println("⏸️ [CPUSchedulerThread] Pausado");
    }

    public void resumeScheduler() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
            System.out.println("▶️ [CPUSchedulerThread] Reanudado");
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                synchronized (pauseLock) {
                    while (paused && running) {
                        pauseLock.wait();
                    }
                }

                if (!running)
                    break;

                // 1. Procesar llegadas de procesos
                processArrivals();

                // ✅ NUEVO: Si es MLFQ, usar lógica especial
                if (scheduler instanceof MultilevelFeedbackQueueScheduler) {
                    MultilevelFeedbackQueueScheduler mlfq = (MultilevelFeedbackQueueScheduler) scheduler;

                    // Guardar procesos terminados antes del ciclo
                    int terminatedBefore = mlfq.getTerminatedQueue().size();

                    // Ejecutar ciclo de MLFQ
                    mlfq.processSingleCycle(globalClock);

                    // ✅ Detectar si terminó algún proceso
                    int terminatedAfter = mlfq.getTerminatedQueue().size();
                    if (terminatedAfter > terminatedBefore) {
                        // Obtener el proceso recién terminado
                        Queue<Process> terminated = mlfq.getTerminatedQueue();
                        Queue<Process> temp = new Queue<>();

                        // Extraer todos los procesos
                        int count = terminated.size();
                        for (int i = 0; i < count; i++) {
                            Process p = terminated.dequeue();
                            temp.enqueue(p);

                            // Solo notificar los nuevos (últimos terminados)
                            if (i >= terminatedBefore) {
                                final Process finishedProcess = p;
                                SwingUtilities.invokeLater(() -> {
                                    if (listener != null) {
                                        listener.onProcessFinished(finishedProcess);
                                    }
                                });
                            }
                        }

                        // Restaurar la cola
                        while (!temp.isEmpty()) {
                            terminated.enqueue(temp.dequeue());
                        }
                    }

                    // Incrementar reloj y notificar
                    synchronized (clockLock) {
                        globalClock++;
                        if (listener != null) {
                            listener.onCycleCompleted(globalClock);
                        }
                    }

                    Thread.sleep(simulationSpeed);
                    continue; // Saltar al siguiente ciclo
                }

                // 2. Lógica para schedulers NO-MLFQ (FCFS, SJF, RR, etc.)
                Process currentProcess;
                synchronized (cpu) {
                    currentProcess = cpu.getCurrentProcess();
                }

                if (currentProcess == null) {
                    Process nextProcess;
                    synchronized (scheduler) {
                        nextProcess = scheduler.selectNextProcess();
                    }

                    if (nextProcess != null) {
                        try {
                            cpuSemaphore.acquire();

                            synchronized (cpu) {
                                cpu.assignProcess(nextProcess);
                            }

                            synchronized (nextProcess) {
                                nextProcess.setState(ProcessState.RUNNING);
                            }

                            if (listener != null) {
                                listener.onProcessSelected(nextProcess);
                            }

                            System.out.println("  ▶️ [CPUSchedulerThread] P" + nextProcess.getPid() +
                                    " asignado a CPU (RT=" + nextProcess.getRemainingTime() + ")");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

                synchronized (cpu) {
                    currentProcess = cpu.getCurrentProcess();

                    if (currentProcess != null) {
                        boolean canContinue;
                        synchronized (currentProcess) {
                            canContinue = currentProcess.execute(globalClock);
                        }

                        if (currentProcess.isFinished()) {
                            System.out.println("  ✅ [CPUSchedulerThread] P" + currentProcess.getPid() + " TERMINADO");

                            synchronized (currentProcess) {
                                currentProcess.setState(ProcessState.TERMINATED);
                                currentProcess.setCompletionTime(globalClock);
                                currentProcess.calculateMetrics(globalClock);
                            }

                            cpu.releaseProcess();
                            cpuSemaphore.release();

                            if (listener != null) {
                                listener.onProcessFinished(currentProcess);
                            }
                        } else if (!canContinue) {
                            System.out.println(
                                    "  🚫 [CPUSchedulerThread] P" + currentProcess.getPid() + " bloqueado por I/O");

                            synchronized (currentProcess) {
                                currentProcess.setState(ProcessState.BLOCKED);
                            }

                            cpu.releaseProcess();
                            cpuSemaphore.release();

                            if (listener != null) {
                                listener.onProcessBlocked(currentProcess);
                            }
                        } else {
                            cpu.executeCycle();
                        }
                    } else {
                        cpu.tickIdle();
                    }
                }

                synchronized (clockLock) {
                    globalClock++;

                    if (listener != null) {
                        listener.onCycleCompleted(globalClock);
                    }
                }

                Thread.sleep(simulationSpeed);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processArrivals() {
        int currentClock;
        synchronized (clockLock) {
            currentClock = globalClock;
        }

        for (int i = 0; i < allProcesses.getSize(); i++) {
            Process p = allProcesses.get(i);

            synchronized (p) {
                if (p.getArrivalTime() == currentClock && p.getState() == ProcessState.NEW) {
                    p.setState(ProcessState.READY);

                    synchronized (scheduler) {
                        scheduler.addProcess(p);
                    }

                    System.out.println("🕐 [CPUSchedulerThread] Ciclo " + currentClock +
                            ": P" + p.getPid() + " LLEGÓ (AT=" + p.getArrivalTime() + ")");
                }
            }
        }
    }

    public void stopScheduler() {
        running = false;

        if (paused) {
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll();
            }
        }

        interrupt();
    }

    public boolean isRunning() {
        return running;
    }
}