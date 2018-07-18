/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.interpreterast.types;

/**
 *
 * @author Alexandru Babeanu
 */
public class LockedPointer {
    
    /**
     *
     */
    public static final long NO_OWNER = -1;
    
    private class OwnerId { // Basically a mutable Long. It can be shared between objects so that one change will modify all objects.
    
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
     *
     * @param id
     */
    public LockedPointer(long id){
        this.owner = new OwnerId(id);
        this.value = null;
    }
    
    private LockedPointer(OwnerId owner) {
        this.owner = owner;
        this.value = null;
    }
    
    /**
     *
     * @return
     */
    public LockedPointer makeWithinStruct() {
        return new LockedPointer(this.owner);
    }
    
    /**
     *
     * @param id
     * @return
     */
    public boolean isOwner(long id) {
        return this.owner.getId() == id;
    }
    
    /**
     *
     * @return
     * @throws RuntimeException
     */
    public Object getValue() throws RuntimeException{
        
        if (!isOwner(Thread.currentThread().getId()))
            throw new RuntimeException("Shared resource accessed without locking");
        
        if (this.value == null)
            throw new RuntimeException("Null pointer exception");
        return this.value;
    }
    
    /**
     *
     * @param id
     */
    public void setOwner(long id) {
        this.owner.set(id);
    }
    
    /**
     *
     * @param val
     */
    public void setValue(Object val) {
        
        if (!isOwner(Thread.currentThread().getId()))
            throw new RuntimeException("Shared resource accessed without locking");
        
        this.value = val;
    }
    
    // objects from the same structure tree will share the OwnerId object;
    // objects from different structures may have the same OwnerId value, but will have different objects

    /**
     *
     * @param other
     * @return
     */
    public boolean sameStructure(LockedPointer other) {
        return this.owner == other.owner;
    }
    
}
