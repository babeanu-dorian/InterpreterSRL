/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.types;

/**
 * A wrapper that restricts access to objects, used to implement shared variables.
 * @author Alexandru Babeanu
 */
public class LockedPointer {
    
    /**
     * The owner id of a variable whose lock is not currently held by any thread.
     */
    public static final long NO_OWNER = -1;
    
    /**
     * Auxiliary inner class used to store the id of the thread that can access the underlying resource.
     * It is basically a mutable {@link java.lang.Long}:
     * it can be shared and modified, so that the change is reflected in all instances
     */
    private class OwnerId {
    
        private long id;

        public OwnerId(long id) {
            this.id = id;
        }

        public long getId() {
            return this.id;
        }

        public void set(long id) {
            this.id = id;
        }
    }
    
    private final OwnerId owner; // all LockedPointer objects in the same structure will use the same OwnerId object
    private Object value;
    
    /**
     * Construct a new {@link com.bachelor_project.interpreterast.types.LockedPointer} that is not
     * part of an existing structure.
     * @param id the id of the {@link java.lang.Thread} that owns this resource, use
     * {@link com.bachelor_project.interpreterast.types.LockedPointer#NO_OWNER} when there is no owner at construction
     * @see com.bachelor_project.interpreterast.types.LockedPointer#NO_OWNER
     * @see java.lang.Thread#getId()
     */
    public LockedPointer(long id){
        this.owner = new OwnerId(id);
        this.value = null;
    }
    
    /**
     * Auxiliary constructor to be used by
     * {@link com.bachelor_project.interpreterast.types.LockedPointer#makeWithinStruct() }
     * @param owner the {@link com.bachelor_project.interpreterast.types.LockedPointer.OwnerId}
     * object used by the current {@link com.bachelor_project.interpreterast.types.LockedPointer},
     * and therefore by the whole structure
     * @see com.bachelor_project.interpreterast.types.LockedPointer#makeWithinStruct()
     * @see com.bachelor_project.interpreterast.types.LockedPointer.OwnerId
     */
    private LockedPointer(OwnerId owner) {
        this.owner = owner;
        this.value = null;
    }
    
    /**
     * Returns a new {@link com.bachelor_project.interpreterast.types.LockedPointer} object
     * that will share the owner with this {@link com.bachelor_project.interpreterast.types.LockedPointer}.
     * @return a new {@link com.bachelor_project.interpreterast.types.LockedPointer} object that shares
     * owner with this {@link com.bachelor_project.interpreterast.types.LockedPointer}
     */
    public LockedPointer makeWithinStruct() {
        return new LockedPointer(this.owner);
    }
    
    /**
     * Checks if a {@link java.lang.Thread} is the owner of this resource.
     * @param id the id of the {@link java.lang.Thread}
     * @return <b>true</b> if the {@link java.lang.Thread} is the owner, <b>false</b> otherwise
     * @see java.lang.Thread#getId()
     */
    public boolean isOwner(long id) {
        return this.owner.getId() == id;
    }
    
    /**
     * Returns the underlying object (the value of the shared variable).
     * @return the value of the shared variable
     * @throws RuntimeException if the current {@link java.lang.Thread} does not own the lock to this resource,
     * or if the shared variable has not been initialised
     * @see java.lang.Thread#getId()
     * @see java.lang.Thread#currentThread()
     */
    public Object getValue() throws RuntimeException{
        
        if (!isOwner(Thread.currentThread().getId()))
            throw new RuntimeException("Shared resource accessed without locking");
        
        if (this.value == null)
            throw new RuntimeException("Null pointer exception");
        return this.value;
    }
    
    /**
     * Changes the owner of the resource.
     * @param id the id of the new owner {@link java.lang.Thread}
     * @see java.lang.Thread#getId()
     */
    public void setOwner(long id) {
        this.owner.set(id);
    }
    
    /**
     * Changes the value of the shared variable.
     * @param val the new value
     * @throws RuntimeException if the current {@link java.lang.Thread} does not own the lock to this resource
     * @see java.lang.Thread#getId()
     * @see java.lang.Thread#currentThread()
     */
    public void setValue(Object val) throws RuntimeException {
        
        if (!isOwner(Thread.currentThread().getId()))
            throw new RuntimeException("Shared resource accessed without locking");
        
        this.value = val;
    }
    
    // objects from the same structure tree will share the OwnerId object;
    // objects from different structures may have the same OwnerId value, but will have different objects

    /**
     * Checks whether another {@link com.bachelor_project.interpreterast.types.LockedPointer}
     * is part of the same structure. {@link com.bachelor_project.interpreterast.types.LockedPointer}
     * objects from the same structure tree will share the
     * {@link com.bachelor_project.interpreterast.types.LockedPointer.OwnerId} object.
     * {@link com.bachelor_project.interpreterast.types.LockedPointer} objects from different structures
     * may have the same {@link com.bachelor_project.interpreterast.types.LockedPointer.OwnerId} value,
     * but will have different objects.
     * @param other the other {@link com.bachelor_project.interpreterast.types.LockedPointer} object
     * @return <b>true</b> if <b>other</b> is part of the same structure, <b>false</b> otherwise.
     */
    public boolean sameStructure(LockedPointer other) {
        return this.owner == other.owner;
    }
    
}
