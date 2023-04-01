package com.krokochik.ideasforummfa.model;

@FunctionalInterface
public interface CallbackTask<V> {
    void run(V value);
}
