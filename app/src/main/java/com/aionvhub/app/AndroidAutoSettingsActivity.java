package com.aionvhub.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Configuração visual do que será exposto pelo Hub no Android Auto. */
public class AndroidAutoSettingsActivity extends Activity {
    private final int bg = Color.rgb(9, 14, 18);
    private final int surface = Color.rgb(23, 32, 38);
    private final int text = Color.rgb(244, 247, 248);
    private final int muted = Color.rgb(157, 175, 183);
    private final int accent = Color.rgb(75, 211, 205);
    private LinearLayout appsContainer;
    private TextView selectionStatus;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(40));
        scroll.addView(root);
        setContentView(scroll);

        TextView brand = text("AION V HUB", 13, accent);
        brand.setLetterSpacing(.14f);
        root.addView(brand);
        root.addView(text("Configurar Android Auto", 30, text));
        root.addView(text("Escolha quais menus e quais aplicativos do tablet ficarão visíveis na central do carro.", 15, muted));

        root.addView(section("MENUS NO CARRO"));
        LinearLayout features = panel();
        addFeature(features, HubMediaCatalog.APPS_ID, "Apps", "Aplicativos selecionados do tablet");
        addFeature(features, HubMediaCatalog.FAVORITES_ID, "Favoritos", "Streams e itens favoritos");
        addFeature(features, HubMediaCatalog.IPTV_ID, "IPTV / Streams", "Fontes de mídia configuradas");
        addFeature(features, HubMediaCatalog.RADIOS_ID, "Rádios", "Streams de áudio e rádios");
        addFeature(features, HubMediaCatalog.DIAGNOSTICS_ID, "Diagnóstico", "Estado técnico do Hub");
        root.addView(features);

        root.addView(section("APPS NO ANDROID AUTO"));
        LinearLayout appsMode = panel();
        CheckBox onlySelected = check("Mostrar somente os apps selecionados", AndroidAutoConfigStore.onlySelectedApps(this));
        onlySelected.setOnCheckedChangeListener((button, checked) -> {
            AndroidAutoConfigStore.setOnlySelectedApps(this, checked);
            refreshStatus();
        });
        appsMode.addView(onlySelected);
        selectionStatus = text("", 13, muted);
        selectionStatus.setPadding(0, dp(4), 0, dp(8));
        appsMode.addView(selectionStatus);
        root.addView(appsMode);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, dp(8));
        Button all = button("Selecionar todos");
        all.setOnClickListener(v -> selectAll(true));
        actions.addView(all, new LinearLayout.LayoutParams(0, -2, 1f));
        Button none = button("Limpar seleção");
        none.setOnClickListener(v -> selectAll(false));
        LinearLayout.LayoutParams noneParams = new LinearLayout.LayoutParams(0, -2, 1f);
        noneParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(none, noneParams);
        root.addView(actions);

        appsContainer = new LinearLayout(this);
        appsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(appsContainer);
        renderApps();
        refreshStatus();

        root.addView(section("APLICAR"));
        LinearLayout note = panel();
        note.addView(text("As alterações ficam salvas no tablet. Para a central reler o catálogo, volte ao Android Auto e reabra o AION V Hub.", 14, muted));
        Button defaults = button("Restaurar padrão");
        defaults.setOnClickListener(v -> {
            AndroidAutoConfigStore.restoreDefaults(this);
            Toast.makeText(this, "Configuração padrão restaurada.", Toast.LENGTH_SHORT).show();
            recreate();
        });
        note.addView(defaults);
        root.addView(note);
    }

    private void addFeature(LinearLayout parent, String mediaId, String title, String subtitle) {
        CheckBox box = check(title, AndroidAutoConfigStore.isFeatureEnabled(this, mediaId));
        box.setOnCheckedChangeListener((button, checked) ->
                AndroidAutoConfigStore.setFeatureEnabled(this, mediaId, checked));
        parent.addView(box);
        TextView sub = text(subtitle, 12, muted);
        sub.setPadding(dp(34), 0, 0, dp(8));
        parent.addView(sub);
    }

    private void renderApps() {
        appsContainer.removeAllViews();
        List<HubAppCatalog.LaunchableApp> apps = HubAppCatalog.listLaunchable(this);
        for (HubAppCatalog.LaunchableApp app : apps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            row.setBackground(cardBackground(surface));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, dp(4), 0, dp(4));

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(HubAppCatalog.icon(this, app));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

            TextView label = text(app.label, 15, text);
            label.setPadding(dp(12), 0, dp(8), 0);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));

            CheckBox selected = check("", AndroidAutoConfigStore.isAppSelected(this, app.packageName));
            selected.setOnCheckedChangeListener((button, checked) -> {
                AndroidAutoConfigStore.setAppSelected(this, app.packageName, checked);
                refreshStatus();
            });
            row.addView(selected);
            appsContainer.addView(row, rowParams);
        }
    }

    private void selectAll(boolean selected) {
        AndroidAutoConfigStore.clearSelectedApps(this);
        if (selected) {
            for (HubAppCatalog.LaunchableApp app : HubAppCatalog.listLaunchable(this)) {
                AndroidAutoConfigStore.setAppSelected(this, app.packageName, true);
            }
        }
        renderApps();
        refreshStatus();
    }

    private void refreshStatus() {
        if (selectionStatus == null) return;
        int selected = AndroidAutoConfigStore.selectedPackages(this).size();
        int visible = AndroidAutoConfigStore.visibleApps(this).size();
        if (AndroidAutoConfigStore.onlySelectedApps(this)) {
            selectionStatus.setText(selected + " selecionados • " + visible + " disponíveis no carro");
        } else {
            selectionStatus.setText("Todos os apps iniciáveis aparecem no carro • " + selected + " já marcados para uso no modo selecionado");
        }
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(cardBackground(surface));
        return panel;
    }

    private CheckBox check(String title, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(title);
        box.setTextColor(text);
        box.setTextSize(15);
        box.setChecked(checked);
        box.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
        return box;
    }

    private Button button(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(14);
        return b;
    }

    private TextView section(String value) {
        TextView v = text(value, 12, accent);
        v.setLetterSpacing(.12f);
        v.setPadding(0, dp(18), 0, dp(8));
        return v;
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0, dp(4), 0, dp(4));
        return v;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1), Color.rgb(38, 52, 59));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
