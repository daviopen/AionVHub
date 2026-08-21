package com.aionvhub.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Configuração única usada pela tela do tablet, simulador e MediaBrowser do Android Auto. */
public final class HubCarConfig {
    private static final String PREFS = "aion_car_config";
    private static final String KEY_ENABLED_MENUS = "enabled_menus";
    private static final String KEY_SELECTED_APPS = "selected_apps";
    private static final String KEY_APPS_EXPLICIT = "apps_explicit";

    private HubCarConfig() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Set<String> enabledMenus(Context context) {
        Set<String> stored = prefs(context).getStringSet(KEY_ENABLED_MENUS, null);
        if (stored == null) return new LinkedHashSet<>(HubCarMenuPolicy.all());
        return new LinkedHashSet<>(stored);
    }

    public static boolean isMenuEnabled(Context context, String menuId) {
        return enabledMenus(context).contains(menuId);
    }

    public static void setMenuEnabled(Context context, String menuId, boolean enabled) {
        Set<String> menus = enabledMenus(context);
        if (enabled) menus.add(menuId); else menus.remove(menuId);
        prefs(context).edit().putStringSet(KEY_ENABLED_MENUS, new HashSet<>(menus)).apply();
    }

    public static List<String> enabledMenuIds(Context context) {
        return HubCarMenuPolicy.filter(enabledMenus(context));
    }

    public static boolean hasExplicitAppSelection(Context context) {
        return prefs(context).getBoolean(KEY_APPS_EXPLICIT, false);
    }

    public static Set<String> selectedPackageNames(Context context) {
        if (!hasExplicitAppSelection(context)) {
            LinkedHashSet<String> all = new LinkedHashSet<>();
            for (HubAppCatalog.LaunchableApp app : HubAppCatalog.listLaunchable(context)) all.add(app.packageName);
            return all;
        }
        Set<String> stored = prefs(context).getStringSet(KEY_SELECTED_APPS, null);
        return stored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stored);
    }

    public static List<HubAppCatalog.LaunchableApp> selectedApps(Context context) {
        Set<String> selected = selectedPackageNames(context);
        List<HubAppCatalog.LaunchableApp> out = new ArrayList<>();
        for (HubAppCatalog.LaunchableApp app : HubAppCatalog.listLaunchable(context)) {
            if (selected.contains(app.packageName)) out.add(app);
        }
        return out;
    }

    public static boolean isAppSelected(Context context, String packageName) {
        return selectedPackageNames(context).contains(packageName);
    }

    /** Na primeira alteração parte do estado atual (todos visíveis) para evitar migração destrutiva. */
    public static void setAppSelected(Context context, String packageName, boolean selected) {
        Set<String> packages = selectedPackageNames(context);
        if (selected) packages.add(packageName); else packages.remove(packageName);
        prefs(context).edit()
                .putBoolean(KEY_APPS_EXPLICIT, true)
                .putStringSet(KEY_SELECTED_APPS, new HashSet<>(packages))
                .apply();
    }

    public static void selectAllApps(Context context) {
        Set<String> all = new LinkedHashSet<>();
        for (HubAppCatalog.LaunchableApp app : HubAppCatalog.listLaunchable(context)) all.add(app.packageName);
        prefs(context).edit()
                .putBoolean(KEY_APPS_EXPLICIT, true)
                .putStringSet(KEY_SELECTED_APPS, new HashSet<>(all))
                .apply();
    }

    public static void clearApps(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_APPS_EXPLICIT, true)
                .putStringSet(KEY_SELECTED_APPS, new HashSet<>())
                .apply();
    }

    public static void reset(Context context) {
        prefs(context).edit().clear().apply();
    }
}
