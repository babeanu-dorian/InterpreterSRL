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
 * Encodes a function or variable identifier.
 * @author Alexandru Babeanu
 */
public class Identifier implements Statement {
    private final String name;
    
    /**
     * Constructs an identifier that can be executed.
     * @param name the name of the variable/function
     */
    public Identifier(String name) {
        this.name = name;
    }

    /**
     * Constructs a {@link com.bachelor_project.interpreterast.statements.FunctionCall} object and passes execution to it.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable the scope of the instruction
     * @return the return value of the {@link com.bachelor_project.interpreterast.statements.FunctionCall}
     * @throws RuntimeException
     * @see com.bachelor_project.interpreterast.statements.FunctionCall
     * @see com.bachelor_project.reactive.SignalGuard
     */
    @Override
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException{
        return execute(guard, translationTable, new ArrayList<Parameter>());
    }
    
    /**
     * Constructs a {@link com.bachelor_project.interpreterast.statements.FunctionCall} object and passes execution to it.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable the scope of the instruction
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects encoding the function arguments
     * @return the return value of the {@link com.bachelor_project.interpreterast.statements.FunctionCall}
     * @throws RuntimeException
     * @see com.bachelor_project.interpreterast.statements.FunctionCall
     * @see com.bachelor_project.interpreterast.statements.Parameter
     * @see com.bachelor_project.reactive.SignalGuard
     */
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable, List<Parameter> parameterList) throws RuntimeException{
        return new FunctionCall(this, new ArrayList<Statement>()).execute(guard, translationTable, parameterList);
    }
    
    /**
     * Returns the {@link com.bachelor_project.interpreterast.statements.Statement} object that is mapped to this
     * {@link com.bachelor_project.interpreterast.statements.Identifier} in the provided scope.
     * @param translationTable the scope of the instruction
     * @return the {@link com.bachelor_project.interpreterast.statements.Statement} object that is mapped to this
     * {@link com.bachelor_project.interpreterast.statements.Identifier} in the provided scope,
     * or <b>this</b> if there is no mapping
     * @see com.bachelor_project.interpreterast.statements.Statement
     */
    public Statement translate(Map<String, Statement> translationTable) {
        if (translationTable == null || !translationTable.containsKey(this.name))
            return this;
        return translationTable.get(this.name);
    }
    
    /**
     * Returns the identifier as a {@link java.lang.String}
     * @return the underlying {@link java.lang.String}
     */
    public String getName() {
        return this.name;
    }
}
