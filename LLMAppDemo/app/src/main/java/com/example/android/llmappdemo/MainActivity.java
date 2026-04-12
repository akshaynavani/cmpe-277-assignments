package com.example.android.llmappdemo;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_RESPONSE = "response";
    private static final String KEY_PROVIDER = "provider";

    private RadioGroup providerGroup;
    private TextInputLayout promptInputLayout;
    private TextInputEditText promptText;
    private MaterialButton sendButton;
    private MaterialButton cancelButton;
    private LinearProgressIndicator progressBar;
    private TextView responseText;

    private LLMTask currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        providerGroup = findViewById(R.id.providerGroup);
        promptInputLayout = findViewById(R.id.promptInputLayout);
        promptText = findViewById(R.id.promptText);
        sendButton = findViewById(R.id.sendButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressBar = findViewById(R.id.progressBar);
        responseText = findViewById(R.id.responseText);

        if (savedInstanceState != null) {
            responseText.setText(savedInstanceState.getString(KEY_RESPONSE, ""));
            providerGroup.check(savedInstanceState.getInt(KEY_PROVIDER, R.id.radioGroq));
        }

        sendButton.setOnClickListener(v -> {
            String prompt = Objects.requireNonNull(promptText.getText()).toString().trim();
            if (prompt.isEmpty()) {
                promptInputLayout.setError("Enter a prompt");
                return;
            }
            promptInputLayout.setError(null);
            hideKeyboard();
            sendPrompt(prompt);
        });

        cancelButton.setOnClickListener(v -> cancelTask());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_RESPONSE, responseText.getText().toString());
        outState.putInt(KEY_PROVIDER, providerGroup.getCheckedRadioButtonId());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentTask != null) currentTask.cancel(true);
    }

    private void sendPrompt(String prompt) {
        LLMTask.Provider provider = providerGroup.getCheckedRadioButtonId() == R.id.radioGroq
                ? LLMTask.Provider.GROQ
                : LLMTask.Provider.GEMINI;

        String apiKey = provider == LLMTask.Provider.GROQ
                ? BuildConfig.GROQ_API_KEY
                : BuildConfig.GEMINI_API_KEY;

        setLoading(true);
        responseText.setText("Waiting for response...");

        currentTask = new LLMTask(provider, apiKey, result -> {
            setLoading(false);
            responseText.setText(result.isError ? "Error: " + result.text : result.text);
            if (!result.isError) promptText.setText("");
        });
        currentTask.execute(prompt);
    }

    private void cancelTask() {
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        setLoading(false);
        responseText.setText("");
        promptText.setText("");
        promptInputLayout.setError(null);
    }

    private void setLoading(boolean loading) {
        sendButton.setEnabled(!loading);
        cancelButton.setEnabled(loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View focus = getCurrentFocus();
        if (imm != null && focus != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }
}
