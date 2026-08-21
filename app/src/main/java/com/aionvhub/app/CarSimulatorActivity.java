package com.aionvhub.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Simulador navegável da central usando exatamente a mesma configuração do Android Auto real. */
public class CarSimulatorActivity extends Activity {
    private final int bg = Color.rgb(7, 11, 14);
    private final int surface = Color.rgb(18, 27, 32);
    private final int surface2 = Color.rgb(26, 37, 43);
    private final int text = Color.rgb(244, 247, 248);
    private final int muted = Color.rgb(151, 170, 179);
    private final int accent = Color.rgb(75, 211, 205);
    private final int border = Color.rgb(49, 67, 74);

    private LinearLayout content;
    private String current = HubMediaCatalog.ROOT_ID;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildShell();
        renderRoot();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) renderRoot();
    }

    @Override public void onBackPressed() {
        if (!HubMediaCatalog.ROOT_ID.equals(current)) {
            renderRoot();
            return;
        }
        super.onBackPressed();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(dp(18), dp(12), dp(18), dp(14));
        setContentView(root);

        LinearLayout vehicleBar = new LinearLayout(this);
        vehicleBar.setGravity(Gravity.CENTER_VERTICAL);
        TextView car = label("AION V", 12, accent);
        car.setLetterSpacing(.13f);
        vehicleBar.addView(car);
        TextView mode = label("   CENTRAL DEMO", 12, muted);
        vehicleBar.addView(mode, new LinearLayout.LayoutParams(0, -2, 1f));
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        vehicleBar.addView(label("USB  •  " + time, 12, text));
        root.addView(vehicleBar);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(10), 0, dp(10));
        ImageView logo = new ImageView(this);
        logo.setImageBitmap(HubArtwork.brand(dp(48)));
        header.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(label("AION V Hub", 22, text));
        copy.addView(label("Simulação baseada na configuração atual", 12, muted));
        header.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(header);

        LinearLayout frame = new LinearLayout(this);
        frame.setOrientation(LinearLayout.HORIZONTAL);
        frame.setBackground(carFrame());
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, 0, 1f);
        fp.setMargins(0, 0, 0, dp(9));
        root.addView(frame, fp);

        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setPadding(dp(7), dp(12), dp(7), dp(12));
        rail.setBackgroundColor(Color.rgb(12, 18, 22));
        TextView home = railButton("⌂");
        home.setOnClickListener(v -> renderRoot());
        rail.addView(home);
        frame.addView(rail, new LinearLayout.LayoutParams(dp(62), -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(18));
        scroll.addView(content);
        frame.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1f));

        LinearLayout map = new LinearLayout(this);
        map.setOrientation(LinearLayout.VERTICAL);
        map.setGravity(Gravity.CENTER);
        map.setPadding(dp(12), dp(12), dp(12), dp(12));
        map.setBackground(mapBackground());
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, -1, .38f);
        mp.setMargins(dp(4), dp(4), dp(4), dp(4));
        frame.addView(map, mp);
        TextView nav = label("NAVEGAÇÃO", 10, Color.rgb(86, 100, 107));
        nav.setLetterSpacing(.12f);
        map.addView(nav);
        TextView roads = label("╱    ╲\n  ╲ ╱\n   ●\n ╱   ╲\n╲    ╱", 27, Color.rgb(112, 126, 132));
        roads.setGravity(Gravity.CENTER);
        map.addView(roads, new LinearLayout.LayoutParams(-1, 0, 1f));
        map.addView(label("Mapa ativo", 11, Color.rgb(86, 100, 107)));

        TextView disclaimer = label("Demo local • usa a mesma configuração do Android Auto • o host real pode adaptar o layout", 10, muted);
        disclaimer.setGravity(Gravity.CENTER);
        root.addView(disclaimer);
    }

    private void renderRoot() {
        current = HubMediaCatalog.ROOT_ID;
        content.removeAllViews();
        content.addView(label("Minha central", 27, text));
        content.addView(label("Toque em uma função para navegar como no carro.", 13, muted));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        content.addView(grid, full(dp(14)));

        List<String> menus = HubCarConfig.enabledMenuIds(this);
        if (menus.isEmpty()) {
            content.addView(emptyState("Nenhum menu habilitado", "Abra Minha Central e escolha o que deseja exibir."));
            return;
        }
        for (String id : menus) grid.addView(menuTile(id), gridParams());
    }

    private View menuTile(String id) {
        HubMediaCatalog.Entry entry = HubMediaCatalog.find(id);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(14), dp(16), dp(14), dp(14));
        tile.setBackground(card(surface2));
        tile.setClickable(true);
        tile.setOnClickListener(v -> renderMenu(id));

        ImageView art = new ImageView(this);
        art.setImageBitmap(HubArtwork.forEntry(this, id, dp(62)));
        tile.addView(art, new LinearLayout.LayoutParams(dp(62), dp(62)));
        TextView title = label(entry == null ? id : entry.title, 17, text);
        title.setGravity(Gravity.CENTER);
        tile.addView(title);
        TextView subtitle = label(entry == null ? "" : entry.subtitle, 11, muted);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setMaxLines(2);
        tile.addView(subtitle);
        return tile;
    }

    private void renderMenu(String id) {
        current = id;
        content.removeAllViews();
        HubMediaCatalog.Entry entry = HubMediaCatalog.find(id);
        TextView back = label("‹ Voltar", 14, accent);
        back.setPadding(0, dp(4), 0, dp(10));
        back.setOnClickListener(v -> renderRoot());
        content.addView(back);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView art = new ImageView(this);
        art.setImageBitmap(HubArtwork.forEntry(this, id, dp(48)));
        titleRow.addView(art, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(label(entry == null ? id : entry.title, 24, text));
        copy.addView(label(entry == null ? "" : entry.subtitle, 11, muted));
        titleRow.addView(copy);
        content.addView(titleRow);

        if (HubMediaCatalog.APPS_ID.equals(id)) renderApps();
        else if (HubMediaCatalog.IPTV_ID.equals(id)) renderIptv();
        else if (HubMediaCatalog.FAVORITES_ID.equals(id)) renderFavorites();
        else if (HubMediaCatalog.RADIOS_ID.equals(id)) renderRadios();
        else if (HubMediaCatalog.DIAGNOSTICS_ID.equals(id)) renderDiagnostics();
    }

    private void renderApps() {
        List<HubAppCatalog.LaunchableApp> apps = HubCarConfig.selectedApps(this);
        if (apps.isEmpty()) {
            content.addView(emptyState("Nenhum app selecionado", "Escolha os apps em Minha Central."));
            return;
        }
        content.addView(label(apps.size() + " apps disponíveis no carro", 12, accent));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        content.addView(grid, full(dp(10)));
        for (HubAppCatalog.LaunchableApp app : apps) grid.addView(appTile(app), gridParams());
    }

    private View appTile(HubAppCatalog.LaunchableApp app) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(9), dp(11), dp(9), dp(9));
        tile.setBackground(card(surface2));
        tile.setClickable(true);
        tile.setOnClickListener(v -> Toast.makeText(this,
                "No carro: " + app.label + " tenta abrir no tablet", Toast.LENGTH_SHORT).show());
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(HubAppCatalog.icon(this, app));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));
        TextView name = label(app.label, 11, text);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        tile.addView(name);
        return tile;
    }

    private void renderIptv() {
        HubStreamStore.Config config = HubStreamStore.get(this);
        if (!config.configured()) {
            content.addView(emptyState("Nenhuma transmissão configurada", "Configure uma fonte IPTV/stream no tablet."));
            return;
        }
        LinearLayout c = infoCard();
        ImageView art = new ImageView(this);
        art.setImageBitmap(HubArtwork.forEntry(this, HubMediaCatalog.CUSTOM_STREAM_ID, dp(70)));
        c.addView(art, new LinearLayout.LayoutParams(dp(70), dp(70)));
        c.addView(label(config.title, 20, text));
        c.addView(label("Stream configurado • pronto para reprodução", 12, accent));
        content.addView(c, full(dp(15)));
    }

    private void renderFavorites() {
        HubStreamStore.Config config = HubStreamStore.get(this);
        if (config.configured() && config.favorite) {
            LinearLayout c = infoCard();
            c.addView(label("★ " + config.title, 19, text));
            c.addView(label("Fonte favoritada", 12, accent));
            content.addView(c, full(dp(15)));
        } else content.addView(emptyState("Nenhum favorito", "Marque uma fonte como favorita no tablet."));
    }

    private void renderRadios() {
        content.addView(emptyState("Rádios", "A estrutura está pronta para rádios e streams de áudio."));
    }

    private void renderDiagnostics() {
        LinearLayout c = infoCard();
        c.addView(label("● MediaBrowser", 18, accent));
        c.addView(label("Bridge validado no AION V", 13, text));
        c.addView(label("Shizuku: opcional", 12, muted));
        c.addView(label("Configuração da demo = configuração do carro", 12, muted));
        content.addView(c, full(dp(15)));
    }

    private LinearLayout infoCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        c.setBackground(card(surface));
        return c;
    }

    private View emptyState(String title, String subtitle) {
        LinearLayout c = infoCard();
        c.addView(label(title, 19, text));
        c.addView(label(subtitle, 13, muted));
        c.setLayoutParams(full(dp(16)));
        return c;
    }

    private TextView railButton(String value) {
        TextView v = label(value, 25, text);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(8), dp(7), dp(8), dp(7));
        v.setBackground(card(Color.rgb(24, 31, 35)));
        return v;
    }

    private GradientDrawable card(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(17));
        d.setStroke(dp(1), border);
        return d;
    }

    private GradientDrawable carFrame() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(31, 38, 42), Color.rgb(18, 24, 28), Color.rgb(10, 15, 18)});
        d.setCornerRadius(dp(22));
        d.setStroke(dp(2), Color.rgb(66, 82, 89));
        return d;
    }

    private GradientDrawable mapBackground() {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(233, 236, 231), Color.rgb(210, 218, 213)});
        d.setCornerRadius(dp(18));
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

    private GridLayout.LayoutParams gridParams() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = -2;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        return p;
    }

    private LinearLayout.LayoutParams full(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, top, 0, 0);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
