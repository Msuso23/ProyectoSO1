package EDD;

import Modelos.Process;

/**
 * Cola de prioridad para procesos
 * Los procesos con menor valor de prioridad se extraen primero
 */
public class PriorityQueue {
    private Lista<Process> list;

    public PriorityQueue() {
        this.list = new Lista<>();
    }

    /**
     * Agrega un proceso manteniendo el orden de prioridad
     * Menor valor de prioridad = mayor prioridad
     */
    

    /**
     * Remueve y retorna el proceso con mayor prioridad
     */
    public Process dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("La cola está vacía");
        }
        return list.removeFirst();
    }

    /**
     * Retorna el proceso con mayor prioridad sin removerlo
     */
    public Process peek() {
        if (isEmpty()) {
            throw new IllegalStateException("La cola está vacía");
        }
        return list.getFirst();
    }

    /**
     * Verifica si la cola está vacía
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Obtiene el tamaño de la cola
     */
    public int size() {
        return list.getSize();
    }

    /**
     * Limpia la cola
     */
    public void vaciar() {
        list.vaciar();
    }

    /**
     * Verifica si contiene un proceso
     */
    public boolean contains(Process process) {
        return list.contains(process);
    }

    /**
     * Remueve un proceso específico
     */
    public boolean remove(Process process) {
        return list.remove(process);
    }

    /**
     * Obtiene todos los procesos como array
     */
    public Process[] toArray() {
        Object[] objects = list.toArray();
        Process[] processes = new Process[objects.length];
        for (int i = 0; i < objects.length; i++) {
            processes[i] = (Process) objects[i];
        }
        return processes;
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
