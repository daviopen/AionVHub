package com.aionvhub.app;

import android.app.Activity;
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
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Editor visual do que aparece no Android Auto, com prévia baseada na mesma configuração real. */
public class CarConfigActivity extends Activity {
    private final int bg = Color.rgb(8, 13, 17);
    private final int surface = Color.rgb(20, 29, 34);
    private final int surface2 = Color.rgb(27, 38, 44);
    private final int text = Color.rgb(244, 247, 248);
    private final int muted = Color.rgb(151, 170, 179);
    private final int accent = Color.rgb(75, 211, 205);
    private final int border = Color.rgb(43, 61, 68);

    private LinearLayout preview;
    private GridLayout appsGrid;
    private String filter = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshApps();
        refreshPreview();
    }

    private void buildUi() {
        ScrollView outer = new ScrollView(this);
        outer.setFillViewport(true);
        outer.setBackgroundColor(bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(22), dp(22), dp(34));
        outer.addView(root);
        setContentView(outer);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageBitmap(HubArtwork.brand(dp(54)));
        header.addView(logo, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.setPadding(dp(12), 0, 0, 0);
        headerText.addView(label("MINHA CENTRAL", 12, accent));
        headerText.addView(label("Monte seu Android Auto", 28, text));
        headerText.addView(label("O que você escolhe aqui é o que a demo e o carro recebem.", 14, muted));
        header.addView(headerText, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(header);

        boolean wide = getResources().getConfiguration().screenWidthDp >= 720;
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.setMargins(0, dp(18), 0, 0);
        root.addView(body, bodyParams);

        LinearLayout editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(dp(16), dp(16), dp(16), dp(16));
        editor.setBackground(card(surface));
        body.addView(editor, wide ? new LinearLayout.LayoutParams(0, -2, 1.18f) : new LinearLayout.LayoutParams(-1, -2));

        editor.addView(section("MENUS DO CARRO"));
        for (String menuId : HubCarMenuPolicy.all()) editor.addView(menuSwitch(menuId));

        editor.addView(section("APPS NO ANDROID AUTO"));
        EditText search = new EditText(this);
        search.setHint("Buscar aplicativo");
        search.setHintTextColor(muted);
        search.setTextColor(text);
        search.setSingleLine(true);
        search.setPadding(dp(14), dp(12), dp(14), dp(12));
        search.setBackground(card(surface2));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                refreshApps();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        editor.addView(search, full(dp(6)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(smallButton("Selecionar todos", v -> {
            HubCarConfig.selectAllApps(this);
            refreshApps();
            refreshPreview();
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        actions.addView(smallButton("Limpar", v -> {
            HubCarConfig.clearApps(this);
            refreshApps();
            refreshPreview();
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        editor.addView(actions, full(dp(5)));

        appsGrid = new GridLayout(this);
        appsGrid.setColumnCount(wide ? 4 : 3);
        editor.addView(appsGrid, full(dp(6)));
        refreshApps();

        LinearLayout previewWrap = new LinearLayout(this);
        previewWrap.setOrientation(LinearLayout.VERTICAL);
        previewWrap.setPadding(dp(16), dp(16), dp(16), dp(16));
        previewWrap.setBackground(card(Color.rgb(15, 22, 27)));
        LinearLayout.LayoutParams previewParams = wide
                ? new LinearLayout.LayoutParams(0, -2, .82f)
                : new LinearLayout.LayoutParams(-1, -2);
        previewParams.setMargins(wide ? dp(14) : 0, wide ? 0 : dp(14), 0, 0);
        body.addView(previewWrap, previewParams);

        previewWrap.addView(label("PRÉVIA AO VIVO", 12, accent));
        previewWrap.addView(label("Central do AION V", 20, text));
        previewWrap.addView(label("A prévia muda na hora conforme sua seleção.", 13, muted));

        preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(14), dp(14), dp(14), dp(14));
        preview.setBackground(carScreen());
        previewWrap.addView(preview, full(dp(12)));

        Button demo = smallButton("Abrir simulação navegável", v ->
                startActivity(new android.content.Intent(this, CarSimulatorActivity.class)));
        previewWrap.addView(demo, full(dp(10)));

        Button reset = smallButton("Restaurar padrão", v -> {
            HubCarConfig.reset(this);
            refreshApps();
            refreshPreview();
            recreate();
        });
        previewWrap.addView(reset, full(dp(5)));

        refreshPreview();
    }

    private View menuSwitch(String id) {
        HubMediaCatalog.Entry e = HubMediaCatalog.find(id);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(9), dp(10), dp(9));
        row.setBackground(card(surface2));
        LinearLayout.LayoutParams rp = full(dp(4));
        row.setLayoutParams(rp);

        ImageView icon = new ImageView(this);
        icon.setImageBitmap(HubArtwork.forEntry(this, id, dp(38)));
        row.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(label(e == null ? id : e.title, 16, text));
        copy.addView(label(e == null ? "" : e.subtitle, 12, muted));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(HubCarConfig.isMenuEnabled(this, id));
        toggle.setOnCheckedChangeListener((buttonView, checked) -> {
            HubCarConfig.setMenuEnabled(this, id, checked);
            refreshPreview();
        });
        row.addView(toggle);
        return row;
    }

    private void refreshApps() {
        if (appsGrid == null) return;
        appsGrid.removeAllViews();
        List<HubAppCatalog.LaunchableApp> apps = HubAppCatalog.listLaunchable(this);
        for (HubAppCatalog.LaunchableApp app : apps) {
            String haystack = (app.label + " " + app.packageName).toLowerCase(Locale.ROOT);
            if (!filter.isEmpty() && !haystack.contains(filter)) continue;
            appsGrid.addView(appChoice(app), appParams());
        }
    }

    private View appChoice(HubAppCatalog.LaunchableApp app) {
        boolean selected = HubCarConfig.isAppSelected(this, app.packageName);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(8), dp(10), dp(8), dp(9));
        box.setBackground(appCard(selected));
        box.setClickable(true);
        box.setOnClickListener(v -> {
            HubCarConfig.setAppSelected(this, app.packageName, !HubCarConfig.isAppSelected(this, app.packageName));
            refreshApps();
            refreshPreview();
        });

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(HubAppCatalog.icon(this, app));
        box.addView(icon, new LinearLayout.LayoutParams(dp(43), dp(43)));
        TextView name = label((selected ? "✓ " : "") + app.label, 12, selected ? accent : text);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        box.addView(name);
        return box;
    }

    private void refreshPreview() {
        if (preview == null) return;
        preview.removeAllViews();

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageBitmap(HubArtwork.brand(dp(38)));
        top.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(9), 0, 0, 0);
        copy.addView(label("AION V Hub", 17, text));
        copy.addView(label("Android Auto • prévia", 11, muted));
        top.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        preview.addView(top);

        GridLayout menus = new GridLayout(this);
        menus.setColumnCount(3);
        for (String id : HubCarConfig.enabledMenuIds(this)) {
            HubMediaCatalog.Entry e = HubMediaCatalog.find(id);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(5), dp(8), dp(5), dp(7));
            ImageView art = new ImageView(this);
            art.setImageBitmap(HubArtwork.forEntry(this, id, dp(34)));
            item.addView(art, new LinearLayout.LayoutParams(dp(34), dp(34)));
            TextView title = label(e == null ? id : e.title, 10, text);
            title.setGravity(Gravity.CENTER);
            title.setMaxLines(1);
            item.addView(title);
            menus.addView(item, previewItemParams());
        }
        preview.addView(menus, full(dp(12)));

        List<HubAppCatalog.LaunchableApp> selected = HubCarConfig.selectedApps(this);
        preview.addView(label(selected.size() + " apps selecionados", 12, accent));
        LinearLayout appStrip = new LinearLayout(this);
        appStrip.setOrientation(LinearLayout.HORIZONTAL);
        int max = Math.min(5, selected.size());
        for (int i = 0; i < max; i++) {
            ImageView ai = new ImageView(this);
            ai.setImageDrawable(HubAppCatalog.icon(this, selected.get(i)));
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(34), dp(34));
            ap.setMargins(0, dp(5), dp(7), 0);
            appStrip.addView(ai, ap);
        }
        if (selected.size() > max) appStrip.addView(label("+" + (selected.size() - max), 12, muted));
        preview.addView(appStrip);
    }

    private TextView section(String value) {
        TextView v = label(value, 11, accent);
        v.setLetterSpacing(.12f);
        v.setPadding(0, dp(12), 0, dp(5));
        return v;
    }

    private Button smallButton(String title, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextColor(text);
        b.setTextSize(13);
        b.setBackground(card(surface2));
        b.setOnClickListener(listener);
        return b;
    }

    private GradientDrawable card(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1), border);
        return d;
    }

    private GradientDrawable appCard(boolean selected) {
        GradientDrawable d = card(selected ? Color.rgb(22, 54, 57) : surface2);
        d.setStroke(dp(selected ? 2 : 1), selected ? accent : border);
        return d;
    }

    private GradientDrawable carScreen() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(16, 27, 32), Color.rgb(8, 13, 17)}
        );
        d.setCornerRadius(dp(22));
        d.setStroke(dp(2), Color.rgb(54, 73, 79));
        return d;
    }

    private TextView label(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0, dp(2), 0, dp(2));
        return v;
    }

    private LinearLayout.LayoutParams full(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, top, 0, 0);
        return p;
    }

    private GridLayout.LayoutParams appParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -2;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        return p;
    }

    private GridLayout.LayoutParams previewItemParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -2;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
