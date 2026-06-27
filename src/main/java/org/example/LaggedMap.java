package org.example;

import java.util.*;

// ==========================================
// סעיף א': מבנה המחלקה והבנאי
// ==========================================
public class LaggedMap<K, V> {
    private int draftSeconds;
    private Map<K, V> publishedMap;
    private Map<K, LinkedList<V>> historyMap;
    private Map<K, V> snapshotMap;
    private List<Thread> draftThreads;

    public LaggedMap(int draftSeconds) {
        this.draftSeconds = draftSeconds;
        this.publishedMap = new HashMap<>();
        this.historyMap = new HashMap<>();
        this.snapshotMap = new HashMap<>();
        this.draftThreads = new ArrayList<>();

        startHistoryCleaner();
        startSnapshotThread();
    }

    // ==========================================
    // סעיף ב': מתודת put
    // ==========================================
    public synchronized void put(K key, V value) {
        Thread putThread = new Thread(() -> {
            try {
                Thread.sleep(draftSeconds * 1000);
                synchronized (this) {
                    if (publishedMap.containsKey(key)) {
                        V oldVal = publishedMap.get(key);
                        if (!historyMap.containsKey(key)) {
                            historyMap.put(key, new LinkedList<>());
                        }
                        historyMap.get(key).add(oldVal);
                    }
                    publishedMap.put(key, value);
                    draftThreads.remove(Thread.currentThread());
                }
            } catch (InterruptedException e) {

            }
        });
        draftThreads.add(putThread);
        putThread.start();
    }

    // ==========================================
    // סעיף ג': מתודת get
    // ==========================================
    public synchronized V get(K key) {
        return publishedMap.get(key);
    }

    // ==========================================
    // סעיף ד': מתודת abort
    // ==========================================
    public synchronized void abort() {
        for (Thread t : draftThreads) {
            t.interrupt();
        }
        draftThreads.clear();
    }

    // ==========================================
    // סעיף ה': היסטורייה מוגבלת
    // ==========================================
    private void startHistoryCleaner() {
        Thread cleanerThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    synchronized (this) {
                        for (K key : historyMap.keySet()) {
                            LinkedList<V> history = historyMap.get(key);
                            while (history.size() > 3) {
                                history.removeFirst();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanerThread.start();
    }

    // ==========================================
    // סעיף ו': מתודת remove
    // ==========================================
    public synchronized void remove(K key, boolean full) {
        Thread removeThread = new Thread(() -> {
            try {
                Thread.sleep(draftSeconds * 1000);
                synchronized (this) {
                    if (full) {
                        publishedMap.remove(key);
                        historyMap.remove(key);
                    } else {
                        if (historyMap.containsKey(key) && !historyMap.get(key).isEmpty()) {
                            V prevVal = historyMap.get(key).removeLast();
                            publishedMap.put(key, prevVal);
                        } else {
                            publishedMap.remove(key);
                            historyMap.remove(key);
                        }
                    }
                    draftThreads.remove(Thread.currentThread());
                }
            } catch (InterruptedException e) {
            }
        });
        draftThreads.add(removeThread);
        removeThread.start();
    }

    // ==========================================
    // סעיף ז': שמירת מצב ושחזור rollback
    // ==========================================
    private void startSnapshotThread() {
        Thread snapshotThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000);
                    synchronized (this) {
                        snapshotMap = new HashMap<>(publishedMap);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        snapshotThread.start();
    }

    public synchronized void rollback() {
        abort();
        publishedMap = new HashMap<>(snapshotMap);
    }
    //תשובה לסעיף ח
    // ראשית כל מדובר במקרה קלאסי של מירוץ תהליכונים Race Condition
    //בגלל ששמנו את המילה synchronized על המתודות, Java לא מאפשרת לשני ת'רדים לשנות את הנתונים באותו הזמן
    //ברגע t=2.00 , ת'רד המחיקה מסיים את ההשהיה שלו בדיוק באותו שבריר שנייה שבו ת'רד אחר קורא ל-abort().
    //בגלל שהם הגיעו בדיוק ביחד, הכל תלוי במי שמצליח להיכנס ראשון לפעולה:
    //תרחיש 1 (המחיקה ניצחה): ת'רד המחיקה נכנס ראשון ומבצע את המחיקה בהצלחה. כשת'רד ה-abort נכנס מיד אחריו, המחיקה כבר הסתיימה ואין לה מה לבטל.
    // התוצאה עבור המפתח: הערך נמחק מהמפה (או שחזר לערך הקודם שלו מההיסטוריה, תלוי ב-full
    //תרחיש 2 (הביטול ניצח): ת'רד ה-abort נכנס ראשון. הוא תופס את ת'רד המחיקה כשהוא עדיין ברשימה, ועוצר אותו עם interrupt().
    // התוצאה עבור המפתח: פעולת המחיקה מתבטלת לחלוטין,
    // והערך המקורי שהיה במפתח נשאר בדיוק אותו הדבר, כאילו לא קרה כלום.
}