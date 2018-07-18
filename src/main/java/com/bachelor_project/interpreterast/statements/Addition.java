/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.statements;

import com.bachelor_project.interpreterast.types.LockedPointer;
import com.bachelor_project.reactive.SignalGuard;
import java.util.Map;

/**
 *
 * @author Alexandru Babeanu
 */
public class Addition extends BinaryOperation{

    /**
     *
     * @param lhs
     * @param rhs
     */
    public Addition(Statement lhs, Statement rhs) {
        super(lhs, rhs);
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
        Object left = getLhsResult(guard, translationTable);
        
        if (left instanceof LockedPointer) {
            left = ((LockedPointer) left).getValue();
        }
        
        Object right = getRhsResult(guard, translationTable);
        
        if (right instanceof LockedPointer) {
            right = ((LockedPointer) right).getValue();
        }
        
        if (left instanceof String) {
            if (right instanceof String || right instanceof Double || right instanceof Integer || right instanceof Boolean) {
                return left.toString() + right.toString();
            }
            undefinedOperationError("+", left.getClass().getName(), right.getClass().getName());
        }
        
        if (right instanceof String) {
            if (left instanceof Double || left instanceof Integer || left instanceof Boolean) {
                return left.toString() + right.toString();
            }
            undefinedOperationError("+", left.getClass().getName(), right.getClass().getName());
        }
        
        if (!(left instanceof Number && right instanceof Number)) {
            undefinedOperationError("+", left.getClass().getName(), right.getClass().getName());
        }
        
        if (left instanceof Double || right instanceof Double) {
            return ((Number) left).doubleValue() + ((Number) right).doubleValue();
        }
        
        return ((Number) left).intValue() + ((Number) right).intValue();
    }
    
}
