package Modelos;

/**
 * Tipo de proceso (afecta el comportamiento de I/O)
 */
public enum ProcessType {
    CPU_BOUND("CPU-Bound"), 
    IO_BOUND("I/O-Bound"), 
    MIXED("Mixto"); 

    private final String displayName;

    ProcessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
