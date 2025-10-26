/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EDD;

/**
 *
 * @author susov
 * @param <T>
 */
public class Queue <T> {
    private Lista <T> list;
    
    public Queue() {
        this.list = new Lista<>();
    }

    public void enqueue(T data) {
        list.insertarFinal(data);
    }

    /**
     * Remueve y retorna el primer elemento
     */
    public T dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("La cola está vacía");
        }
        return list.removeFirst();
    }

    /**
     * Retorna el primer elemento sin removerlo
     */
    public T peek() {
        if (isEmpty()) {
            System.out.println("La lista esta vacia");;
        }
        return (T) list.getHead();
    }

    /**
     * Verifica si está vacía
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Retorna el tamaño
     */
    public int size() {
        return list.getSize();
    }

    /**
     * Vacia la cola
     */
    public void clear() {
        list.vaciar();
    }

    /**
     * Verifica si contiene un elemento
     */
    public boolean contains(T data) {
        return list.contains(data);
    }

    /**
     * Remueve un elemento específico de la cola
     */
    public boolean remove(T data) {
        return list.remove(data);
    }

    /**
     * Obtiene todos los elementos como array
     */
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public String toString() {
        return list.toString();
    }
    
}
