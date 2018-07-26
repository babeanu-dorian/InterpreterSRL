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
 * A {@link com.bachelor_project.interpreterast.statements.Statement} bound to a translation table.
 * This is class is needed because instructions passed as arguments to functions will use the scope of the caller,
 * not the local scope of the function.
 * @author Alexandru Babeanu
 */
public class Parameter implements Statement {
    
    private final Statement statement;
    private final Map<String, Statement> translationTable;
    
    /**
     * Constructs a {@link com.bachelor_project.interpreterast.statements.Parameter} from a
     * {@link com.bachelor_project.interpreterast.statements.Statement} and a translation table.
     * @param statement the encoding of the instruction to be executed
     * @param translationTable the scope of the encoded instruction
     */
    public Parameter(Statement statement, Map<String, Statement> translationTable) {
        this.statement = statement;
        this.translationTable = translationTable;
    }
    
    /**
     * Creates a copy with an extended translation table.
     * @param translations the additional {java.lang.String} to
     * {@link com.bachelor_project.interpreterast.statements.Statement} mappings.
     * @return the copy with an extended translation table
     * @see com.bachelor_project.interpreterast.statements.Statement
     */
    public Parameter makeWithAdditionalTranslations(Map<String, Statement> translations) {
        Map<String, Statement> newTranslationTable = new HashMap<String, Statement>(this.translationTable);
        newTranslationTable.putAll(translations);
        return new Parameter(this.statement, newTranslationTable);
    }
    
    /**
     * Returns the underlying {@link com.bachelor_project.interpreterast.statements.Statement} object.
     * @return underlying {@link com.bachelor_project.interpreterast.statements.Statement} object
     * @see com.bachelor_project.interpreterast.statements.Statement
     */
    public Statement getStatement() {
        return this.statement;
    }
    
    /**
     * Returns an unmodifiable {@link java.util.Map} view of the underlying translation table.
     * @return an unmodifiable {@link java.util.Map} view of the underlying translation table
     */
    public Map<String, Statement> getTranslationTable() {
        return Collections.unmodifiableMap(this.translationTable);
    }

    /**
     * Call the 
     * {@link com.bachelor_project.interpreterast.statements.Statement#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map) }
     * method of the underlying {@link com.bachelor_project.interpreterast.statements.Statement} with the underlying
     * translation table. Ignores the translation table passed as parameter.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable <b>ignored</b>
     * @return the return value of {@link com.bachelor_project.interpreterast.statements.Statement#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map) }
     * @throws RuntimeException
     * @see com.bachelor_project.interpreterast.statements.Statement
     * @see com.bachelor_project.reactive.SignalGuard
     */
    @Override
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException{
        // ignore the local scope (passed to this function) and use the scope of the caller (this.translationTable)
        return this.statement.execute(guard, this.translationTable);
    }
    
    /**
     * Execute the underlying {@link com.bachelor_project.interpreterast.statements.Statement}
     * with the underlying translation table and with additional parameters. This is relevant
     * only for {@link com.bachelor_project.interpreterast.statements.FunctionCall}
     * and {@link com.bachelor_project.interpreterast.statements.Identifier}.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable <b>ignored</b>
     * @param parameterList the additional arguments
     * @return the return value of {@link com.bachelor_project.interpreterast.statements.Statement#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map) }
     * @throws RuntimeException
     * @see com.bachelor_project.interpreterast.statements.Statement
     * @see com.bachelor_project.interpreterast.statements.FunctionCall
     * @see com.bachelor_project.interpreterast.statements.Identifier
     * @see com.bachelor_project.reactive.SignalGuard
     */
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable, List<Parameter> parameterList) throws RuntimeException{
        
        if (this.statement instanceof FunctionCall)
            return ((FunctionCall) this.statement).execute(guard, this.translationTable, parameterList);
        
        if (this.statement instanceof Identifier)
            return ((Identifier) this.statement).execute(guard, this.translationTable, parameterList);
        
        if (parameterList.size() > 0) {
            System.err.println("Warning: discarded unnecessary parameters"); // TODO: specify the location
        }
        
        return this.statement.execute(guard, this.translationTable);
    }
}
