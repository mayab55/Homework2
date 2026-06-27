package org.example;

import java.util.LinkedList;
import java.util.List;

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

    // מחזיר עותק של ההיסטוריה - לא חושף את המבנה הפנימי
    public synchronized List<V> getHistoryCopy() {
        return new LinkedList<>(history);
    }

    // מנגנון ניקוי: מסיר ערכים ישנים מסוף הרשימה
    public synchronized void trimHistory(int maxSize) {
        while (history.size() > maxSize) {
            history.removeLast();
        }
    }

    // משמש ליצירת עותק ב-snapshot וב-rollback
    public synchronized void copyHistoryFrom(List<V> source) {
        history.clear();
        history.addAll(source);
    }

    // אטומי: מבצע rollback אם יש היסטוריה, אחרת מחזיר false (יש למחוק את המפתח)
    public synchronized boolean rollbackOrRemove() {
        if (history.isEmpty()) {
            return false;
        }
        currentValue = history.removeFirst();
        return true;
    }
}
