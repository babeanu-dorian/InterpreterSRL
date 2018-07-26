/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.statements;

import com.bachelor_project.reactive.SignalGuard;
import java.util.Map;

/**
 * Interface for classes that encode <b>SRL</b> instructions.
 * @author Alexandru Babeanu
 */
public interface Statement {
    
    /**
     * Runs the encoded instruction.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param translationTable the scope of the encoded instruction
     * @return the return value of the encoded instruction
     * @throws RuntimeException
     * @see com.bachelor_project.reactive.SignalGuard
     */
    public abstract Object execute(SignalGuard guard, Map<String, Statement> translationTable) throws RuntimeException;

}
