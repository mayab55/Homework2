package org.example;

import java.util.*;

public class LaggedMap<K, V> {

    private int draftSeconds;
    private Map<K, V> officialMap = new HashMap<>();
    private Map<K, List<Draft<V>>> draftsMap = new HashMap<>();
    private Map<K, List<V>> historyMap = new HashMap<>();


    public LaggedMap(int draftSeconds) {
        this.draftSeconds = draftSeconds;

        Thread backgroundThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);

                    synchronized (LaggedMap.this) {
                        publishReadyDrafts();
                        cleanOldHistory();
                    }

                } catch (InterruptedException e) {
                    System.out.println("ה-Thread ברקע נעצר.");
                }
            }
        });

        backgroundThread.setDaemon(true);
        backgroundThread.start();
    }


    public void put(K key, V value) {
        Draft<V> newDraft = new Draft<>(value, false, draftSeconds);

        if (!draftsMap.containsKey(key)) {
            draftsMap.put(key, new ArrayList<>());
        }

        draftsMap.get(key).add(newDraft);

        System.out.println("השינוי נכנס לטיוטה וממתין לפרסום!");
    }


    private void publishReadyDrafts() {
    }

    private void cleanOldHistory() {
        for (List<V> historyList : historyMap.values()) {

            while (historyList.size() > 3) {
                historyList.remove(0);
            }
        }
    }
}