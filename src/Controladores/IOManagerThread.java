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
    private Scheduler scheduler;
    private volatile boolean running;
    private volatile boolean paused = false; // ✅ NUEVO: Flag de pausa
    private final Object pauseLock = new Object(); // ✅ NUEVO: Lock para pausar
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

    public void updateScheduler(Scheduler newScheduler) {
        synchronized (this) {
            this.scheduler = newScheduler;
            System.out.println("  🔄 [IOManagerThread] Scheduler actualizado a: " + newScheduler.getAlgorithmName());
        }
    }

    public void setListener(IOListener listener) {
        this.listener = listener;
    }

    // ✅ NUEVO: Método para pausar el hilo
    public void pauseIOManager() {
        paused = true;
        System.out.println("⏸️ [IOManagerThread] Pausado");
    }

    // ✅ NUEVO: Método para reanudar el hilo
    public void resumeIOManager() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Despertar el hilo pausado
            System.out.println("▶️ [IOManagerThread] Reanudado");
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

                ioSemaphore.acquire();

                Queue<Process> unblocked;
                synchronized (ioManager) {
                    unblocked = ioManager.processIOCycle();
                }

                // ✅ MODIFICADO: Log de debugging al devolver procesos
                while (!unblocked.isEmpty()) {
                    Process p = unblocked.dequeue();

                    synchronized (p) {
                        p.setState(ProcessState.READY);
                    }

                    // ✅ NUEVO: Verificar que el scheduler esté disponible
                    if (scheduler == null) {
                        System.err.println("⚠️ [IOManagerThread] ERROR: scheduler es null al devolver P" + p.getPid());
                        continue;
                    }

                    synchronized (scheduler) {
                        scheduler.addProcess(p);
                        System.out.println("  🔓 [IOManagerThread] P" + p.getPid() + " devuelto a scheduler (RT="
                                + p.getRemainingTime() + ")");
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

        // ✅ NUEVO: Si está pausado, despertarlo para que pueda terminar
        if (paused) {
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll();
            }
        }

        ioSemaphore.release();
        interrupt();
    }

    public boolean isRunning() {
        return running;
    }
}