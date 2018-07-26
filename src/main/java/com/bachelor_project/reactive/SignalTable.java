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
 * Implements an observable signal environment.
 * Uses a {@link java.util.Map} from signal names ({@link java.lang.String}) to boolean values ({@link java.lang.Boolean}) to encode the signal environment,
 * and a <a href="http://reactivex.io/RxJava/javadoc/rx/subjects/BehaviorSubject.html">BehaviorSubject</a> to propagate changes to its observers.
 * <br>Note: This class is not thread-safe. Use locks for the map and
 * <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/subjects/Subject.html#toSerialized--">Subject.toSerialized()</a>
 * for the <a href="http://reactivex.io/RxJava/javadoc/rx/subjects/BehaviorSubject.html">BehaviorSubject</a>.
 * @author Alexandru Babeanu
 * @see <a href="http://reactivex.io/RxJava/javadoc/rx/subjects/BehaviorSubject.html">BehaviorSubject</a>
 * @see <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/subjects/Subject.html#toSerialized--">Subject.toSerialized()</a>
 */
public class SignalTable {
    
    private final Map<String, Boolean> table;
    private final Subject<Map<String, Boolean>> observable;
    
    /**
     * Constructs an empty signal table.
     */
    public SignalTable() {
        this.table = new HashMap<String, Boolean>();
        this.observable = BehaviorSubject.<Map<String, Boolean>>create();
    }
    
    /**
     * Offers access to the underlying <a href="http://reactivex.io/RxJava/javadoc/rx/subjects/BehaviorSubject.html">BehaviorSubject</a>.
     * @return the underlying <a href="http://reactivex.io/RxJava/javadoc/rx/subjects/BehaviorSubject.html">BehaviorSubject</a>
     * @see <a href="http://reactivex.io/RxJava/javadoc/rx/subjects/BehaviorSubject.html">BehaviorSubject</a>
     */
    public Subject<Map<String, Boolean>> getObservable() {
        return this.observable;
    }
    
    /**
     * Returns whether a signal name is present in the table or not.
     * @param key the name of the signal
     * @return <b>true<\b> if <b>key</b> is a signal name in the table, <b>false</b> otherwise
     */
    public boolean containsKey(String key) {
        return this.table.containsKey(key);
    }
    
    /**
     * Returns a {@link java.util.Set} view of the signal names in the table.
     * @return a {@link java.util.Set} view of the signal names in the table
     */
    public Set<String> keySet() {
        return this.table.keySet();
    }
    
    /**
     * Returns whether a signal is present or absent in the environment.
     * @param key the name of the signal
     * @return <b>true<\b> if <b>key</b> is a present signal, <b>false</b> otherwise
     */
    public Boolean get(String key) {
        return this.table.get(key);
    }
    
    /**
     * Returns an unmodifiable view of the underlying {@link java.util.Map}.
     * @return an unmodifiable view of the underlying {@link java.util.Map}
     */
    public Map<String, Boolean> getMap() {
        return Collections.unmodifiableMap(this.table);
    }
    
    /**
     * Adds a new mapping (<b>key></b>, <b>val</b>) in the signal table and presents all registered subscribers
     * with an unmodifiable view of the underlying {@link java.util.Map}.
     * @param key the signal name
     * @param val <b>true</b> for present, <b>false</b> for absent
     */
    public void put(String key, Boolean val) {
        this.table.put(key, val);
        this.observable.onNext(getMap());
    }

    /**
     * Adds all the mappings in <b>map</b> to the signal table and presents all registered subscribers
     * with an unmodifiable view of the underlying {@link java.util.Map}.
     * @param map a {@link java.util.Map} containing (signal name, signal state) pairings
     */
    public void putAll(Map<String, Boolean> map) {
        this.table.putAll(map);
        this.observable.onNext(getMap());
    }
    
    /**
     * Maps all signals in the table to <b>false</b> (absent) and presents all registered subscribers
     * with an unmodifiable view of the underlying {@link java.util.Map}.
     */
    public void resetSignals() {
        this.table.replaceAll((String key, Boolean val) -> {
            return false;
        });
        this.observable.onNext(getMap());
    }
    
}
