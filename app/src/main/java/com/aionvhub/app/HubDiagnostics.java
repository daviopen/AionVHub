package com.aionvhub.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class HubDiagnostics {
    private static final String PREFS = "aion_hub_diagnostics";
    private static final String KEY_LOG = "event_log";
    private static final String KEY_LAST = "last_event";
    private static final String KEY_LAST_TS = "last_event_ts";
    private static final int MAX_LINES = 12;

    private HubDiagnostics() {}

    public static void event(Context context, String message) {
        String ts = new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(new Date());
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String old = p.getString(KEY_LOG, "");
        String line = ts + "  " + message;
        String merged = line + (old == null || old.isEmpty() ? "" : "\n" + old);
        String[] lines = merged.split("\n");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length && i < MAX_LINES; i++) {
            if (i > 0) out.append('\n');
            out.append(lines[i]);
        }
        p.edit()
                .putString(KEY_LOG, out.toString())
                .putString(KEY_LAST, message)
                .putLong(KEY_LAST_TS, System.currentTimeMillis())
                .apply();
    }

    public static String getLog(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LOG, "Nenhum evento automotivo registrado ainda.");
    }

    public static String getLastEvent(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST, "nenhum");
    }

    public static long getLastEventTimestamp(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_TS, 0L);
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
