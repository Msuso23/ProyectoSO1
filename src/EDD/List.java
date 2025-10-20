/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EDD;

/**
 *
 * @author susov
 */
public class List {
    private Nodo Head; 
    private Nodo Tail; 
    private int size; 

    public Nodo getHead() {
        return Head;
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

    
    public List() {
        this.Head = null;
        this.size = 0; 
    }
    

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    
    
    public boolean isEmpty(){
        return this.Head == null;
    }
    

    public void vaciar(){
        this.Head = null;
        this.Tail = null; // También limpiar 'last'
        this.size = 0;   
    }
    
    public void insertBegin(Object element) {
        Nodo nodo = new Nodo(element);
        if (isEmpty()) {
            setHead(nodo);
        } else {
           nodo.setNext(getHead());
           setHead(nodo);
        }
        size++;
    }

    public void insertarFinal(Object dato){
        Nodo pNew = new Nodo(dato);
        if(isEmpty()){
            Head = pNew;
        }else{
            Nodo aux = Head;
            while (aux.getNext() != null){
                aux = aux.getNext();
            }
            aux.setNext(pNew);
        }
        size++;
    }
    
    
    //Metodo de insertar un valor por una posicion 
    public void insertarPosicion(int posicion, Object valor){
        if(posicion>=0 && posicion<size){
            Nodo nuevo = new Nodo(valor);
            if(posicion == 0){
                nuevo.setNext(Head);
                Head = nuevo;
            }
            else{
                if(posicion == size-1){
                    Nodo aux = Head;
                    while(aux.getNext() != null){
                        aux = aux.getNext();
                    }
                    aux.setNext(nuevo);              
                }
                else{
                    Nodo aux = Head;
                    for (int i = 0; i < (posicion-1); i++) {
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
    

    
}
