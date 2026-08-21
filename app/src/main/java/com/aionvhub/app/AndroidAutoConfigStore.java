package com.aionvhub.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Preferências locais que definem exatamente o que o AION V Hub expõe no Android Auto. */
public final class AndroidAutoConfigStore {
    private static final String PREFS = "android_auto_config";
    private static final String KEY_ONLY_SELECTED_APPS = "only_selected_apps";
    private static final String KEY_SELECTED_APPS = "selected_apps";
    private static final String FEATURE_PREFIX = "feature_";

    private AndroidAutoConfigStore() {}

    public static boolean isFeatureEnabled(Context context, String mediaId) {
        return prefs(context).getBoolean(FEATURE_PREFIX + mediaId, true);
    }

    public static void setFeatureEnabled(Context context, String mediaId, boolean enabled) {
        prefs(context).edit().putBoolean(FEATURE_PREFIX + mediaId, enabled).apply();
    }

    public static boolean onlySelectedApps(Context context) {
        return prefs(context).getBoolean(KEY_ONLY_SELECTED_APPS, false);
    }

    public static void setOnlySelectedApps(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ONLY_SELECTED_APPS, enabled).apply();
    }

    public static Set<String> selectedPackages(Context context) {
        Set<String> stored = prefs(context).getStringSet(KEY_SELECTED_APPS, Collections.emptySet());
        return new HashSet<>(stored == null ? Collections.emptySet() : stored);
    }

    public static boolean isAppSelected(Context context, String packageName) {
        return selectedPackages(context).contains(packageName);
    }

    public static void setAppSelected(Context context, String packageName, boolean selected) {
        Set<String> apps = selectedPackages(context);
        if (selected) apps.add(packageName);
        else apps.remove(packageName);
        prefs(context).edit().putStringSet(KEY_SELECTED_APPS, apps).apply();
    }

    public static void clearSelectedApps(Context context) {
        prefs(context).edit().putStringSet(KEY_SELECTED_APPS, new HashSet<>()).apply();
    }

    public static List<HubAppCatalog.LaunchableApp> visibleApps(Context context) {
        List<HubAppCatalog.LaunchableApp> all = HubAppCatalog.listLaunchable(context);
        if (!onlySelectedApps(context)) return all;

        Set<String> selected = selectedPackages(context);
        List<HubAppCatalog.LaunchableApp> out = new ArrayList<>();
        for (HubAppCatalog.LaunchableApp app : all) {
            if (selected.contains(app.packageName)) out.add(app);
        }
        return out;
    }

    public static void restoreDefaults(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
