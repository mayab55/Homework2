package org.example;

import java.util.*;
import java.util.concurrent.*;

public class LaggedMap<K, V> {

    private final int draftSeconds;
    private final ConcurrentHashMap<K, Draft<V>> publishedMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, List<ScheduledFuture<?>>> pendingDrafts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private volatile Map<K, Draft<V>> snapshotMap = new HashMap<>();

    public LaggedMap(int draftSeconds) {
        this.draftSeconds = draftSeconds;
        scheduler.scheduleAtFixedRate(this::cleanExcessHistory, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::createSnapshot, 1, 1, TimeUnit.MINUTES);
    }

    public void put(K key, V value) {
        ScheduledFuture<?> futureTask = scheduler.schedule(() -> {
            publishedMap.compute(key, (k, currentDraft) -> {
                if (currentDraft == null) {
                    return new Draft<>(value);
                } else {
                    currentDraft.updateValue(value);
                    return currentDraft;
                }
            });
            cleanUpPendingDraftRef(key);
        }, draftSeconds, TimeUnit.SECONDS);

        pendingDrafts.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(futureTask);
    }

    public V get(K key) {
        Draft<V> draft = publishedMap.get(key);
        return draft != null ? draft.getCurrentValue() : null;
    }

    public void abort() {
        for (K key : new ArrayList<>(pendingDrafts.keySet())) {
            List<ScheduledFuture<?>> futures = pendingDrafts.remove(key);
            if (futures != null) {
                for (ScheduledFuture<?> future : futures) {
                    future.cancel(false);
                }
            }
        }
    }

    private void cleanExcessHistory() {
        for (Draft<V> draft : publishedMap.values()) {
            draft.trimHistory(3);
        }
    }

    public void remove(K key, boolean full) {
        ScheduledFuture<?> futureTask = scheduler.schedule(() -> {
            if (full) {
                publishedMap.remove(key);
            } else {
                Draft<V> draft = publishedMap.get(key);
                if (draft != null && !draft.rollbackOrRemove()) {
                    publishedMap.remove(key);
                }
            }
            cleanUpPendingDraftRef(key);
        }, draftSeconds, TimeUnit.SECONDS);

        pendingDrafts.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(futureTask);
    }

    private synchronized void createSnapshot() {
        Map<K, Draft<V>> newSnapshot = new HashMap<>();
        for (Map.Entry<K, Draft<V>> entry : publishedMap.entrySet()) {
            Draft<V> original = entry.getValue();
            Draft<V> clone = new Draft<>(original.getCurrentValue());
            clone.copyHistoryFrom(original.getHistoryCopy());
            newSnapshot.put(entry.getKey(), clone);
        }
        snapshotMap = newSnapshot;
    }

    public synchronized void rollback() {
        abort();
        publishedMap.clear();
        for (Map.Entry<K, Draft<V>> entry : snapshotMap.entrySet()) {
            Draft<V> original = entry.getValue();
            Draft<V> clone = new Draft<>(original.getCurrentValue());
            clone.copyHistoryFrom(original.getHistoryCopy());
            publishedMap.put(entry.getKey(), clone);
        }
    }

    private void cleanUpPendingDraftRef(K key) {
        List<ScheduledFuture<?>> futures = pendingDrafts.get(key);
        if (futures != null) {
            futures.removeIf(Future::isDone);
            if (futures.isEmpty()) {
                pendingDrafts.remove(key);
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
