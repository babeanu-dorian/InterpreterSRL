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
 * Implements the <b>if</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class IfFunctionDefinition implements FunctionDefinition {

    /**
     * Evaluates the truth value of a statement and makes a decision on which instruction block to run based on the result. 
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing:
     * <ol>
     *     <li> the condition statement</li>
     *     <li> the block of instructions to be executed if the condition is true</li>
     *     <li> the block of instructions to be executed if the condition is false</li>
     * </ol>
     * @throws RuntimeException when the parameter list does not contain exactly 3 elements or when the condition argument
     * does not evaluate to a {@link java.lang.Boolean}
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() != 3)
            throw new RuntimeException("Error: wrong number of arguments in call to function if");
        
        Object condition = parameterList.get(0).execute(guard, null);

        if (condition instanceof LockedPointer)
            condition = ((LockedPointer) condition).getValue();

        if ( !(condition instanceof Boolean) )
            throw new RuntimeException("Argument 0 in call to function if is of type "
                    + condition.getClass().getName() + ", boolean expected");

        if (((Boolean) condition)) {
            guard.executeStatement(parameterList.get(1), null);
        } else {
            guard.executeStatement(parameterList.get(2), null);
        }
    }
}
