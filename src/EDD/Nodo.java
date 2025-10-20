/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EDD;

/**
 *
 * @author susov
 */
public class Nodo {
    private Object dato;
    private Nodo next;

    public Nodo(Object Dato) {
        this.dato = Dato;
        this.next = null;
    }

    public Object getDato() {
        return dato;
    }

    public void setDato(Object element) {
        this.dato = element;
    }

    public Nodo getNext() {
        return next;
    }

    public void setNext(Nodo next) {
        this.next = next;
    }
}
