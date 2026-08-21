package com.aionvhub.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistência simples da primeira fonte de mídia do Hub. */
public final class HubStreamStore {
    private static final String PREFS = "aion_hub_stream";
    private static final String KEY_TITLE = "title";
    private static final String KEY_URL = "url";
    private static final String KEY_FAVORITE = "favorite";

    private HubStreamStore() {}

    public static void save(Context context, String title, String url, boolean favorite) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit()
                .putString(KEY_TITLE, clean(title).isEmpty() ? "Minha transmissão" : clean(title))
                .putString(KEY_URL, clean(url))
                .putBoolean(KEY_FAVORITE, favorite)
                .apply();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static Config get(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new Config(
                p.getString(KEY_TITLE, "Minha transmissão"),
                p.getString(KEY_URL, ""),
                p.getBoolean(KEY_FAVORITE, true)
        );
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    public static final class Config {
        public final String title;
        public final String url;
        public final boolean favorite;

        Config(String title, String url, boolean favorite) {
            this.title = title == null ? "Minha transmissão" : title;
            this.url = url == null ? "" : url;
            this.favorite = favorite;
        }

        public boolean configured() {
            return !url.trim().isEmpty();
        }
    }
}
