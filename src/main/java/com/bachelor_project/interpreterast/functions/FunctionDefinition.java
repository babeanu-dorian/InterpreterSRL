/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.reactive.SignalGuard;
import java.util.List;

/**
 * Interface for classes that encode function definitions (parameter list + instruction block).
 * @author Alexandru Babeanu
 */
public interface FunctionDefinition {
    
    /**
     * Execute the function when called with the given parameters.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects encoding the function arguments
     * @throws RuntimeException
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException;
    
}
