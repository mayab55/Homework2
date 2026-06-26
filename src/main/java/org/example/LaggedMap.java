package org.example;

import java.util.*;
import java.util.concurrent.*;

public class LaggedMap<K, V> {

    private final int draftSeconds;
    private final ConcurrentHashMap<K, Draft<V>> publishedMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, List<ScheduledFuture<?>>> pendingDrafts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private Map<K, Draft<V>> snapshotMap = new HashMap<>();
    private volatile boolean abortFlag = false;

    public LaggedMap(int draftSeconds) {
        this.draftSeconds = draftSeconds;
        scheduler.scheduleAtFixedRate(this::cleanExcessHistory, 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::createSnapshot, 1, 1, TimeUnit.MINUTES);
    }

    public void put(K key, V value) {
        ScheduledFuture<?> futureTask = scheduler.schedule(() -> {
            if (abortFlag) return;

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
        abortFlag = true;

        for (K key : pendingDrafts.keySet()) {
            List<ScheduledFuture<?>> futures = pendingDrafts.remove(key);
            if (futures != null) {
                for (ScheduledFuture<?> future : futures) {
                    future.cancel(false);
                }
            }
        }
    }

    private void cleanExcessHistory() {
        for (Map.Entry<K, Draft<V>> entry : publishedMap.entrySet()) {
            Draft<V> draft = entry.getValue();
            synchronized (draft) {
                while (draft.getHistory().size() > 3) {
                    draft.getHistory().removeLast();
                }
            }
        }
    }

    public void remove(K key, boolean full) {
        ScheduledFuture<?> futureTask = scheduler.schedule(() -> {
            if (abortFlag) return;

            if (full) {
                publishedMap.remove(key);
            } else {
                Draft<V> draft = publishedMap.get(key);
                if (draft != null) {
                    synchronized (draft) {
                        if (draft.getHistory().isEmpty()) {
                            publishedMap.remove(key);
                        } else {
                            draft.rollbackToPrevious();
                        }
                    }
                }
            }
            cleanUpPendingDraftRef(key);
        }, draftSeconds, TimeUnit.SECONDS);

        pendingDrafts.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(futureTask);
    }

    private synchronized void createSnapshot() {
        snapshotMap = new HashMap<>();
        for (Map.Entry<K, Draft<V>> entry : publishedMap.entrySet()) {
            Draft<V> originalDraft = entry.getValue();
            synchronized (originalDraft) {
                Draft<V> clonedDraft = new Draft<>(originalDraft.getCurrentValue());
                clonedDraft.getHistory().addAll(originalDraft.getHistory());
                snapshotMap.put(entry.getKey(), clonedDraft);
            }
        }
    }

    public synchronized void rollback() {
        abort();
        publishedMap.clear();
        for (Map.Entry<K, Draft<V>> entry : snapshotMap.entrySet()) {
            Draft<V> originalDraft = entry.getValue();
            Draft<V> clonedDraft = new Draft<>(originalDraft.getCurrentValue());
            clonedDraft.getHistory().addAll(originalDraft.getHistory());
            publishedMap.put(entry.getKey(), clonedDraft);
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
