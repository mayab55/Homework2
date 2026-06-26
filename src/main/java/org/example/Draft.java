package org.example;

import java.util.LinkedList;

public class Draft<V> {
    private V currentValue;
    private final LinkedList<V> history = new LinkedList<>();

    public Draft(V value) {
        this.currentValue = value;
    }

    public synchronized V getCurrentValue() {
        return currentValue;
    }

    public synchronized void updateValue(V newValue) {
        if (this.currentValue != null) {
            history.addFirst(this.currentValue);
        }
        this.currentValue = newValue;
    }

    public synchronized LinkedList<V> getHistory() {
        return history;
    }

    public synchronized void rollbackToPrevious() {
        if (!history.isEmpty()) {
            this.currentValue = history.removeFirst();
        }
    }
}
