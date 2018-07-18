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
public class Value implements Statement {
    
    private final Object value;
    
    /**
     *
     * @param val
     */
    public Value(Object val) {
        this.value = val;
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
        return this.value;
    }
    
}
