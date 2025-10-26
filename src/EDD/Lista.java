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
public class Lista<T> {
    private Nodo<T> Head;
    private Nodo<T> Tail;
    private int size;

    public Lista() {
        this.Head = null;
        this.Tail = null;
        this.size = 0;
    }

    public Nodo getHead() {
        return Head;
    }

    public T getFirst() {
        if (isEmpty()) {
            System.out.println("La lista esta vacia");
        }
        return Head.getData();
    }

    public void setHead(Nodo Head) {
        this.Head = Head;
    }

    public Nodo getTail() {
        return Tail;
    }

    public void setTail(Nodo Tail) {
        this.Tail = Tail;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isEmpty() {
        return this.Head == null;
    }

    public void vaciar() {
        this.Head = null;
        this.Tail = null; 
        this.size = 0;
    }

    public void insertBegin(Object element) {
        Nodo<T> nodo = new Nodo<>((T) element);
        if (isEmpty()) {
            Head = nodo;
            Tail = nodo;
        } else {
            nodo.setNext(Head);
            Head.setPrevious(nodo);
            Head = nodo;
        }
        size++;
    }

    public void insertarFinal(Object dato) {
        Nodo<T> pNew = new Nodo<>((T) dato);
        if (isEmpty()) {
            Head = pNew;
            Tail = pNew;
        } else {
            Tail.setNext(pNew);
            pNew.setPrevious(Tail);
            Tail = pNew;
        }
        size++;
    }

    // Metodo de insertar un valor por una posicion
    public void insertarPosicion(int posicion, Object valor) {
        if (posicion >= 0 && posicion < size) {
            Nodo nuevo = new Nodo(valor);
            if (posicion == 0) {
                nuevo.setNext(Head);
                Head = nuevo;
            } else {
                if (posicion == size - 1) {
                    Nodo aux = Head;
                    while (aux.getNext() != null) {
                        aux = aux.getNext();
                    }
                    aux.setNext(nuevo);
                } else {
                    Nodo aux = Head;
                    for (int i = 0; i < (posicion - 1); i++) {
                        aux = aux.getNext();
                    }
                    Nodo siguiente = aux.getNext();
                    aux.setNext(nuevo);
                    nuevo.setNext(siguiente);
                }
            }
            size++;
        }
    }

    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice fuera de rango");
        }
        return getNodeAt(index).getData();
    }

    private Nodo<T> getNodeAt(int index) {
        Nodo<T> current;

        if (index < size / 2) {
            current = Head;
            for (int i = 0; i < index; i++) {
                current = current.getNext();
            }
        } else {
            current = Tail;
            for (int i = size - 1; i > index; i--) {
                current = current.getPrevious();
            }
        }

        return current;
    }

    public T removeLast() {
        if (isEmpty()) {
            System.out.println("La lista esta vacia");
        }

        T data = Tail.getData();
        Tail = Tail.getPrevious();

        if (Tail == null) {
            Head = null;
        } else {
            Tail.setNext(null);
        }

        size--;
        return data;
    }

    public T remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice fuera de rango");
        }

        if (index == 0) {
            return removeFirst();
        }

        if (index == size - 1) {
            return removeLast();
        }

        Nodo<T> current = getNodeAt(index);
        Nodo<T> previous = current.getPrevious();
        Nodo<T> next = current.getNext();

        previous.setNext(next);
        next.setPrevious(previous);
        size--;

        return current.getData();
    }

    public boolean remove(T data) {
        Nodo<T> current = Head;
        int index = 0;

        while (current != null) {
            if (current.getData().equals(data)) {
                remove(index);
                return true;
            }
            current = current.getNext();
            index++;
        }

        return false;
    }

    public T removeFirst() {
        if (isEmpty()) {
            throw new IllegalStateException("La lista está vacía");
        }

        T data = Head.getData();
        Head = Head.getNext();

        if (Head == null) {
            Tail = null;
        } else {
            Head.setPrevious(null);
        }

        size--;
        return data;
    }

    public int indexOf(T data) {
        Nodo<T> current = Head;
        int index = 0;

        while (current != null) {
            if (current.getData().equals(data)) {
                return index;
            }
            current = current.getNext();
            index++;
        }

        return -1;
    }

    public boolean contains(T data) {
        return indexOf(data) != -1;
    }

    public Object[] toArray() {
        Object[] array = new Object[size];
        Nodo<T> current = Head;
        int index = 0;

        while (current != null) {
            array[index++] = current.getData();
            current = current.getNext();
        }

        return array;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        Nodo<T> current = Head;

        while (current != null) {
            sb.append(current.getData());
            if (current.getNext() != null) {
                sb.append(", ");
            }
            current = current.getNext();
        }
        sb.append("]");
        return sb.toString();
    }

}
