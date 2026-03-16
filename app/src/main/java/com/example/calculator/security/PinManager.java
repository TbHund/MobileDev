package com.example.calculator.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class PinManager {
    private static final String PREFS_NAME = "secure_prefs";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_SALT = "pin_salt";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCK_TIME = "lock_time";

    private static final String KEY_IS_AUTHENTICATED = "is_authenticated";

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 30000;

    private SharedPreferences sharedPreferences;
    private Context context;

    public PinManager(Context context) {
        this.context = context;
        initEncryptedPrefs();
    }

    private void initEncryptedPrefs() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            sharedPreferences = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public void setAuthenticated(boolean authenticated) {
        sharedPreferences.edit().putBoolean(KEY_IS_AUTHENTICATED, authenticated).apply();
    }

    public boolean isAuthenticated() {
        return sharedPreferences.getBoolean(KEY_IS_AUTHENTICATED, false);
    }

    public void clearAuthentication() {
        sharedPreferences.edit().putBoolean(KEY_IS_AUTHENTICATED, false).apply();
    }

    public boolean isPinSet() {
        return sharedPreferences.contains(KEY_PIN_HASH);
    }

    public boolean savePin(String pin) {
        try {
            byte[] salt = generateSalt();
            String hash = hashPin(pin, salt);

            sharedPreferences.edit()
                    .putString(KEY_PIN_HASH, hash)
                    .putString(KEY_SALT, Base64.encodeToString(salt, Base64.DEFAULT))
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCK_TIME, 0)
                    .apply();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkPin(String pin) {
        if (isLocked()) {
            return false;
        }

        String storedHash = sharedPreferences.getString(KEY_PIN_HASH, null);
        String saltString = sharedPreferences.getString(KEY_SALT, null);

        if (storedHash == null || saltString == null) {
            return false;
        }

        try {
            byte[] salt = Base64.decode(saltString, Base64.DEFAULT);
            String hash = hashPin(pin, salt);

            if (hash.equals(storedHash)) {
                sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply();
                return true;
            } else {
                int attempts = sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
                sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply();

                if (attempts >= MAX_ATTEMPTS) {
                    sharedPreferences.edit()
                            .putLong(KEY_LOCK_TIME, System.currentTimeMillis())
                            .apply();
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isLocked() {
        long lockTime = sharedPreferences.getLong(KEY_LOCK_TIME, 0);
        if (lockTime == 0) return false;

        if (System.currentTimeMillis() - lockTime > LOCK_DURATION_MS) {
            sharedPreferences.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCK_TIME, 0)
                    .apply();
            return false;
        }
        return true;
    }

    public long getLockTimeRemaining() {
        long lockTime = sharedPreferences.getLong(KEY_LOCK_TIME, 0);
        if (lockTime == 0) return 0;

        long remaining = LOCK_DURATION_MS - (System.currentTimeMillis() - lockTime);
        return remaining > 0 ? remaining / 1000 : 0;
    }

    public void resetPin() {
        sharedPreferences.edit()
                .remove(KEY_PIN_HASH)
                .remove(KEY_SALT)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCK_TIME, 0)
                .apply();
    }

    private String hashPin(String pin, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        byte[] hash = md.digest(pin.getBytes());
        return Base64.encodeToString(hash, Base64.DEFAULT);
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public int getRemainingAttempts() {
        int attempts = sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0);
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
}