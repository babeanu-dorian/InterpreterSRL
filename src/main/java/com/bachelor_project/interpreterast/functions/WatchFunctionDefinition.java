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
 *
 * @author Alexandru Babeanu
 */
public class WatchFunctionDefinition implements FunctionDefinition {

    /**
     *
     * @param guard
     * @param parameterList
     * @throws RuntimeException
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
