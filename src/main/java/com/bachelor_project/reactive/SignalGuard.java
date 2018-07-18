/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.reactive;

import com.bachelor_project.interpreterast.functions.StructFunctionDefinition;
import com.bachelor_project.interpreterast.statements.Statement;
import com.bachelor_project.interpreterast.statements.Value;
import com.bachelor_project.interpreterast.types.LockedPointer;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author AlexandruBabeanu
 */
public class SignalGuard {
    
    private static abstract class Instruction{
        
        public enum Command{
            WHEN,
            WATCH
        };
        
        private final String signal;
        
        public Instruction(String signalName) {
            this.signal = signalName;
        }
        
        public String getSignal() {
            return this.signal;
        }
        
        public abstract Command getCommand();
    }
    
    private class WhenInstruction extends Instruction{
        
        public WhenInstruction(String signalName) {
            super(signalName);
        }

        @Override
        public Command getCommand() {
            return Command.WHEN;
        }
        
        @Override
        public String toString() {
            return "WHEN_" + getSignal();
        }
        
    }
    
    private class WatchInstruction extends Instruction{
        
        public WatchInstruction(String signalName) {
            super(signalName);
        }

        @Override
        public Command getCommand() {
            return Command.WATCH;
        }
        
        @Override
        public String toString() {
            return "WATCH_" + getSignal();
        }
        
    }
    
    private final Program environment;
    private final List<Instruction> checks;
    private final Set<String> remainingWhenChecks;
    private final List<LockedPointer> resources;
    private final Map<String, Statement> globalScope;
    private Disposable signalTableSubscriber;
    private final Disposable endInstantSubscriber;
    private final Lock lock;
    private final Condition blockCondition;
    private boolean hasResources;
    private int stopCount; // how many checks have to be removed before the activated watch is removed 
    
    /**
     *
     * @param environment
     */
    public SignalGuard(Program environment) {
        this.environment = environment;
        this.checks = new ArrayList();
        this.remainingWhenChecks = new HashSet<String>();
        this.resources = new ArrayList<LockedPointer>();
        this.globalScope = new HashMap<String, Statement>(environment.getGlobalScope());
        this.signalTableSubscriber = null;
        this.endInstantSubscriber = environment.subscribeToInstantSignaler(this::nextInstant);
        this.lock = new ReentrantLock();
        this.blockCondition = this.lock.newCondition();
        this.hasResources = true;
        this.stopCount = 0;
    }
    
    /**
     *
     * @return
     */
    public Program getEnvironment() {
        return this.environment;
    }
    
    /**
     *
     * @return
     */
    public Map<String, Statement> getGlobalScope() {
        return Collections.unmodifiableMap(this.globalScope);
    }
    
    /**
     *
     * @return
     */
    public boolean isAborting() {
        return stopCount > 0;
    }
    
    // set up the thread's private variable

    /**
     *
     */
    public void addPrivateStructToScope() {
        LockedPointer privateScope = new LockedPointer(Thread.currentThread().getId());
        StructFunctionDefinition.makeStruct(privateScope);
        this.globalScope.put("private", new Value(privateScope));
    }
    
    /**
     *
     * @param signalTable
     */
    public void checkWhen(Map<String, Boolean> signalTable) {
        this.lock.lock();
        try {
            this.remainingWhenChecks.removeIf((String signal) -> signalTable.get(signal));
            
            if (this.remainingWhenChecks.isEmpty() && this.signalTableSubscriber != null) {
                // the subscriber will be null when this function is called by subscribe instead of onNext
                this.environment.getScheduler().incrementActiveThreadCount();
                this.signalTableSubscriber.dispose();
                this.signalTableSubscriber = null;
                this.blockCondition.signal();
            }
        } finally {
            this.lock.unlock();
        }
        
    }
    
    // returns the index of the activated WATCH statement, checks.size() if there is no active WATCH
    private int checkWatch(Map<String, Boolean> signalTable) {
        for (int i = 0; i != this.checks.size(); ++i) {
            Instruction check = this.checks.get(i);
            
            // stop on the first WHEN instruction that is not satisfied
            if (check.getCommand() == Instruction.Command.WHEN && !signalTable.get(check.getSignal()))
                return this.checks.size();
            
            // process the first WATCH command that is satisfied
            if (check.getCommand() == Instruction.Command.WATCH && signalTable.get(check.getSignal())) {
                return i;
            }
        }
        return this.checks.size();
    }
    
    /**
     *
     * @param signalTable
     */
    public void nextInstant(Map<String, Boolean> signalTable) {
        
        this.lock.lock();
        try {
            int watchIdx = checkWatch(signalTable);
            
            this.stopCount = this.checks.size() - watchIdx;
            
            // refresh WHEN checks (since all signals will be reset)
            // do so only up to the first activated WATCH
            this.remainingWhenChecks.clear();
            for (int i = 0; i != watchIdx; ++i) {
                Instruction check = this.checks.get(i);
                if (check.getCommand() == Instruction.Command.WHEN) {
                    this.remainingWhenChecks.add(check.getSignal());
                }
            }
            
            if (this.stopCount != 0) {
                this.signalTableSubscriber.dispose();
                this.signalTableSubscriber = null;
                this.environment.getScheduler().incrementActiveThreadCount();
                this.blockCondition.signal();
            }
        } finally {
            this.lock.unlock();
        }
        
    }
    
    /**
     *
     */
    public void grantResources() {
        this.lock.lock();
        try {
            this.hasResources = true;
            this.environment.getScheduler().incrementActiveThreadCount();
            this.blockCondition.signal();
        } finally {
            this.lock.unlock();
        }
    }
    
    private void releaseResources() {
        if (this.resources.isEmpty())
            return;
        
        this.hasResources = false;
        this.environment.getScheduler().releaseResources(this.resources);
    }
    
    /**
     *
     * @param whenList
     * @param statement
     * @param translationTable
     * @throws RuntimeException
     */
    public void executeWhen(List<String> whenList, Statement statement, Map<String, Statement> translationTable) throws RuntimeException {

        releaseResources();
        
        whenList.forEach((String signal) -> {
            this.checks.add(new WhenInstruction(signal));
            this.remainingWhenChecks.add(signal);
        });
        
        executeStatement(statement, translationTable);
        
        removeChecks(whenList.size());
    }
    
    /**
     *
     * @param watchList
     * @param statement
     * @param translationTable
     * @throws RuntimeException
     */
    public void executeWatch(List<String> watchList, Statement statement, Map<String, Statement> translationTable) throws RuntimeException {
        
        watchList.forEach((String signal) -> {
            this.checks.add(new WatchInstruction(signal));
        });
        
        executeStatement(statement, translationTable);
        
        removeChecks(watchList.size());
    }
    
    /**
     *
     * @param resources
     * @param statement
     * @param translationTable
     * @throws RuntimeException
     */
    public void executeLock(List<LockedPointer> resources, Statement statement, Map<String, Statement> translationTable) throws RuntimeException {
        
        releaseResources();
        
        this.resources.addAll(resources);
        this.hasResources = false;
        executeStatement(statement, translationTable);
        this.resources.removeAll(resources);
        
        if (this.hasResources) // might not have the resources when being aborted by a watch instruction
            this.environment.getScheduler().releaseResources(resources);
    }

    /**
     *
     * @param statement
     * @param translationTable
     * @throws RuntimeException
     */
    public void executeStatement(Statement statement, Map<String, Statement> translationTable) throws RuntimeException {
        
        if (!this.remainingWhenChecks.isEmpty() && this.stopCount == 0)
            waitOnSignalTable();
        
        if (this.stopCount == 0) {
            
            if (!this.hasResources) {
                if (this.resources.isEmpty()) {
                    this.hasResources = true;
                } else {
                    waitOnResources();
                }
            }
            statement.execute(this, translationTable);
        }
    }
    
    /**
     *
     */
    public void unsubscribe() {
        this.endInstantSubscriber.dispose();
    }
    
    private void waitOnSignalTable() {
        this.lock.lock();
        try {
            this.signalTableSubscriber = this.environment.subscribeToSignalTable(this::checkWhen);
            
            if (this.remainingWhenChecks.isEmpty()) { // don't wait on nothing
                this.signalTableSubscriber.dispose();
                this.signalTableSubscriber = null;
                return;
            }
            
            this.environment.getScheduler().decrementActiveThreadCount();
            
            while (!this.remainingWhenChecks.isEmpty() && this.stopCount == 0) {
                this.blockCondition.await();
            }
            
        } catch (InterruptedException ex) {
            Logger.getLogger(SignalGuard.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.lock.unlock();
        }
    }
    
    private void waitOnResources() {
        this.lock.lock();
        try {
            this.environment.getScheduler().requestResources(this.resources);
            this.environment.getScheduler().decrementActiveThreadCount();
            while (!this.hasResources) {
                this.blockCondition.await();
            }
            
        } catch (InterruptedException ex) {
            Logger.getLogger(SignalGuard.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            this.lock.unlock();
        }
    }
    
    private void removeChecks(int amount) {
        this.checks.subList(this.checks.size() - amount, this.checks.size()).clear();
        this.stopCount -= amount;
        if (this.stopCount < 0)
            this.stopCount = 0;
    }
}
