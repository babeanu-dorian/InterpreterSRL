/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.statements.Statement;
import com.bachelor_project.interpreterast.statements.Value;
import com.bachelor_project.interpreterast.types.LockedPointer;
import com.bachelor_project.reactive.SignalGuard;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexandru Babeanu
 */
public class UserFunctionDefinition implements FunctionDefinition{
    private final List<String> parameterNames;
    private final Statement functionCode;
    
    /**
     *
     * @param parameterNames
     * @param functionCode
     */
    public UserFunctionDefinition(List<String> parameterNames, Statement functionCode) {
        this.parameterNames = parameterNames;
        this.functionCode = functionCode;
    }
    
    /**
     *
     * @param guard
     * @param parameterList
     * @throws RuntimeException
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (this.parameterNames.size() > parameterList.size())
            throw new RuntimeException("Not enough parameters in function call"); //TODO: specify where the call is
        else if (this.parameterNames.size() < parameterList.size())
            System.err.println("Warning: discarded unnecessary parameters"); // TODO: specify the location
        
        Map<String, Statement> translationTable = new HashMap<String, Statement>(guard.getGlobalScope());
        LockedPointer functionLocalScope = ((LockedPointer) translationTable.get("private").execute(guard, null)).makeWithinStruct();
        StructFunctionDefinition.makeStruct(functionLocalScope);
        translationTable.put("here", new Value(functionLocalScope));
        
        // rename the parameters
        for (int i = 0; i < this.parameterNames.size(); ++i) {
            translationTable.put(this.parameterNames.get(i), parameterList.get(i));
        }
        
        this.functionCode.execute(guard, translationTable);
    }
}
