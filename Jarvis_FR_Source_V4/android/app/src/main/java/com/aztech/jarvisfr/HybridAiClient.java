package com.aztech.jarvisfr;

import android.util.Base64;
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
 * Conversation et parole Gemini sous quota gratuit. Aucun abonnement payant,
 * aucun endpoint ECARX et aucun achat intégrés dans cette application.
 * L'utilisateur doit fournir une clé personnelle Google AI Studio associée
 * à un projet NON FACTURÉ. Tout dépassement de quota est refusé (429).
 */
public final class HybridAiClient {
    private static final String TEXT_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent";
    private static final String VOICE_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-tts-preview:generateContent";
    private static final int TEXT_MAX = 150 * 1024;
    private static final int SPEECH_JSON_MAX = 6 * 1024 * 1024;
    private static final int AUDIO_MAX = 3 * 1024 * 1024;

    public static final class AiError extends Exception {
        public final int status;
        public AiError(int status, String message) { super(message); this.status = status; }
    }

    public String answer(String apiKey, String input) throws IOException, JSONException, AiError {
        checkKey(apiKey);
        if (input == null || input.trim().length() < 2 || input.length() > 500)
            throw new IllegalArgumentException("Question absente ou trop longue");

        JSONObject body = new JSONObject();
        JSONObject instruction = new JSONObject();
        instruction.put("parts", new JSONArray().put(new JSONObject().put("text",
                "Tu es Jarvis FR, assistant vocal automobile. Réponds exclusivement en français, " +
                "en une ou deux phrases claires. N'affirme jamais avoir consulté les capteurs " +
                "ni commandé les organes physiques de la voiture. " +
                "Tu ne peux actionner aucune vitre, climatisation, frein, direction ou équipement. " +
                "Ne propose aucune manipulation dangereuse en conduisant. " +
                "Ne demande pas de données privées sans nécessité.")));
        body.put("systemInstruction", instruction);
        body.put("contents", new JSONArray().put(
                new JSONObject().put("role", "user").put("parts",
                        new JSONArray().put(new JSONObject().put("text", input.trim())))));
        body.put("generationConfig", new JSONObject().put("maxOutputTokens", 400));
        JSONObject parsed = postJson(TEXT_ENDPOINT, apiKey, body, TEXT_MAX);
        JSONArray candidates = parsed.optJSONArray("candidates");
        if (candidates != null) {
            StringBuilder answer = new StringBuilder();
            for (int i = 0; i < candidates.length(); i++) {
                JSONObject candidate = candidates.optJSONObject(i);
                if (candidate == null) continue;
                JSONObject content = candidate.optJSONObject("content");
                JSONArray parts = content == null ? null : content.optJSONArray("parts");
                if (parts == null) continue;
                for (int j = 0; j < parts.length(); j++) {
                    JSONObject part = parts.optJSONObject(j);
                    if (part != null && part.has("text")) {
                        if (answer.length() > 0) answer.append(' ');
                        answer.append(part.optString("text", ""));
                    }
                }
                if (answer.length() > 0) break;
            }
            String result = answer.toString().trim();
            if (!result.isEmpty()) return result.substring(0, Math.min(result.length(), 1000));
        }
        throw new AiError(502, "Gemini n'a pas renvoyé de texte utilisable");
    }

    /** Génère un fichier WAV mono PCM16 24 kHz directement lisible sur Android 9. */
    public byte[] speech(String apiKey, String frenchText)
            throws IOException, JSONException, AiError {
        checkKey(apiKey);
        if (frenchText == null || frenchText.trim().isEmpty())
            throw new IllegalArgumentException("Texte vocal absent");
        JSONObject body = new JSONObject();
        body.put("contents", new JSONArray().put(
                new JSONObject().put("parts", new JSONArray().put(
                        new JSONObject().put("text", "Prononce en français, lentement et clairement : "
                                + frenchText.substring(0, Math.min(900, frenchText.length()))))));
        JSONObject speechConfig = new JSONObject().put("voiceConfig", new JSONObject()
                .put("prebuiltVoiceConfig", new JSONObject().put("voiceName", "Kore")));
        body.put("generationConfig", new JSONObject()
                .put("responseModalities", new JSONArray().put("AUDIO"))
                .put("speechConfig", speechConfig));
        JSONObject parsed = postJson(VOICE_ENDPOINT, apiKey, body, SPEECH_JSON_MAX);
        JSONArray candidates = parsed.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0)
            throw new AiError(502, "Gemini n'a pas fourni de voix");
        JSONObject first = candidates.optJSONObject(0);
        JSONObject content = first == null ? null : first.optJSONObject("content");
        JSONArray parts = content == null ? null : content.optJSONArray("parts");
        if (parts == null) throw new AiError(502, "Format audio Gemini inattendu");
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.optJSONObject(i);
            JSONObject audio = part == null ? null : part.optJSONObject("inlineData");
            if (audio == null) continue;
            String contentType = audio.optString("mimeType", "").toLowerCase(java.util.Locale.ROOT);
            String encoded = audio.optString("data", "");
            if (encoded.isEmpty() || encoded.length() > 5 * 1024 * 1024)
                throw new AiError(502, "Fichier audio absent ou trop volumineux");
            byte[] pcm;
            try { pcm = Base64.decode(encoded, Base64.DEFAULT); }
            catch (IllegalArgumentException e) { throw new AiError(502, "Audio encodé illisible"); }
            if (pcm.length < 100 || pcm.length > AUDIO_MAX)
                throw new AiError(502, "Durée audio hors limite");
            if (pcm.length > 12 && pcm[0] == 'R' && pcm[1] == 'I'
                    && pcm[2] == 'F' && pcm[3] == 'F') return pcm;
            if (contentType.contains("mp3") || contentType.contains("mpeg"))
                throw new AiError(502, "Encodage audio inattendu");
            // Génération WAV en PCM16 mono 24 kHz ; format attendu par Gemini TTS.
            return wrapWav(pcm, 24000);
        }
        throw new AiError(502, "Gemini n'a pas renvoyé de données audio");
    }

    private static byte[] wrapWav(byte[] pcm, int sampleRate) throws IOException {
        if ((pcm.length & 1) != 0) throw new IOException("PCM 16 bits tronqué");
        ByteArrayOutputStream out = new ByteArrayOutputStream(pcm.length + 44);
        out.write(new byte[]{'R','I','F','F'});
        le32(out, pcm.length + 36);
        out.write(new byte[]{'W','A','V','E','f','m','t',' '});
        le32(out, 16); le16(out, 1); le16(out, 1);
        le32(out, sampleRate); le32(out, sampleRate * 2);
        le16(out, 2); le16(out, 16);
        out.write(new byte[]{'d','a','t','a'});
        le32(out, pcm.length);
        out.write(pcm);
        return out.toByteArray();
    }
    private static void le16(ByteArrayOutputStream out, int value) {
        out.write(value & 255); out.write((value >>> 8) & 255);
    }
    private static void le32(ByteArrayOutputStream out, int value) {
        for (int i = 0; i < 4; i++) out.write((value >>> (8 * i)) & 255);
    }

    private static JSONObject postJson(String address, String key, JSONObject body, int maximum)
            throws IOException, JSONException, AiError {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod("POST");
        c.setConnectTimeout(8000);
        c.setReadTimeout(25000);
        c.setDoOutput(true);
        c.setRequestProperty("x-goog-api-key", key);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json");
        try {
            byte[] request = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(request.length);
            try (OutputStream stream = c.getOutputStream()) { stream.write(request); }
            int status = c.getResponseCode();
            if (status < 200 || status >= 300) {
                String reason = status == 400 ? "Requête non acceptée par Gemini"
                        : status == 401 || status == 403 ? "Clé Gemini refusée ou accès non autorisé"
                        : status == 404 ? "Modèle Gemini indisponible pour ce compte"
                        : status == 429 ? "Quota gratuit atteint : aucune requête payante tentée"
                        : status >= 500 ? "Service Gemini temporairement indisponible"
                        : "Erreur Gemini HTTP " + status;
                throw new AiError(status, reason);
            }
            String ct = c.getContentType();
            if (ct == null || !ct.toLowerCase(java.util.Locale.ROOT).contains("json"))
                throw new AiError(502, "Réponse Gemini non JSON");
            try (InputStream stream = c.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    if (output.size() + read > maximum)
                        throw new IOException("Réponse Gemini trop volumineuse");
                    output.write(buffer, 0, read);
                }
                return new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            }
        } finally { c.disconnect(); }
    }

    private static void checkKey(String token) {
        if (token == null || token.length() < 12 || token.length() > 512
                || token.contains("\n") || token.contains("\r"))
            throw new IllegalArgumentException("Clé Gemini personnelle absente ou invalide");
    }
}
