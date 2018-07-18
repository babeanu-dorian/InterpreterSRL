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
 *
 * @author Alexandru Babeanu
 */
public class ThreadFunctionDefinition implements FunctionDefinition {

    /**
     *
     * @param guard
     * @param parameterList
     * @throws RuntimeException
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() != 1)
            throw new RuntimeException("Error: wrong number of arguments in call to function thread");

        new ThreadedStatement(guard.getEnvironment(), parameterList.get(0)).execute(null, null);
    }
    
}
