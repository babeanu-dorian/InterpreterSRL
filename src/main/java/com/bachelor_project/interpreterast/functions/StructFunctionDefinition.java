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
 *
 * @author Alexandru Babeanu
 */
public class StructFunctionDefinition  implements FunctionDefinition {

    /**
     *
     * @param ptr
     */
    public static void makeStruct(LockedPointer ptr) {
        ptr.setValue(new HashMap<String, LockedPointer>());
    }
    
    /**
     *
     * @param guard
     * @param parameterList
     * @throws RuntimeException
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
