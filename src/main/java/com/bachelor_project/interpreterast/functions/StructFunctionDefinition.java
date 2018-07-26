/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.functions;

import com.bachelor_project.interpreterast.statements.Parameter;
import com.bachelor_project.interpreterast.types.LockedPointer;
import com.bachelor_project.reactive.SignalGuard;
import java.util.List;
import java.util.HashMap;

/**
 * Implements the <b>struct</b> construct in the <b>SRL</b> language.
 * @author Alexandru Babeanu
 */
public class StructFunctionDefinition  implements FunctionDefinition {

    /**
     * Assigns a structure to a {@link com.bachelor_project.interpreterast.types.LockedPointer} object.
     * In SRL, a structure is a map from {@link java.lang.String} objects to
     * {@link com.bachelor_project.interpreterast.types.LockedPointer} objects.
     * @param ptr the {@link com.bachelor_project.interpreterast.types.LockedPointer} object in which
     * the structure will be stored.
     */
    public static void makeStruct(LockedPointer ptr) {
        ptr.setValue(new HashMap<String, LockedPointer>());
    }
    
    /**
     * Creates a structure and assigns it to the provided variable.
     * @param guard the {@link com.bachelor_project.reactive.SignalGuard} object that manages the current {@link java.lang.Thread}
     * @param parameterList a list of {@link com.bachelor_project.interpreterast.statements.Parameter} objects containing exactly one
     * {@link com.bachelor_project.interpreterast.statements.Value} object that evaluates to a
     * {@link com.bachelor_project.interpreterast.types.LockedPointer} object in which the structure will be stored.
     * @throws RuntimeException when the parameter list does not contain exactly one element, or when that element
     * does not contain a variable.
     * @see com.bachelor_project.interpreterast.statements.Value
     * @see com.bachelor_project.interpreterast.types.LockedPointer
     * @see com.bachelor_project.reactive.SignalGuard
     * @see com.bachelor_project.interpreterast.statements.Parameter
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() != 1)
            throw new RuntimeException("Wrong number of arguments in call to function struct");
        
        Object ptr = parameterList.get(0).execute(guard, null);
        
        if ( !(ptr instanceof LockedPointer) )
            throw new RuntimeException("Argument 0 of struct function cannot be a constant");
        
        makeStruct((LockedPointer) ptr);
        
    }
    
}
