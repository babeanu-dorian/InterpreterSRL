/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.statements;

import com.bachelor_project.reactive.SignalGuard;
import java.util.Map;

/**
 * Superclass for classes that encode binary operators.
 * @author Alexandru Babeanu
 */
public abstract class BinaryOperation implements Statement{
    
    private final Statement lhs;
    private final Statement rhs;
    
    /**
     * Constructs a {@link com.bachelor_project.interpreterast.statements.BinaryOperation} object
     * from the two {@link com.bachelor_project.interpreterast.statements.Statement} objects encoding
     * the operands.
     * @param lhs the left-hand side operand
     * @param rhs the right-hand side operand
     * @see com.bachelor_project.interpreterast.statements.Statement
     */
    public BinaryOperation(Statement lhs, Statement rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }
    
    /**
     * Executes the left-hand side operand and returns its result.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable the scope of the encoded instruction
     * @return the value of the encoded left-hand side operand
     * @throws RuntimeException
     * @see com.bachelor_project.reactive.SignalGuard
     */
    public Object getLhsResult(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException  {
        return this.lhs.execute(guard, translationTable);
    }
    
    /**
     * Executes the right-hand side operand and returns its result.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable the scope of the encoded instruction
     * @return the value of the encoded right-hand side operand
     * @throws RuntimeException
     * @see com.bachelor_project.reactive.SignalGuard
     */
    public Object getRhsResult(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException  {
        return this.rhs.execute(guard, translationTable);
    }
    
    /**
     * Used for throwing an exception with a specific message
     * when an operation is performed on types it is not defined on. 
     * @param operation the name or symbol of the operation
     * @param type1 the type of the left-hand side operand
     * @param type2 the type of the right-hand side operand
     * @throws RuntimeException always
     */
    protected void undefinedOperationError(String operation, String type1, String type2) throws RuntimeException {
        throw new RuntimeException("Operator " + operation + " is undefined for the types " + type1 + " and " + type2);
    }
    
}
