/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.types.LockedPointer;
import com.bachelor_project.reactive.SignalGuard;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the <b>lock</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class LockFunctionDefinition implements FunctionDefinition{

    /**
     * Registers a set of requests with the scheduler and executes the given instruction block after said resources were granted.
     * Delegates most of its tasks to {@link com.bachelor_project.reactive.SignalGuard#executeLock(java.util.List, com.bachelor_project.interpreterast.statements.Statement, java.util.Map)}.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing
     * the shared variables, as well as the block of instructions that uses them
     * @throws RuntimeException when the parameter list has less than 2 elements or when some parameter other than the last one is not a shared variable
     * @see com.bachelor_project.reactive.SignalGuard#executeLock(java.util.List, com.bachelor_project.interpreterast.statements.Statement, java.util.Map)
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() < 2)
            throw new RuntimeException("Too few arguments in call to function: lock");
        List<LockedPointer> resources = new ArrayList<LockedPointer>();
        
        for (int i = 0; i < parameterList.size() - 1; ++i) {
            Object ptr = parameterList.get(i).execute(guard, null);
            if ( !(ptr instanceof LockedPointer) ) {
                throw new RuntimeException("Argument " + i + " of call to function lock is a constant");
            }
            
            resources.add((LockedPointer) ptr);
        }
        guard.executeLock(resources, parameterList.get(parameterList.size() - 1), null);
    }
    
}
