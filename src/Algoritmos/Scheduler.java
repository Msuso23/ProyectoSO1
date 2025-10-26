package Algoritmos;

import EDD.Lista;
import Modelos.Process;

/**
 * Interfaz base para todos los algoritmos de planificación
 */
public interface Scheduler {

    /**
     * Agrega un proceso a la cola de listos
     */
    void addProcess(Process process);

    /**
     * Selecciona el siguiente proceso a ejecutar
     * 
     * @return El proceso seleccionado o null si no hay procesos
     */
    Process selectNextProcess();

    /**
     * Ejecuta la simulación completa
     * 
     * @return Lista de procesos con métricas calculadas
     */
    Lista<Process> executeSimulation();

    /**
     * Retorna el nombre del algoritmo
     */
    String getAlgorithmName();

    /**
     * Reinicia el planificador
     */
    void reset();

    /**
     * Verifica si hay procesos en cola
     */
    boolean hasProcesses();
}