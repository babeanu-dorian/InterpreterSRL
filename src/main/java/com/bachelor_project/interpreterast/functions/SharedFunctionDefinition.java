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
 *
 * @author Alexandru Babeanu
 */
public class SharedFunctionDefinition implements FunctionDefinition{

    /**
     *
     * @param guard
     * @param parameterList
     * @throws RuntimeException
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
