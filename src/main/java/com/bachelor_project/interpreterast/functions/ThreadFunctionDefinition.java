/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.ThreadedStatement;
import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.reactive.SignalGuard;
import java.util.List;

/**
 * Implements the <b>thread</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class ThreadFunctionDefinition implements FunctionDefinition {

    /**
     * Executes an instruction block in a different {@link java.lang.Thread}.
     * Constructs a {@link com.bachelor_project.interpreterast.statements.ThreadedStatement} with the provided
     * {@link com.bachelor_project.interpreterast.statements.Parameter} then executes it.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing exactly
     * one element that encodes the instruction block to be run on a different {@link java.lang.Thread}
     * @throws RuntimeException when the number of parameters in the list is different than 1
     * @see com.bachelor_project.interpreterast.statements.ThreadedStatement
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() != 1)
            throw new RuntimeException("Error: wrong number of arguments in call to function thread");

        new ThreadedStatement(guard.getEnvironment(), parameterList.get(0)).execute(null, null);
    }
    
}
