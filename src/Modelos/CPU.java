package Modelos;

/**
 * Modelo que representa la CPU del sistema con soporte para I/O
 */
public class CPU {
    private Process currentProcess; 
    private int totalExecutionTime; 
    private int idleTime; 
    private int contextSwitchTime;
    private boolean idle; 
    private int clockTime; 

    public CPU() {
        this.currentProcess = null;
        this.totalExecutionTime = 0;
        this.idleTime = 0;
        this.contextSwitchTime = 0;
        this.idle = true;
        this.clockTime = 0;
    }

    /**
     * Asigna un proceso a la CPU
     */
    public void assignProcess(Process process) {
        if (currentProcess != null && currentProcess != process) {
            currentProcess.incrementContextSwitches();
            contextSwitchTime++;
        }

        this.currentProcess = process;
        this.idle = false;
        if (process != null) {
            process.setState(ProcessState.RUNNING);
        }
    }

    /**
     * Ejecuta un ciclo de CPU (método original para compatibilidad)
     * 
     * @return true si el proceso terminó
     */
    public boolean executeCycle() {
        if (currentProcess == null) {
            idle = true;
            idleTime++;
            return false;
        }

        currentProcess.execute(clockTime);
        totalExecutionTime++;
        clockTime++;

        if (currentProcess.isFinished()) {
            currentProcess.setState(ProcessState.TERMINATED);
            currentProcess.setCompletionTime(clockTime);
            currentProcess.calculateMetrics();
            return true;
        }

        return false;
    }

    /**
     * Ejecuta un ciclo del proceso actual con manejo de I/O
     * 
     * @param currentTime Tiempo actual de la simulación
     * @return true si el proceso puede continuar, false si necesita I/O
     */
    public boolean executeCycle(int currentTime) {
        if (currentProcess == null) {
            idleTime++;
            idle = true;
            clockTime++;
            return false;
        }

        boolean canContinue = currentProcess.execute(currentTime);
        totalExecutionTime++;
        clockTime++;
        idle = false;

        return canContinue;
    }

    /**
     * Libera la CPU
     */
    public Process releaseProcess() {
        Process temp = currentProcess;
        if (temp != null) {
            if (temp.getState() == ProcessState.RUNNING) {
                temp.setState(ProcessState.READY);
            }
        }
        currentProcess = null;
        idle = true;
        return temp;
    }

    /**
     * Avanza el reloj cuando está ociosa
     */
    public void tickIdle() {
        clockTime++;
        idleTime++;
        idle = true;
    }

    // Getters
    public Process getCurrentProcess() {
        return currentProcess;
    }

    public int getClockTime() {
        return clockTime;
    }

    public boolean isIdle() {
        return idle;
    }

    public boolean isBusy() {
        return !idle && currentProcess != null;
    }

    public void resetClock() {
        this.clockTime = 0;
    }

    /**
     * Obtiene el tiempo total de ejecución
     */
    public int getTotalExecutionTime() {
        return totalExecutionTime;
    }

    /**
     * Obtiene el tiempo inactivo
     */
    public int getIdleTime() {
        return idleTime;
    }

    /**
     * Obtiene el tiempo en cambios de contexto
     */
    public int getContextSwitchTime() {
        return contextSwitchTime;
    }

    /**
     * Calcula el porcentaje de utilización del CPU
     */
    public double getUtilization() {
        if (totalExecutionTime + idleTime == 0) {
            return 0.0;
        }
        return (double) totalExecutionTime / (totalExecutionTime + idleTime) * 100.0;
    }

    /**
     * Reinicia las estadísticas del CPU
     */
    public void reset() {
        currentProcess = null;
        totalExecutionTime = 0;
        idleTime = 0;
        contextSwitchTime = 0;
        idle = true;
        clockTime = 0;
    }

    @Override
    public String toString() {
        if (currentProcess != null) {
            return String.format("CPU[Ejecutando: %s]", currentProcess);
        }
        return "CPU[Inactivo]";
    }
}