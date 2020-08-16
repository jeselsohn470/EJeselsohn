package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.HashMap;
import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable> extends MinHeap<E> {
    @SuppressWarnings("unchecked")
    public MinHeapImpl(){
        this.elements = (E[]) new Comparable[5];
        this.count = 0;
        this.elementsToArrayIndex = new HashMap<>();
    }
    @Override
    public void reHeapify(E element) {
        int position = getArrayIndex(element);
        downHeap(position);
        upHeap(position);
    }
    @Override
    protected int getArrayIndex(E element) {
        return elementsToArrayIndex.get(element);
    }
    @Override
    @SuppressWarnings("unchecked")
    protected void doubleArraySize() {
        E[] temp = (E[])new Comparable[this.elements.length*2];
        for (int i=0; i <this.elements.length; i++){
            temp[i] = this.elements[i];
        }
        this.elements = temp;
    }
    @Override
    protected  void swap(int i, int j)
    {
        E temp = this.elements[i];
        this.elements[i] = this.elements[j];
        this.elements[j] = temp;
        //updating the HashMap
        elementsToArrayIndex.put(elements[i], i);
        elementsToArrayIndex.put(elements[j],j);
    }
    @Override
    public void insert(E x)
    {
        // double size of array if necessary
        if (this.count >= this.elements.length - 1)
        {
            this.doubleArraySize();
        }
        //add x to the bottom of the heap
        this.elements[++this.count] = x;
        //add new element to HashMap
        elementsToArrayIndex.put(x,this.count);
        //percolate it up to maintain heap order property
        this.upHeap(this.count);
    }
    @Override
    public E removeMin()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Heap is empty");
        }
        E min = this.elements[1];
        //swap root with last, decrement count
        this.swap(1, this.count--);
        //move new root down as needed
        this.downHeap(1);
        this.elements[this.count + 1] = null;//null it to prepare for GC
        //remove min from HashTable
        elementsToArrayIndex.remove(min);

        return min;
    }
}
