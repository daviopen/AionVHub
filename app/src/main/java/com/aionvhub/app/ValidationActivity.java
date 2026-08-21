package com.aionvhub.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Validação local da v0.8.1, sem depender de Shizuku. */
public class ValidationActivity extends Activity {
    private final int bg = Color.rgb(9, 14, 18);
    private final int surface = Color.rgb(22, 31, 37);
    private final int text = Color.rgb(244, 247, 248);
    private final int muted = Color.rgb(159, 176, 184);
    private final int accent = Color.rgb(75, 211, 205);
    private LinearLayout results;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(38));
        scroll.addView(root);
        setContentView(scroll);

        TextView eyebrow = label("AION V HUB", 13, accent);
        eyebrow.setLetterSpacing(.12f);
        root.addView(eyebrow);
        root.addView(label("Validação v" + BuildConfig.VERSION_NAME, 30, text));
        root.addView(label("Testes rápidos do Hub antes de usar no Android Auto.", 15, muted));

        LinearLayout preview = new LinearLayout(this);
        preview.setGravity(Gravity.CENTER);
        preview.setPadding(0, dp(16), 0, dp(16));
        addPreview(preview, HubMediaCatalog.APPS_ID);
        addPreview(preview, HubMediaCatalog.IPTV_ID);
        addPreview(preview, HubMediaCatalog.RADIOS_ID);
        addPreview(preview, HubMediaCatalog.DIAGNOSTICS_ID);
        root.addView(preview);

        Button run = new Button(this);
        run.setText("Executar todos os testes");
        run.setAllCaps(false);
        run.setTextSize(16);
        run.setOnClickListener(v -> runTests());
        root.addView(run, new LinearLayout.LayoutParams(-1, -2));

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(dp(16), dp(14), dp(16), dp(14));
        results.setBackground(cardBackground());
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(0, dp(14), 0, 0);
        root.addView(results, rp);
        results.addView(label("Toque em “Executar todos os testes”.", 14, muted));
    }

    private void runTests() {
        results.removeAllViews();
        int passed = 0;
        int failed = 0;

        try {
            List<HubAppCatalog.LaunchableApp> apps = HubAppCatalog.listLaunchable(this);
            boolean ok = !apps.isEmpty();
            addResult(ok, "Descoberta de apps", apps.size() + " apps iniciáveis visíveis");
            if (ok) passed++; else failed++;

            int checked = 0;
            int icons = 0;
            for (HubAppCatalog.LaunchableApp app : apps) {
                if (checked++ >= 12) break;
                if (HubAppCatalog.icon(this, app) != null) icons++;
            }
            ok = checked == 0 || icons == checked;
            addResult(ok, "Ícones dos apps", icons + "/" + checked + " carregados");
            if (ok) passed++; else failed++;
        } catch (Throwable t) {
            addResult(false, "Descoberta de apps", t.getClass().getSimpleName());
            failed++;
        }

        try {
            boolean ok = HubArtwork.brand(128).getWidth() == 128 &&
                    HubArtwork.forEntry(this, HubMediaCatalog.APPS_ID, 64).getHeight() == 64 &&
                    HubArtwork.forEntry(this, HubMediaCatalog.IPTV_ID, 64).getWidth() == 64;
            addResult(ok, "Artwork AION V", ok ? "brand + categorias renderizados" : "dimensão inesperada");
            if (ok) passed++; else failed++;
        } catch (Throwable t) {
            addResult(false, "Artwork AION V", t.getClass().getSimpleName());
            failed++;
        }

        try {
            PackageManager pm = getPackageManager();
            ServiceInfo service = pm.getServiceInfo(new ComponentName(this, HubMediaService.class), 0);
            boolean noBlockingPermission = service.permission == null || service.permission.trim().isEmpty();
            boolean ok = service.exported && noBlockingPermission;
            addResult(ok, "MediaService Android Auto", "exported=" + service.exported + " • permission=" + (service.permission == null ? "nenhuma" : service.permission));
            if (ok) passed++; else failed++;
        } catch (Throwable t) {
            addResult(false, "MediaService Android Auto", t.getClass().getSimpleName());
            failed++;
        }

        try {
            PackageInfo p = getPackageManager().getPackageInfo("com.google.android.projection.gearhead", 0);
            addResult(true, "Android Auto", p.versionName == null ? "instalado" : "v" + p.versionName);
            passed++;
        } catch (Throwable t) {
            addResult(false, "Android Auto", "não localizado");
            failed++;
        }

        HubStreamStore.Config stream = HubStreamStore.get(this);
        addInfo("Stream configurado", stream.configured() ? stream.title : "nenhum • não é falha");

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network active = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities caps = cm == null || active == null ? null : cm.getNetworkCapabilities(active);
        boolean internet = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean validated = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        addInfo("Rede", "INTERNET=" + (internet ? "SIM" : "NÃO") + " • validada=" + (validated ? "SIM" : "NÃO"));
        addInfo("Shizuku", "opcional • não participa destes testes");

        TextView summary = label((failed == 0 ? "✓ " : "⚠ ") + passed + " testes OK • " + failed + " falhas", 18, failed == 0 ? accent : text);
        summary.setPadding(0, dp(14), 0, dp(4));
        results.addView(summary, 0);
        HubDiagnostics.event(this, "VALIDATION passed=" + passed + " failed=" + failed);
    }

    private void addPreview(LinearLayout row, String mediaId) {
        ImageView image = new ImageView(this);
        image.setImageBitmap(HubArtwork.forEntry(this, mediaId, dp(48)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(48), dp(48));
        p.setMargins(dp(6), 0, dp(6), 0);
        row.addView(image, p);
    }

    private void addResult(boolean ok, String title, String detail) {
        results.addView(label((ok ? "✓ " : "✕ ") + title + " — " + detail, 14, ok ? text : Color.rgb(255, 185, 160)));
    }

    private void addInfo(String title, String detail) {
        results.addView(label("• " + title + " — " + detail, 14, muted));
    }

    private GradientDrawable cardBackground() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(surface);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1), Color.rgb(38, 52, 59));
        return d;
    }

    private TextView label(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
