package Controladores;

import Modelos.Process;
import Modelos.ProcessState;

/**
 * Hilo que representa un proceso en ejecución
 * Cada proceso tiene su propio hilo que se bloquea/desbloquea según su estado
 */
public class ProcessThread extends Thread {
    private final Process process;
    private final Semaphore executionSemaphore;
    private volatile boolean running;
    private volatile boolean terminated;
    private ProcessThreadListener listener;

    /**
     * Interface para notificar eventos del proceso
     */
    public interface ProcessThreadListener {
        void onProcessNeedsIO(Process process);
        void onProcessFinished(Process process);
        void onCycleExecuted(Process process);
    }

    public ProcessThread(Process process) {
        super("ProcessThread-P" + process.getPid());
        this.process = process;
        this.executionSemaphore = new Semaphore(0);
        this.running = true;
        this.terminated = false;
        setDaemon(true);
    }

    public void setListener(ProcessThreadListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            while (running && !terminated) {
                executionSemaphore.acquire();
                
                if (terminated) break;

                synchronized (process) {
                    if (process.getState() == ProcessState.RUNNING) {
                        int currentTime = (int) System.currentTimeMillis();
                        boolean canContinue = process.execute(currentTime);

                        if (listener != null) {
                            listener.onCycleExecuted(process);
                        }

                        if (process.isFinished()) {
                            process.setState(ProcessState.TERMINATED);
                            terminated = true;
                            if (listener != null) {
                                listener.onProcessFinished(process);
                            }
                            break;
                        }

                        if (!canContinue) {
                            process.setState(ProcessState.BLOCKED);
                            if (listener != null) {
                                listener.onProcessNeedsIO(process);
                            }
                        }
                    }
                }

                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Permite que el proceso ejecute un ciclo
     */
    public void allowExecution() {
        if (!terminated) {
            executionSemaphore.release();
        }
    }

    /**
     * Detiene el hilo del proceso
     */
    public void stopProcess() {
        running = false;
        terminated = true;
        executionSemaphore.release();
    }

    public Process getProcess() {
        return process;
    }

    public boolean isProcessTerminated() {
        return terminated;
    }
}
