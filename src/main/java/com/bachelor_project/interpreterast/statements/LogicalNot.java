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
public class LogicalNot implements Statement{
    
    private final Statement toNegate;
    
    /**
     *
     * @param toNegate
     */
    public LogicalNot(Statement toNegate) {
        this.toNegate = toNegate;
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
        Object val = toNegate.execute(guard, translationTable);
        
        if (val instanceof LockedPointer) {
            val = ((LockedPointer) val).getValue();
        }
        
        if (val instanceof Boolean)
            return !((Boolean) val);
        
        throw new RuntimeException("Operation NOT is undefined for the type " + val.getClass().getName());
    }
    
}
