/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.statements;

import com.bachelor_project.reactive.SignalGuard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alexandru Babeanu
 */
public class FunctionCall implements Statement {
    
    private final Identifier id;
    private final List<Statement> parameters;
    
    /**
     *
     * @param id
     * @param parameters
     */
    public FunctionCall(Identifier id, List<Statement> parameters) {
        this.id = id;
        this.parameters = parameters;
    }

    /**
     *
     * @param guard
     * @param translationTable
     * @return
     * @throws RuntimeException
     */
    @Override
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException {
        return execute(guard, translationTable, new ArrayList<Parameter>());    
    }
    
    /**
     *
     * @param guard
     * @param translationTable
     * @param parameterList
     * @return
     * @throws RuntimeException
     */
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable, List<Parameter> parameterList) throws RuntimeException{
        List<Parameter> fullParameterList = new ArrayList<Parameter>();
        //System.out.println("In " + this.id.getName() + " call: " + translationTable);
        parameters.forEach((Statement param) -> {
            if (param instanceof Identifier && ((Identifier) param).translate(translationTable) instanceof Parameter) {
                // prevent chain wrapping of parameter in parameter
                fullParameterList.add((Parameter) ((Identifier) param).translate(translationTable));
                //System.out.println(fullParameterList.get(fullParameterList.size() - 1).getTranslationTable());
            }
            else {
                fullParameterList.add(new Parameter(param, translationTable));
                //System.out.println(translationTable);
            }
        });
        fullParameterList.addAll(parameterList);
        
        Statement function = id.translate(translationTable);
        
        if (function instanceof Identifier) {
            guard.getEnvironment().callFunction(((Identifier) function).getName(), fullParameterList, guard);
            return null; // modify for implementing function return
        }
        
        if (function instanceof Parameter)
            return ((Parameter) function).execute(guard, translationTable, fullParameterList);
        
        if (function instanceof Value)
            return ((Value) function).execute(guard, translationTable);
        
        throw new RuntimeException("Error: Identifier was translated to a statement that isn't a Parameter, Identifier or Value, this should never happen");
    }
    
}
