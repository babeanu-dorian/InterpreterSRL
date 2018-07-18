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
import java.util.Map;

/**
 *
 * @author Alexandru Babeanu
 */
public class StructFieldsFunctionDefinition  implements FunctionDefinition {

    /**
     *
     * @param guard
     * @param parameterList
     * @throws RuntimeException
     */
    @Override
    public void call(SignalGuard guard, List<Parameter> parameterList) throws RuntimeException {
        
        if (parameterList.size() != 2)
            throw new RuntimeException("Wrong number of arguments in call to function struct_fields");
        
        Object struct = parameterList.get(0).execute(guard, null);
        Object fields = parameterList.get(1).execute(guard, null);
        
        if (struct instanceof LockedPointer)
            struct = ((LockedPointer) struct).getValue();
        
        if ( !(struct instanceof Map) )
            throw new RuntimeException("Error: Argument 0 in call to function struct_fields is not a struct");
        
        if ( !(fields instanceof LockedPointer) )
            throw new RuntimeException("Error: Argument 1 in call to function struct_fields cannot be a constant");
        
        int length = 0;
        
        Map<String, LockedPointer> fieldList = new HashMap<String, LockedPointer>();
        for (String fieldName : ((Map<String, LockedPointer>) struct).keySet()) {
            LockedPointer ptr = ((LockedPointer) fields).makeWithinStruct();
            ptr.setValue(fieldName);
            fieldList.put(Integer.toString(length), ptr);
            length++;
        };
        
        LockedPointer ptr = ((LockedPointer) fields).makeWithinStruct();
        ptr.setValue(length);
        fieldList.put("length", ptr);
        
        ((LockedPointer) fields).setValue(fieldList);
        
    }
    
}
