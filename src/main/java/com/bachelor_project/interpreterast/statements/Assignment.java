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
public class Assignment extends BinaryOperation{
    
    /**
     *
     * @param lhs
     * @param rhs
     */
    public Assignment(Statement lhs, Statement rhs) {
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
        
        Object to = getLhsResult(guard, translationTable);
        
        if (!(to instanceof LockedPointer)) {
            throw new RuntimeException("The left hand side of an assignment cannot be a constant");
        }
        
        LockedPointer target = (LockedPointer) to;
        
        Object from = getRhsResult(guard, translationTable);
        Object value = from;
        
        if (from instanceof LockedPointer) {
            value = ((LockedPointer) from).getValue();
            
            if ((value instanceof Map) && !target.sameStructure((LockedPointer) from)) {
                throw new RuntimeException("You cannot assign structures to pointers with different access points");
            }
        }
        
        target.setValue(value);
        
        return target;
    }
    
}
