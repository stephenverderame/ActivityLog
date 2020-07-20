package com.sev.activitylog;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Internally uses a min heap to quickly find the least commonly used data
 */
public class RepeatCache {
    private transient final int HEAP_SIZE;
    private static transient final String HEAP_FILE = "heap.db";
    private static transient RepeatCache instance;
    private CacheEntry[] heap;
    private Context ctx;
    private RepeatCache(Context ctx){
        int size = 32;
        try(ObjectInputStream input = new ObjectInputStream(ctx.openFileInput(HEAP_FILE))){
            size = input.readInt();
            heap = (CacheEntry[])input.readObject();
        } catch (Exception e){
            e.printStackTrace();
            heap = new CacheEntry[size];
        }
        HEAP_SIZE = size;
        this.ctx = ctx;
    }
    public synchronized void serialize(){
        for(int i = 0; i < HEAP_SIZE; ++i){
            if(heap[i] != null) {
                try {
                    if (heap[i].heights == null && heap[i].heightFuture != null && heap[i].heightFuture.isDone())
                        heap[i].heights = heap[i].heightFuture.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        try(ObjectOutputStream output = new ObjectOutputStream(ctx.openFileOutput(HEAP_FILE, Context.MODE_PRIVATE))){
            output.writeInt(HEAP_SIZE);
            output.writeObject(heap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public synchronized void deleteFile() {
        ctx.deleteFile(HEAP_FILE);
        heap = new CacheEntry[HEAP_SIZE];
    }
    public synchronized void save(MapImage img, Future<Pos[][]> heights){
        for(int i = 0; i < HEAP_SIZE; ++i){
            if(heap[i] != null && isReusable(heap[i].map.mapBounds, img.mapBounds)){
                ++heap[i].priority;
                heapifyDown(i);
                return;
            }else if(heap[i] == null){
                heap[i] = new CacheEntry(img, heights);
                heapifyUp(i);
                return;
            }
        }
        heap[0] = new CacheEntry(img, heights); //if not free space -> 0 index is least commonly used data
        heapifyDown(0);

    }
    public synchronized final CacheEntry get(MapBounds b) {
        int closestMatch = -1;
        for(int i = HEAP_SIZE - 1; i >= 0; --i){
            if(heap[i] != null && isReusable(heap[i].map.mapBounds, b)){
                if(closestMatch > 0)
                    closestMatch = closerMatch(closestMatch, i, b);
                else
                    closestMatch = i;
            }
        }
        return closestMatch > 0 ? heap[closestMatch] : null;
    }
    private boolean isReusable(MapBounds storedImage, MapBounds newImage){
        return Math.abs(storedImage.top) - Math.abs(newImage.top) >= 0 && Math.abs(storedImage.top) - Math.abs(newImage.top) < 0.2 && storedImage.top * newImage.top > 0 &&
            Math.abs(storedImage.bottom) - Math.abs(newImage.bottom) <= 0 && Math.abs(storedImage.bottom) - Math.abs(newImage.bottom) > -0.2 && storedImage.bottom * newImage.bottom > 0 &&
                storedImage.right >= newImage.right && storedImage.right - newImage.right < 0.2 && storedImage.right * newImage.right > 0 &&
                storedImage.left <= newImage.left && storedImage.left - newImage.left > -0.2 && storedImage.left * newImage.left > 0;
    }
    private int closerMatch(int a, int b, MapBounds newMap){
        return Math.sqrt(Math.pow(heap[a].map.mapBounds.top - newMap.top, 2) + Math.pow(heap[a].map.mapBounds.bottom - newMap.bottom, 2) +
                Math.pow(heap[a].map.mapBounds.left - newMap.left, 2) + Math.pow(heap[a].map.mapBounds.right - newMap.right, 2)) <
                Math.sqrt(Math.pow(heap[b].map.mapBounds.top - newMap.top, 2) + Math.pow(heap[b].map.mapBounds.bottom - newMap.bottom, 2) +
                        Math.pow(heap[b].map.mapBounds.left - newMap.left, 2) + Math.pow(heap[b].map.mapBounds.right - newMap.right, 2)) ?
                a : b;
    }

    /**
     * If a node is inserted, there may be nodes above it with a greater priority.
     * @param alteredNode
     */
    private synchronized void heapifyUp(int alteredNode){
        if(heap[alteredNode].priority < heap[alteredNode / 2].priority && alteredNode != 0){
            CacheEntry temp = heap[alteredNode / 2];
            heap[alteredNode / 2] = heap[alteredNode];
            heap[alteredNode] = temp;
            heapifyUp(alteredNode / 2);

        }
    }

    /**
     * If a node's priority increased, there may be nodes below it with a lesser priority. In this case these nodes should switch
     * @param alteredNode
     */
    private synchronized void heapifyDown(int alteredNode){
        if(alteredNode < HEAP_SIZE) {
            if (alteredNode * 2 < HEAP_SIZE && heap[alteredNode * 2] != null && heap[alteredNode].priority > heap[alteredNode * 2].priority) {
                CacheEntry temp = heap[alteredNode * 2];
                heap[alteredNode * 2] = heap[alteredNode];
                heap[alteredNode] = temp;
                heapifyDown(alteredNode * 2);
            }
            else if(alteredNode * 2 + 1 < HEAP_SIZE && heap[alteredNode * 2 + 1] != null && heap[alteredNode].priority > heap[alteredNode * 2 + 1].priority){
                CacheEntry temp = heap[alteredNode * 2 + 1];
                heap[alteredNode * 2 + 1] = heap[alteredNode];
                heap[alteredNode] = temp;
                heapifyDown(alteredNode * 2 + 1);
            }

        }
    }
    public synchronized static RepeatCache getInstance(Context ctx){
        if(instance == null)
            instance = new RepeatCache(ctx);
        else
            instance.ctx = ctx;
        return instance;
    }
}
class CacheEntry implements Serializable {
    public MapImage map;
    public Pos[][] heights;
    public transient Future<Pos[][]> heightFuture;
    public int priority;
    CacheEntry(){
        map = null;
        heights = null;
        priority = 1;
    }
    CacheEntry(MapImage i, Future<Pos[][]> fu){
        map = i;
        try {
            if (fu.isDone())
                heights = fu.get();
            else throw new Exception("Future not ready!");
        } catch (Exception e) {
            e.printStackTrace();
            heights = null;
            heightFuture = fu;
        }
    }
}
