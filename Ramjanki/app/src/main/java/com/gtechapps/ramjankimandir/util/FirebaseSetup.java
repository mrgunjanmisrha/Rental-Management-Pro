package com.gtechapps.ramjankimandir.util;

import android.content.Context;

import com.gtechapps.ramjankimandir.BuildConfig;

public final class FirebaseSetup {

    private FirebaseSetup() {
    }

    public static boolean isConfigured() {
        return BuildConfig.HAS_GOOGLE_SERVICES;
    }

    public static String defaultWebClientId(Context context) {
        int id = context.getResources().getIdentifier("default_web_client_id", "string", context.getPackageName());
        if (id == 0) {
            return "";
        }
        return context.getString(id);
    }
}
