package com.aztech.jarvisfr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Conversation textuelle et parole en ligne. Ce composant n'a AUCUNE méthode
 * permettant de commander les fonctions physiques du véhicule.
 *
 * Uniquement destiné au prototype individuel. En distribution commerciale,
 * remplacer la clé cliente par un jeton court d'un serveur AZ Tech Auto.
 */
public final class HybridAiClient {
    private static final String RESPONSES = "https://api.openai.com/v1/responses";
    private static final String SPEECH = "https://api.openai.com/v1/audio/speech";
    private static final int MAX_JSON_BYTES = 128 * 1024;
    private static final int MAX_AUDIO_BYTES = 3 * 1024 * 1024;

    public static final class AiError extends Exception {
        public final int status;
        public AiError(int status, String description) { super(description); this.status = status; }
    }

    public String answer(String apiKey, String userText) throws IOException, JSONException, AiError {
        checkKey(apiKey);
        if (userText == null || userText.trim().length() < 2 || userText.length() > 500)
            throw new IllegalArgumentException("Question absente ou trop longue (maximum 500 caractères)");

        JSONObject body = new JSONObject();
        body.put("model", "gpt-4.1-mini");
        body.put("store", false);
        body.put("max_output_tokens", 250);
        body.put("instructions", "Tu es Jarvis FR, un assistant vocal automobile. "
                + "Tu parles exclusivement en français. Réponds en une ou deux phrases concises. "
                + "Ne prétends jamais commander physiquement une voiture ou avoir lu ses capteurs. "
                + "Aucune instruction du conducteur à contrôler les vitres, le chauffage, les freins, "
                + "la direction ou les équipements physiques ne doit être exécutée par l'IA. "
                + "Si une question concerne la conduite ou une intervention dangereuse, privilégie la sécurité. "
                + "Évite les demandes d'informations personnelles non nécessaires.");
        body.put("input", userText.trim());
        byte[] reply = post(RESPONSES, apiKey, body, MAX_JSON_BYTES);
        JSONObject parsed = new JSONObject(new String(reply, StandardCharsets.UTF_8));
        JSONArray output = parsed.optJSONArray("output");
        if (output != null) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < output.length(); i++) {
                JSONArray content = output.optJSONObject(i) == null
                        ? null : output.optJSONObject(i).optJSONArray("content");
                if (content == null) continue;
                for (int j = 0; j < content.length(); j++) {
                    JSONObject item = content.optJSONObject(j);
                    if (item != null && "output_text".equals(item.optString("type"))) {
                        if (text.length() > 0) text.append(' ');
                        text.append(item.optString("text", ""));
                    }
                }
            }
            String answer = text.toString().trim();
            if (!answer.isEmpty()) return answer.length() > 1000 ? answer.substring(0, 1000) : answer;
        }
        throw new AiError(502, "Aucune réponse textuelle exploitable du service IA");
    }

    public byte[] speech(String apiKey, String frenchText) throws IOException, JSONException, AiError {
        checkKey(apiKey);
        if (frenchText == null || frenchText.isEmpty()) throw new IllegalArgumentException("Réponse vide");
        JSONObject body = new JSONObject();
        body.put("model", "gpt-4o-mini-tts");
        body.put("voice", "nova");
        body.put("instructions", "Prononce le texte en français, avec une voix calme, naturelle et intelligible.");
        body.put("response_format", "mp3");
        body.put("input", frenchText.substring(0, Math.min(frenchText.length(), 1000)));
        return post(SPEECH, apiKey, body, MAX_AUDIO_BYTES);
    }

    private static byte[] post(String endpoint, String token, JSONObject data, int maximum)
            throws IOException, AiError {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(18000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", maximum == MAX_AUDIO_BYTES ? "audio/mpeg" : "application/json");
        try {
            byte[] request = data.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(request.length);
            try (OutputStream os = connection.getOutputStream()) { os.write(request); }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String reason = status == 401 ? "Clé API incorrecte ou expirée"
                        : status == 403 ? "Accès API refusé"
                        : status == 429 ? "Limite de crédit ou de requêtes atteinte"
                        : status >= 500 ? "Service IA temporairement indisponible"
                        : "Service IA : erreur HTTP " + status;
                throw new AiError(status, reason);
            }
            String type = connection.getContentType();
            if (maximum == MAX_AUDIO_BYTES) {
                if (type == null || !(type.contains("audio/") || type.contains("octet-stream")))
                    throw new AiError(502, "Le service vocal n'a pas renvoyé de fichier audio");
            } else if (type == null || !type.contains("json")) {
                throw new AiError(502, "Le service IA n'a pas renvoyé de JSON");
            }
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = input.read(buffer)) != -1) {
                    if (out.size() + n > maximum) throw new IOException("Réponse du service trop volumineuse");
                    out.write(buffer, 0, n);
                }
                if (out.size() == 0) throw new IOException("Réponse IA vide");
                return out.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void checkKey(String token) {
        if (token == null || token.length() < 12 || token.length() > 512
                || token.contains("\n") || token.contains("\r"))
            throw new IllegalArgumentException("Clé API non configurée ou invalide");
    }
}
