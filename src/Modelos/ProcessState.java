package Modelos;

/**
 * Enumeración de los estados posibles de un proceso
 * Modelo extendido con 6 estados
 */
public enum ProcessState {
    NEW("Nuevo"), // Proceso creado pero no admitido
    READY("Listo"), // En cola de listos, esperando CPU
    RUNNING("Ejecutando"), // Ejecutándose en CPU
    BLOCKED("Bloqueado"), // Bloqueado esperando I/O
    SUSPENDED("Suspendido"), // Suspendido (memoria swap)
    TERMINATED("Terminado");// Finalizado

    private final String description;

    ProcessState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}