package Controladores;

import Algoritmos.Scheduler;
import EDD.Queue;
import Modelos.Process;
import Modelos.ProcessState;

/**
 * Hilo dedicado para procesar operaciones de I/O en paralelo
 * Simula dispositivos de I/O trabajando concurrentemente con el CPU
 */
public class IOManagerThread extends Thread {
    private final IOManager ioManager;
    private final Scheduler scheduler;
    private volatile boolean running;
    private final Semaphore ioSemaphore;
    private IOListener listener;

    /**
     * Interface para notificar eventos de I/O
     */
    public interface IOListener {
        void onIOCompleted(Process process);
        void onProcessBlocked(Process process);
    }

    public IOManagerThread(IOManager ioManager, Scheduler scheduler) {
        super("IOManagerThread");
        this.ioManager = ioManager;
        this.scheduler = scheduler;
        this.running = true;
        this.ioSemaphore = new Semaphore(1);
        setDaemon(true);
    }

    public void setListener(IOListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            while (running) {
                ioSemaphore.acquire();
                
                Queue<Process> unblocked;
                synchronized (ioManager) {
                    unblocked = ioManager.processIOCycle();
                }

                while (!unblocked.isEmpty()) {
                    Process p = unblocked.dequeue();
                    
                    synchronized (p) {
                        p.setState(ProcessState.READY);
                    }
                    
                    synchronized (scheduler) {
                        scheduler.addProcess(p);
                    }

                    if (listener != null) {
                        listener.onIOCompleted(p);
                    }
                }

                ioSemaphore.release();

                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Agrega un proceso a la cola de bloqueados
     */
    public void blockProcess(Process process) {
        try {
            ioSemaphore.acquire();
            
            synchronized (process) {
                process.setState(ProcessState.BLOCKED);
            }
            
            synchronized (ioManager) {
                ioManager.blockProcess(process);
            }

            if (listener != null) {
                listener.onProcessBlocked(process);
            }
            
            ioSemaphore.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Detiene el hilo de I/O
     */
    public void stopIOManager() {
        running = false;
        ioSemaphore.release();
        interrupt();
    }

    public boolean isRunning() {
        return running;
    }
}
