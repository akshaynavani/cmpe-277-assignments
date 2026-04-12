package com.example.android.clapappdemo;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClapDetectorTest {

    private static final float THRESHOLD = 5.0f;
    private static final float NEAR = 0f;
    private static final float FAR = 10f;

    // Simple recording listener so tests can inspect what callbacks fired
    private static class RecordingListener implements ClapDetector.Listener {
        int handNearCount = 0;
        int clapCount = 0;
        int lastTotalClaps = 0;
        int readyCount = 0;

        @Override public void onHandNear()            { handNearCount++; }
        @Override public void onClap(int totalClaps)  { clapCount++; lastTotalClaps = totalClaps; }
        @Override public void onReady()               { readyCount++; }
    }

    private RecordingListener listener;
    private ClapDetector detector;

    @Before
    public void setUp() {
        listener = new RecordingListener();
        detector = new ClapDetector(THRESHOLD, listener);
    }

    // ── State queries ────────────────────────────────────────────────────────

    @Test
    public void initialState_noClapsCounted() {
        assertEquals(0, detector.getClapCount());
    }

    @Test
    public void initialState_handNotNear() {
        assertFalse(detector.isHandNear());
    }

    @Test
    public void nearThreshold_isStoredCorrectly() {
        assertEquals(THRESHOLD, detector.getNearThreshold(), 0.001f);
    }

    // ── Single clap gesture ──────────────────────────────────────────────────

    @Test
    public void nearReading_setsHandNearAndFiresCallback() {
        detector.onProximityChanged(NEAR);

        assertTrue(detector.isHandNear());
        assertEquals(1, listener.handNearCount);
    }

    @Test
    public void nearThenFar_registersOneClap() {
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);

        assertEquals(1, detector.getClapCount());
        assertEquals(1, listener.clapCount);
        assertEquals(1, listener.lastTotalClaps);
    }

    @Test
    public void nearThenFar_handNearIsClearedAfterClap() {
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);

        assertFalse(detector.isHandNear());
    }

    // ── Duplicate events (same phase repeated) ────────────────────────────────

    @Test
    public void multipleNearEvents_onlyOneHandNearCallbackFired() {
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(NEAR);

        assertEquals(1, listener.handNearCount);
    }

    @Test
    public void multipleNearEventsThenFar_stillCountsAsOneClap() {
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);

        assertEquals(1, detector.getClapCount());
    }

    @Test
    public void farWithoutPriorNear_doesNotRegisterClap() {
        detector.onProximityChanged(FAR);

        assertEquals(0, detector.getClapCount());
        assertEquals(0, listener.clapCount);
    }

    @Test
    public void farWithoutPriorNear_firesReadyCallback() {
        detector.onProximityChanged(FAR);

        assertEquals(1, listener.readyCount);
    }

    // ── Multiple claps ───────────────────────────────────────────────────────

    @Test
    public void threeFullCycles_countsThreeClaps() {
        for (int i = 0; i < 3; i++) {
            detector.onProximityChanged(NEAR);
            detector.onProximityChanged(FAR);
        }

        assertEquals(3, detector.getClapCount());
        assertEquals(3, listener.clapCount);
    }

    @Test
    public void clapCallback_totalClapsReflectsRunningTotal() {
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);
        assertEquals(1, listener.lastTotalClaps);

        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);
        assertEquals(2, listener.lastTotalClaps);
    }

    // ── Threshold boundary ───────────────────────────────────────────────────

    @Test
    public void distanceJustBelowThreshold_treatedAsNear() {
        detector.onProximityChanged(THRESHOLD - 0.1f);

        assertTrue(detector.isHandNear());
    }

    @Test
    public void distanceExactlyAtThreshold_treatedAsFar() {
        detector.onProximityChanged(THRESHOLD);

        assertFalse(detector.isHandNear());
        assertEquals(1, listener.readyCount);
    }

    @Test
    public void distanceAboveThreshold_treatedAsFar() {
        detector.onProximityChanged(THRESHOLD + 1f);

        assertFalse(detector.isHandNear());
    }

    // ── Reset ────────────────────────────────────────────────────────────────

    @Test
    public void reset_clearsClapCount() {
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);

        detector.reset();

        assertEquals(0, detector.getClapCount());
    }

    @Test
    public void reset_clearsHandNearState() {
        detector.onProximityChanged(NEAR); // hand is near, mid-gesture
        detector.reset();

        assertFalse(detector.isHandNear());
    }

    @Test
    public void afterReset_clapCountingResumesFromZero() {
        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);
        detector.reset();

        detector.onProximityChanged(NEAR);
        detector.onProximityChanged(FAR);

        assertEquals(1, detector.getClapCount());
    }
}
