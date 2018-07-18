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
public class IndexOperation extends BinaryOperation {

    /**
     *
     * @param lhs
     * @param rhs
     */
    public IndexOperation(Statement lhs, Statement rhs) {
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
        
        Object right = getRhsResult(guard, translationTable);
        
        if (right instanceof LockedPointer) {
            right = ((LockedPointer) right).getValue();
        }
        
        if ( !(left instanceof LockedPointer)) {
            undefinedOperationError("index", left.getClass().getName(), right.getClass().getName());
        }
        
        Object maybeStruct = ((LockedPointer) left).getValue();
        
        if ( !(maybeStruct instanceof Map)) {
            undefinedOperationError("index ", maybeStruct.getClass().getName(), right.getClass().getName());
        }
        
        Map<String, LockedPointer> struct = (Map<String, LockedPointer>) maybeStruct;
        
        // if the key is not yet present, add it
        if (!struct.keySet().contains(right.toString()))
            struct.put(right.toString(), ((LockedPointer) left).makeWithinStruct());

        return struct.get(right.toString());
    }
    
}
