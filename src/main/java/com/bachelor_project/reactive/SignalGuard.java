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
 * Manages the execution of the current {@link java.lang.Thread} with respect to
 * <b>when</b>, <b>watch</b> and <b>lock</b> instructions.
 * @author AlexandruBabeanu
 */
public class SignalGuard {
    
    /**
    * Auxiliary inner superclass used for encoding <b>when</b> and <b>watch</b> conditions.
    */
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
    
    /**
    * Auxiliary inner class used for encoding <b>when</b> conditions.
    */
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
    
    /**
    * Auxiliary inner class used for encoding <b>watch</b> conditions.
    */
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
     * Constructs a {@link com.bachelor_project.reactive.SignalGuard} object for the current {@link java.lang.Thread}
     * in the provided {@link com.bachelor_project.reactive.Program}.
     * @param environment the {@link com.bachelor_project.reactive.Program} object that will use the
     * {@link com.bachelor_project.reactive.SignalGuard} object
     * @see com.bachelor_project.reactive.Program
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
     * Returns the {@link com.bachelor_project.reactive.Program} object that encodes the current <b>SRL</b> program.
     * @return the {@link com.bachelor_project.reactive.Program} object that encodes the current <b>SRL</b> program
     * @see com.bachelor_project.reactive.Program
     */
    public Program getEnvironment() {
        return this.environment;
    }
    
    /**
     * Returns a {@link java.util.Map} view representing the global scope of the current thread.
     * The global scope of a thread contains the global scope of the program, obtained by calling
     * {@link com.bachelor_project.reactive.Program#getGlobalScope() }, and the <b>private</b> structure added by calling
     * {@link com.bachelor_project.reactive.SignalGuard#addPrivateStructToScope() }.
     * @return a {@link java.util.Map} view representing the global scope of the current thread
     * @see com.bachelor_project.reactive.Program#getGlobalScope()
     * @see com.bachelor_project.reactive.SignalGuard#addPrivateStructToScope()
     */
    public Map<String, Statement> getGlobalScope() {
        return Collections.unmodifiableMap(this.globalScope);
    }
    
    /**
     * Returns whether or not the current instruction is enclosed by an activated <b>watch</b> instruction. 
     * @return <b>true</b> if the current instruction should be aborted, <b>false</b> otherwise.
     */
    public boolean isAborting() {
        return stopCount > 0;
    }

    /**
     * Adds a structure stored in a variable called <b>private</b> to the global scope of the current thread.
     * @see com.bachelor_project.interpreterast.functions.StructFunctionDefinition#makeStruct(com.bachelor_project.interpreterast.types.LockedPointer)
     * @see com.bachelor_project.interpreterast.types.LockedPointer
     * @see com.bachelor_project.interpreterast.statements.Value
     */
    public void addPrivateStructToScope() {
        LockedPointer privateScope = new LockedPointer(Thread.currentThread().getId());
        StructFunctionDefinition.makeStruct(privateScope);
        this.globalScope.put("private", new Value(privateScope));
    }
    
    /**
     * Handles the reaction to changes in the signal environment.
     * Evaluates signal environment with respect to <b>when</b> conditions.
     * If they have all been met, the corresponding {@link java.lang.Thread} is signalled to resume execution.
     * @param signalTable a {@link java.util.Map} view of the signal environment
     * @see java.util.concurrent.locks.Condition
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
    
    /**
     * Returns the index in <b>checks</b> of the activated WATCH instruction,
     * <b>checks.size()</b> if there is no active WATCH
     */
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
     * Handles the reaction to the transition from one instant to the next.
     * Performs the <b>abort</b> operation associated with the <b>watch</b> construct and signals
     * the relevant {@link java.lang.Thread} to resume execution.
     * @param signalTable a {@link java.util.Map} view of the signal environment
     * @see java.util.concurrent.locks.Condition
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
     * Function called by the {@link com.bachelor_project.reactive.Scheduler} when granting resources to the
     * {@link java.lang.Thread} associated with this {@link com.bachelor_project.reactive.SignalGuard}.
     * @see com.bachelor_project.reactive.Scheduler
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
    
    /**
     * Uses {@link com.bachelor_project.reactive.Scheduler#releaseResources(java.util.List) } to
     * release all the owned locks before going into waiting.
     * @see com.bachelor_project.reactive.Scheduler#releaseResources(java.util.List) 
     */
    private void releaseResources() {
        if (this.resources.isEmpty())
            return;
        
        this.hasResources = false;
        this.environment.getScheduler().releaseResources(this.resources);
    }
    
    /**
     * Implements the execution of a <b>when</b> instruction. The additional checks are added to a list, then the
     * {@link com.bachelor_project.reactive.SignalGuard#executeStatement(com.bachelor_project.interpreterast.statements.Statement, java.util.Map) }
     * method is called to handle the execution of the enclosed instruction block.
     * @param whenList the list of signals associated with the <b>when</b> instruction
     * @param statement the instruction block enclosed within the <b>when</b> instruction
     * @param translationTable the scope of the instruction block
     * @throws RuntimeException
     * @see com.bachelor_project.reactive.SignalGuard#executeStatement(com.bachelor_project.interpreterast.statements.Statement, java.util.Map)
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
     * Implements the execution of a <b>watch</b> instruction. The additional checks are added to a list, then the
     * {@link com.bachelor_project.reactive.SignalGuard#executeStatement(com.bachelor_project.interpreterast.statements.Statement, java.util.Map) }
     * method is called to handle the execution of the enclosed instruction block.
     * @param watchList the list of signals associated with the <b>watch</b> instruction
     * @param statement the instruction block enclosed within the <b>watch</b> instruction
     * @param translationTable the scope of the instruction block
     * @throws RuntimeException
     * @see com.bachelor_project.reactive.SignalGuard#executeStatement(com.bachelor_project.interpreterast.statements.Statement, java.util.Map)
     */
    public void executeWatch(List<String> watchList, Statement statement, Map<String, Statement> translationTable) throws RuntimeException {
        
        watchList.forEach((String signal) -> {
            this.checks.add(new WatchInstruction(signal));
        });
        
        executeStatement(statement, translationTable);
        
        removeChecks(watchList.size());
    }
    
    /**
     * Implements the execution of a <b>lock</b> instruction. Add the provided
     * {@link com.bachelor_project.interpreterast.types.LockedPointer} objects to the list of
     * resource locks required to proceed execution. Uses the method
     * {@link com.bachelor_project.reactive.SignalGuard#executeStatement(com.bachelor_project.interpreterast.statements.Statement, java.util.Map) }
     * to execute the enclosed instruction block.
     * @param resources the list of shared variables associated with the <b>lock</b> instruction
     * @param statement the instruction block enclosed within the <b>lock</b> instruction
     * @param translationTable the scope of the instruction block
     * @throws RuntimeException
     * @see com.bachelor_project.interpreterast.types.LockedPointer
     * @see com.bachelor_project.reactive.SignalGuard#executeStatement(com.bachelor_project.interpreterast.statements.Statement, java.util.Map)
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
     * Manages the execution of instructions with respect to waiting for signals or resources to be available
     * and aborting statements within activated <b>watch</b> instructions. Uses
     * {@link java.util.concurrent.locks.Condition#await() } to block the current
     * {@link java.lang.Thread} until the conditions required for executing the given
     * {@link com.bachelor_project.interpreterast.statements.Statement} have been met,
     * or the corresponding instruction was aborted.
     * @param statement the {@link com.bachelor_project.interpreterast.statements.Statement} object
     * encoding the instruction to be run
     * @param translationTable  the scope of the statement
     * @throws RuntimeException
     * @see java.util.concurrent.locks.Condition#await()
     * @see com.bachelor_project.interpreterast.statements.Statement
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
     * Deregister from the end of instant observable.
     */
    public void unsubscribe() {
        this.endInstantSubscriber.dispose();
    }
    
    /**
     * Auxiliary function for waiting for the required signals to become present or for the current instant to end.
     * Uses {@link com.bachelor_project.reactive.Program#subscribeToSignalTable(io.reactivex.functions.Consumer) }
     * to subscribe to the signal environment.
     * @see com.bachelor_project.reactive.Program#subscribeToSignalTable(io.reactivex.functions.Consumer)
     */
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
    
    /**
     * Auxiliary function for waiting on resource locks.
     * Uses {@link com.bachelor_project.reactive.Scheduler#requestResources(java.util.List) }.
     * @see com.bachelor_project.reactive.Scheduler#requestResources(java.util.List)
     */
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
    
    /**
     * Removes {@link com.bachelor_project.reactive.SignalGuard.Instruction} objects from the list of
     * <b>when</b> and <b>watch</b> checks.
     * @param amount the amount of checks to remove from the list
     */
    private void removeChecks(int amount) {
        this.checks.subList(this.checks.size() - amount, this.checks.size()).clear();
        this.stopCount -= amount;
        if (this.stopCount < 0)
            this.stopCount = 0;
    }
}
