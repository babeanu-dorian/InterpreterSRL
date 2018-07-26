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
 * Implements a function defined by a programmer using the <b>let</b> construct.
 * @author Alexandru Babeanu
 */
public class UserFunctionDefinition implements FunctionDefinition{
    private final List<String> parameterNames;
    private final Statement functionCode;
    
    /**
     * Constructs an object that encodes a function from an instruction block and a list of parameter names.
     * @param parameterNames a list of {@link java.lang.String} objects containing the names of the function parameters
     * @param functionCode a {@link com.bachelor_project.interpreterast.statements.Statement} object that encodes the
     * function body
     * @see com.bachelor_project.interpreterast.statements.Statement
     */
    public UserFunctionDefinition(List<String> parameterNames, Statement functionCode) {
        this.parameterNames = parameterNames;
        this.functionCode = functionCode;
    }
    
    /**
     * Executes a call to a function defined in the program.
     * It first constructs the translation table that encodes the scope of the function,
     * then uses it to executes the function body.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing the
     * arguments passed to the function call
     * @throws RuntimeException if the number of arguments is less than the number of expected arguments
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
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
