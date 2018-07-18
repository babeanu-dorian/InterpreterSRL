/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Identifier;
import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.reactive.SignalGuard;
import java.util.List;

/**
 * Implements the <b>emit</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class EmitFunctionDefinition implements FunctionDefinition {

    /**
     * Makes a set of signals available in the signal environment.
     * It uses {@link com.bachelor_project.interpreterast.statements.Identifier#translate(java.util.Map) } to obtain the signal identifiers and calls {@link com.bachelor_project.reactive.Program#emitSignal(java.lang.String) } to modify the signal environment.
     *
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing the signal identifiers
     * @throws RuntimeException when the parameters in the list contain other statements besides identifiers
     * @see com.bachelor_project.reactive.Program#emitSignal(java.lang.String)
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     * @see com.bachelor_project.interpreterast.statements.Identifier#translate(java.util.Map)
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        for (int i = 0; i < parameterList.size(); ++i) {
            
            Parameter param = parameterList.get(i);
            if ( !(param.getStatement() instanceof Identifier &&
                   ((Identifier) param.getStatement()).translate(param.getTranslationTable()) instanceof Identifier) ) {
                throw new RuntimeException("Argument " + i + " of call to function emit is not a signal");
            }
            
            String signalName = ((Identifier) ((Identifier) param.getStatement()).translate(param.getTranslationTable())).getName();
            guard.getEnvironment().emitSignal(signalName);
        }
    }
    
}
