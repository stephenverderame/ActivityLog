package com.sev.activitylog;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

//employs principle of temporal locality in the ride cache
public class CircularLinkedList<T> implements Serializable {
    private int maxSize;
    private T[] data;
    private int index;
    private boolean maxedOut;
    public CircularLinkedList(Class<T> type, int maxSize){
        this.maxSize = maxSize;
        data = (T[])Array.newInstance(type, maxSize);
        index = 0;
        maxedOut = false;
    }
    public void add(T obj){
        if(index >= maxSize) {
            index = 0;
            maxedOut = true;
        }
        data[index++] = obj;
    }
    public T get(int index){
        if(index < maxSize)
            return data[index];
        return null;
    }
    public int size(){
        return maxedOut ? maxSize : index;
    }
}
class UniqueList<T> implements Iterable<T> {
    private LinkedList<T> list;
    public UniqueList(){
        list = new LinkedList<>();
    }
    private int findIndex(T t, boolean[] exists) {
        if(list.size() == 0) return 0;
        int index = 0, start = 0, end = list.size() - 1;
        do{
            index = (start + end) / 2;
            if(((Comparable<T>)list.get(index)).compareTo(t) == 0){
                exists[0] = true;
                return index;
            }
            else if(((Comparable<T>)list.get(index)).compareTo(t) < 0){
                start = index + 1;
            }
            else if(((Comparable<T>)list.get(index)).compareTo(t) > 0){
                end = index - 1;
            }

        } while(start <= end);
        exists[0] = false;
        return ((Comparable<T>)t).compareTo(list.get(index)) > 0 ? index + 1 : index;
    }
    public synchronized void add(T item) {
        boolean[] flag = new boolean[]{false};
        int index = findIndex(item, flag);
        if (!flag[0]) {
            list.add(index, item);
        }
    }
    public synchronized int size() {
        return list.size();
    }
    public synchronized T get(int i) {
        return list.get(i);
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {
        return new UniqueIterator(0);
    }

    class UniqueIterator implements Iterator<T> {
        private int index;
        public UniqueIterator(int i){
            index = i;
        }
        @Override
        public synchronized  boolean hasNext() {
            return index < size();
        }

        @Override
        public synchronized T next() {
            return get(index++);
        }
    }
}
