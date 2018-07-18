/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.statements.Statement;
import com.bachelor_project.interpreterast.types.LockedPointer;
import com.bachelor_project.reactive.SignalGuard;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the <b>print</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class PrintFunctionDefinition implements FunctionDefinition{
    
    
    /**
    * Auxiliary inner class for {@link com.bachelor_project.interpreterast.functions.PrintFunctionDefinition}.
    * An instance of this class will be passed to
    * {@link com.bachelor_project.reactive.SignalGuard#executeLock(java.util.List, com.bachelor_project.interpreterast.statements.Statement, java.util.Map)}
    * to print once the lock for the output stream is acquired.
    */
    private static class PrintWithLock implements Statement {

        private final String msg;
        private final LockedPointer out;
        
        public PrintWithLock(String msg, LockedPointer out) {
            this.msg = msg;
            this.out = out;
        }
        
        @Override
        public Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException {
            PrintWriter outputWriter = (PrintWriter) out.getValue();
            outputWriter.print(msg);
            outputWriter.flush();
            return null;
        }
        
    }

    /**
     * Prints the result of the given expression to the standard output. To do so deterministically,
     * this method will use {@link com.bachelor_project.reactive.SignalGuard#executeLock(java.util.List, com.bachelor_project.interpreterast.statements.Statement, java.util.Map)}
     * to first obtain the lock for the standard output. To do this,a call to {@link com.bachelor_project.reactive.SignalGuard#getGlobalScope()} is made to retrieve
     * the table of global shared variables, which includes the variable <i>#output</i>. This variable holds a
     * {@link java.io.PrintWriter} object, which writes to the standard output. A PrintWIthLock object is passed to
     * {@link com.bachelor_project.reactive.SignalGuard#executeLock(java.util.List, com.bachelor_project.interpreterast.statements.Statement, java.util.Map)} to print once
     * the lock is acquired.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a singleton list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing an expression
     * @throws RuntimeException when the number of arguments is different than 1
     * @see com.bachelor_project.reactive.SignalGuard#executeLock(java.util.List, com.bachelor_project.interpreterast.statements.Statement, java.util.Map)
     * @see com.bachelor_project.reactive.SignalGuard#getGlobalScope()
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() != 1)
            throw new RuntimeException("Wrong number of arguments in call to function print");
        
        List<LockedPointer> resources = new ArrayList<LockedPointer>();
        resources.add((LockedPointer) guard.getGlobalScope().get("#output").execute(guard, null));
        
        Object msg = parameterList.get(0).execute(guard, null);
        if (msg instanceof LockedPointer)
            msg = ((LockedPointer) msg).getValue();
        
        guard.executeLock(resources, new PrintWithLock(msg.toString(), resources.get(0)), null);
    }
    
}
