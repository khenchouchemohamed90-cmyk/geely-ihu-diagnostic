package com.aztech.jarvisfr;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stockage chiffré propre au prototype individuel.
 * Ne JAMAIS utiliser une clé API commune dans une APK commerciale.
 */
public final class PrivateAiKeyStore {
    private static final String ALIAS = "aztech.jarvisfr.personal.api.2026";
    private static final String PREFS = "private_ai_credentials";
    private static final String DATA = "encrypted_api_key";
    private static final String IV = "encrypted_api_iv";
    private final Context context;

    public PrivateAiKeyStore(Context context) { this.context = context.getApplicationContext(); }

    public synchronized void save(String token) throws Exception {
        if (token == null || token.trim().length() < 12 || token.length() > 512
                || token.contains("\n") || token.contains("\r"))
            throw new IllegalArgumentException("Clé API invalide");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        byte[] cipherText = cipher.doFinal(token.trim().getBytes(StandardCharsets.UTF_8));
        boolean saved = prefs().edit()
                .putString(DATA, Base64.encodeToString(cipherText, Base64.NO_WRAP))
                .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .commit();
        if (!saved) throw new IllegalStateException("Enregistrement sécurisé impossible");
    }

    public synchronized String load() {
        String encrypted = prefs().getString(DATA, null);
        String vector = prefs().getString(IV, null);
        if (encrypted == null || vector == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(),
                    new GCMParameterSpec(128, Base64.decode(vector, Base64.NO_WRAP)));
            String token = new String(cipher.doFinal(
                    Base64.decode(encrypted, Base64.NO_WRAP)), StandardCharsets.UTF_8);
            return token.length() >= 12 ? token : null;
        } catch (Exception error) {
            // Clé Keystore invalidée (ex. réinitialisation OEM) : aucun plaintext.
            clear();
            return null;
        }
    }

    public synchronized void clear() {
        prefs().edit().remove(DATA).remove(IV).commit();
    }

    public boolean configured() {
        return load() != null;
    }

    private SharedPreferences prefs() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SecretKey getKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        KeyStore.Entry existing = store.getEntry(ALIAS, null);
        if (existing instanceof KeyStore.SecretKeyEntry)
            return ((KeyStore.SecretKeyEntry) existing).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
