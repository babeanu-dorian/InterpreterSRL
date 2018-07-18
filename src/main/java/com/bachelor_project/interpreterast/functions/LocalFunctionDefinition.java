/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.statements.Identifier;
import com.bachelor_project.interpreterast.statements.Statement;
import com.bachelor_project.reactive.SignalGuard;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the <b>local</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class LocalFunctionDefinition implements FunctionDefinition{

    /**
     * Adds new signals to the signal environment, such that they're only accessible from the instruction block to be executed, then executes said instruction block.
     * Each identifier passed as argument will be mapped to a new, unique signal name in the translation table of the instruction block (last argument).
     * To produce this new name, as well as make an entry for it in the signal table, this method will call {@link com.bachelor_project.reactive.Program#addLocalSignal()}.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing
     * the identifiers for the local signals, as well as the block of instructions to which the signals are bound
     * @throws RuntimeException when the parameter list has less than two elements or when some parameter other than the last one is not an identifier
     * @see com.bachelor_project.reactive.Program#addLocalSignal() 
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        if (parameterList.size() < 2)
            throw new RuntimeException("Too few arguments in call to function: local");
        
        Parameter innerStatement = parameterList.get(parameterList.size() - 1);
        Map<String, Statement> additionalTranlations = new HashMap<String, Statement>();

        for (int i = 0; i < parameterList.size() - 1; ++i) {
            
            Parameter param = parameterList.get(i);
            if ( !(param.getStatement() instanceof Identifier &&
                   ((Identifier) param.getStatement()).translate(param.getTranslationTable()) instanceof Identifier) ) {
                throw new RuntimeException("Argument " + i + " of call to function local is not an identifier");
            }
            
            String idName = ((Identifier) ((Identifier) param.getStatement()).translate(param.getTranslationTable())).getName();
            
            // overwrite translations for already present identifiers
            additionalTranlations.put(idName, new Identifier(guard.getEnvironment().addLocalSignal()));
            
        }
        
        innerStatement.makeWithAdditionalTranslations(additionalTranlations).execute(guard, null);
    }
    
}
