package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {
    private int count;
    private Node<T> top;

    public StackImpl(){
        this.count =0;
        this.top = null;
    }

    private class Node<T> {
        private T t;
        private Node<T> next;
        private Node(T t) {
            this.t = t;
            this.next = null;
        }
        private T element()
        {
            return t;
        }
    }
    @Override
    public void push(T element) {
        if (element ==null){
            throw new IllegalArgumentException();
        }
        Node<T> node = new Node<T>(element);
        node.next = top;
        top = node;
        count++;
    }
    @Override
    public T pop() {
        if (size() == 0) {
            return null;
        }
        T output = top.element();
        top = top.next;
        count--;
        return output;
    }
    @Override
    public T peek() {
        if(size() == 0) {
            return null;
        }
        T output = top.element();
        return output;
    }
    @Override
    public int size() {
        return count;
    }
}