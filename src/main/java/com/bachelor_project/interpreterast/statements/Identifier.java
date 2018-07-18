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
public class Identifier implements Statement {
    private final String name;
    
    /**
     *
     * @param name
     */
    public Identifier(String name) {
        this.name = name;
    }

    /**
     *
     * @param guard
     * @param translationTable
     * @return
     * @throws RuntimeException
     */
    @Override
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException{
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
        return new FunctionCall(this, new ArrayList<Statement>()).execute(guard, translationTable, parameterList);
    }
    
    /**
     *
     * @param translationTable
     * @return
     */
    public Statement translate(Map<String, Statement> translationTable) {
        if (translationTable == null || !translationTable.containsKey(this.name))
            return this;
        return translationTable.get(this.name);
    }
    
    /**
     *
     * @return
     */
    public String getName() {
        return this.name;
    }
}
