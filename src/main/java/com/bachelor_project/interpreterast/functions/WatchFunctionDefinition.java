/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.statements.Identifier;
import com.bachelor_project.reactive.SignalGuard;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the <b>watch</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class WatchFunctionDefinition implements FunctionDefinition {

    /**
     * Executes an instruction block until the end of the first instant in which the mentioned signals are present.
     * Uses the method {@link com.bachelor_project.reactive.SignalGuard#executeWatch(java.util.List, com.bachelor_project.interpreterast.statements.Statement, java.util.Map) }.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing
     * the identifiers for the signals, as well as the block of instructions to be executed
     * @throws RuntimeException when the number of parameters in the list is less than two or when the parameters
     * before the last one do not contain signal identifiers
     * @see com.bachelor_project.reactive.SignalGuard#executeWatch(java.util.List, com.bachelor_project.interpreterast.statements.Statement, java.util.Map)
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() < 2)
            throw new RuntimeException("Too few arguments in call to function: watch");
        
        List watchList = new ArrayList<String>();
        
        for (int i = 0; i < parameterList.size() - 1; ++i) {
            Parameter param = parameterList.get(i);
            if ( !(param.getStatement() instanceof Identifier &&
                   ((Identifier) param.getStatement()).translate(param.getTranslationTable()) instanceof Identifier) ) {
                throw new RuntimeException("Argument " + i + " of call to function watch is not a signal");
            }
            
            String signal = ((Identifier) ((Identifier) param.getStatement()).translate(param.getTranslationTable())).getName();
            watchList.add(signal);
        }
        
        guard.executeWatch(watchList, parameterList.get(parameterList.size() - 1), null);
    }
    
}
