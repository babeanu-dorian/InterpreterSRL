/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.types.LockedPointer;
import com.bachelor_project.reactive.SignalGuard;
import java.util.List;

/**
 * Implements the <b>loop</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class LoopFunctionDefinition implements FunctionDefinition {

    /**
     * Executes the given block of instructions until the condition evaluates to false.
     * The looping can also be interrupted by an activated <b>watch</b> statement.
     * This method uses {@link com.bachelor_project.reactive.SignalGuard#isAborting()} to detect that.
     * An instruction block without constructs that suspend execution will cause an instantaneous loop.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing a condition
     * statement and a block of instructions
     * @throws RuntimeException when the parameter list does not contain exactly 2 elements or when the condition argument
     * does not evaluate to a {@link java.lang.Boolean}.
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() != 2)
            throw new RuntimeException("Error: wrong number of arguments in call to function loop");
        
        while (true) {
            Object condition = parameterList.get(0).execute(guard, null);
            
            if (condition instanceof LockedPointer)
                condition = ((LockedPointer) condition).getValue();
            
            if ( !(condition instanceof Boolean) )
                throw new RuntimeException("Argument 0 in call to function loop is of type "
                        + condition.getClass().getName() + ", boolean expected");
            
            if (guard.isAborting() || !((Boolean) condition))
                break;
            
            guard.executeStatement(parameterList.get(1), null);
        }
        
    }
    
}
