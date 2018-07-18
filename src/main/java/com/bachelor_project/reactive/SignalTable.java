/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bachelor_project.reactive;

import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Alexandru Babeanu
 * 
 * Warning: This class is not thread-safe. Use locks for the map and toSerialized for the Observable.
 */
public class SignalTable {
    
    private final Map<String, Boolean> table;
    private final Subject<Map<String, Boolean>> observable;
    
    /**
     *
     */
    public SignalTable() {
        this.table = new HashMap<String, Boolean>();
        this.observable = BehaviorSubject.<Map<String, Boolean>>create();
    }
    
    /**
     *
     * @return
     */
    public Subject<Map<String, Boolean>> getObservable() {
        return this.observable;
    }
    
    /**
     *
     * @param key
     * @return
     */
    public boolean containsKey(String key) {
        return this.table.containsKey(key);
    }
    
    /**
     *
     * @return
     */
    public Set<String> keySet() {
        return this.table.keySet();
    }
    
    /**
     *
     * @param key
     * @return
     */
    public Boolean get(String key) {
        return this.table.get(key);
    }
    
    /**
     *
     * @return
     */
    public Map<String, Boolean> getMap() {
        return Collections.unmodifiableMap(this.table);
    }
    
    /**
     *
     * @param key
     * @param val
     */
    public void put(String key, Boolean val) {
        this.table.put(key, val);
        this.observable.onNext(getMap());
    }

    /**
     *
     * @param map
     */
    public void putAll(Map<String, Boolean> map) {
        this.table.putAll(map);
        this.observable.onNext(getMap());
    }
    
    /**
     *
     */
    public void resetSignals() {
        this.table.replaceAll((String key, Boolean val) -> {
            return false;
        });
        this.observable.onNext(getMap());
    }
    
}
