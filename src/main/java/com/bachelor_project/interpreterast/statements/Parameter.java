/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.statements;

import com.bachelor_project.reactive.SignalGuard;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alexandru Babeanu
 */
public class Parameter implements Statement {
    
    private final Statement statement;
    private final Map<String, Statement> translationTable;
    
    /**
     *
     * @param statement
     * @param translationTable
     */
    public Parameter(Statement statement, Map<String, Statement> translationTable) {
        this.statement = statement;
        this.translationTable = translationTable;
    }
    
    /**
     *
     * @param translations
     * @return
     */
    public Parameter makeWithAdditionalTranslations(Map<String, Statement> translations) {
        Map<String, Statement> newTranslationTable = new HashMap<String, Statement>(this.translationTable);
        newTranslationTable.putAll(translations);
        return new Parameter(this.statement, newTranslationTable);
    }
    
    /**
     *
     * @return
     */
    public Statement getStatement() {
        return this.statement;
    }
    
    /**
     *
     * @return
     */
    public Map<String, Statement> getTranslationTable() {
        return Collections.unmodifiableMap(this.translationTable);
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
        // ignore the local scope (passed to this function) and use the scope of the caller (this.translationTable)
        return statement.execute(guard, this.translationTable);
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
        
        //System.out.println("In parameter: " + this.translationTable);
        if (this.statement instanceof FunctionCall)
            return ((FunctionCall) this.statement).execute(guard, this.translationTable, parameterList);
        
        if (this.statement instanceof Identifier)
            return ((Identifier) this.statement).execute(guard, this.translationTable, parameterList);
        
        if (parameterList.size() > 0) {
            System.err.println("Warning: discarded unnecessary parameters"); // TODO: specify the location
        }
        
        return statement.execute(guard, this.translationTable);
    }
}
