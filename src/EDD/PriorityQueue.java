package EDD;


/**
 * Cola de prioridad para procesos
 * Los procesos con menor valor de prioridad se extraen primero
 */
public class PriorityQueue {
    private List<Process> list;

    public PriorityQueue() {
        this.list = new List<>();
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
        return list.getHead();
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
    

    @Override
    public String toString() {
        return list.toString();
    }
}
