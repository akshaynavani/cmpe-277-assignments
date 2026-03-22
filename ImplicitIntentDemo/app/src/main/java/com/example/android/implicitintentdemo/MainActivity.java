package com.example.android.implicitintentdemo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ImplicitIntentDemo";
    private static final String KEY_URL_LAUNCHED = "url_launched";
    private static final String KEY_PHONE_LAUNCHED = "phone_launched";

    private boolean urlLaunched = false;
    private boolean phoneLaunched = false;

    private TextInputLayout urlInputLayout;
    private TextInputEditText urlText;
    private TextInputLayout phoneInputLayout;
    private TextInputEditText ringText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            urlLaunched = savedInstanceState.getBoolean(KEY_URL_LAUNCHED, false);
            phoneLaunched = savedInstanceState.getBoolean(KEY_PHONE_LAUNCHED, false);
        }

        urlInputLayout = findViewById(R.id.urlInputLayout);
        urlText = findViewById(R.id.urlText);
        phoneInputLayout = findViewById(R.id.phoneInputLayout);
        ringText = findViewById(R.id.ringText);

        // Web link — lambda replaces anonymous inner class
        findViewById(R.id.urlButton).setOnClickListener(v -> {
            String url = Objects.requireNonNull(urlText.getText()).toString().trim();

            if (url.isEmpty()) {
                urlInputLayout.setError("Please enter a URL");
                return;
            }

            urlInputLayout.setError(null);

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            Log.v(TAG, "Launching URL: " + url);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                urlLaunched = true;
            } catch (ActivityNotFoundException e) {
                urlInputLayout.setError("No browser app found on this device");
            }
        });

        // Phone dialer — lambda replaces anonymous inner class
        findViewById(R.id.ringButton).setOnClickListener(v -> {
            String phoneNumber = Objects.requireNonNull(ringText.getText()).toString().trim();
            boolean isValidUS = (!phoneNumber.startsWith("+1") && phoneNumber.length() == 10)
                    || (phoneNumber.startsWith("+1") && phoneNumber.length() == 12);

            if (!isValidUS) {
                phoneInputLayout.setError("Enter a valid 10-digit US phone number");
                return;
            }

            phoneInputLayout.setError(null);

            if (!phoneNumber.startsWith("+1")) {
                phoneNumber = "+1" + phoneNumber;
            }

            Log.v(TAG, "Dialing: " + phoneNumber);
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
                phoneLaunched = true;
            } catch (ActivityNotFoundException e) {
                phoneInputLayout.setError("No phone app found on this device");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (urlLaunched) {
            urlText.setText("");
            urlInputLayout.setError(null);
            urlLaunched = false;
        }
        if (phoneLaunched) {
            ringText.setText("");
            phoneInputLayout.setError(null);
            phoneLaunched = false;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_URL_LAUNCHED, urlLaunched);
        outState.putBoolean(KEY_PHONE_LAUNCHED, phoneLaunched);
    }

    public void finishApp(View v) {
        finish();
    }
}
