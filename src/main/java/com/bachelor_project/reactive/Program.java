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
 * Encodes an executable interpretation of an <b>SRL</b> program.
 * @author Alexandru Babeanu
 */
public class Program {
    
    /**
     * Auxiliary inner class used to encode the set of actions to be performed at the end of an instant.
     */
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
     * Constructs an executable interpretation of an <b>SRL</b> program. Requires the signal interface,
     * declared through the <b>signal_domain</b> construct, the list of shared variables declared through
     * the <b>shared_data</b> construct, the table that encodes the functions declared using the <b>let</b>
     * construct, and the encoding of the program instructions.
     * @param signalList a list of the signal names in the signal interface of the program
     * @param sharedData a list of identifiers for shared variables
     * @param declarations a table that maps each function name to the
     * {@link com.bachelor_project.interpreterast.functions.FunctionDefinition} object that encodes said function
     * @param statements the sequence of program instructions
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
     * Offers access to the underlying {@link com.bachelor_project.reactive.Scheduler} object.
     * @return the underlying {@link com.bachelor_project.reactive.Scheduler} object
     * @see com.bachelor_project.reactive.Scheduler
     */
    public Scheduler getScheduler() {
        return this.scheduler;
    }
    
    /**
     * Returns a {@link java.util.Map} view of the global scope of the program.
     * This scope consists of a table that maps the identifiers of the shared variables
     * declared with the <b>shared_data</b> construct to the
     * {@link com.bachelor_project.interpreterast.statements.Statement} objects used to encode them.
     * @return a {@link java.util.Map} view of the global scope of the program
     * @see com.bachelor_project.interpreterast.statements.Value
     * @see com.bachelor_project.interpreterast.types.LockedPointer
     */
    public Map<String, Statement> getGlobalScope() {
        return Collections.unmodifiableMap(this.sharedData);
    }

    /**
     * Creates a new entry in the signal table and returns the signal name.
     * This new name is obtained by concatenating the symbol "#" with a number.
     * It is ensured to be unique, because user declared identifiers cannot
     * start with the symbol "#" and the number is always incremented.
     * @return a new, unique, signal name
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
     * Allows threads to subscribe to the signal table.
     * @param consumer the object that performs the event reaction
     * @return a <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/disposables/Disposable.html">Disposable</a>
     * object used to unsubscribe from the observable
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#subscribe--">Observable.subscribe()</a>
     * @see <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/disposables/Disposable.html">Disposable</a>
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
     * Allows threads to subscribe to the underlying
     * <a href="http://reactivex.io/RxJava/javadoc/rx/subjects/PublishSubject.html">PublishSubject</a>,
     * which will notify them when an instant has ended.
     * @param consumer the object that performs the event reaction
     * @return a <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/disposables/Disposable.html">Disposable</a>
     * object used to unsubscribe from the observable
     * @see <a href="http://reactivex.io/RxJava/javadoc/rx/subjects/PublishSubject.html">PublishSubject</a>
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#subscribe--">Observable.subscribe()</a>
     * @see <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/disposables/Disposable.html">Disposable</a>
     */
    public synchronized Disposable subscribeToInstantSignaler(Consumer<Map<String, Boolean>> consumer) {
        return this.endInstantSignaler.subscribe(consumer);
    }
    
    /**
     * Makes a signal present in the signal environment.
     * @param signalName the name of the emitted signal
     * @throws RuntimeException when the name of the emitted signal is not in the table
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
    
    /**
     * Auxiliary function for parsing the input signals of an instant.
     * @param in the {@link java.util.Scanner} that performs the reading
     * @throws IOException
     * @see java.util.Scanner
     */
    private void readInput(Scanner in) throws IOException {
        String[] input = in.nextLine().split("\\s+");
        for (String sig : input) {
            if (!sig.isEmpty())
                emitSignal(sig);
        }
    }
    
    /**
     * Executes the interpreted program.
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
     * Executes a call to a function declared via the <b>let</b> construct.
     * Calls the {@link com.bachelor_project.interpreterast.functions.FunctionDefinition#call(com.bachelor_project.reactive.SignalGuard, java.util.List) }
     * method of the {@link com.bachelor_project.interpreterast.functions.FunctionDefinition} object that is mapped
     * to the provided function name in the table of functions.
     * @param functionName the name of the function being called
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects encoding the call arguments
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object associated with the current {@link java.lang.Thread}
     * @throws RuntimeException when the function name does not match any declared function
     * @see com.bachelor_project.interpreterast.functions.FunctionDefinition#call(com.bachelor_project.reactive.SignalGuard, java.util.List)
     * @see com.bachelor_project.interpreterast.statements.Parameter
     * @see com.bachelor_project.reactive.SignalGuard
     */
    public void callFunction(String functionName, List<Parameter> parameterList, SignalGuard guard) throws RuntimeException{
        if (!functions.containsKey(functionName))
            throw new RuntimeException("Error: " + functionName + " is not a function");
        functions.get(functionName).call(guard, parameterList);
    }
    
    /**
     * Returns a table containing all the functions that are inherently defined in the language.
     * @return a table that maps each language construct to its encoding
     * {@link com.bachelor_project.interpreterast.functions.FunctionDefinition} object
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
