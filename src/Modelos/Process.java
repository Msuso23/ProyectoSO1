package Modelos;

/**
 * Modelo que representa un proceso en el sistema operativo con soporte completo
 * para I/O
 */
public class Process {
    private int pid;
    private String name;

    private int arrivalTime;
    private int burstTime;
    private int remainingTime;

    private int priority;
    private int basePriority;
    private ProcessType type;

    private int ioCycle;
    private int ioDuration;
    private int currentCycle;
    private int ioRemaining;
    private int totalIoTime;

    private int memorySize;
    private int programCounter;
    private int memoryAddressRegister;

    private ProcessState state;
    private int currentQueueLevel;
    private int timeInCurrentQueue;

    private int completionTime;
    private int turnaroundTime;
    private int waitingTime;
    private int responseTime;
    private int firstResponseTime;
    private boolean hasResponded;

    private int contextSwitches;
    private int ioOperations;

    private int quantumUsedInCurrentLevel = 0;

    /**
     * Constructor completo
     */
    public Process(int pid, String name, int arrivalTime, int burstTime, int priority,
            ProcessType type, int ioCycle, int ioDuration, int memorySize) {
        this.pid = pid;
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
        this.basePriority = priority;
        this.type = type;
        this.ioCycle = ioCycle;
        this.ioDuration = ioDuration;
        this.memorySize = memorySize;

        this.state = ProcessState.NEW;
        this.currentQueueLevel = 0;
        this.timeInCurrentQueue = 0;
        this.currentCycle = 0;
        this.ioRemaining = 0;
        this.totalIoTime = 0;
        this.programCounter = 0;
        this.memoryAddressRegister = 0;

        this.completionTime = 0;
        this.turnaroundTime = 0;
        this.waitingTime = 0;
        this.responseTime = -1;
        this.firstResponseTime = -1;
        this.hasResponded = false;

        this.contextSwitches = 0;
        this.ioOperations = 0;
    }

    /**
     * Constructor simplificado (sin I/O)
     */
    public Process(int pid, String name, int arrivalTime, int burstTime, int priority) {
        this(pid, name, arrivalTime, burstTime, priority, ProcessType.CPU_BOUND, 0, 0, 100);
    }

    /**
     * Constructor simplificado (sin prioridad ni I/O)
     */
    public Process(int pid, String name, int arrivalTime, int burstTime) {
        this(pid, name, arrivalTime, burstTime, 0, ProcessType.CPU_BOUND, 0, 0, 100);
    }

    /**
     * Ejecuta un ciclo del proceso
     * 
     * @param currentTime Tiempo actual de la simulación
     * @return true si completó una instrucción, false si necesita I/O
     */
    public boolean execute(int currentTime) {
        // Si es la primera vez que se ejecuta, registrar response time
        if (!hasResponded) {
            firstResponseTime = currentTime;
            responseTime = currentTime - arrivalTime;
            hasResponded = true;
        }

        // Ejecutar una unidad de tiempo
        remainingTime--;
        programCounter++;
        currentCycle++;

        int baseAddress = pid * 1024;
        memoryAddressRegister = baseAddress + programCounter;

        if (ioCycle > 0 && currentCycle >= ioCycle && remainingTime > 0) {
            currentCycle = 0;
            ioRemaining = ioDuration;
            ioOperations++;
            return false;
        }

        return true;
    }

    /**
     * Procesa un ciclo de I/O
     * 
     * @return true si completó el I/O, false si aún le falta
     */
    public boolean processIO() {
        if (ioRemaining > 0) {
            ioRemaining--;
            totalIoTime++;
            return ioRemaining == 0;
        }
        return true;
    }

    /**
     * Verifica si el proceso ha terminado
     */
    public boolean isFinished() {
        return remainingTime <= 0;
    }

    /**
     * Obtiene el quantum usado en el nivel actual
     */
    public int getQuantumUsedInCurrentLevel() {
        return quantumUsedInCurrentLevel;
    }

    /**
     * Establece el quantum usado en el nivel actual
     */
    public void setQuantumUsedInCurrentLevel(int quantum) {
        this.quantumUsedInCurrentLevel = quantum;
    }

    /**
     * Resetea el quantum usado (cuando cambia de nivel o inicia ejecución)
     */
    public void resetQuantumUsedInCurrentLevel() {
        this.quantumUsedInCurrentLevel = 0;
    }

    /**
     * Verifica si está bloqueado esperando I/O
     */
    public boolean isBlocked() {
        return state == ProcessState.BLOCKED && ioRemaining > 0;
    }

    /**
     * Incrementa tiempo de espera (cuando no está ejecutando ni bloqueado)
     */
    public void incrementWaitingTime() {
        if (state == ProcessState.READY) {
            waitingTime++;
            timeInCurrentQueue++;
        }
    }

    /**
     * Calcula métricas finales
     */
    public void calculateMetrics(int currentTime) {
        this.completionTime = currentTime;
        this.turnaroundTime = completionTime - arrivalTime;
    }

    /**
     * Calcula las métricas finales del proceso (sobrecarga para compatibilidad)
     */
    public void calculateMetrics() {
        turnaroundTime = completionTime - arrivalTime;
    }

    /**
     * Degrada la prioridad (mueve a cola de menor prioridad)
     */
    public void degradePriority() {
        if (currentQueueLevel < 3) {
            currentQueueLevel++;
            timeInCurrentQueue = 0;
        }
    }

    /**
     * Mejora la prioridad (mueve a cola de mayor prioridad) - Aging
     */
    public void improvePriority() {
        if (currentQueueLevel > 0) {
            currentQueueLevel--;
            timeInCurrentQueue = 0;
        }
    }

    /**
     * Restaura la prioridad base
     */
    public void restoreBasePriority() {
        this.priority = basePriority;
        this.currentQueueLevel = 0;
        this.timeInCurrentQueue = 0;
    }

    /**
     * Incrementa el contador de context switches
     */
    public void incrementContextSwitches() {
        contextSwitches++;
    }
    
    public String getDetailedInfo() {
        return String.format(
                "PID: %d | Name: %s | State: %s | AT: %d | BT: %d | RT: %d | Priority: %d | Queue: %d | PC: %d",
                pid, name, state, arrivalTime, burstTime, remainingTime, priority, currentQueueLevel, programCounter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Process process = (Process) obj;
        return pid == process.pid;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(pid);
    }

    public void setCompletionTime(int time) {
        this.completionTime = time;
    }

    // Getters y Setters
    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public void setBurstTime(int burstTime) {
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getBasePriority() {
        return basePriority;
    }

    public void setBasePriority(int basePriority) {
        this.basePriority = basePriority;
    }

    public ProcessType getType() {
        return type;
    }

    public void setType(ProcessType type) {
        this.type = type;
    }

    public int getIoCycle() {
        return ioCycle;
    }

    public void setIoCycle(int ioCycle) {
        this.ioCycle = ioCycle;
    }

    public int getIoDuration() {
        return ioDuration;
    }

    public void setIoDuration(int ioDuration) {
        this.ioDuration = ioDuration;
    }

    public int getCurrentCycle() {
        return currentCycle;
    }

    public int getIoRemaining() {
        return ioRemaining;
    }

    public int getTotalIoTime() {
        return totalIoTime;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int pc) {
        this.programCounter = pc;
    }

    public int getMemoryAddressRegister() {
        return memoryAddressRegister;
    }

    public void setMemoryAddressRegister(int mar) {
        this.memoryAddressRegister = mar;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getCurrentQueueLevel() {
        return currentQueueLevel;
    }

    public void setCurrentQueueLevel(int level) {
        this.currentQueueLevel = level;
        this.timeInCurrentQueue = 0;
    }

    public int getTimeInCurrentQueue() {
        return timeInCurrentQueue;
    }

    public int getCompletionTime() {
        return completionTime;
    }

    public int getTurnaroundTime() {
        return turnaroundTime;
    }

    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public int getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }

    public boolean hasResponded() {
        return hasResponded;
    }

    public int getContextSwitches() {
        return contextSwitches;
    }

    public int getIoOperations() {
        return ioOperations;
    }

    @Override
    public String toString() {
        return String.format("P%d[%s]", pid, name);
    }

    /**
     * Información detallada del proceso
     */
    
}