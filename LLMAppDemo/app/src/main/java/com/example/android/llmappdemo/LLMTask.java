package com.example.android.llmappdemo;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * AsyncTask that sends a prompt to either the OpenAI or Gemini API on a
 * background thread and delivers the response to the main thread via Callback.
 *
 * AsyncTask type parameters:
 *   String  — input:    the user prompt passed to execute()
 *   Void    — progress: no progress updates published
 *   Result  — output:   response text or error returned by doInBackground
 */
@SuppressWarnings("deprecation")
public class LLMTask extends AsyncTask<String, Void, LLMTask.Result> {

    private static final String TAG = "LLMTask";

    // Groq is OpenAI-compatible — same request/response format, different URL and model
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";

    // Gemini uses the API key as a query param, not a header
    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=%s";

    private static final int MAX_TOKENS = 512;
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    // ── Public types ──────────────────────────────────────────────────────────

    public enum Provider { GROQ, GEMINI }

    /** Wraps either a successful response or an error message. */
    public static class Result {
        public final String text;
        public final boolean isError;

        Result(String text, boolean isError) {
            this.text = text;
            this.isError = isError;
        }
    }

    /** Delivered on the main thread once the task completes or fails. */
    public interface Callback {
        void onComplete(Result result);
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final Provider provider;
    private final String apiKey;
    private final Callback callback;

    public LLMTask(Provider provider, String apiKey, Callback callback) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.callback = callback;
    }

    // ── AsyncTask methods ─────────────────────────────────────────────────────

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "Task starting — provider: " + provider
                + ", thread: " + Thread.currentThread().getName());
    }

    /** Runs on a background worker thread — never touches the UI. */
    @Override
    protected Result doInBackground(String... params) {
        Log.d(TAG, "doInBackground on thread: " + Thread.currentThread().getName());
        String prompt = params[0];
        try {
            String json = provider == Provider.GROQ
                    ? callGroq(prompt)
                    : callGemini(prompt);
            String content = provider == Provider.GROQ
                    ? parseGroqResponse(json)
                    : parseGeminiResponse(json);
            return new Result(content, false);
        } catch (Exception e) {
            Log.e(TAG, "API call failed", e);
            return new Result(e.getMessage(), true);
        }
    }

    /** Runs on the main thread — safe to update the UI here. */
    @Override
    protected void onPostExecute(Result result) {
        Log.d(TAG, "onPostExecute on thread: " + Thread.currentThread().getName());
        callback.onComplete(result);
    }

    // ── Groq (OpenAI-compatible) ───────────────────────────────────────────────

    private String callGroq(String prompt) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(GROQ_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        JSONObject body = new JSONObject();
        body.put("model", GROQ_MODEL);
        body.put("messages", new JSONArray().put(message));
        body.put("max_tokens", MAX_TOKENS);

        return post(conn, body.toString());
    }

    /**
     * Extracts the assistant reply from a Groq (OpenAI-compatible) response.
     * Static so it can be unit tested without Android.
     */
    static String parseGroqResponse(String json) throws JSONException {
        return new JSONObject(json)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    // ── Gemini ────────────────────────────────────────────────────────────────

    private String callGemini(String prompt) throws Exception {
        String urlStr = String.format(GEMINI_URL_TEMPLATE, apiKey);
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);

        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject body = new JSONObject().put("contents", new JSONArray().put(content));

        return post(conn, body.toString());
    }

    /**
     * Extracts the reply text from a Gemini generateContent response.
     * Static so it can be unit tested without Android.
     */
    static String parseGeminiResponse(String json) throws JSONException {
        return new JSONObject(json)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }

    // ── Shared HTTP helper ────────────────────────────────────────────────────

    private String post(HttpURLConnection conn, String jsonBody) throws Exception {
        byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int status = conn.getResponseCode();
        boolean ok = (status == HttpURLConnection.HTTP_OK);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                ok ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        } finally {
            conn.disconnect();
        }

        if (!ok) throw new Exception("HTTP " + status + ": " + sb);
        return sb.toString();
    }
}
