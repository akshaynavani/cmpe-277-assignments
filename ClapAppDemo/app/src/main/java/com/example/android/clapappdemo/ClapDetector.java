package com.example.android.clapappdemo;

/**
 * Pure-Java clap detection state machine.
 *
 * A clap is defined as one complete near → far proximity cycle:
 *   1. distance drops below nearThreshold  → "hand near"  phase begins
 *   2. distance rises back to nearThreshold or above → clap is registered
 *
 * This class has no Android dependencies and can be unit-tested directly.
 */
public class ClapDetector {

    /** Callback interface — implemented by MainActivity to update the UI. */
    public interface Listener {
        void onHandNear();
        void onClap(int totalClaps);
        void onReady();
    }

    private final float nearThreshold;
    private final Listener listener;

    private boolean handNear = false;
    private int clapCount = 0;

    public ClapDetector(float nearThreshold, Listener listener) {
        this.nearThreshold = nearThreshold;
        this.listener = listener;
    }

    /**
     * Feed a raw proximity reading into the detector.
     * Called from MainActivity.onSensorChanged().
     */
    public void onProximityChanged(float distance) {
        if (distance < nearThreshold) {
            if (!handNear) {
                handNear = true;
                listener.onHandNear();
            }
        } else {
            if (handNear) {
                handNear = false;
                clapCount++;
                listener.onClap(clapCount);
            } else {
                listener.onReady();
            }
        }
    }

    public void reset() {
        clapCount = 0;
        handNear = false;
    }

    public int getClapCount() {
        return clapCount;
    }

    public boolean isHandNear() {
        return handNear;
    }

    public float getNearThreshold() {
        return nearThreshold;
    }
}
