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
 * Encode a function call in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class FunctionCall implements Statement {
    
    private final Identifier id;
    private final List<Statement> parameters;
    
    /**
     * Constructs a {@link com.bachelor_project.interpreterast.statements.FunctionCall} object from
     * an {@link com.bachelor_project.interpreterast.statements.Identifier} and a list of
     * {@link com.bachelor_project.interpreterast.statements.Statement} objects encoding the arguments.
     * @param id the {@link com.bachelor_project.interpreterast.statements.Identifier} used in the function call
     * @param parameters the arguments used in the call
     * @see com.bachelor_project.interpreterast.statements.Statement
     * @see com.bachelor_project.interpreterast.statements.Identifier
     */
    public FunctionCall(Identifier id, List<Statement> parameters) {
        this.id = id;
        this.parameters = parameters;
    }

    /**
     * Executes a function call with no arguments. Delegates its work to
     * {@link com.bachelor_project.interpreterast.statements.FunctionCall#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map, java.util.List) }.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable the scope of the instruction
     * @return the output of the function call (currently always <b>null</b>)
     * @throws RuntimeException
     * @see com.bachelor_project.interpreterast.statements.FunctionCall#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map, java.util.List)
     * @see com.bachelor_project.reactive.SignalGuard
     */
    @Override
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException {
        return execute(guard, translationTable, new ArrayList<Parameter>());    
    }
    
    /**
     * Executes a function call with arguments. It first translates the underlying {@link com.bachelor_project.interpreterast.statements.Identifier}
     * in the given scope, via the method {@link com.bachelor_project.interpreterast.statements.Identifier#translate(java.util.Map) },
     * then uses the method {@link com.bachelor_project.reactive.Program#callFunction(java.lang.String, java.util.List, com.bachelor_project.reactive.SignalGuard) }
     * to execute the function code.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable the scope of the instruction
     * @param parameterList the list of arguments
     * @return the output of the function call (currently always <b>null</b>)
     * @throws RuntimeException
     * @see com.bachelor_project.interpreterast.statements.Identifier#translate(java.util.Map)
     * @see com.bachelor_project.reactive.Program#callFunction(java.lang.String, java.util.List, com.bachelor_project.reactive.SignalGuard)
     * @see com.bachelor_project.reactive.SignalGuard
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
        
        throw new RuntimeException("Error: Identifier was translated to a statement that isn't a Parameter, Identifier or Value. This indicates a mistake in the parser code.");
    }
    
}
