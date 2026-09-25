package com.example.learnkeep;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private static final String PREF_NAME ="learnkeep_session";
    private static final String KEY_IS_LOGIN="isLogin";
    private static final String KEY_NAME="name";
    private static final String KEY_EMAIL="email";
    private static final String KEY_TOKEN="token";
    private static final String KEY_PIC_BASE64="profilePicBase64";
    private static final String KEY_PIC_PATH="profilePicPath";
    private static final String KEY_ONBOARDING="hasSeenOnboarding";

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ── Login / Logout ────────────────────────────────────────────────
    public void saveLogin(String name, String email, String token) {
        editor.putBoolean(KEY_IS_LOGIN, true);
        editor.putString(KEY_NAME,  name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGIN, false);
    }

    public String getName()  { return prefs.getString(KEY_NAME,  ""); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public String getToken() { return prefs.getString(KEY_TOKEN, ""); }

    public void logout() {
        // Keep onboarding flag and profile pic; clear auth data only
        String picBase64 = getProfilePicBase64();
        String picPath   = getProfilePicPath();
        editor.clear();
        if (picBase64 != null) editor.putString(KEY_PIC_BASE64, picBase64);
        if (picPath   != null) editor.putString(KEY_PIC_PATH,   picPath);
        editor.putBoolean(KEY_ONBOARDING, true); // don't show onboarding again
        editor.apply();
    }

    // ── Profile Picture (offline local storage) ───────────────────────
    public void saveProfilePicBase64(String base64) {
        editor.putString(KEY_PIC_BASE64, base64);
        editor.apply();
    }

    public String getProfilePicBase64() {
        return prefs.getString(KEY_PIC_BASE64, null);
    }

    public String getProfilePicPath() {
        return prefs.getString(KEY_PIC_PATH, null);
    }
}
