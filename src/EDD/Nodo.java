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
public class Nodo <T> {
    private T dato;
    private Nodo <T> next;
    private Nodo <T> previous;

    public Nodo(T dato) {
        this.dato = dato;
        this.next = null;
        this.previous = null;
    }

    

    public T getData() {
        return dato;
    }

    public void setData(T dato) {
        this.dato = dato;
    }

    public Nodo<T> getNext() {
        return next;
    }

    public void setNext(Nodo<T> next) {
        this.next = next;
    }

    public Nodo<T> getPrevious() {
        return previous;
    }

    public void setPrevious(Nodo<T> previous) {
        this.previous = previous;
    }

    @Override
    public String toString() {
        return "Node{data=" + dato + "}";
    }
}
