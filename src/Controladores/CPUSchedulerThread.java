package Controladores;

import Algoritmos.Scheduler;
import EDD.Lista;
import Modelos.CPU;
import Modelos.Process;
import Modelos.ProcessState;

/**
 * Hilo que ejecuta el planificador de CPU
 * Selecciona procesos y los asigna al CPU de forma concurrente
 */
public class CPUSchedulerThread extends Thread {
    private final Scheduler scheduler;
    private final CPU cpu;
    private final Lista<Process> allProcesses;
    private final Semaphore cpuSemaphore;
    private volatile boolean running;
    private volatile int globalClock;
    private final Object clockLock;
    private SchedulerListener listener;

    /**
     * Interface para notificar eventos del scheduler
     */
    public interface SchedulerListener {
        void onProcessSelected(Process process);
        void onContextSwitch(Process oldProcess, Process newProcess);
        void onCycleCompleted(int clock);
    }

    public CPUSchedulerThread(Scheduler scheduler, CPU cpu, Lista<Process> allProcesses, Semaphore cpuSemaphore) {
        super("CPUSchedulerThread");
        this.scheduler = scheduler;
        this.cpu = cpu;
        this.allProcesses = allProcesses;
        this.cpuSemaphore = cpuSemaphore;
        this.running = true;
        this.globalClock = 0;
        this.clockLock = new Object();
        setDaemon(true);
    }

    public void setListener(SchedulerListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            while (running) {
                synchronized (clockLock) {
                    processArrivals();

                    if (cpu.isIdle()) {
                        Process nextProcess = null;
                        synchronized (scheduler) {
                            if (scheduler.hasProcesses()) {
                                nextProcess = scheduler.selectNextProcess();
                            }
                        }

                        if (nextProcess != null) {
                            cpuSemaphore.acquire();
                            
                            Process oldProcess = cpu.getCurrentProcess();
                            cpu.assignProcess(nextProcess);
                            nextProcess.setState(ProcessState.RUNNING);

                            if (listener != null) {
                                if (oldProcess != null && oldProcess != nextProcess) {
                                    listener.onContextSwitch(oldProcess, nextProcess);
                                } else {
                                    listener.onProcessSelected(nextProcess);
                                }
                            }

                            cpuSemaphore.release();
                        }
                    }

                    globalClock++;
                    
                    if (listener != null) {
                        listener.onCycleCompleted(globalClock);
                    }
                }

                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Procesa llegadas de procesos en el ciclo actual
     */
    private void processArrivals() {
        synchronized (allProcesses) {
            for (int i = 0; i < allProcesses.getSize(); i++) {
                Process p = allProcesses.get(i);
                synchronized (p) {
                    if (p.getArrivalTime() == globalClock && p.getState() == ProcessState.NEW) {
                        p.setState(ProcessState.READY);
                        synchronized (scheduler) {
                            scheduler.addProcess(p);
                        }
                    }
                }
            }
        }
    }

    /**
     * Detiene el hilo del scheduler
     */
    public void stopScheduler() {
        running = false;
        interrupt();
    }

    public int getGlobalClock() {
        synchronized (clockLock) {
            return globalClock;
        }
    }

    public void setGlobalClock(int clock) {
        synchronized (clockLock) {
            this.globalClock = clock;
        }
    }

    public boolean isRunning() {
        return running;
    }
}
