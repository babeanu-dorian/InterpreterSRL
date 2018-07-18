/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.statements;

import com.bachelor_project.reactive.SignalGuard;
import java.util.Map;

/**
 *
 * @author Alexandru Babeanu
 */
public abstract class BinaryOperation implements Statement{
    
    private final Statement lhs;
    private final Statement rhs;
    
    /**
     *
     * @param lhs
     * @param rhs
     */
    public BinaryOperation(Statement lhs, Statement rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }
    
    /**
     *
     * @param guard
     * @param translationTable
     * @return
     * @throws RuntimeException
     */
    public Object getLhsResult(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException  {
        return this.lhs.execute(guard, translationTable);
    }
    
    /**
     *
     * @param guard
     * @param translationTable
     * @return
     * @throws RuntimeException
     */
    public Object getRhsResult(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException  {
        return this.rhs.execute(guard, translationTable);
    }
    
    /**
     *
     * @param operation
     * @param type1
     * @param type2
     * @throws RuntimeException
     */
    protected void undefinedOperationError(String operation, String type1, String type2) throws RuntimeException {
        throw new RuntimeException("Operator " + operation + " is undefined for the types " + type1 + " and " + type2);
    }
    
}
