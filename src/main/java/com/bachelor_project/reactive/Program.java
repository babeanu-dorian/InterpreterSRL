/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.reactive;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.statements.*;
import com.bachelor_project.interpreterast.functions.*;
import com.bachelor_project.interpreterast.types.LockedPointer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Alexandru Babeanu
 */
public class Program {
    
    private class EndInstantHandler implements Runnable {

        private long instantCount = 0;
        
        @Override
        public void run() {
            Program.this.signalLock.lock();
            try {
                printOutput(this.instantCount);
                ++this.instantCount;
                Program.this.endInstantSignaler.onNext(Program.this.signalTable.getMap());
                Program.this.signalTable.resetSignals();
            } finally {
                Program.this.signalLock.unlock();
            }
        }
        
        private void printOutput(long instantCount) {
            Program.this.out.print("Instant " + instantCount + " ended. Output:");
            Program.this.signalTable.keySet().forEach(key -> {

                if (key.startsWith("#"))
                    return;

                if (signalTable.get(key)) {
                    Program.this.out.print(" " + key);
                }
            });
            Program.this.out.println();
            Program.this.out.flush();
        }
        
    }
    
    private final Scanner in;
    private final PrintWriter out;
    private final Scheduler scheduler;
    private final SignalTable signalTable;
    private final Map<String, Statement> sharedData;
    private final Map<String, FunctionDefinition> functions;
    private final ThreadedStatement statements;
    private final Subject<Map<String, Boolean>> signalTableObservable;
    private final Subject<Map<String, Boolean>> endInstantSignaler;
    private final Lock signalLock;
    private long localSignalCounter = 0;   // used for renaming local signals
    
    /**
     *
     * @param signalList
     * @param sharedData
     * @param declarations
     * @param statements
     */
    public Program(List<String> signalList, List<String> sharedData, Map<String, FunctionDefinition> declarations, StatementSequence statements) {
        
        // input and output streams
        this.in = new Scanner(System.in);
        this.out = new PrintWriter(System.out);
        
        // scheduler
        this.scheduler = new Scheduler();
        
        // signal table
        this.signalTable = new SignalTable();
        signalList.forEach(s -> this.signalTable.put(s, false));
        
        // shared variables
        List<LockedPointer> sharedPointers = new ArrayList<LockedPointer>();
        this.sharedData = new HashMap<String, Statement>();
        sharedData.forEach(var -> {
            LockedPointer ptr = new LockedPointer(LockedPointer.NO_OWNER);
            sharedPointers.add(ptr);
            this.sharedData.put(var, new Value(ptr));
        });
        
        // shared input and output streams
        LockedPointer inputStream = new LockedPointer(Thread.currentThread().getId());
        LockedPointer outputStream = new LockedPointer(Thread.currentThread().getId());
        inputStream.setValue(this.in);
        outputStream.setValue(this.out);
        inputStream.setOwner(LockedPointer.NO_OWNER);
        outputStream.setOwner(LockedPointer.NO_OWNER);
        sharedPointers.add(inputStream);
        sharedPointers.add(outputStream);
        this.sharedData.put("#input", new Value(inputStream));
        this.sharedData.put("#output",  new Value(outputStream));
        
        // register shared data witht he scheduler
        this.scheduler.declareResources(sharedPointers);
        
        // functions
        this.functions = declarations;
        
        // observables, locks, condition variables
        this.signalTableObservable = this.signalTable.getObservable().toSerialized();
        this.endInstantSignaler = PublishSubject.<Map<String, Boolean>>create().toSerialized();
        this.signalLock = new ReentrantLock();
        
        // statements
        this.statements = new ThreadedStatement(this, new Parameter(statements, this.sharedData));
    }
    
    /**
     *
     * @return
     */
    public Scheduler getScheduler() {
        return this.scheduler;
    }
    
    /**
     *
     * @return
     */
    public Map<String, Statement> getGlobalScope() {
        return Collections.unmodifiableMap(this.sharedData);
    }
    
    // creates a new entry in the signal table and returns the signal name

    /**
     *
     * @return
     */
    public String addLocalSignal() {
        this.signalLock.lock();
        try {
            String newName = "#" + localSignalCounter;
            signalTable.put(newName, false);
            localSignalCounter++;
            return newName;
        } finally {
            this.signalLock.unlock();
        }
    }
    
    /**
     *
     * @param consumer
     * @return
     */
    public Disposable subscribeToSignalTable(Consumer<Map<String, Boolean>> consumer) {
        this.signalLock.lock();
        try {
            return this.signalTableObservable.subscribe(consumer);
        } finally {
            this.signalLock.unlock();
        }
    }
    
    /**
     *
     * @param consumer
     * @return
     */
    public synchronized Disposable subscribeToInstantSignaler(Consumer<Map<String, Boolean>> consumer) {
        return this.endInstantSignaler.subscribe(consumer);
    }
    
    /**
     *
     * @param signalName
     * @throws RuntimeException
     */
    public void emitSignal(String signalName) throws RuntimeException {
        
        this.signalLock.lock();
        try{
            if (!signalTable.containsKey(signalName))
                throw new RuntimeException("Error: Undeclared signal " + signalName + " in call to emit");

            signalTable.put(signalName, true);
        } finally {
            this.signalLock.unlock();
        }
    }
    
    private void readInput(Scanner in) throws IOException {
        String[] input = in.nextLine().split("\\s+");
        for (String sig : input) {
            if (!sig.isEmpty())
                emitSignal(sig);
        }
    }
    
    /**
     *
     * @throws RuntimeException
     * @throws IOException
     */
    public void execute() throws RuntimeException, IOException{
        
        Runnable endInstantHandler = new EndInstantHandler();
        
        this.statements.execute(null, null);
        
        while(in.hasNextLine()) {
            
            readInput(in);
            
            this.scheduler.nextInstant(endInstantHandler);
        }
    }
    
    /**
     *
     * @param functionName
     * @param parameterList
     * @param guard
     * @throws RuntimeException
     */
    public void callFunction(String functionName, List<Parameter> parameterList, SignalGuard guard) throws RuntimeException{
        if (!functions.containsKey(functionName))
            throw new RuntimeException("Error: " + functionName + " is not a function");
        functions.get(functionName).call(guard, parameterList);
    }
    
    /**
     *
     * @return
     */
    public static Map<String, FunctionDefinition> keywordDefinitions() {
        Map<String, FunctionDefinition> keywordFunctions = new HashMap<String, FunctionDefinition>();
        
        keywordFunctions.put("thread", new ThreadFunctionDefinition());
        keywordFunctions.put("emit", new EmitFunctionDefinition());
        keywordFunctions.put("local", new LocalFunctionDefinition());
        keywordFunctions.put("when", new WhenFunctionDefinition());
        keywordFunctions.put("watch", new WatchFunctionDefinition());
        keywordFunctions.put("shared", new SharedFunctionDefinition());
        keywordFunctions.put("lock", new LockFunctionDefinition());
        keywordFunctions.put("struct", new StructFunctionDefinition());
        keywordFunctions.put("struct_fields", new StructFieldsFunctionDefinition());
        keywordFunctions.put("if", new IfFunctionDefinition());
        keywordFunctions.put("loop", new LoopFunctionDefinition());
        keywordFunctions.put("print", new PrintFunctionDefinition());
        
        return keywordFunctions;
    }
}
