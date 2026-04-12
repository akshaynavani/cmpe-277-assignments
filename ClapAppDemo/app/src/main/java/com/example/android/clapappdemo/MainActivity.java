package com.example.android.clapappdemo;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "ClapAppDemo";
    private static final String KEY_CLAP_COUNT = "clap_count";
    private static final float PROXIMITY_NEAR_THRESHOLD = 5.0f; // cm

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private MediaPlayer mediaPlayer;

    private MaterialCardView statusCard;
    private TextView statusText;
    private TextView clapCountText;
    private TextView sensorInfoText;

    private ClapDetector clapDetector;
    private boolean sensorAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusCard = findViewById(R.id.statusCard);
        statusText = findViewById(R.id.statusText);
        clapCountText = findViewById(R.id.clapCountText);
        sensorInfoText = findViewById(R.id.sensorInfoText);

        clapDetector = new ClapDetector(PROXIMITY_NEAR_THRESHOLD, new ClapDetector.Listener() {
            @Override
            public void onHandNear() {
                setStatus("Hand Detected", R.color.colorStatusHandNear, 2);
            }

            @Override
            public void onClap(int totalClaps) {
                clapCountText.setText(String.valueOf(totalClaps));
                setStatus("Clap!", R.color.colorStatusClap, 2);
                playClap();
            }

            @Override
            public void onReady() {
                setStatus("Ready", R.color.colorStatusReady, 0);
            }
        });

        if (savedInstanceState != null) {
            int saved = savedInstanceState.getInt(KEY_CLAP_COUNT, 0);
            for (int i = 0; i < saved; i++) {
                clapDetector.onProximityChanged(0f);
                clapDetector.onProximityChanged(Float.MAX_VALUE);
            }
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (proximitySensor != null) {
            sensorAvailable = true;
            sensorInfoText.setText("Max range: " + proximitySensor.getMaximumRange()
                    + " cm  |  Near threshold: \u2264 " + PROXIMITY_NEAR_THRESHOLD + " cm");
        } else {
            sensorInfoText.setText("Proximity sensor not available on this device");
            setStatus("No Sensor", R.color.colorStatusReady, 0);
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.clap);

        findViewById(R.id.resetButton).setOnClickListener(v -> {
            clapDetector.reset();
            clapCountText.setText("0");
            setStatus("Ready", R.color.colorStatusReady, 0);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorAvailable) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            setStatus("Ready", R.color.colorStatusReady, 0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CLAP_COUNT, clapDetector.getClapCount());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float distance = event.values[0];
        Log.v(TAG, "Proximity: " + distance + " cm");
        clapDetector.onProximityChanged(distance);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    /**
     * Updates the status card text, text color, and stroke in one call.
     * strokeWidthDp = 0 removes the stroke entirely.
     */
    private void setStatus(String text, @ColorRes int colorRes, int strokeWidthDp) {
        int color = ContextCompat.getColor(this, colorRes);
        statusText.setText(text);
        statusText.setTextColor(color);
        int strokePx = (int) (strokeWidthDp * getResources().getDisplayMetrics().density);
        statusCard.setStrokeWidth(strokePx);
        statusCard.setStrokeColor(color);
    }

    private void playClap() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.seekTo(0);
            }
            mediaPlayer.start();
        }
    }
}
