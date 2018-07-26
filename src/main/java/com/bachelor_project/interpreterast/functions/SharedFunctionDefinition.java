/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Identifier;
import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.statements.Statement;
import com.bachelor_project.interpreterast.statements.Value;
import com.bachelor_project.interpreterast.types.LockedPointer;
import com.bachelor_project.reactive.SignalGuard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements the <b>shared</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class SharedFunctionDefinition implements FunctionDefinition{

    /**
     * Creates new shared variables that are only accessible from the instruction block to be executed, then executes said instruction block.
     * Creates a new {@link com.bachelor_project.interpreterast.types.LockedPointer} for each variable identifier,
     * registers them with the scheduler using {@link com.bachelor_project.reactive.Scheduler#declareResources(java.util.List)},
     * and stores them in {@link com.bachelor_project.interpreterast.statements.Value} objects.
     * These objects will be matched with their identifiers in the translation table of the last
     * {@link com.bachelor_project.interpreterast.statements.Parameter} in the list by using the method
     * {@link com.bachelor_project.interpreterast.statements.Parameter#makeWithAdditionalTranslations(java.util.Map) }.
     * The method {@link com.bachelor_project.interpreterast.statements.Parameter#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map) }
     * is then called on this {@link com.bachelor_project.interpreterast.statements.Parameter} to execute the inner instruction block.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing
     * the identifiers for the local shared variables, as well as the block of instructions to which the variables are bound
     * @throws RuntimeException when the parameter list has less than two elements or when some parameter other than the last one is not an identifier
     * @see com.bachelor_project.interpreterast.types.LockedPointer
     * @see com.bachelor_project.interpreterast.statements.Value
     * @see com.bachelor_project.interpreterast.statements.Parameter#makeWithAdditionalTranslations(java.util.Map)
     * @see com.bachelor_project.reactive.Scheduler#declareResources(java.util.List)
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        if (parameterList.size() < 2)
            throw new RuntimeException("Too few arguments in call to function: local");
        
        Parameter innerStatement = parameterList.get(parameterList.size() - 1);
        Map<String, Statement> additionalTranlations = new HashMap<String, Statement>();
        List<LockedPointer> resources = new ArrayList<LockedPointer>();

        for (int i = 0; i < parameterList.size() - 1; ++i) {
            
            Parameter param = parameterList.get(i);
            if ( !(param.getStatement() instanceof Identifier &&
                   ((Identifier) param.getStatement()).translate(param.getTranslationTable()) instanceof Identifier) ) {
                throw new RuntimeException("Argument " + i + " of call to function shared is not an identifier");
            }
            
            String idName = ((Identifier) ((Identifier) param.getStatement()).translate(param.getTranslationTable())).getName();
            
            // create a new resource (locked pointer)
            LockedPointer var = new LockedPointer(LockedPointer.NO_OWNER);
            resources.add(var);
            
            // overwrite translations for already present identifiers
            additionalTranlations.put(idName, new Value(var));
            
        }
        
        // register new resources with the scheduler
        guard.getEnvironment().getScheduler().declareResources(resources);
        
        innerStatement.makeWithAdditionalTranslations(additionalTranlations).execute(guard, null);
    }
    
}
