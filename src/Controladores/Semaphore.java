package Controladores;

/**
 * Implementación de un semáforo contador usando primitivas de sincronización de Java
 * NO usa java.util.concurrent.Semaphore, implementado desde cero
 */
public class Semaphore {
    private int permits;
    private final int maxPermits;

    /**
     * Constructor de semáforo con permisos iniciales
     * @param permits Número de permisos disponibles
     */
    public Semaphore(int permits) {
        this.permits = permits;
        this.maxPermits = permits;
    }

    /**
     * Adquiere un permiso (P operation / wait)
     * Bloquea el hilo si no hay permisos disponibles
     */
    public synchronized void acquire() throws InterruptedException {
        while (permits <= 0) {
            wait();
        }
        permits--;
    }

    /**
     * Intenta adquirir un permiso sin bloquear
     * @return true si adquirió el permiso, false si no hay disponibles
     */
    public synchronized boolean tryAcquire() {
        if (permits > 0) {
            permits--;
            return true;
        }
        return false;
    }

    /**
     * Libera un permiso (V operation / signal)
     * Despierta a un hilo en espera si lo hay
     */
    public synchronized void release() {
        if (permits < maxPermits) {
            permits++;
            notify();
        }
    }

    /**
     * Obtiene el número de permisos disponibles
     */
    public synchronized int availablePermits() {
        return permits;
    }

    /**
     * Libera todos los hilos en espera
     */
    public synchronized void releaseAll() {
        permits = maxPermits;
        notifyAll();
    }
}
