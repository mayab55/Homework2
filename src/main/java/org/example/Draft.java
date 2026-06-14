package org.example;

public class Draft<V> {

    private V value;
    private long publishTime;
    private boolean isDelete;

    public Draft(V value, boolean isDelete, int draftSeconds) {
        this.value = value;
        this.isDelete = isDelete;
        this.publishTime = System.currentTimeMillis() + (draftSeconds * 1000L);
    }

    public long getPublishTime() {
        return publishTime;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public V getValue() {
        return value;
    }
}