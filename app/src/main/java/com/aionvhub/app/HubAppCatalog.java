package com.aionvhub.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Descoberta dinâmica dos aplicativos iniciáveis instalados no tablet. */
public final class HubAppCatalog {
    public static final String MEDIA_ID_PREFIX = "app:";

    private HubAppCatalog() {}

    public static List<LaunchableApp> listLaunchable(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved = pm.queryIntentActivities(query, PackageManager.MATCH_ALL);
        Map<String, LaunchableApp> unique = new LinkedHashMap<>();

        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
            String packageName = info.activityInfo.packageName;
            if (context.getPackageName().equals(packageName)) continue;

            CharSequence loaded = info.loadLabel(pm);
            String label = loaded == null || loaded.toString().trim().isEmpty()
                    ? packageName
                    : loaded.toString().trim();

            if (!unique.containsKey(packageName)) {
                unique.put(packageName, new LaunchableApp(packageName, label));
            }
        }

        List<LaunchableApp> out = new ArrayList<>(unique.values());
        Collections.sort(out, Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT)));
        return out;
    }

    public static LaunchableApp find(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) return null;
        for (LaunchableApp app : listLaunchable(context)) {
            if (packageName.equals(app.packageName)) return app;
        }
        return null;
    }

    public static Drawable icon(Context context, LaunchableApp app) {
        try {
            return context.getPackageManager().getApplicationIcon(app.packageName);
        } catch (Exception ignored) {
            return context.getApplicationInfo().loadIcon(context.getPackageManager());
        }
    }

    public static boolean launch(Context context, String packageName) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) return false;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        return true;
    }

    public static String mediaId(String packageName) {
        return MEDIA_ID_PREFIX + packageName;
    }

    public static boolean isMediaId(String mediaId) {
        return mediaId != null && mediaId.startsWith(MEDIA_ID_PREFIX) && mediaId.length() > MEDIA_ID_PREFIX.length();
    }

    public static String packageFromMediaId(String mediaId) {
        return isMediaId(mediaId) ? mediaId.substring(MEDIA_ID_PREFIX.length()) : null;
    }

    public static final class LaunchableApp {
        public final String packageName;
        public final String label;

        public LaunchableApp(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
    }
}
