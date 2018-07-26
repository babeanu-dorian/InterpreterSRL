/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.statements;

import com.bachelor_project.reactive.Program;
import com.bachelor_project.reactive.SignalGuard;
import java.util.Map;

/**
 * Allows {@link com.bachelor_project.interpreterast.statements.Statement#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map) }
 * to run on a different {@link java.lang.Thread}.
 * @author Alexandru Babeanu
 * @see com.bachelor_project.interpreterast.statements.Statement#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map)
 * @see java.lang.Thread#start()
 */
public class ThreadedStatement extends Thread implements Statement {
    
    private final Program environment;
    private final Parameter statement;
    private final SignalGuard guard;
    private boolean executed;
    
    /**
     *
     * @param environment
     * @param statement
     */
    public ThreadedStatement(Program environment, Parameter statement) {
        this.environment = environment;
        this.statement = statement;
        this.guard = new SignalGuard(environment);
        this.executed = false;
    }

    /**
     *
     */
    @Override
    public void run() {
        this.guard.addPrivateStructToScope();
        this.statement.makeWithAdditionalTranslations(this.guard.getGlobalScope()).execute(guard, null);
        this.guard.unsubscribe();
        this.environment.getScheduler().deregisterThread();
    }

    /**
     *
     * @param guard
     * @param translationTable
     * @return
     * @throws RuntimeException
     */
    @Override
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException {
        
        if (this.executed)
            throw new RuntimeException("ThreadedStatement executed more than once");
        
        this.executed = true;
        this.environment.getScheduler().registerThread(getId(), Thread.currentThread().getId(), this.guard);
        this.environment.getScheduler().incrementActiveThreadCount();   // always before actually starting the thread
                                                                        // (to ensure that the count can't reach 0 before
                                                                        // the scheduler is notified)
        this.start();
        return null;
    }
    
}
