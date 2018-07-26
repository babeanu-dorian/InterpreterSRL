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
 * Handles the lock requests for shared variables as well as the transition from one instant to the next.
 * Keeps a list of lock requests and a counter for the number of active threads. When the counter reaches 0,
 * if there are any lock requests in the list, the locks are granted to threads in the order of their priority.
 * If the list of requests is empty, all threads are notified no proceed to the next instance.
 * @author Alexandru Babeanu
 */
public class Scheduler {
    
    /**
     * Auxiliary inner class used to store information about a thread in the program.
     */
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
     * Constructs a new scheduler.
     */
    public Scheduler() {
        this.threadOrder = new LinkedList<Long>();
        this.threads = new HashMap<Long, ThreadInfo>();
        this.resources = new HashMap<LockedPointer, Boolean>();
        this.instantLock = new ReentrantLock();
        this.endInstantCondition = this.instantLock.newCondition();
    }
    
    /**
     * Registers a thread as part of the interpreted program. Add the relevant information in a list,
     * at a position that reflects the priority of the new thread. Called at the beginning of
     * {@link com.bachelor_project.interpreterast.statements.ThreadedStatement#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map) }
     * @param id the id of the thread being registered
     * @param parentId the id of the thread that spawned the registered thread
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the registered thread
     * @see com.bachelor_project.reactive.SignalGuard
     * @see java.lang.Thread#getId()
     * @see com.bachelor_project.interpreterast.statements.ThreadedStatement#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map)
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
     * Stop managing a thread. Called when
     * {@link com.bachelor_project.interpreterast.statements.ThreadedStatement#run()}
     * finishes execution.
     * @see com.bachelor_project.interpreterast.statements.ThreadedStatement#run() }
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
     * Increment the number of active threads. Used by a thread when waking up another.
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
     * Decrement the number of active threads. Used by a thread before entering a waiting state.
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
     * Waits until all threads in the program reach a suspended/terminated state,
     * then either distributes resources, if any are requested, or executes the provided
     * {@link java.lang.Runnable} to perform the transition from one instant to the next.
     * @param endInstantHandler the  {@link java.lang.Runnable} to execute at the end of an instant
     * @see java.lang.Runnable#run()
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
     * Used to register shared variables that need to be managed by the scheduler.
     * @param resources the list of {@link com.bachelor_project.interpreterast.types.LockedPointer} objects that
     * encode the new shared variables
     * @see com.bachelor_project.interpreterast.types.LockedPointer
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
     * Used by a thread to register its requests for shared variables.
     * @param resources the list of {@link com.bachelor_project.interpreterast.types.LockedPointer} objects that
     * encode the requested shared variables
     * @throws RuntimeException when a thread requests a resource that was not registered with the scheduler
     * @see com.bachelor_project.interpreterast.types.LockedPointer
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
     * Used by a thread to release resource locks.
     * @param resources the list of {@link com.bachelor_project.interpreterast.types.LockedPointer} objects that
     * encode the released shared variables
     * @see com.bachelor_project.interpreterast.types.LockedPointer
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
    
    /**
     * Auxiliary function for distributing resources to threads in the order of their priority.
     * For each thread, it will grant either all requests of no requests. Uses
     * {@link com.bachelor_project.reactive.SignalGuard#grantResources() } to signal a thread that
     * its requests were granted.
     * @see @link com.bachelor_project.reactive.SignalGuard#grantResources()
     */
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
            if (allAvailable) {
                this.threads.get(id).requests.forEach(resource -> {
                    this.resources.put(resource, false);
                    resource.setOwner(id);
                });
                this.threads.get(id).requests.clear();
                this.threads.get(id).guard.grantResources();
            }
        });
        
    }
}
