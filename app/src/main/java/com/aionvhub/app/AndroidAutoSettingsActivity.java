package com.aionvhub.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Editor visual da experiência que o AION V Hub expõe no Android Auto. */
public class AndroidAutoSettingsActivity extends Activity {
    private final int bg = Color.rgb(9, 14, 18);
    private final int surface = Color.rgb(22, 30, 36);
    private final int surface2 = Color.rgb(29, 40, 47);
    private final int selectedSurface = Color.rgb(22, 61, 64);
    private final int text = Color.rgb(244, 247, 248);
    private final int muted = Color.rgb(157, 175, 183);
    private final int accent = Color.rgb(75, 211, 205);
    private final int stroke = Color.rgb(43, 58, 66);

    private LinearLayout featureArea;
    private GridLayout appsGrid;
    private LinearLayout previewArea;
    private TextView selectionStatus;
    private EditText search;
    private boolean filterSelected;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(18));
        page.setBackgroundColor(bg);
        setContentView(page);

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setFillViewport(true);
        LinearLayout.LayoutParams editorParams = new LinearLayout.LayoutParams(0, -1, 1.18f);
        editorParams.setMargins(0, 0, dp(12), 0);
        page.addView(editorScroll, editorParams);

        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(dp(10), dp(6), dp(10), dp(30));
        editorScroll.addView(editor);

        TextView brand = label("AION V HUB", 12, accent);
        brand.setLetterSpacing(.14f);
        editor.addView(brand);
        editor.addView(label("Minha central", 30, text));
        editor.addView(label("Monte no tablet o que você quer ver no carro.", 15, muted));

        editor.addView(section("1 · ESCOLHA OS MENUS"));
        featureArea = new LinearLayout(this);
        featureArea.setOrientation(LinearLayout.VERTICAL);
        editor.addView(featureArea);
        renderFeatures();

        editor.addView(section("2 · ESCOLHA OS APPS"));
        LinearLayout mode = new LinearLayout(this);
        mode.setOrientation(LinearLayout.HORIZONTAL);
        mode.setGravity(Gravity.CENTER_VERTICAL);
        mode.setPadding(0, 0, 0, dp(8));
        Button selectedMode = compactButton("Selecionados no carro");
        selectedMode.setOnClickListener(v -> {
            AndroidAutoConfigStore.setOnlySelectedApps(this, true);
            refreshSelectionStatus();
            renderPreview();
        });
        mode.addView(selectedMode, new LinearLayout.LayoutParams(0, -2, 1f));
        Button allMode = compactButton("Todos no carro");
        allMode.setOnClickListener(v -> {
            AndroidAutoConfigStore.setOnlySelectedApps(this, false);
            refreshSelectionStatus();
            renderPreview();
        });
        LinearLayout.LayoutParams allParams = new LinearLayout.LayoutParams(0, -2, 1f);
        allParams.setMargins(dp(8), 0, 0, 0);
        mode.addView(allMode, allParams);
        editor.addView(mode);

        selectionStatus = label("", 13, muted);
        selectionStatus.setPadding(0, 0, 0, dp(8));
        editor.addView(selectionStatus);

        search = new EditText(this);
        search.setHint("Pesquisar aplicativos…");
        search.setHintTextColor(muted);
        search.setTextColor(text);
        search.setSingleLine(true);
        search.setPadding(dp(16), dp(12), dp(16), dp(12));
        search.setBackground(cardBackground(surface));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderApps(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        editor.addView(search, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setPadding(0, dp(8), 0, dp(8));
        Button showAll = compactButton("Todos");
        showAll.setOnClickListener(v -> { filterSelected = false; renderApps(); });
        filters.addView(showAll);
        Button showSelected = compactButton("Selecionados");
        showSelected.setOnClickListener(v -> { filterSelected = true; renderApps(); });
        LinearLayout.LayoutParams fs = new LinearLayout.LayoutParams(-2, -2);
        fs.setMargins(dp(8), 0, 0, 0);
        filters.addView(showSelected, fs);
        editor.addView(filters);

        appsGrid = new GridLayout(this);
        appsGrid.setColumnCount(3);
        editor.addView(appsGrid, new LinearLayout.LayoutParams(-1, -2));
        renderApps();
        refreshSelectionStatus();

        LinearLayout resetRow = new LinearLayout(this);
        resetRow.setOrientation(LinearLayout.HORIZONTAL);
        resetRow.setPadding(0, dp(16), 0, 0);
        Button clear = compactButton("Limpar apps");
        clear.setOnClickListener(v -> {
            AndroidAutoConfigStore.clearSelectedApps(this);
            AndroidAutoConfigStore.setOnlySelectedApps(this, true);
            renderApps();
            refreshSelectionStatus();
            renderPreview();
        });
        resetRow.addView(clear);
        Button defaults = compactButton("Restaurar padrão");
        defaults.setOnClickListener(v -> {
            AndroidAutoConfigStore.restoreDefaults(this);
            Toast.makeText(this, "Central restaurada.", Toast.LENGTH_SHORT).show();
            renderFeatures();
            renderApps();
            refreshSelectionStatus();
            renderPreview();
        });
        LinearLayout.LayoutParams rd = new LinearLayout.LayoutParams(-2, -2);
        rd.setMargins(dp(8), 0, 0, 0);
        resetRow.addView(defaults, rd);
        editor.addView(resetRow);

        LinearLayout previewColumn = new LinearLayout(this);
        previewColumn.setOrientation(LinearLayout.VERTICAL);
        previewColumn.setPadding(dp(14), dp(12), dp(14), dp(14));
        previewColumn.setBackground(cardBackground(Color.rgb(13, 19, 23)));
        page.addView(previewColumn, new LinearLayout.LayoutParams(0, -1, .82f));

        TextView previewBrand = label("PRÉVIA NO CARRO", 12, accent);
        previewBrand.setLetterSpacing(.12f);
        previewColumn.addView(previewBrand);
        previewColumn.addView(label("Veja antes de dirigir", 24, text));
        previewColumn.addView(label("A prévia representa a UX planejada; o Android Auto pode ajustar o layout final.", 12, muted));

        previewArea = new LinearLayout(this);
        previewArea.setOrientation(LinearLayout.VERTICAL);
        previewArea.setPadding(dp(14), dp(14), dp(14), dp(14));
        previewArea.setBackground(carFrame());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        previewParams.setMargins(0, dp(14), 0, dp(12));
        previewColumn.addView(previewArea, previewParams);

        Button simulate = primaryButton("Simular navegação");
        simulate.setOnClickListener(v -> startActivity(new Intent(this, CarSimulatorActivity.class)));
        previewColumn.addView(simulate, new LinearLayout.LayoutParams(-1, -2));
        renderPreview();
    }

    private void renderFeatures() {
        if (featureArea == null) return;
        featureArea.removeAllViews();
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        featureArea.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        addFeatureCard(grid, HubMediaCatalog.APPS_ID, "Apps");
        addFeatureCard(grid, HubMediaCatalog.FAVORITES_ID, "Favoritos");
        addFeatureCard(grid, HubMediaCatalog.IPTV_ID, "IPTV");
        addFeatureCard(grid, HubMediaCatalog.RADIOS_ID, "Rádios");
        addFeatureCard(grid, HubMediaCatalog.DIAGNOSTICS_ID, "Diagnóstico");
    }

    private void addFeatureCard(GridLayout grid, String mediaId, String title) {
        boolean enabled = AndroidAutoConfigStore.isFeatureEnabled(this, mediaId);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(12), dp(10), dp(10));
        card.setBackground(cardBackground(enabled ? selectedSurface : surface));
        card.setClickable(true);
        card.setOnClickListener(v -> {
            AndroidAutoConfigStore.setFeatureEnabled(this, mediaId, !AndroidAutoConfigStore.isFeatureEnabled(this, mediaId));
            renderFeatures();
            renderPreview();
        });

        ImageView icon = new ImageView(this);
        icon.setImageBitmap(HubArtwork.forEntry(this, mediaId, dp(42)));
        card.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView name = label(title, 13, text);
        name.setGravity(Gravity.CENTER);
        card.addView(name);
        TextView state = label(enabled ? "✓ NO CARRO" : "OCULTO", 10, enabled ? accent : muted);
        state.setGravity(Gravity.CENTER);
        card.addView(state);
        grid.addView(card, gridParams());
    }

    private void renderApps() {
        if (appsGrid == null) return;
        appsGrid.removeAllViews();
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase();
        List<HubAppCatalog.LaunchableApp> apps = HubAppCatalog.listLaunchable(this);
        for (HubAppCatalog.LaunchableApp app : apps) {
            boolean selected = AndroidAutoConfigStore.isAppSelected(this, app.packageName);
            if (filterSelected && !selected) continue;
            String hay = (app.label + " " + app.packageName).toLowerCase();
            if (!q.isEmpty() && !hay.contains(q)) continue;
            appsGrid.addView(appCard(app, selected), gridParams());
        }
    }

    private View appCard(HubAppCatalog.LaunchableApp app, boolean selected) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(10), dp(11), dp(10), dp(10));
        card.setBackground(cardBackground(selected ? selectedSurface : surface2));
        card.setClickable(true);
        card.setOnClickListener(v -> {
            boolean now = !AndroidAutoConfigStore.isAppSelected(this, app.packageName);
            AndroidAutoConfigStore.setAppSelected(this, app.packageName, now);
            if (now) AndroidAutoConfigStore.setOnlySelectedApps(this, true);
            renderApps();
            refreshSelectionStatus();
            renderPreview();
        });

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(HubAppCatalog.icon(this, app));
        card.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView name = label(app.label, 12, text);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        card.addView(name, new LinearLayout.LayoutParams(-1, -2));
        TextView mark = label(selected ? "✓" : "+", 14, selected ? accent : muted);
        mark.setGravity(Gravity.CENTER);
        card.addView(mark);
        return card;
    }

    private void refreshSelectionStatus() {
        if (selectionStatus == null) return;
        int selected = AndroidAutoConfigStore.selectedPackages(this).size();
        int visible = AndroidAutoConfigStore.visibleApps(this).size();
        selectionStatus.setText(AndroidAutoConfigStore.onlySelectedApps(this)
                ? selected + " apps escolhidos • " + visible + " serão mostrados no carro"
                : "Modo Todos • " + visible + " apps podem aparecer no carro");
    }

    private void renderPreview() {
        if (previewArea == null) return;
        previewArea.removeAllViews();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageBitmap(HubArtwork.brand(dp(38)));
        top.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(38)));
        TextView title = label("AION V Hub", 18, text);
        title.setPadding(dp(10), 0, 0, 0);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView aa = label("● AA", 11, accent);
        top.addView(aa);
        previewArea.addView(top);

        GridLayout menus = new GridLayout(this);
        menus.setColumnCount(2);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.setMargins(0, dp(14), 0, dp(10));
        previewArea.addView(menus, mp);
        addPreviewMenu(menus, HubMediaCatalog.APPS_ID, "Apps");
        addPreviewMenu(menus, HubMediaCatalog.FAVORITES_ID, "Favoritos");
        addPreviewMenu(menus, HubMediaCatalog.IPTV_ID, "IPTV");
        addPreviewMenu(menus, HubMediaCatalog.RADIOS_ID, "Rádios");
        addPreviewMenu(menus, HubMediaCatalog.DIAGNOSTICS_ID, "Diagnóstico");

        if (AndroidAutoConfigStore.isFeatureEnabled(this, HubMediaCatalog.APPS_ID)) {
            List<HubAppCatalog.LaunchableApp> visible = AndroidAutoConfigStore.visibleApps(this);
            TextView appsTitle = label(visible.isEmpty() ? "Nenhum app escolhido" : "Apps rápidos", 12, muted);
            appsTitle.setPadding(0, dp(4), 0, dp(5));
            previewArea.addView(appsTitle);
            LinearLayout quick = new LinearLayout(this);
            quick.setOrientation(LinearLayout.HORIZONTAL);
            int max = Math.min(4, visible.size());
            for (int i = 0; i < max; i++) {
                HubAppCatalog.LaunchableApp app = visible.get(i);
                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(HubAppCatalog.icon(this, app));
                item.addView(icon, new LinearLayout.LayoutParams(dp(34), dp(34)));
                TextView label = label(app.label, 9, text);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(1);
                item.addView(label);
                quick.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
            }
            previewArea.addView(quick);
        }
    }

    private void addPreviewMenu(GridLayout grid, String mediaId, String title) {
        if (!AndroidAutoConfigStore.isFeatureEnabled(this, mediaId)) return;
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(9), dp(9), dp(9), dp(9));
        tile.setBackground(cardBackground(surface));
        ImageView icon = new ImageView(this);
        icon.setImageBitmap(HubArtwork.forEntry(this, mediaId, dp(30)));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView name = label(title, 11, text);
        name.setPadding(dp(7), 0, 0, 0);
        tile.addView(name);
        grid.addView(tile, previewGridParams());
    }

    private GridLayout.LayoutParams gridParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -2;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        return p;
    }

    private GridLayout.LayoutParams previewGridParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -2;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(3), dp(3), dp(3), dp(3));
        return p;
    }

    private TextView section(String value) {
        TextView v = label(value, 11, accent);
        v.setLetterSpacing(.12f);
        v.setPadding(0, dp(18), 0, dp(8));
        return v;
    }

    private Button compactButton(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(12);
        return b;
    }

    private Button primaryButton(String title) {
        Button b = compactButton(title);
        b.setTextSize(15);
        b.setTextColor(Color.rgb(8, 20, 22));
        b.setBackground(cardBackground(accent));
        b.setPadding(dp(14), dp(13), dp(14), dp(13));
        return b;
    }

    private TextView label(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0, dp(3), 0, dp(3));
        return v;
    }

    private GradientDrawable cardBackground(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable carFrame() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(31, 37, 41), Color.rgb(20, 26, 30), Color.rgb(13, 18, 21)});
        d.setCornerRadius(dp(24));
        d.setStroke(dp(2), Color.rgb(64, 82, 90));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
