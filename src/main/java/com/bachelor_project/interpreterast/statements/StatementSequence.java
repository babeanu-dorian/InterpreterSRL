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
 *
 * @author Alexandru Babeanu
 */
public class StatementSequence implements Statement{

    List<Statement> statements;
    
    /**
     *
     * @param statementList
     */
    public StatementSequence(List<Statement> statementList) {
        this.statements = statementList;
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
        
        for (Statement statement : this.statements) {
            guard.executeStatement(statement, translationTable);
            if (guard.isAborting())
                break;
        };
        
        return null; // modify to impement function returns
    }
    
}
