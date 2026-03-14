package com.example.android.lifecycle.util;

public class Counter {

    private static Counter INSTANCE = new Counter();

    private Integer count = 0;

    private Counter() {}

    public static Counter getInstance() { return INSTANCE; }

    public void incrementCount() {
        count++;
    }

    public void incrementBy(int n) {
        count += n;
    }

    public void reset() {
        count = 0;
    }

    public Integer getCount() {
        return count;
    }
}
