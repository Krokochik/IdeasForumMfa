package com.krokochik.ideasforummfa.model;

@FunctionalInterface
public interface Condition<V> {
    Boolean check(V value);
}
