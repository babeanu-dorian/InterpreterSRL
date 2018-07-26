/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.statements;

import com.bachelor_project.reactive.SignalGuard;
import java.util.List;
import java.util.Map;

/**
 * Encodes a sequence of instructions (A;B;...).
 * @author Alexandru Babeanu
 */
public class StatementSequence implements Statement{

    List<Statement> statements;
    
    /**
     * Constructs an executable sequence of statements.
     * @param statementList the list of {@link com.bachelor_project.interpreterast.statements.Statement} objects in the sequence
     * @see com.bachelor_project.interpreterast.statements.Statement
     */
    public StatementSequence(List<Statement> statementList) {
        this.statements = statementList;
    }
    
    /**
     * Executes the statements the sequence in order. Uses {@link com.bachelor_project.reactive.SignalGuard#isAborting() }
     * to check if execution was aborted mid-sequence, and terminates execution appropriately.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable the scope of the instruction
     * @return <b>null</b>
     * @throws RuntimeException
     * @see com.bachelor_project.interpreterast.statements.Statement#execute(com.bachelor_project.reactive.SignalGuard, java.util.Map)
     * @see com.bachelor_project.reactive.SignalGuard#isAborting()
     * @see com.bachelor_project.reactive.SignalGuard
     */
    @Override
    public Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException {
        
        for (Statement statement : this.statements) {
            guard.executeStatement(statement, translationTable);
            if (guard.isAborting())
                break;
        };
        
        return null; // modify to impement function returns
    }
    
}
