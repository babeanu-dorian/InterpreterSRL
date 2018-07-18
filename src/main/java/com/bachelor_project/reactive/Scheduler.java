/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.reactive;

import com.bachelor_project.interpreterast.types.LockedPointer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alexandru Babeanu
 */
public class Scheduler {
    
    private static class ThreadInfo {
        
        private int childrenCount;
        private final long parentId;
        private final SignalGuard guard;
        private final List<LockedPointer> requests;
        
        public ThreadInfo(long parentId, SignalGuard guard) {
            this.childrenCount = 0;
            this.parentId = parentId;
            this.guard = guard;
            this.requests = new ArrayList<LockedPointer>();
        }
    }
    
    private final List<Long> threadOrder;
    private final Map<Long, ThreadInfo> threads;
    private final Map<LockedPointer, Boolean> resources;
    private final Lock instantLock;
    private final Condition endInstantCondition;
    private long activeThreads = 0;
    
    /**
     *
     */
    public Scheduler() {
        this.threadOrder = new LinkedList<Long>();
        this.threads = new HashMap<Long, ThreadInfo>();
        this.resources = new HashMap<LockedPointer, Boolean>();
        this.instantLock = new ReentrantLock();
        this.endInstantCondition = this.instantLock.newCondition();
    }
    
    /**
     *
     * @param id
     * @param parentId
     * @param guard
     */
    public void registerThread(long id, long parentId, SignalGuard guard) {
        this.instantLock.lock();
        try {
            this.threads.put(id, new ThreadInfo(parentId, guard));
            
            if (this.threadOrder.isEmpty()) {
                this.threadOrder.add(id);
            } else {
                int childId = this.threads.get(parentId).childrenCount++;
                ListIterator<Long> it = this.threadOrder.listIterator();
                
                // put the new thread at the correct position in the hierarchy
                while (it.hasNext()) {
                    if (it.next() == parentId) {
                        for (; childId != 0; --childId) {
                            it.next();
                        }
                        it.add(id);
                        break;
                    }
                }
            }
            
        } finally {
            this.instantLock.unlock();
        }
    }
    
    /**
     *
     */
    public void deregisterThread() {
        long id = Thread.currentThread().getId();
        this.instantLock.lock();
        try {
            this.threadOrder.remove(id);
            // decrement the parent's childrenCount (the parent might no longer be present)
            --this.threads.getOrDefault(this.threads.get(id).parentId, new ThreadInfo(-1, null)).childrenCount;
            this.threads.remove(id);
            this.decrementActiveThreadCount();
        } finally {
            this.instantLock.unlock();
        }
    }
    
    /**
     *
     */
    public void incrementActiveThreadCount() {
        
        this.instantLock.lock();
        try {
            ++this.activeThreads;
        } finally {
            this.instantLock.unlock();
        }
    }
    
    /**
     *
     */
    public void decrementActiveThreadCount() {
        this.instantLock.lock();
        try {
            --this.activeThreads;
            if (this.activeThreads == 0)
                this.endInstantCondition.signal();
        } finally {
            this.instantLock.unlock();
        }
    }
    
    /**
     *
     * @param endInstantHandler
     */
    public void nextInstant(Runnable endInstantHandler) {
        
        this.instantLock.lock();
        try {
            if (this.activeThreads == 0) {
                distributeResources();
            }
            
            while (this.activeThreads != 0) {
                this.endInstantCondition.await();
                if (this.activeThreads == 0) {
                    distributeResources();
                }
            }

            endInstantHandler.run();

        } catch (InterruptedException ex) {
            Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.instantLock.unlock();
        }
    }
    
    /**
     *
     * @param resources
     */
    public void declareResources(List<LockedPointer> resources) {
        this.instantLock.lock();
        try{
            resources.forEach(ptr -> this.resources.put(ptr, true));
        } finally {
            this.instantLock.unlock();
        }
    }
    
    /**
     *
     * @param resources
     * @throws RuntimeException
     */
    public void requestResources(List<LockedPointer> resources) throws RuntimeException{
        
        this.instantLock.lock();
        try{
            if (!this.resources.keySet().containsAll(resources))
                throw new RuntimeException("Attempt to lock a resource that was not declared as shared.");

            this.threads.get(Thread.currentThread().getId()).requests.addAll(resources);
        } finally {
            this.instantLock.unlock();
        }
        
    }
    
    /**
     *
     * @param resources
     */
    public void releaseResources(List<LockedPointer> resources) {
        this.instantLock.lock();
        try{
            resources.forEach(resource -> {
                resource.setOwner(LockedPointer.NO_OWNER);
                this.resources.put(resource, true);
            });
        } finally {
            this.instantLock.unlock();
        }
    }
    
    private void distributeResources() {
        
        // instantLock should be locked by the calling function
        this.threadOrder.forEach(id -> {
            
            if (this.threads.get(id).requests.isEmpty())
                return;
            
            boolean allAvailable = true;
            for (LockedPointer resource : this.threads.get(id).requests) {
                if (!this.resources.get(resource)) {
                    allAvailable = false;
                    break;
                }
            }
            //System.out.println("all Available " + allAvailable);
            if (allAvailable) {
                //System.out.println("Requests for " + id + " : " + this.threads.get(id).requests);
                this.threads.get(id).requests.forEach(resource -> {
                    this.resources.put(resource, false);
                    resource.setOwner(id);
                });
                //System.out.println(this.resources);
                this.threads.get(id).requests.clear();
                this.threads.get(id).guard.grantResources();
            }
        });
        
    }
}
