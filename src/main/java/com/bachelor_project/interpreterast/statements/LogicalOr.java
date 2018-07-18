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
public class LogicalOr extends BinaryOperation{

    /**
     *
     * @param lhs
     * @param rhs
     */
    public LogicalOr(Statement lhs, Statement rhs) {
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
        
        if (!(left instanceof Boolean && right instanceof Boolean)) {
            undefinedOperationError("OR", left.getClass().getName(), right.getClass().getName());
        }
        
        return ((Boolean) left) || ((Boolean) right);
    }
    
}